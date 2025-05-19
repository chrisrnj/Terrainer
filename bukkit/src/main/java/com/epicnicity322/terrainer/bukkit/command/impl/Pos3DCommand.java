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
import com.epicnicity322.epicpluginlib.bukkit.command.TabCompleteRunnable;
import com.epicnicity322.epicpluginlib.bukkit.lang.MessageSender;
import com.epicnicity322.terrainer.bukkit.TerrainerPlugin;
import com.epicnicity322.terrainer.bukkit.command.TerrainerCommand;
import com.epicnicity322.terrainer.bukkit.util.CommandUtil;
import com.epicnicity322.terrainer.core.location.Coordinate;
import com.epicnicity322.terrainer.core.location.WorldCoordinate;
import com.epicnicity322.terrainer.core.util.PlayerUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;
import java.util.UUID;
import java.util.stream.IntStream;

public abstract class Pos3DCommand extends TerrainerCommand {
    private Pos3DCommand() {
    }

    protected abstract boolean isFirst();

    @Override
    public @NotNull String getPermission() {
        return "terrainer.select.command.3d";
    }

    @Override
    protected @NotNull CommandRunnable getNoPermissionRunnable() {
        return CommandUtil.noPermissionRunnable();
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
            if (!sender.hasPermission("terrainer.select.command.3d.coordinates.world")) {
                lang.send(sender, lang.get("Select.Error.Coordinates"));
                return;
            }
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

        WorldCoordinate[] selections = PlayerUtil.selections(player == null ? null : player.getUniqueId());

        selections[isFirst() ? 0 : 1] = new WorldCoordinate(world.getUID(), new Coordinate(x, y, z));

        lang.send(sender, lang.get("Select.Success." + (isFirst() ? "First" : "Second")).replace("<world>", world.getName())
                .replace("<coord>", "X: " + x + ", Y: " + y + ", Z: " + z));

        if (selections[0] != null && selections[1] != null) {
            lang.send(sender, lang.get("Select.Success.Suggest").replace("<label>", label));
        }
    }

    @Override
    protected @NotNull TabCompleteRunnable getTabCompleteRunnable() {
        return (completions, label, sender, args) -> {
            if (!sender.hasPermission("terrainer.select.command.coordinates")) return;

            if (args.length == 2) {
                if (args[1].isEmpty()) {
                    if (sender instanceof Player p) completions.add(Integer.toString(p.getLocation().getBlockX()));
                    else IntStream.range(-5, 6).forEach(i -> completions.add(Integer.toString(i)));
                }
            } else if (args.length == 3) {
                if (args[2].isEmpty()) {
                    if (sender instanceof Player p) completions.add(Integer.toString(p.getLocation().getBlockY()));
                    else IntStream.range(-5, 6).forEach(i -> completions.add(Integer.toString(i)));
                }
            } else if (args.length == 4) {
                if (args[3].isEmpty()) {
                    if (sender instanceof Player p) completions.add(Integer.toString(p.getLocation().getBlockZ()));
                    else IntStream.range(-5, 6).forEach(i -> completions.add(Integer.toString(i)));
                }
            } else if (args.length == 5) {
                if (!sender.hasPermission("terrainer.select.command.3d.coordinates.world")) return;
                for (World world : Bukkit.getWorlds()) {
                    String name = world.getName();
                    if (!sender.hasPermission("terrainer.world." + name.toLowerCase(Locale.ROOT))) continue;
                    if (name.startsWith(args[4])) completions.add(name);
                }
            }
        };
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

        @Override
        public void reloadCommand() {
            setAliases(TerrainerPlugin.getLanguage().get("Commands.Select.Command 3D First"));
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

        @Override
        public void reloadCommand() {
            setAliases(TerrainerPlugin.getLanguage().get("Commands.Select.Command 3D Second"));
        }
    }
}
