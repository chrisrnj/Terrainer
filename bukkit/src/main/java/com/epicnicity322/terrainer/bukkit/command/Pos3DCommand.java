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
import com.epicnicity322.epicpluginlib.bukkit.lang.MessageSender;
import com.epicnicity322.terrainer.bukkit.TerrainerPlugin;
import com.epicnicity322.terrainer.core.Coordinate;
import com.epicnicity322.terrainer.core.WorldCoordinate;
import com.epicnicity322.terrainer.core.terrain.TerrainManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;
import java.util.UUID;

public abstract class Pos3DCommand extends Command implements Position {
    private Pos3DCommand() {
    }

    @Override
    public @NotNull String getPermission() {
        return "terrainer.select.command.3d";
    }

    // /tr pos13d [x] [y] [z] [world]
    // /tr pos23d [x] [y] [z] [world]
    @Override
    public void run(@NotNull String label, @NotNull CommandSender sender, @NotNull String[] args) {
        MessageSender lang = TerrainerPlugin.getLanguage();
        Integer x = null;
        if (args.length > 1) {
            if (!sender.hasPermission("terrainer.select.command.3d.coordinates")) {
                lang.send(sender, lang.get("Select.Error.Coordinates"));
                return;
            }
            try {
                x = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                lang.send(sender, lang.get("General.Not A Number").replace("<value>", args[1]));
                return;
            }
        }
        Integer y = null;
        if (args.length > 2) {
            try {
                y = Integer.parseInt(args[2]);
            } catch (NumberFormatException e) {
                lang.send(sender, lang.get("General.Not A Number").replace("<value>", args[2]));
                return;
            }
        }
        Integer z = null;
        if (args.length > 3) {
            try {
                z = Integer.parseInt(args[3]);
            } catch (NumberFormatException e) {
                lang.send(sender, lang.get("General.Not A Number").replace("<value>", args[3]));
                return;
            }
        }
        World world = null;
        if (args.length > 4) {
            world = Bukkit.getWorld(args[4]);
            if (world == null) {
                try {
                    world = Bukkit.getWorld(UUID.fromString(args[4]));
                } catch (IllegalArgumentException ignored) {
                }
                if (world == null) {
                    lang.send(sender, lang.get("General.World Not Found").replace("<value>", args[4]));
                    return;
                }
            }
        }

        Player player = sender instanceof Player p ? p : null;

        if (player == null && (x == null || y == null || z == null || world == null)) {
            lang.send(sender, lang.get("Invalid Arguments.Error").replace("<label>", label).replace("<label2>", args[0])
                    .replace("<args>", "<x> <y> <z> <" + lang.get("Invalid Arguments.World") + ">"));
            return;
        }

        if (player != null) {
            Location loc = player.getLocation();
            if (x == null) x = loc.getBlockX();
            if (y == null) y = loc.getBlockY();
            if (z == null) z = loc.getBlockZ();
            if (world == null) world = loc.getWorld();
        }

        if (!sender.hasPermission("terrainer.world." + world.getName().toLowerCase(Locale.ROOT))) {
            lang.send(sender, lang.get("Select.Error.World"));
            return;
        }

        WorldCoordinate[] selections = TerrainManager.getSelection(player == null ? null : player.getUniqueId());

        selections[isFirst() ? 0 : 1] = new WorldCoordinate(world.getUID(), new Coordinate(x, y, z));

        lang.send(sender, lang.get("Select.Success." + (isFirst() ? "First" : "Second")).replace("<world>", world.getName())
                .replace("<coord>", "X: " + x + ", Y: " + y + ", Z: " + z));

        if (selections[0] != null && selections[1] != null) {
            lang.send(sender, lang.get("Select.Success.Suggest").replace("<label>", label));
        }
    }

    public static final class Pos13DCommand extends Pos3DCommand {
        @Override
        public boolean isFirst() {
            return true;
        }

        @Override
        public @NotNull String getName() {
            return "pos13d";
        }
    }

    public static final class Pos23DCommand extends Pos3DCommand {
        @Override
        public boolean isFirst() {
            return false;
        }

        @Override
        public @NotNull String getName() {
            return "pos23d";
        }
    }
}
