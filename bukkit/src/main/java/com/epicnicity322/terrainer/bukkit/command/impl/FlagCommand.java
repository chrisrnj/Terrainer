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
import com.epicnicity322.epicpluginlib.core.logger.ConsoleLogger;
import com.epicnicity322.terrainer.bukkit.TerrainerPlugin;
import com.epicnicity322.terrainer.bukkit.command.TerrainerCommand;
import com.epicnicity322.terrainer.bukkit.event.flag.UserFlagSetEvent;
import com.epicnicity322.terrainer.bukkit.event.flag.UserFlagUnsetEvent;
import com.epicnicity322.terrainer.bukkit.gui.FlagListGUI;
import com.epicnicity322.terrainer.bukkit.util.CommandUtil;
import com.epicnicity322.terrainer.core.Terrainer;
import com.epicnicity322.terrainer.core.config.Configurations;
import com.epicnicity322.terrainer.core.flag.Flag;
import com.epicnicity322.terrainer.core.flag.FlagTransformException;
import com.epicnicity322.terrainer.core.flag.Flags;
import com.epicnicity322.terrainer.core.flag.PlayerFlag;
import com.epicnicity322.terrainer.core.terrain.Terrain;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.HumanEntity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public final class FlagCommand extends TerrainerCommand {
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
    protected @NotNull CommandRunnable getNotEnoughArgsRunnable() {
        MessageSender lang = TerrainerPlugin.getLanguage();
        return (label, sender, args) -> lang.send(sender, lang.get("Invalid Arguments.Error").replace("<label>", label).replace("<label2>", args[0]).replace("<args>", lang.get("Invalid Arguments.Flag" + (sender instanceof HumanEntity ? " Optional" : "")) + lang.get("Invalid Arguments.Terrain Optional")));
    }

    @Override
    public void reloadCommand() {
        setAliases(TerrainerPlugin.getLanguage().get("Commands.Flag.Command"));
    }

    @Override
    public void run(@NotNull String label, @NotNull CommandSender sender0, @NotNull String[] args0) {
        MessageSender lang = TerrainerPlugin.getLanguage();
        CommandUtil.findTerrain("terrainer.flag.others", "terrainer.flag.world", true, label, sender0, args0, lang.getColored("Flags.Select"), arguments -> {
            String[] args = arguments.preceding();
            Terrain terrain = arguments.terrain();
            CommandSender sender = arguments.sender();
            Flag<?> flag = null;
            CommandUtil.TargetResponse specificPlayer = null;
            int flagDataIndex = 0;

            // getting flag and specific player
            if (args.length > 1) {
                if (args[0].equalsIgnoreCase("player") || args[0].equalsIgnoreCase(lang.get("Commands.Flag.Specific"))) {
                    specificPlayer = CommandUtil.target(1, null, sender, args);
                    if (specificPlayer == null) return;
                    if (specificPlayer == CommandUtil.TargetResponse.ALL || specificPlayer == CommandUtil.TargetResponse.CONSOLE) {
                        getNotEnoughArgsRunnable().run(label, sender, args0);
                        return;
                    }

                    if (args.length > 2) {
                        flag = Flags.matchFlag(args[2]);
                        flagDataIndex = 3;
                        if (flag == null) {
                            lang.send(sender, lang.get("Flags.Error.Not Found").replace("<value>", args[2]));
                            return;
                        }
                        if (!(flag instanceof PlayerFlag<?>)) {
                            lang.send(sender, lang.get("Flags.Error.Not Player Specific").replace("<flag>", args[2]));
                            return;
                        }
                    }
                } else if (args[0].equalsIgnoreCase("default") || args[0].equalsIgnoreCase(lang.get("Commands.Flag.Default"))) {
                    flag = Flags.matchFlag(args[1]);
                    flagDataIndex = 2;
                    if (flag == null) {
                        lang.send(sender, lang.get("Flags.Error.Not Found").replace("<value>", args[1]));
                        return;
                    }
                } else {
                    getNotEnoughArgsRunnable().run(label, sender, args0);
                    return;
                }
            }

            // no args, open gui.
            if (flag == null) {
                if (!(sender instanceof HumanEntity player)) {
                    getNotEnoughArgsRunnable().run(label, sender, args0);
                    return;
                }

                new FlagListGUI(player, terrain, specificPlayer == null ? null : specificPlayer.id()).open(player);
                return;
            }

            if (!sender.hasPermission(flag.editPermission())) {
                lang.send(sender, lang.get("General.No Permission"));
                return;
            }

            flagCommand(flag, terrain, label, args0[0], sender, args, specificPlayer, flagDataIndex);
        });
    }

    private <T> void flagCommand(@NotNull Flag<T> flag, @NotNull Terrain terrain, @NotNull String label, @NotNull String label2, @NotNull CommandSender sender, @NotNull String[] args, @Nullable CommandUtil.TargetResponse specificPlayer, int inputIndex) {
        MessageSender lang = TerrainerPlugin.getLanguage();
        String localized = Configurations.FLAGS.getConfiguration().getString(flag.id() + ".Display Name").orElse(flag.id());
        String input = CommandUtil.join(args, inputIndex);
        UUID memberId = specificPlayer == null ? null : specificPlayer.id();

        // command sent with no values for the flag.
        if (input.isEmpty()) {
            // remove flag if it is already set.
            if (memberId == null ? terrain.flags().view().containsKey(flag.id()) : terrain.memberFlags().containsFlag(memberId, flag)) {
                UserFlagUnsetEvent e = new UserFlagUnsetEvent(sender, terrain, flag, false, memberId == null ? null : Bukkit.getOfflinePlayer(memberId));
                Bukkit.getPluginManager().callEvent(e);
                if (e.isCancelled()) return;

                if (memberId == null) {
                    terrain.flags().removeFlag(flag);
                    lang.send(sender, lang.get("Flags.Default.Unset").replace("<flag>", localized).replace("<name>", terrain.name()));

                    if (terrain.usesDefaultFlagValues()) {
                        String formatted;
                        try {
                            formatted = flag.formatter().apply(flag.defaultValue());
                        } catch (Throwable t) {
                            formatted = flag.defaultValue().toString();
                        }
                        lang.send(sender, lang.get("Flags.Default.Unset Alert").replace("<flag>", localized).replace("<name>", terrain.name()).replace("<state>", formatted));
                    }
                } else {
                    terrain.memberFlags().removeFlag(memberId, flag);
                    lang.send(sender, lang.get("Flags.Specific.Unset").replace("<flag>", localized).replace("<name>", terrain.name()).replace("<who>", specificPlayer.who().get()));
                }
                return;
            }

            // Boolean flags don't need input.
            if (Boolean.class.isAssignableFrom(flag.dataType())) {
                @SuppressWarnings("unchecked") Flag<Boolean> booleanFlag = (Flag<Boolean>) flag;
                boolean newState = !booleanFlag.defaultValue();

                UserFlagSetEvent e = new UserFlagSetEvent(sender, terrain, flag, Boolean.toString(newState), false, memberId == null ? null : Bukkit.getOfflinePlayer(memberId));
                Bukkit.getPluginManager().callEvent(e);
                if (e.isCancelled()) return;

                if (memberId == null) {
                    terrain.flags().putFlag(booleanFlag, newState);
                    lang.send(sender, lang.get("Flags.Default.Set").replace("<flag>", localized).replace("<name>", terrain.name()).replace("<state>", lang.get(newState ? "Flags.Allow" : "Flags.Deny")));
                } else {
                    terrain.memberFlags().putFlag(memberId, booleanFlag, newState);
                    lang.send(sender, lang.get("Flags.Specific.Set").replace("<flag>", localized).replace("<name>", terrain.name()).replace("<state>", lang.get(newState ? "Flags.Allow" : "Flags.Deny")).replace("<who>", specificPlayer.who().get()));
                }
            } else {
                getNotEnoughArgsRunnable().run(label, sender, new String[]{label2});
            }
            return;
        }

        UserFlagSetEvent e = new UserFlagSetEvent(sender, terrain, flag, input, false, memberId == null ? null : Bukkit.getOfflinePlayer(memberId));
        Bukkit.getPluginManager().callEvent(e);
        if (e.isCancelled()) return;

        putFlag(terrain, flag, specificPlayer, input, sender, localized);
    }

    private <T> void putFlag(@NotNull Terrain terrain, @NotNull Flag<T> flag, @Nullable CommandUtil.TargetResponse specificPlayer, @NotNull String input, @NotNull CommandSender player, @NotNull String localized) {
        MessageSender lang = TerrainerPlugin.getLanguage();
        try {
            T data = flag.transformer().apply(input);

            if (specificPlayer == null) {
                terrain.flags().putFlag(flag, data);
            } else {
                terrain.memberFlags().putFlag(specificPlayer.id(), flag, data);
            }

            try {
                input = flag.formatter().apply(data);
            } catch (Throwable ignored) {
            }

            if (specificPlayer == null) {
                lang.send(player, lang.get("Flags.Default.Set").replace("<flag>", localized).replace("<name>", terrain.name()).replace("<state>", input));
            } else {
                lang.send(player, lang.get("Flags.Specific.Set").replace("<flag>", localized).replace("<name>", terrain.name()).replace("<state>", input).replace("<who>", specificPlayer.who().get()));
            }
        } catch (FlagTransformException e) {
            lang.send(player, lang.get("Flags.Error.Default").replace("<flag>", localized).replace("<message>", e.getMessage()));
        } catch (Throwable t) {
            Terrainer.logger().log("Unable to parse input '" + input + "' as data for flag with ID '" + flag.id() + "':", ConsoleLogger.Level.ERROR);
            t.printStackTrace();
            lang.send(player, lang.get("Flags.Error.Unknown"));
        }
    }

    @Override
    protected @NotNull TabCompleteRunnable getTabCompleteRunnable() {
        MessageSender lang = TerrainerPlugin.getLanguage();
        String specificFlag = lang.get("Commands.Flag.Specific");
        String defaultFlag = lang.get("Commands.Flag.Default");

        return (completions, label, sender, args) -> {
            switch (args.length) {
                case 2 -> {
                    if (specificFlag.startsWith(args[1])) completions.add(specificFlag);
                    if (defaultFlag.startsWith(args[1])) completions.add(defaultFlag);
                    if (!completions.isEmpty()) return;
                }
                case 3 -> {
                    if (args[1].equalsIgnoreCase(specificFlag)) {
                        CommandUtil.addTargetTabCompletion(completions, args);
                        completions.remove("*");
                        completions.remove("null");
                    } else if (args[1].equalsIgnoreCase(defaultFlag)) {
                        addFlagTabCompletion(args[2], sender, completions, false);
                        if (!completions.isEmpty()) return;
                    }
                }
                case 4 -> {
                    if (args[1].equalsIgnoreCase(specificFlag)) {
                        addFlagTabCompletion(args[3], sender, completions, true);
                        if (!completions.isEmpty()) return;
                    } else if (args[1].equalsIgnoreCase(defaultFlag)) {
                        addFlagValueTabCompletion(args[2], args[3], sender, completions);
                        return;
                    }
                }
                case 5 -> {
                    if (args[1].equalsIgnoreCase(specificFlag)) {
                        addFlagValueTabCompletion(args[3], args[4], sender, completions);
                        return;
                    }
                }
            }
            CommandUtil.addTerrainTabCompletion(completions, "terrainer.flag.others", "terrainer.flag.world", true, sender, args);
        };
    }

    private void addFlagTabCompletion(@NotNull String arg, @NotNull CommandSender sender, @NotNull List<String> completions, boolean player) {
        arg = arg.toLowerCase(Locale.ROOT).replace('_', '-');

        for (Flag<?> flag : Flags.values()) {
            if (!sender.hasPermission(flag.editPermission())) continue;
            if (player && !(flag instanceof PlayerFlag<?>)) continue;
            String id = flag.commandFriendlyId();
            if (id.startsWith(arg)) completions.add(id);
        }
    }

    private void addFlagValueTabCompletion(@NotNull String flagName, @NotNull String arg, @NotNull CommandSender sender, @NotNull List<String> completions) {
        Flag<?> flag = Flags.matchFlag(flagName);
        if (flag == null) return;
        if (!sender.hasPermission(flag.editPermission())) return;
        arg = arg.toLowerCase(Locale.ROOT);

        if (Boolean.class.isAssignableFrom(flag.dataType())) {
            MessageSender lang = TerrainerPlugin.getLanguage();
            String allow = lang.get("Commands.Flag.Allow");
            String deny = lang.get("Commands.Flag.Deny");
            if (allow.startsWith(arg)) completions.add(allow);
            if (deny.startsWith(arg)) completions.add(deny);
        } else if (Collection.class.isAssignableFrom(flag.dataType())) {
            if (arg.isEmpty()) completions.add("Value1,Value2,Value3");
        } else if (Map.class.isAssignableFrom(flag.dataType())) {
            if (arg.isEmpty()) completions.add("Key1=A,Key2=B,Key3=C");
        }
    }
}
