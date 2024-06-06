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
import com.epicnicity322.terrainer.bukkit.util.CommandUtil;
import com.epicnicity322.terrainer.core.terrain.Terrain;
import com.epicnicity322.terrainer.core.terrain.TerrainManager;
import com.epicnicity322.terrainer.core.terrain.WorldTerrain;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.lang.ref.WeakReference;
import java.util.Objects;

public final class DeleteCommand extends Command {
    @Override
    public @NotNull String getName() {
        return "delete";
    }

    @Override
    public @NotNull String getPermission() {
        return "terrainer.delete";
    }

    @Override
    protected @NotNull CommandRunnable getNoPermissionRunnable() {
        return CommandUtil.noPermissionRunnable();
    }

    @Override
    public void run(@NotNull String label, @NotNull CommandSender sender, @NotNull String[] args) {
        MessageSender lang = TerrainerPlugin.getLanguage();
        CommandUtil.findTerrain("terrainer.delete.others", "terrainer.delete.world", false, label, sender, args, lang.getColored("Delete.Select"), commandArguments -> {
            Terrain terrain = commandArguments.terrain();
            // If world terrain is not found in list of loaded worlds, remove like a regular terrain.
            boolean worldTerrain = terrain instanceof WorldTerrain && Bukkit.getWorlds().stream().anyMatch(world -> world.getUID().equals(terrain.world()));

            lang.send(sender, lang.get("Delete." + (worldTerrain ? "World " : "") + "Confirmation").replace("<label>", label).replace("<label2>", lang.get("Commands.Confirm.Confirm")).replace("<name>", terrain.name()));

            String name = terrain.name();
            WeakReference<Terrain> terrainRef = new WeakReference<>(terrain);
            int confirmationHash = Objects.hash("delete", terrain.id());

            ConfirmCommand.requestConfirmation(sender, () -> {
                ConfirmCommand.cancelConfirmations(confirmationHash);
                Terrain terrain1 = terrainRef.get();
                if (terrain1 == null) return;

                if (TerrainManager.remove(terrain1) != null) {
                    boolean isReallyWorldTerrain = terrain1 instanceof WorldTerrain && Bukkit.getWorlds().stream().anyMatch(world -> world.getUID().equals(terrain1.world()));
                    lang.send(sender, lang.get("Delete." + (isReallyWorldTerrain ? "World " : "") + "Success").replace("<name>", terrain1.name()));
                    if (isReallyWorldTerrain) TerrainManager.loadWorld(terrain1.world(), terrain1.name());
                    // Cancelling all confirmations related to this terrain.
                    ConfirmCommand.cancelConfirmations(Objects.hash("transfer", terrain1.id()));
                    ConfirmCommand.cancelConfirmations(Objects.hash("resize", terrain1.id()));
                } else {
                    lang.send(sender, lang.get("Delete.Error"));
                }
            }, () -> {
                Terrain terrain1 = terrainRef.get();
                return lang.getColored("Delete.Confirmation Description").replace("<name>", terrain1 == null ? name : terrain1.name());
            }, confirmationHash);
        });
    }
}
