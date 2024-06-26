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
import com.epicnicity322.terrainer.bukkit.event.terrain.UserNameTerrainEvent;
import com.epicnicity322.terrainer.bukkit.util.CommandUtil;
import com.epicnicity322.terrainer.core.config.Configurations;
import com.epicnicity322.terrainer.core.event.terrain.IUserNameTerrainEvent;
import com.epicnicity322.terrainer.core.terrain.Terrain;
import com.epicnicity322.terrainer.core.terrain.WorldTerrain;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public final class RenameCommand extends Command {
    @Override
    public @NotNull String getName() {
        return "rename";
    }

    @Override
    public @NotNull String getPermission() {
        return "terrainer.rename";
    }

    @Override
    protected @NotNull CommandRunnable getNoPermissionRunnable() {
        return CommandUtil.noPermissionRunnable();
    }

    @SuppressWarnings("deprecation")
    @Override
    public void run(@NotNull String label, @NotNull CommandSender sender0, @NotNull String[] args) {
        MessageSender lang = TerrainerPlugin.getLanguage();
        CommandUtil.findTerrain("terrainer.rename.others", "terrainer.rename.world", false, label, sender0, args, lang.getColored("Rename.Select"), arguments -> {
            Terrain terrain = arguments.terrain();
            CommandSender sender = arguments.sender();

            if (terrain instanceof WorldTerrain) {
                lang.send(sender, lang.get("Rename.Error.World Terrain"));
                return;
            }

            String previousName = terrain.name();
            String newName = ChatColor.translateAlternateColorCodes('&', CommandUtil.join(arguments.preceding(), 0)).trim();
            String stripped = ChatColor.stripColor(newName);
            boolean reset;

            if (stripped.isBlank()) {
                newName = Terrain.defaultName(terrain.id(), terrain.owner());
                if (previousName.equals(newName)) {
                    lang.send(sender, lang.get("Rename.Error.Same").replace("<name>", newName));
                    return;
                }
                reset = true;
            } else {
                if (ChatColor.translateAlternateColorCodes('&', previousName).equals(newName)) {
                    lang.send(sender, lang.get("Rename.Error.Same").replace("<name>", newName));
                    return;
                }
                int maxNameLength = Configurations.CONFIG.getConfiguration().getNumber("Max Name Length").orElse(26).intValue();

                if (stripped.isBlank() || (stripped.length() > maxNameLength && !sender.hasPermission("terrainer.bypass.name-length"))) {
                    lang.send(sender, lang.get("Rename.Error.Name Length").replace("<max>", Integer.toString(maxNameLength)));
                    return;
                }
                if (!sender.hasPermission("terrainer.bypass.name-blacklist") && IUserNameTerrainEvent.isBlackListed(stripped)) {
                    lang.send(sender, lang.get("Rename.Error.Blacklisted"));
                    return;
                }
                reset = false;
            }

            var event = new UserNameTerrainEvent(terrain, sender, previousName, newName, IUserNameTerrainEvent.NameReason.RENAME);
            Bukkit.getPluginManager().callEvent(event);

            if (!event.isCancelled()) {
                terrain.setName(event.newName());
                lang.send(sender, lang.get("Rename.Re" + (reset ? "set" : "named")).replace("<new>", newName).replace("<old>", previousName));
            }
        });
    }
}
