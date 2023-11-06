/*
 * Terrainer - A minecraft terrain claiming protection plugin.
 * Copyright (C) 2023 Christiano Rangel
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
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

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
        CommandUtil.CommandArguments commandArguments = CommandUtil.findTerrain("terrainer.delete.others", false, label, sender, args);
        if (commandArguments == null) return;
        Terrain terrain = commandArguments.terrain();

        lang.send(sender, lang.get("Delete.Confirmation").replace("<label>", label).replace("<label2>", lang.get("Commands.Confirm.Confirm")).replace("<name>", terrain.name()));

        int confirmationHash = Objects.hash("delete", terrain.id());

        ConfirmCommand.requestConfirmation(sender, () -> {
            if (TerrainManager.remove(terrain) != null) {
                lang.send(sender, lang.get("Delete.Success").replace("<name>", terrain.name()));
                // Cancelling all confirmations related to this terrain.
                ConfirmCommand.cancelConfirmations(confirmationHash);
                ConfirmCommand.cancelConfirmations(Objects.hash("transfer", terrain.id()));
            }
        }, () -> lang.getColored("Delete.Confirmation Description").replace("<name>", terrain.name()), confirmationHash);
    }
}
