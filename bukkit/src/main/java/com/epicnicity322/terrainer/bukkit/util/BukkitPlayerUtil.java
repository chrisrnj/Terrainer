/*
 * Terrainer - A minecraft terrain claiming protection plugin.
 * Copyright (C) 2024 Christiano Rangel
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

import com.epicnicity322.terrainer.bukkit.TerrainerPlugin;
import com.epicnicity322.terrainer.core.Coordinate;
import com.epicnicity322.terrainer.core.WorldCoordinate;
import com.epicnicity322.terrainer.core.config.Configurations;
import com.epicnicity322.terrainer.core.util.PlayerUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.UUID;

public final class BukkitPlayerUtil extends PlayerUtil<Player, CommandSender> {
    private static final @NotNull HashMap<UUID, TaskFactory.CancellableTask> markerKillTasks = new HashMap<>();
    private final @NotNull TerrainerPlugin plugin;
    private final @NotNull NamespacedKey blockLimitKey;
    private final @NotNull NamespacedKey claimLimitKey;
    private final @NotNull NamespacedKey resetFlyKey;

    public BukkitPlayerUtil(@NotNull TerrainerPlugin plugin) {
        super(TerrainerPlugin.getLanguage());
        this.plugin = plugin;
        blockLimitKey = new NamespacedKey(plugin, "block-limit");
        claimLimitKey = new NamespacedKey(plugin, "claim-limit");
        resetFlyKey = new NamespacedKey(plugin, "reset-fly-on-leave");
    }

    @Override
    public boolean canFly(@NotNull Player player) {
        return player.getAllowFlight();
    }

    @Override
    public void setResetFly(@NotNull Player player, boolean checkPermission) {
        player.getPersistentDataContainer().set(resetFlyKey, PersistentDataType.INTEGER, checkPermission ? 1 : 0);
    }

    @Override
    public boolean isSneaking(@NotNull Player player) {
        return player.isSneaking();
    }

    @Override
    public boolean hasPermission(@NotNull Player player, @NotNull String permission) {
        return player.hasPermission(permission);
    }

    @Override
    public @NotNull String getOwnerName(@Nullable UUID uuid) {
        if (uuid == null) {
            return TerrainerPlugin.getLanguage().get("Target.Console");
        } else {
            String name = Bukkit.getOfflinePlayer(uuid).getName();
            return name == null ? uuid.toString() : name;
        }
    }

    @Override
    protected @NotNull UUID getUniqueId(@NotNull Player player) {
        return player.getUniqueId();
    }

    @Override
    protected @NotNull CommandSender getConsoleRecipient() {
        return Bukkit.getConsoleSender();
    }

    @Override
    public long getAdditionalMaxBlockLimit(@NotNull Player player) {
        return player.getPersistentDataContainer().getOrDefault(blockLimitKey, PersistentDataType.LONG, 0L);
    }

    @Override
    public void setAdditionalMaxBlockLimit(@NotNull Player player, long blockLimit) {
        if (blockLimit <= 0) {
            player.getPersistentDataContainer().remove(blockLimitKey);
        } else {
            player.getPersistentDataContainer().set(blockLimitKey, PersistentDataType.LONG, blockLimit);
        }
    }

    @Override
    public int getAdditionalMaxClaimLimit(@NotNull Player player) {
        return player.getPersistentDataContainer().getOrDefault(claimLimitKey, PersistentDataType.INTEGER, 0);
    }

    @Override
    public void setAdditionalMaxClaimLimit(@NotNull Player player, int claimLimit) {
        if (claimLimit <= 0) {
            player.getPersistentDataContainer().remove(claimLimitKey);
        } else {
            player.getPersistentDataContainer().set(claimLimitKey, PersistentDataType.INTEGER, claimLimit);
        }
    }

    @Override
    protected @NotNull WorldCoordinate location(@NotNull Player player) {
        Location loc = player.getLocation();
        return new WorldCoordinate(player.getWorld().getUID(), new Coordinate(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ()));
    }

    @Override
    public void showMarkers(@NotNull Player player, int y) {
        super.showMarkers(player, y);
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
    protected void killMarker(@NotNull Player player, int id) throws Throwable {
        TerrainerPlugin.getNMSHandler().killEntity(player, id);
    }

    @Override
    protected int spawnMarker(@NotNull Player player, double x, double y, double z) throws Throwable {
        return TerrainerPlugin.getNMSHandler().spawnMarkerEntity(player, (int) x, (int) y, (int) z);
    }
}
