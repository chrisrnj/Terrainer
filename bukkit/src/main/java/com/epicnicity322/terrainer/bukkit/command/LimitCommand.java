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
import com.epicnicity322.terrainer.bukkit.util.BukkitPlayerUtil;
import com.epicnicity322.terrainer.bukkit.util.CommandUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

public final class LimitCommand extends Command {
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

        if (args.length < 3) {
            if (who.hasPermission("terrainer.bypass.limit")) {
                lang.send(sender, lang.get("Limits.Info.No Limits." + (you ? "You" : "Other")).replace("<other>", who.getName()));
                return;
            }

            lang.send(sender, lang.get("Limits.Info.Header." + (you ? "You" : "Other")).replace("<other>", who.getName()));
            BukkitPlayerUtil pUtil = TerrainerPlugin.getPlayerUtil();
            lang.send(sender, lang.get("Limits.Info.Blocks").replace("<used>", Long.toString(pUtil.getUsedBlockLimit(who.getUniqueId()))).replace("<max>", Long.toString(pUtil.getMaxBlockLimit(who))));
            lang.send(sender, lang.get("Limits.Info.Claims").replace("<used>", Integer.toString(pUtil.getUsedClaimLimit(who.getUniqueId()))).replace("<max>", Integer.toString(pUtil.getMaxClaimLimit(who))));
            lang.send(sender, lang.get("Limits.Info.Footer").replace("<label>", label));
        } else {
            String operation = args[2];
            boolean permission = sender.hasPermission("terrainer.limit." + operation.toLowerCase(Locale.ROOT));

            if (args.length == 5) {
                String limitType = args[3];
                Boolean blocksType = null;

                if (limitType.equalsIgnoreCase("blocks")) {
                    permission = permission && sender.hasPermission("terrainer.limit.edit.blocks");
                    blocksType = true;
                } else if (limitType.equalsIgnoreCase("claims")) {
                    permission = permission && sender.hasPermission("terrainer.limit.edit.claims");
                    blocksType = false;
                }

                if (blocksType != null) {
                    long value;
                    try {
                        value = Long.parseLong(args[4]);
                    } catch (NumberFormatException e) {
                        lang.send(sender, lang.get("General.Not A Number").replace("<value>", args[4]));
                        return;
                    }

                    BukkitPlayerUtil util = TerrainerPlugin.getPlayerUtil();
                    long current = blocksType ? util.getAdditionalMaxBlockLimit(who) : util.getAdditionalMaxClaimLimit(who);
                    long finalValue;

                    if (operation.equalsIgnoreCase("give")) {
                        if (current >= (blocksType ? Long.MAX_VALUE : Integer.MAX_VALUE)) {
                            lang.send(sender, lang.get("Limits.Edit.Can Not Give"));
                            return;
                        }
                        finalValue = current + value;
                    } else if (operation.equalsIgnoreCase("set")) {
                        finalValue = value;
                    } else if (operation.equalsIgnoreCase("take")) {
                        if (current <= 0) {
                            lang.send(sender, lang.get("Limits.Edit.Can Not Take"));
                            return;
                        }
                        finalValue = current - value;
                    } else {
                        lang.send(sender, lang.get("Invalid Arguments.Error").replace("<label>", label).replace("<label2>", args[0]).
                                replace("<args>", "<give|set|take> <claims|blocks> <value>"));
                        return;
                    }

                    if (!permission) {
                        lang.send(sender, lang.get("General.No Permission"));
                        return;
                    }

                    if (finalValue < 0) finalValue = 0;

                    if (blocksType) {
                        util.setAdditionalMaxBlockLimit(who, finalValue);
                        lang.send(sender, lang.get("Limits.Edit.Blocks").replace("<player>", who.getName()).replace("<value>", Long.toString(util.getMaxBlockLimit(who))));
                    } else {
                        if (finalValue > Integer.MAX_VALUE) finalValue = Integer.MAX_VALUE;
                        util.setAdditionalMaxClaimLimit(who, (int) finalValue);
                        lang.send(sender, lang.get("Limits.Edit.Claims").replace("<player>", who.getName()).replace("<value>", Long.toString(util.getMaxClaimLimit(who))));
                    }
                    return;
                }
            }

            lang.send(sender, lang.get("Invalid Arguments.Error").replace("<label>", label).replace("<label2>", args[0]).
                    replace("<args>", "<give|set|take> <claims|blocks> <value>"));
        }
    }
}
