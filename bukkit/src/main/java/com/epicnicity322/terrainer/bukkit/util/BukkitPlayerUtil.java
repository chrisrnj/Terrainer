/*
 * Terrainer - A minecraft terrain claiming protection plugin.
 * Copyright (C) 2025 Christiano Rangel
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.epicnicity322.terrainer.bukkit.util;

import com.epicnicity322.epicpluginlib.core.EpicPluginLib;
import com.epicnicity322.terrainer.bukkit.TerrainerPlugin;
import com.epicnicity322.terrainer.bukkit.hook.NMSHandler;
import com.epicnicity322.terrainer.core.Terrainer;
import com.epicnicity322.terrainer.core.config.Configurations;
import com.epicnicity322.terrainer.core.location.Coordinate;
import com.epicnicity322.terrainer.core.location.WorldCoordinate;
import com.epicnicity322.terrainer.core.util.PlayerUtil;
import com.epicnicity322.yamlhandler.Configuration;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

public final class BukkitPlayerUtil extends PlayerUtil<Player, CommandSender> {
    private static final @NotNull HashMap<UUID, TaskFactory.CancellableTask> markerKillTasks = new HashMap<>();
    private final @NotNull TerrainerPlugin plugin;
    private final @NotNull NamespacedKey blockLimitKey;
    private final @NotNull NamespacedKey claimLimitKey;
    private final @NotNull NamespacedKey resetFlyKey;
    private final @NotNull NMSHandler nmsHandler;

    public BukkitPlayerUtil(@NotNull TerrainerPlugin plugin, @NotNull NMSHandler nmsHandler, @NotNull TreeSet<Map.Entry<String, Long>> defaultBlockLimits, @NotNull TreeSet<Map.Entry<String, Integer>> defaultClaimLimits, @NotNull AtomicBoolean nestedTerrainsCountTowardsBlockLimit, @NotNull AtomicBoolean perWorldBlockLimit, @NotNull AtomicBoolean perWorldClaimLimit, @NotNull AtomicBoolean sumIfTheresMultipleBlockLimitPermissions, @NotNull AtomicBoolean sumIfTheresMultipleClaimLimitPermissions) {
        super(defaultBlockLimits, defaultClaimLimits, nestedTerrainsCountTowardsBlockLimit, perWorldBlockLimit, perWorldClaimLimit, sumIfTheresMultipleBlockLimitPermissions, sumIfTheresMultipleClaimLimitPermissions);

        this.plugin = plugin;
        this.nmsHandler = nmsHandler;
        blockLimitKey = new NamespacedKey(plugin, "block-limit");
        claimLimitKey = new NamespacedKey(plugin, "claim-limit");
        resetFlyKey = new NamespacedKey(plugin, "reset-fly-on-leave");
    }

    @Override
    public boolean canFly(@NotNull Player player) {
        return player.getAllowFlight();
    }

    @Override
    public void setCanFly(@NotNull Player player, boolean canFly) {
        player.setAllowFlight(canFly);
    }

    @Override
    public void setResetFly(@NotNull Player player, boolean value) {
        PersistentDataContainer container = player.getPersistentDataContainer();

        if (value) {
            Configuration config = Configurations.CONFIG.getConfiguration();
            int hasFlightPermission = player.hasPermission(config.getString("Fly Permission").orElse("essentials.fly")) ? 1 : 0;

            container.set(resetFlyKey, PersistentDataType.INTEGER, hasFlightPermission);
        } else {
            container.remove(resetFlyKey);
        }
    }

    @Override
    public boolean shouldResetFly(@NotNull Player player) {
        Integer resetFly = player.getPersistentDataContainer().get(resetFlyKey, PersistentDataType.INTEGER);
        if (resetFly == null) return false;
        boolean hadFlightPermissionBefore = resetFly == 1;
        Configuration config = Configurations.CONFIG.getConfiguration();

        // If they had the permission, only reset flight if they still have it. Otherwise, always reset flight unless Strict Fly Return is true.
        return hadFlightPermissionBefore ? player.hasPermission(config.getString("Fly Permission").orElse("essentials.fly")) : !config.getBoolean("Strict Fly Return").orElse(false);
    }

    @Override
    public boolean isSneaking(@NotNull Player player) {
        return player.isSneaking();
    }

    @Override
    public boolean isFlying(@NotNull Player player) {
        return player.isFlying();
    }

    @Override
    public boolean isGliding(@NotNull Player player) {
        return player.isGliding();
    }

    @Override
    public void setGliding(@NotNull Player player, boolean glide) {
        player.setGliding(glide);
    }

    @Override
    public void applyEffect(@NotNull Player player, @NotNull String effect, int power) {
        //noinspection deprecation - backwards compatibility
        PotionEffectType type = PotionEffectType.getByName(effect);
        if (type == null) return;

        // Ensure the potion effect is applied on the right thread.
        if (EpicPluginLib.Platform.isFolia() || !plugin.getServer().isPrimaryThread()) {
            plugin.getTaskFactory().runDelayed(player, 1, () -> player.addPotionEffect(new PotionEffect(type, Integer.MAX_VALUE, power, false, false)), null);
        } else {
            player.addPotionEffect(new PotionEffect(type, Integer.MAX_VALUE, power, false, false));
        }
    }

    @Override
    public void removeEffect(@NotNull Player player, @NotNull String effect) {
        //noinspection deprecation - backwards compatibility
        PotionEffectType type = PotionEffectType.getByName(effect);
        if (type == null) return;

        // Although for "removePotionEffect" it's not necessary to execute on the right thread, we'll ensure that it's
        //called delayed by one tick just like #applyEffect, so calls for both methods are in sync, and there's no risk
        //of the potion effect being removed before it is applied.
        if (EpicPluginLib.Platform.isFolia() || !plugin.getServer().isPrimaryThread()) {
            plugin.getTaskFactory().runDelayed(player, 1, () -> player.removePotionEffect(type), null);
        } else {
            player.removePotionEffect(type);
        }
    }

    @Override
    public void dispatchCommand(@Nullable Player executor, @NotNull String command) {
        try {
            // Ensure the command is dispatched on the right thread.
            if (EpicPluginLib.Platform.isFolia() || !plugin.getServer().isPrimaryThread()) {
                if (executor == null)
                    plugin.getTaskFactory().runGlobalTask(() -> plugin.getServer().dispatchCommand(consoleRecipient(), command));
                else
                    plugin.getTaskFactory().runDelayed(executor, 1, () -> plugin.getServer().dispatchCommand(executor, command), null);
            } else {
                plugin.getServer().dispatchCommand(executor == null ? consoleRecipient() : executor, command);
            }
        } catch (Throwable t) {
            Terrainer.logger().log("Unable to execute command '" + command + "' as " + (executor == null ? "console" : executor.getName()) + ":");
            t.printStackTrace();
        }
    }

    @Override
    public boolean hasPermission(@NotNull Player player, @NotNull String permission) {
        return player.hasPermission(permission);
    }

    @Override
    public @NotNull String playerName(@NotNull Player player) {
        return player.getName();
    }

    @Override
    public @NotNull String ownerName(@Nullable UUID uuid) {
        if (uuid == null) {
            return TerrainerPlugin.getLanguage().get("Target.Console");
        } else {
            String name = plugin.getServer().getOfflinePlayer(uuid).getName();
            return name == null ? uuid.toString() : name;
        }
    }

    @Override
    protected @Nullable Player fetchOnline(@NotNull UUID uuid) {
        return plugin.getServer().getPlayer(uuid);
    }

    @Override
    public @NotNull UUID playerUUID(@NotNull Player player) {
        return player.getUniqueId();
    }

    @Override
    protected @NotNull CommandSender consoleRecipient() {
        return plugin.getServer().getConsoleSender();
    }

    @Override
    public long boughtBlockLimit(@NotNull Player player) {
        return player.getPersistentDataContainer().getOrDefault(blockLimitKey, PersistentDataType.LONG, 0L);
    }

    @Override
    public void setBoughtBlockLimit(@NotNull Player player, long blockLimit) {
        if (blockLimit <= 0) {
            player.getPersistentDataContainer().remove(blockLimitKey);
        } else {
            player.getPersistentDataContainer().set(blockLimitKey, PersistentDataType.LONG, blockLimit);
        }
    }

    @Override
    public int boughtClaimLimit(@NotNull Player player) {
        return player.getPersistentDataContainer().getOrDefault(claimLimitKey, PersistentDataType.INTEGER, 0);
    }

    @Override
    public void setBoughtClaimLimit(@NotNull Player player, int claimLimit) {
        if (claimLimit <= 0) {
            player.getPersistentDataContainer().remove(claimLimitKey);
        } else {
            player.getPersistentDataContainer().set(claimLimitKey, PersistentDataType.INTEGER, claimLimit);
        }
    }

    @Override
    public @NotNull WorldCoordinate playerLocation(@NotNull Player player) {
        Location loc = player.getLocation();
        return new WorldCoordinate(player.getWorld().getUID(), new Coordinate(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ()));
    }

    @Override
    public void showMarkers(@NotNull Player player, int y, boolean terrainMarkers, @Nullable Coordinate location) {
        super.showMarkers(player, y, terrainMarkers, location);
        long showTime = Configurations.CONFIG.getConfiguration().getNumber("Markers.Show Time").orElse(1200).longValue();

        if (showTime != 0) {
            UUID playerID = player.getUniqueId();
            TaskFactory.CancellableTask killMarkersTask = plugin.getTaskFactory().runDelayed(player, showTime, () -> removeMarkers(player), () -> markerKillTasks.remove(playerID));
            if (killMarkersTask == null) return;
            TaskFactory.CancellableTask previous = markerKillTasks.put(playerID, killMarkersTask);
            if (previous != null) previous.cancel();
        }
    }

    @Override
    public void removeMarkers(@NotNull Player player) {
        super.removeMarkers(player);
        TaskFactory.CancellableTask task = markerKillTasks.remove(player.getUniqueId());
        if (task != null) task.cancel();
    }

    @Override
    protected void killMarker(@NotNull Player player, @NotNull SpawnedMarker marker) throws Throwable {
        nmsHandler.killEntity(player, marker);
    }

    @Override
    protected @NotNull SpawnedMarker spawnMarker(@NotNull Player player, double x, double y, double z, boolean edges, boolean selection) throws Throwable {
        return nmsHandler.spawnMarkerEntity(player, (int) x, (int) y, (int) z, edges, selection);
    }

    @Override
    protected void updateSelectionMarkerToTerrainMarker(@NotNull SpawnedMarker marker, @NotNull Player player) throws Throwable {
        nmsHandler.updateSelectionMarkerToTerrainMarker(marker, player);
    }
}
