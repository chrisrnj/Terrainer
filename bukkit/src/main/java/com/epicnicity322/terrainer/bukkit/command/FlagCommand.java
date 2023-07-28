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
import com.epicnicity322.epicpluginlib.core.logger.ConsoleLogger;
import com.epicnicity322.terrainer.bukkit.TerrainerPlugin;
import com.epicnicity322.terrainer.bukkit.event.flag.UserFlagSetEvent;
import com.epicnicity322.terrainer.bukkit.event.flag.UserFlagUnsetEvent;
import com.epicnicity322.terrainer.bukkit.gui.FlagListGUI;
import com.epicnicity322.terrainer.bukkit.util.CommandUtil;
import com.epicnicity322.terrainer.core.Terrainer;
import com.epicnicity322.terrainer.core.terrain.Flag;
import com.epicnicity322.terrainer.core.terrain.FlagTransformException;
import com.epicnicity322.terrainer.core.terrain.Flags;
import com.epicnicity322.terrainer.core.terrain.Terrain;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.HumanEntity;
import org.jetbrains.annotations.NotNull;

public final class FlagCommand extends Command {
    @Override
    public @NotNull String getName() {
        return "flag";
    }

    @Override
    public @NotNull String getPermission() {
        return "terrainer.flag";
    }

    @Override
    protected @NotNull CommandRunnable getNoPermissionRunnable() {
        return CommandUtil.noPermissionRunnable();
    }

    @Override
    public void run(@NotNull String label, @NotNull CommandSender sender, @NotNull String[] args) {
        CommandUtil.CommandArguments arguments = CommandUtil.findTerrain("terrainer.flag.others", true, label, sender, args);
        if (arguments == null) return;
        String label2 = args[0];
        args = arguments.preceding();
        Terrain terrain = arguments.terrain();
        MessageSender lang = TerrainerPlugin.getLanguage();

        // no args, open gui.
        if (args.length == 0) {
            if (sender instanceof HumanEntity player) {
                new FlagListGUI(player, terrain).open(player);
            } else {
                lang.send(sender, lang.get("Invalid Arguments.Error").replace("<label>", label).replace("<label2>", label2)
                        .replace("<args>", lang.get("Invalid Arguments.Flag Optional") + " --t " + lang.get("Invalid Arguments.Terrain")));
                return;
            }
            return;
        }

        Flag<?> flag = Flags.matchFlag(args[0]);

        if (flag == null) {
            lang.send(sender, lang.get("Flags.Error.Not Found").replace("<value>", args[0]));
            return;
        }
        if (!sender.hasPermission(Flags.findPermission(flag))) {
            lang.send(sender, lang.get("General.No Permission"));
            return;
        }

        flagCommand(flag, terrain, label, label2, sender, args);
    }

    private <T> void flagCommand(@NotNull Flag<T> flag, @NotNull Terrain terrain, @NotNull String label, @NotNull String label2, @NotNull CommandSender sender, @NotNull String[] args) {
        MessageSender lang = TerrainerPlugin.getLanguage();
        String localized = lang.get("Flags.Values." + flag.id() + ".Display Name");

        // command sent with no values for the flags.
        if (args.length == 1) {
            // remove flag if it is already set.
            if (terrain.flags().view().containsKey(flag.id())) {
                UserFlagUnsetEvent e = new UserFlagUnsetEvent(sender, terrain, flag, false);
                Bukkit.getPluginManager().callEvent(e);
                if (e.isCancelled()) return;

                terrain.flags().removeFlag(flag);
                lang.send(sender, lang.get("Flags.Unset").replace("<flag>", localized).replace("<name>", terrain.name()));

                if (terrain.usesDefaultFlagValues()) {
                    String formatted;
                    try {
                        formatted = flag.formatter().apply(flag.defaultValue());
                    } catch (Throwable t) {
                        formatted = flag.defaultValue().toString();
                    }
                    lang.send(sender, lang.get("Flags.Default Alert").replace("<flag>", localized).replace("<name>", terrain.name()).replace("<state>", formatted));
                }
                return;
            }

            // Boolean flags don't need input.
            if (flag.defaultValue() instanceof Boolean bool) {
                UserFlagSetEvent e = new UserFlagSetEvent(sender, terrain, flag, Boolean.toString(!bool), false);
                Bukkit.getPluginManager().callEvent(e);
                if (e.isCancelled()) return;

                terrain.flags().putFlag((Flag<? super Boolean>) (Flag<?>) flag, !bool);
                lang.send(sender, lang.get("Flags.Set").replace("<flag>", localized).replace("<name>", terrain.name()).replace("<state>", lang.get(!bool ? "Flags.Allow" : "Flags.Deny")));
            } else {
                lang.send(sender, lang.get("Invalid Arguments.Error").replace("<label>", label).replace("<label2>", label2)
                        .replace("<args>", lang.get("Invalid Arguments.Flag Optional") + " " + lang.get("Invalid Arguments.Terrain Optional")));
            }
            return;
        }

        String input = CommandUtil.join(args, 1);

        UserFlagSetEvent e = new UserFlagSetEvent(sender, terrain, flag, input, false);
        Bukkit.getPluginManager().callEvent(e);
        if (e.isCancelled()) return;

        putFlag(terrain, flag, input, sender, localized);
    }

    // Can not use FlagMap#putFlag(Flag<T> flag, T obj) as FlagMap#putFlag(Flag<?> flag, ? obj), this method ensures
    // the transformer is of the same type.
    private <T> void putFlag(@NotNull Terrain terrain, @NotNull Flag<T> flag, @NotNull String input, @NotNull CommandSender player, @NotNull String localized) {
        MessageSender lang = TerrainerPlugin.getLanguage();
        try {
            T data = flag.transformer().apply(input);

            terrain.flags().putFlag(flag, data);
            try {
                input = flag.formatter().apply(data);
            } catch (Throwable ignored) {
            }
            lang.send(player, lang.get("Flags.Set").replace("<flag>", localized).replace("<name>", terrain.name()).replace("<state>", input));
        } catch (FlagTransformException e) {
            lang.send(player, lang.get("Flags.Error.Default").replace("<flag>", lang.get("Flags.Values." + flag.id() + ".Display Name")).replace("<message>", e.getMessage()));
        } catch (Throwable t) {
            Terrainer.logger().log("Unable to parse input '" + input + "' as data for flag with ID '" + flag.id() + "':", ConsoleLogger.Level.ERROR);
            t.printStackTrace();
            lang.send(player, lang.get("Flags.Error.Unknown"));
        }
    }
}
