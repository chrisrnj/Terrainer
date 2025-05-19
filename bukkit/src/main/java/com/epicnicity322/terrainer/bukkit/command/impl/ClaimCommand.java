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

package com.epicnicity322.terrainer.bukkit.command.impl;

import com.epicnicity322.epicpluginlib.bukkit.command.CommandRunnable;
import com.epicnicity322.epicpluginlib.bukkit.lang.MessageSender;
import com.epicnicity322.terrainer.bukkit.TerrainerPlugin;
import com.epicnicity322.terrainer.bukkit.command.TerrainerCommand;
import com.epicnicity322.terrainer.bukkit.event.terrain.UserCreateTerrainEvent;
import com.epicnicity322.terrainer.bukkit.event.terrain.UserNameTerrainEvent;
import com.epicnicity322.terrainer.bukkit.util.BukkitPlayerUtil;
import com.epicnicity322.terrainer.bukkit.util.CommandUtil;
import com.epicnicity322.terrainer.core.config.Configurations;
import com.epicnicity322.terrainer.core.event.terrain.IUserNameTerrainEvent;
import com.epicnicity322.terrainer.core.location.Coordinate;
import com.epicnicity322.terrainer.core.location.WorldCoordinate;
import com.epicnicity322.terrainer.core.terrain.Terrain;
import com.epicnicity322.terrainer.core.terrain.TerrainManager;
import com.epicnicity322.terrainer.core.util.PlayerUtil;
import com.epicnicity322.terrainer.core.util.TerrainerUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public final class ClaimCommand extends TerrainerCommand {
    @Override
    public @NotNull String getName() {
        return "claim";
    }

    @Override
    public @NotNull String getPermission() {
        return "terrainer.claim";
    }

    @Override
    protected @NotNull CommandRunnable getNoPermissionRunnable() {
        return CommandUtil.noPermissionRunnable();
    }

    @Override
    public void reloadCommand() {
        setAliases(TerrainerPlugin.getLanguage().get("Commands.Claim.Command"));
    }

    @SuppressWarnings("deprecation")
    @Override
    public void run(@NotNull String label, @NotNull CommandSender sender, @NotNull String[] args) {
        MessageSender lang = TerrainerPlugin.getLanguage();
        Player player = sender instanceof Player p ? p : null;
        UUID owner = player == null ? null : player.getUniqueId();
        WorldCoordinate[] selection = PlayerUtil.selections(owner);

        if (selection[0] == null || selection[1] == null) {
            lang.send(sender, lang.get("Create.Error.Not Selected").replace("<label>", label));
            return;
        }
        if (!selection[0].world().equals(selection[1].world())) {
            lang.send(sender, lang.get("Create.Error.Different Worlds").replace("<label>", label));
            return;
        }

        Coordinate first = selection[0].coordinate();
        Coordinate second = selection[1].coordinate();
        World world = Bukkit.getWorld(selection[0].world());

        if (world == null) {
            lang.send(sender, lang.get("Create.Error.World No Longer Exists"));
            return;
        }
        if (!sender.hasPermission("terrainer.world." + world.getName().toLowerCase(Locale.ROOT))) {
            lang.send(sender, lang.get("Select.Error.World"));
            return;
        }

        String name;

        if (args.length > 1) {
            name = ChatColor.translateAlternateColorCodes('&', CommandUtil.join(args, 1)).trim();
            String stripped = ChatColor.stripColor(name);
            int maxNameLength = Configurations.CONFIG.getConfiguration().getNumber("Max Name Length").orElse(26).intValue();

            if (stripped.isBlank() || (stripped.length() > maxNameLength && !sender.hasPermission("terrainer.bypass.name-length"))) {
                lang.send(sender, lang.get("Rename.Error.Name Length").replace("<max>", Integer.toString(maxNameLength)));
                return;
            }
            if (!sender.hasPermission("terrainer.bypass.name-blacklist") && IUserNameTerrainEvent.isBlackListed(stripped)) {
                lang.send(sender, lang.get("Rename.Error.Blacklisted"));
                return;
            }
        } else name = null;

        // Clearing selections.
        selection[0] = null;
        selection[1] = null;

        BukkitPlayerUtil util = TerrainerPlugin.getPlayerUtil();
        Terrain terrain = new Terrain(first, second, world.getUID());

        terrain.setOwner(owner);
        terrain.setName(Terrain.defaultName(terrain.id(), owner));

        if (name != null) {
            String originalName = terrain.name();
            var event = new UserNameTerrainEvent(terrain, sender, originalName, name, IUserNameTerrainEvent.NameReason.CREATION);
            Bukkit.getPluginManager().callEvent(event);
            if (!event.isCancelled()) terrain.setName(event.newName());
        }


        PlayerUtil.ClaimResponse<?> claimResponse = util.performClaimChecks(terrain);

        switch (claimResponse.response().id()) {
            case "AREA_TOO_SMALL" ->
                    lang.send(sender, lang.get("Create.Error.Too Small").replace("<min>", Double.toString((double) claimResponse.variable())));
            case "BLOCK_LIMIT_REACHED" -> {
                assert player != null;
                long needed = (long) claimResponse.variable();
                long limit = util.blockLimit(player);
                long used = util.claimedBlocks(owner, terrain.world());

                lang.send(sender, lang.get("Create.Error.Low Block Limit").replace("<area>", Long.toString(needed - used)).replace("<free>", Long.toString(Math.max(limit - used, 0))));
            }
            case "CLAIM_LIMIT_REACHED" ->
                    lang.send(sender, lang.get("Create.Error.Low Claim Limit").replace("<max>", Integer.toString((int) claimResponse.variable())));
            case "DIMENSIONS_TOO_SMALL" ->
                    lang.send(sender, lang.get("Create.Error.Dimensions").replace("<min>", Double.toString((double) claimResponse.variable())));
            case "OVERLAPPING_OTHERS" -> {
                //noinspection unchecked
                Set<Terrain> overlapping = (Set<Terrain>) claimResponse.variable();
                if (player != null && overlapping.stream().anyMatch(t -> !util.hasInfoPermission(player, t))) {
                    // No permission to see terrain names.
                    lang.send(sender, lang.get("Create.Error.Overlap Several"));
                } else {
                    lang.send(sender, lang.get("Create.Error.Overlap").replace("<overlapping>", TerrainerUtil.listToString(overlapping, Terrain::name)));
                }
            }
            case "SUCCESS" -> {
                if (TerrainManager.add(terrain)) {
                    var create = new UserCreateTerrainEvent(terrain, sender, false);
                    Bukkit.getPluginManager().callEvent(create);
                    if (player != null) util.updateSelectionMarkersToTerrainMarkers(player);

                    lang.send(sender, lang.get("Create.Success").replace("<terrain>", terrain.name())
                            .replace("<used>", Long.toString((long) claimResponse.variable()))
                            .replace("<max>", player == null ? lang.get("Placeholder Values.Infinite Limit") : Long.toString(util.blockLimit(player))));

                    // Removing resizing in case there was any.
                    Terrain resizing = PlayerUtil.currentlyResizing(owner);
                    if (resizing != null) {
                        lang.send(sender, lang.get("Resize.Cancelled").replace("<terrain>", resizing.name()));
                        ConfirmCommand.cancelConfirmation(owner, Objects.hash("resize", resizing.id()));
                        PlayerUtil.setCurrentlyResizing(owner, null);
                    }
                }
                return; // Don't remove markers.
            }
            default -> lang.send(sender, lang.get("Create.Error.Unknown"));
        }

        if (player != null) util.removeMarkers(player);
    }
}
