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
import com.epicnicity322.terrainer.core.WorldCoordinate;
import com.epicnicity322.terrainer.core.terrain.Terrain;
import com.epicnicity322.terrainer.core.terrain.WorldTerrain;
import com.epicnicity322.terrainer.core.util.PlayerUtil;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.lang.ref.WeakReference;
import java.util.Objects;
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
    public void run(@NotNull String label, @NotNull CommandSender sender, @NotNull String[] args) {
        MessageSender lang = TerrainerPlugin.getLanguage();
        CommandUtil.findTerrain("terrainer.resize.others", "terrainer.resize.world", false, label, sender, args, lang.getColored("Resize.Select"), commandArguments -> {
            Terrain terrain = commandArguments.terrain();

            if (terrain instanceof WorldTerrain) {
                lang.send(sender, lang.get("Resize.Error.World Terrain"));
                return;
            }

            Player player = sender instanceof Player p ? p : null;
            UUID playerID = player != null ? player.getUniqueId() : null;
            int confirmationHash = Objects.hash("resize", terrain.id());

            // Cancel other players resizing of this terrain.
            ConfirmCommand.cancelConfirmations(confirmationHash, foundPlayer -> PlayerUtil.setCurrentlyResizing(foundPlayer, null));

            // Player allowed to resize one terrain at a time, cancelling the previous resize if there is one.
            Terrain previousResizing = PlayerUtil.currentlyResizing(playerID);
            if (previousResizing != null) ConfirmCommand.cancelConfirmation(playerID, confirmationHash);

            PlayerUtil.setCurrentlyResizing(playerID, terrain);

            WorldCoordinate[] selections = PlayerUtil.selections(playerID);
            selections[0] = new WorldCoordinate(terrain.world(), terrain.minDiagonal());
            selections[1] = new WorldCoordinate(terrain.world(), terrain.maxDiagonal());

            lang.send(sender, lang.get("Resize.Select").replace("<label>", label));

            if (player != null)
                TerrainerPlugin.getPlayerUtil().showMarkers(player, (int) player.getY() - 1, false, null);

            WeakReference<Terrain> terrainRef = new WeakReference<>(terrain);
            String name = terrain.name();

            ConfirmCommand.requestConfirmation(playerID, () -> {
                ConfirmCommand.cancelConfirmation(playerID, confirmationHash);
                PlayerUtil.setCurrentlyResizing(playerID, null);
                Terrain terrain1 = terrainRef.get();
                if (terrain1 == null) return;
                WorldCoordinate[] selections1 = PlayerUtil.selections(playerID);

                if (selections1[0] == null || selections1[1] == null) {
                    lang.send(sender, lang.get("Resize.Error.Select"));
                    return;
                }

                BukkitPlayerUtil util = TerrainerPlugin.getPlayerUtil();

                if (util.resizeTerrain(player, terrain1, selections1[0], selections1[1])) {
                    lang.send(sender, lang.get("Resize.Success").replace("<terrain>", terrain1.name())
                            .replace("<used>", Long.toString(util.claimedBlocks(terrain1.owner(), terrain1.world())))
                            .replace("<max>", player == null ? lang.get("Placeholder Values.Infinite Limit") : Long.toString(util.blockLimit(player))));
                    if (player != null) util.updateSelectionMarkersToTerrainMarkers(player);
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
