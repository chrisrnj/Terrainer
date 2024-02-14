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
import com.epicnicity322.epicpluginlib.bukkit.command.TabCompleteRunnable;
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

import java.util.Locale;
import java.util.Map;

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
    public void run(@NotNull String label, @NotNull CommandSender sender, @NotNull String[] args0) {
        MessageSender lang = TerrainerPlugin.getLanguage();
        CommandUtil.findTerrain("terrainer.flag.others", "terrainer.flag.world", true, label, sender, args0, lang.getColored("Flags.Select"), arguments -> {
            String[] args = arguments.preceding();
            Terrain terrain = arguments.terrain();

            // no args, open gui.
            if (args.length == 0) {
                if (sender instanceof HumanEntity player) {
                    new FlagListGUI(player, terrain).open(player);
                } else {
                    lang.send(sender, lang.get("Invalid Arguments.Error").replace("<label>", label).replace("<label2>", args0[0]).replace("<args>", lang.get("Invalid Arguments.Flag Optional") + " --t " + lang.get("Invalid Arguments.Terrain")));
                    return;
                }
                return;
            }

            Flag<?> flag = Flags.matchFlag(args[0]);

            if (flag == null) {
                lang.send(sender, lang.get("Flags.Error.Not Found").replace("<value>", args[0]));
                return;
            }
            if (!sender.hasPermission(flag.editPermission())) {
                lang.send(sender, lang.get("General.No Permission"));
                return;
            }

            flagCommand(flag, terrain, label, args0[0], sender, args);
        });
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
            if (Boolean.class.isAssignableFrom(flag.dataType())) {
                Flag<Boolean> booleanFlag = (Flag<Boolean>) flag;
                boolean newState = !booleanFlag.defaultValue();

                UserFlagSetEvent e = new UserFlagSetEvent(sender, terrain, flag, Boolean.toString(newState), false);
                Bukkit.getPluginManager().callEvent(e);
                if (e.isCancelled()) return;

                terrain.flags().putFlag(booleanFlag, newState);
                lang.send(sender, lang.get("Flags.Set").replace("<flag>", localized).replace("<name>", terrain.name()).replace("<state>", lang.get(newState ? "Flags.Allow" : "Flags.Deny")));
            } else {
                lang.send(sender, lang.get("Invalid Arguments.Error").replace("<label>", label).replace("<label2>", label2).replace("<args>", lang.get("Invalid Arguments.Flag Optional") + " " + lang.get("Invalid Arguments.Terrain Optional")));
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

    @Override
    protected @NotNull TabCompleteRunnable getTabCompleteRunnable() {
        return (completions, label, sender, args) -> {
            switch (args.length) {
                case 2 -> {
                    String arg = args[1].toLowerCase(Locale.ROOT).replace('_', '-');

                    for (Flag<?> flag : Flags.values()) {
                        if (!sender.hasPermission(flag.editPermission())) continue;
                        String id = flag.id().toLowerCase(Locale.ROOT).replace(' ', '-');
                        if (id.startsWith(arg)) completions.add(id);
                    }
                    for (Flag<?> flag : Flags.customValues()) {
                        if (!sender.hasPermission(flag.editPermission())) continue;
                        String id = flag.id().toLowerCase(Locale.ROOT).replace(' ', '-');
                        if (id.startsWith(arg)) completions.add(id);
                    }
                }
                case 3 -> {
                    Flag<?> flag = Flags.matchFlag(args[1]);
                    if (flag == null) return;
                    String arg = args[2].toLowerCase(Locale.ROOT);

                    if (Boolean.class.isAssignableFrom(flag.dataType())) {
                        if ("allow".startsWith(arg)) completions.add("allow");
                        if ("deny".startsWith(arg)) completions.add("deny");
                        if ("false".startsWith(arg)) completions.add("false");
                        if ("true".startsWith(arg)) completions.add("true");
                    } else if (Map.class.isAssignableFrom(flag.dataType())) {
                        String suggestion = "Key1=A,Key2=B,Key3=C";
                        if (suggestion.toLowerCase(Locale.ROOT).startsWith(arg)) completions.add(suggestion);
                    }
                }
                default ->
                        CommandUtil.addTerrainTabCompletion(completions, "terrainer.flag.others", true, sender, args);
            }
        };
    }
}
