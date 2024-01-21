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
import com.epicnicity322.terrainer.core.config.Configurations;
import com.epicnicity322.terrainer.core.terrain.Terrain;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public class DescriptionCommand extends Command {
    @Override
    public @NotNull String getName() {
        return "description";
    }

    @Override
    public @NotNull String getPermission() {
        return "terrainer.description";
    }

    @Override
    protected @NotNull CommandRunnable getNoPermissionRunnable() {
        return CommandUtil.noPermissionRunnable();
    }

    @Override
    public void run(@NotNull String label, @NotNull CommandSender sender, @NotNull String[] args) {
        CommandUtil.CommandArguments arguments = CommandUtil.findTerrain("terrainer.description.others", true, label, sender, args);
        if (arguments == null) return;
        Terrain terrain = arguments.terrain();
        args = arguments.preceding();
        MessageSender lang = TerrainerPlugin.getLanguage();

        if (args.length == 0) {
            terrain.setDescription(null);
            lang.send(sender, lang.get("Description.Reset").replace("<terrain>", terrain.name()));
            return;
        }

        String description = ChatColor.translateAlternateColorCodes('&', CommandUtil.join(args, 0)).trim();
        String stripped = ChatColor.stripColor(description);

        if (stripped.isBlank()) {
            terrain.setDescription(null);
            lang.send(sender, lang.get("Description.Reset").replace("<terrain>", terrain.name()));
            return;
        }
        int maxDescriptionLength = Configurations.CONFIG.getConfiguration().getNumber("Max Description Length").orElse(100).intValue();
        if (stripped.length() > maxDescriptionLength) {
            lang.send(sender, lang.get("Description.Error.Length").replace("<max>", Integer.toString(maxDescriptionLength)));
            return;
        }

        terrain.setDescription(description);
        lang.send(sender, lang.get("Description.Set").replace("<description>", description).replace("<terrain>", terrain.name()));
    }
}
