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
import com.epicnicity322.terrainer.bukkit.util.BukkitPlayerUtil;
import com.epicnicity322.terrainer.bukkit.util.CommandUtil;
import com.epicnicity322.terrainer.core.config.Configurations;
import com.epicnicity322.yamlhandler.Configuration;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class LimitCommand extends TerrainerCommand {
    @Override
    public @NotNull String getName() {
        return "limit";
    }

    @Override
    public @NotNull String getPermission() {
        return "terrainer.limit";
    }

    @Override
    protected @NotNull CommandRunnable getNoPermissionRunnable() {
        return CommandUtil.noPermissionRunnable();
    }

    @Override
    protected @NotNull CommandRunnable getNotEnoughArgsRunnable() {
        return (label, sender, args) -> {
            MessageSender lang = TerrainerPlugin.getLanguage();
            if (sender.hasPermission("terrainer.limit.edit.blocks") || sender.hasPermission("terrainer.limit.edit.claims")) {
                String give = lang.get("Commands.Limit.Give");
                String set = lang.get("Commands.Limit.Set");
                String take = lang.get("Commands.Limit.Take");
                String claims = lang.get("Commands.Limit.Claims");
                String blocks = lang.get("Commands.Limit.Claims");

                lang.send(sender, lang.get("Invalid Arguments.Error").replace("<label>", label).replace("<label2>", args[0]).
                        replace("<args>", lang.get("Invalid Arguments.Player Optional") + " [<" + give + "|" + set + "|" + take + "> <" + claims + "|" + blocks + "> <" + lang.get("Invalid Arguments.Amount") + ">]"));
            } else {
                lang.send(sender, lang.get("Invalid Arguments.Error").replace("<label>", label).replace("<label2>", args[0]).replace("<args>", lang.get("Invalid Arguments.Player Optional")));
            }
        };
    }

    @Override
    public void reloadCommand() {
        setAliases(TerrainerPlugin.getLanguage().get("Commands.Limit.Command"));
    }

    // /tr limit [player] [<give|set|take> <blocks|claims> <value>]
    // Does not support offline players, because the limit can vary depending on player's permissions and persistent data.
    @Override
    public void run(@NotNull String label, @NotNull CommandSender sender, @NotNull String[] args) {
        MessageSender lang = TerrainerPlugin.getLanguage();
        Player who;

        if (args.length > 1) {
            who = Bukkit.getPlayer(args[1]);
            if (who == null) {
                lang.send(sender, lang.get("General.Player Not Found").replace("<value>", args[1]));
                return;
            }
        } else {
            if (sender instanceof Player player) {
                who = player;
            } else {
                lang.send(sender, lang.get("Limits.Info.No Limits.You"));
                return;
            }
        }

        boolean you = sender == who;

        if (!you && !sender.hasPermission("terrainer.limit.others")) {
            lang.send(sender, lang.get("Limits.No Others"));
            return;
        }

        // Display limits to player.
        if (args.length < 3) {
            if (who.hasPermission("terrainer.bypass.limit.blocks") && who.hasPermission("terrainer.bypass.limit.claims")) {
                lang.send(sender, lang.get("Limits.Info.No Limits." + (you ? "You" : "Other")).replace("<other>", who.getName()));
                return;
            }

            Configuration config = Configurations.CONFIG.getConfiguration();
            if (config.getBoolean("Limits.Per World Block Limit").orElse(false) || config.getBoolean("Limits.Per World Claim Limit").orElse(false)) {
                lang.send(sender, lang.get("Limits.Info.Header In This World." + (you ? "You" : "Other")).replace("<world>", who.getWorld().getName()).replace("<other>", who.getName()));
            } else {
                lang.send(sender, lang.get("Limits.Info.Header." + (you ? "You" : "Other")).replace("<other>", who.getName()));
            }

            BukkitPlayerUtil pUtil = TerrainerPlugin.getPlayerUtil();
            lang.send(sender, lang.get("Limits.Info.Blocks").replace("<used>", Long.toString(pUtil.claimedBlocks(who.getUniqueId(), who.getWorld().getUID()))).replace("<max>", Long.toString(pUtil.blockLimit(who))));
            lang.send(sender, lang.get("Limits.Info.Claims").replace("<used>", Integer.toString(pUtil.claimedTerrains(who.getUniqueId(), who.getWorld().getUID()))).replace("<max>", Integer.toString(pUtil.claimLimit(who))));
            lang.send(sender, lang.get("Limits.Info.Footer").replace("<label>", label));
            return;
        } else if (args.length != 5) {
            getNotEnoughArgsRunnable().run(label, sender, args);
            return;
        }

        String give = lang.get("Commands.Limit.Give");
        String set = lang.get("Commands.Limit.Set");
        String take = lang.get("Commands.Limit.Take");
        String blocks = lang.get("Commands.Limit.Blocks");
        String claims = lang.get("Commands.Limit.Claims");

        String operation = args[2];
        String limitType = args[3];
        long value;
        boolean blocksType = false;

        if (limitType.equalsIgnoreCase("blocks") || limitType.equalsIgnoreCase(blocks)) {
            blocksType = true;
        } else if (!limitType.equalsIgnoreCase("claims") && !limitType.equalsIgnoreCase(claims)) {
            getNotEnoughArgsRunnable().run(label, sender, args);
            return;
        }

        if (!sender.hasPermission("terrainer.limit.edit." + (blocksType ? "blocks" : "claims"))) {
            lang.send(sender, lang.get("General.No Permission"));
            return;
        }

        try {
            value = Long.parseLong(args[4]);
        } catch (NumberFormatException e) {
            lang.send(sender, lang.get("General.Not A Number").replace("<value>", args[4]));
            return;
        }

        BukkitPlayerUtil util = TerrainerPlugin.getPlayerUtil();
        long current = blocksType ? util.boughtBlockLimit(who) : util.boughtClaimLimit(who);
        long finalValue;

        if (operation.equalsIgnoreCase("give") || operation.equalsIgnoreCase(give)) {
            finalValue = current + value;
            if (finalValue < current || current >= (blocksType ? Long.MAX_VALUE : Integer.MAX_VALUE)) {
                lang.send(sender, lang.get("Limits.Edit.Can Not Give"));
                return;
            }
        } else if (operation.equalsIgnoreCase("set") || operation.equalsIgnoreCase(set)) {
            finalValue = value;
        } else if (operation.equalsIgnoreCase("take") || operation.equalsIgnoreCase(take)) {
            if (current <= 0) {
                lang.send(sender, lang.get("Limits.Edit.Can Not Take"));
                return;
            }
            finalValue = current - value;
        } else {
            getNotEnoughArgsRunnable().run(label, sender, args);
            return;
        }

        if (finalValue < 0) finalValue = 0;

        if (blocksType) {
            util.setBoughtBlockLimit(who, finalValue);
            lang.send(sender, lang.get("Limits.Edit.Blocks").replace("<player>", who.getName()).replace("<value>", Long.toString(util.blockLimit(who))));
        } else {
            if (finalValue > Integer.MAX_VALUE) finalValue = Integer.MAX_VALUE;
            util.setBoughtClaimLimit(who, (int) finalValue);
            lang.send(sender, lang.get("Limits.Edit.Claims").replace("<player>", who.getName()).replace("<value>", Integer.toString(util.claimLimit(who))));
        }
    }

    @Override
    protected @NotNull TabCompleteRunnable getTabCompleteRunnable() {
        return (completions, label, sender, args) -> {
            MessageSender lang = TerrainerPlugin.getLanguage();
            String set = lang.get("Commands.Limit.Set");
            String blocks = lang.get("Commands.Limit.Blocks");

            switch (args.length) {
                case 2 -> {
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        if (player.getName().startsWith(args[1])) completions.add(player.getName());
                    }
                }
                case 3 -> {
                    String give = lang.get("Commands.Limit.Give");
                    String take = lang.get("Commands.Limit.Take");
                    if (give.startsWith(args[2])) completions.add(give);
                    if (set.startsWith(args[2])) completions.add(set);
                    if (take.startsWith(args[2])) completions.add(take);
                }
                case 4 -> {
                    String claims = lang.get("Commands.Limit.Claims");
                    if (blocks.startsWith(args[3])) completions.add(blocks);
                    if (claims.startsWith(args[3])) completions.add(claims);
                }
                case 5 -> {
                    if (args[4].isEmpty()) {
                        if (args[2].equalsIgnoreCase(set)) completions.add("0");
                        completions.add("1");
                        completions.add("10");
                        completions.add("100");
                        if (args[3].equalsIgnoreCase(blocks)) {
                            completions.add("1000");
                            completions.add("10000");
                        }
                    }
                }
            }
        };
    }
}
