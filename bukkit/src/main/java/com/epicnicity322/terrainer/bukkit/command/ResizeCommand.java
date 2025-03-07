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

package com.epicnicity322.terrainer.bukkit.command;

import com.epicnicity322.epicpluginlib.bukkit.command.Command;
import com.epicnicity322.epicpluginlib.bukkit.command.CommandRunnable;
import com.epicnicity322.epicpluginlib.bukkit.lang.MessageSender;
import com.epicnicity322.terrainer.bukkit.TerrainerPlugin;
import com.epicnicity322.terrainer.bukkit.util.BukkitPlayerUtil;
import com.epicnicity322.terrainer.bukkit.util.CommandUtil;
import com.epicnicity322.terrainer.core.location.WorldCoordinate;
import com.epicnicity322.terrainer.core.terrain.Terrain;
import com.epicnicity322.terrainer.core.terrain.WorldTerrain;
import com.epicnicity322.terrainer.core.util.PlayerUtil;
import com.epicnicity322.terrainer.core.util.TerrainerUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.lang.ref.WeakReference;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public final class ResizeCommand extends Command {
    @Override
    public @NotNull String getName() {
        return "resize";
    }

    @Override
    public @NotNull String getPermission() {
        return "terrainer.resize";
    }

    @Override
    protected @NotNull CommandRunnable getNoPermissionRunnable() {
        return CommandUtil.noPermissionRunnable();
    }

    @Override
    public void run(@NotNull String label, @NotNull CommandSender sender0, @NotNull String[] args) {
        MessageSender lang = TerrainerPlugin.getLanguage();
        CommandUtil.findTerrain("terrainer.resize.others", "terrainer.resize.world", false, label, sender0, args, lang.getColored("Resize.Select"), commandArguments -> {
            Terrain terrain = commandArguments.terrain();
            CommandSender sender = commandArguments.sender();

            if (terrain instanceof WorldTerrain) {
                lang.send(sender, lang.get("Resize.Error.World Terrain"));
                return;
            }

            UUID playerID = sender instanceof Player p ? p.getUniqueId() : null;
            int confirmationHash = Objects.hash("resize", terrain.id());

            // Cancel other players resizing of this terrain.
            ConfirmCommand.cancelConfirmations(confirmationHash, foundPlayer -> PlayerUtil.setCurrentlyResizing(foundPlayer, null));

            // Player allowed to resize one terrain at a time, cancelling the previous resize if there is one.
            Terrain previousResizing = PlayerUtil.currentlyResizing(playerID);
            if (previousResizing != null)
                ConfirmCommand.cancelConfirmation(playerID, Objects.hash("resize", previousResizing.id()));

            PlayerUtil.setCurrentlyResizing(playerID, terrain);

            // Setting selections to terrain diagonals
            WorldCoordinate[] selections = PlayerUtil.selections(playerID);
            selections[0] = new WorldCoordinate(terrain.world(), terrain.minDiagonal());
            selections[1] = new WorldCoordinate(terrain.world(), terrain.maxDiagonal());

            lang.send(sender, lang.get("Resize.Tutorial").replace("<label>", label));

            if (sender instanceof Player p)
                TerrainerPlugin.getPlayerUtil().showMarkers(p, (int) p.getLocation().getBlockY() - 1, false, null);

            // Requesting confirmation to resize.
            WeakReference<Terrain> terrainRef = new WeakReference<>(terrain);
            String name = terrain.name();

            ConfirmCommand.requestConfirmation(playerID, sender1 -> {
                ConfirmCommand.cancelConfirmation(playerID, confirmationHash);
                PlayerUtil.setCurrentlyResizing(playerID, null);
                Terrain terrain1 = terrainRef.get();
                if (terrain1 == null) return;
                WorldCoordinate[] selections1 = PlayerUtil.selections(playerID);

                if (selections1[0] == null || selections1[1] == null) {
                    lang.send(sender1, lang.get("Resize.Error.Select"));
                    return;
                }

                BukkitPlayerUtil util = TerrainerPlugin.getPlayerUtil();
                UUID owner = terrain1.owner();
                Terrain tempTerrain = new Terrain(terrain1); // Creating safe terrain clone.

                tempTerrain.setDiagonals(selections1[0].coordinate(), selections1[1].coordinate());

                PlayerUtil.ClaimResponse<?> claimResponse = util.performClaimChecks(tempTerrain);

                switch (claimResponse.response().id()) {
                    case "AREA_TOO_SMALL" ->
                            lang.send(sender1, lang.get("Create.Error.Too Small").replace("<min>", Double.toString((double) claimResponse.variable())));
                    case "BLOCK_LIMIT_REACHED" -> {
                        assert owner != null; // Should never be null, console has infinite block limit.
                        Player player = Bukkit.getPlayer(owner);
                        assert player != null; // Should never be null, otherwise OWNER_OFFLINE would've been the response.
                        long needed = (long) claimResponse.variable();
                        long limit = util.blockLimit(player);
                        long used = util.claimedBlocks(terrain1.owner(), terrain1.world());

                        lang.send(sender1, lang.get((player.equals(sender1) ? "Create" : "Resize") + ".Error.Low Block Limit").replace("<area>", Long.toString(needed - used)).replace("<player>", player.getName()).replace("<free>", Long.toString(Math.max(limit - used, 0))));
                    }
                    case "DIMENSIONS_TOO_SMALL" ->
                            lang.send(sender1, lang.get("Create.Error.Dimensions").replace("<min>", Double.toString((double) claimResponse.variable())));
                    case "OVERLAPPING_OTHERS" -> {
                        //noinspection unchecked
                        Set<Terrain> overlapping = (Set<Terrain>) claimResponse.variable();
                        if (sender1 instanceof Player p && overlapping.stream().anyMatch(t -> !util.hasInfoPermission(p, t))) {
                            // No permission to see terrain names.
                            lang.send(sender1, lang.get("Create.Error.Overlap Several"));
                        } else {
                            lang.send(sender1, lang.get("Create.Error.Overlap").replace("<overlapping>", TerrainerUtil.listToString(overlapping, Terrain::name)));
                        }
                    }
                    case "OWNER_OFFLINE" ->
                            lang.send(sender1, lang.get("Resize.Error.Owner Offline").replace("<player>", util.ownerName(owner)));
                    case "SUCCESS" -> {
                        Player player = owner == null ? null : Bukkit.getPlayer(owner);

                        lang.send(sender1, lang.get("Resize.Success").replace("<terrain>", terrain1.name())
                                .replace("<used>", Long.toString((long) claimResponse.variable()))
                                .replace("<max>", player == null ? lang.get("Placeholder Values.Infinite Limit") : Long.toString(util.blockLimit(player))));
                        terrain1.setDiagonals(selections1[0].coordinate(), selections1[1].coordinate());
                        if (player != null) util.updateSelectionMarkersToTerrainMarkers(player);
                    }
                    default -> lang.send(sender1, lang.get("Resize.Error.Unknown"));
                }

                // Clearing selections
                selections1[0] = null;
                selections1[1] = null;
            }, () -> {
                Terrain terrain1 = terrainRef.get();
                return lang.getColored("Resize.Confirmation Description").replace("<terrain>", terrain1 == null ? name : terrain1.name());
            }, confirmationHash);
        });
    }
}
