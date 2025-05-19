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
import com.epicnicity322.terrainer.bukkit.listener.SelectionListener;
import com.epicnicity322.terrainer.bukkit.util.CommandUtil;
import com.epicnicity322.terrainer.core.config.Configurations;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public final class WandCommand extends TerrainerCommand {
    @Override
    public @NotNull String getName() {
        return "wand";
    }

    @Override
    public @NotNull String getPermission() {
        return "terrainer.wand";
    }

    @Override
    protected @NotNull CommandRunnable getNoPermissionRunnable() {
        return CommandUtil.noPermissionRunnable();
    }

    @Override
    public void reloadCommand() {
        setAliases(TerrainerPlugin.getLanguage().get("Commands.Wand.Command"));
    }

    @Override
    public void run(@NotNull String label, @NotNull CommandSender sender, @NotNull String[] args) {
        MessageSender lang = TerrainerPlugin.getLanguage();
        boolean info = false;

        if (args.length > 1) {
            String infoType = lang.get("Commands.Wand.Info");
            String selectorType = lang.get("Commands.Wand.Selector");

            if (args[1].equalsIgnoreCase("info") || args[1].equalsIgnoreCase(infoType)) {
                info = true;
            } else if (!args[1].equalsIgnoreCase("selector") && !args[1].equalsIgnoreCase(selectorType)) {
                lang.send(sender, lang.get("Invalid Arguments.Error").replace("<label>", label).replace("<label2>", args[0]).replace("<args>", "<" + infoType + "|" + selectorType + "> " + lang.get("Invalid Arguments.Player Optional")));
                return;
            }
        }

        if (!sender.hasPermission("terrainer.wand." + (info ? "info" : "selector"))) {
            lang.send(sender, lang.get("General.No Permission"));
            return;
        }

        long cooldown = Configurations.CONFIG.getConfiguration().getNumber(info ? "Cooldowns.Info Wand" : "Cooldowns.Selector Wand").orElse(0).longValue() * 1000;
        if (CommandUtil.testCooldown(sender, (info ? "infowand" : "selectorwand"), cooldown)) return;

        Player player = sender instanceof Player p ? p : null;
        Player to = player;
        boolean given = false;

        if (args.length > 2 && sender.hasPermission("terrainer.wand.others")) {
            to = Bukkit.getPlayer(args[2]);
            if (to == null) {
                lang.send(sender, lang.get("General.Player Not Found").replace("<value>", args[2]));
                return;
            }
            given = to != sender;
        }
        if (to == null) {
            lang.send(sender, lang.get("General.Not A Player"));
            return;
        }

        double price = Configurations.CONFIG.getConfiguration().getNumber(info ? "Info Wand.Price" : "Selector Wand.Price").orElse(50).doubleValue();

        if (player != null) {
            if (sender.hasPermission("terrainer.wand." + (info ? "info" : "selector") + ".free")) {
                price = 0;
            } else if (price != 0) {
                if (TerrainerPlugin.getEconomyHandler() == null) {
                    lang.send(sender, lang.get("General.No Economy"));
                    return;
                }
                if (!TerrainerPlugin.getEconomyHandler().withdrawPlayer(player, price)) {
                    lang.send(sender, lang.get("General.Not Enough Money").replace("<value>", Double.toString(price)));
                    return;
                }
            }
        }

        CommandUtil.updateCooldown(sender, (info ? "infowand" : "selectorwand"));

        ItemStack item = info ? SelectionListener.getInfoWand() : SelectionListener.getSelectorWand();

        // If inventory was full, drop item.
        if (!to.getInventory().addItem(item).isEmpty()) to.getWorld().dropItem(to.getLocation(), item);

        @SuppressWarnings("deprecation") String itemName = item.getItemMeta().getDisplayName();

        if (given) {
            lang.send(sender, lang.get("Wand.Given").replace("<type>", itemName).replace("<player>", to.getName()).replace("<price>", Double.toString(price)));
            lang.send(to, lang.get("Wand.Received").replace("<player>", sender.getName()).replace("<type>", itemName));
        } else {
            lang.send(sender, lang.get("Wand.Bought").replace("<type>", itemName).replace("<price>", Double.toString(price)));
        }
    }

    @Override
    protected @NotNull TabCompleteRunnable getTabCompleteRunnable() {
        return (completions, label, sender, args) -> {
            if (args.length == 2) {
                String infoType = TerrainerPlugin.getLanguage().get("Commands.Wand.Info");
                String selectorType = TerrainerPlugin.getLanguage().get("Commands.Wand.Selector");

                if (sender.hasPermission("terrainer.wand.info")) {
                    if (infoType.startsWith(args[1])) completions.add(infoType);
                }
                if (sender.hasPermission("terrainer.wand.selector")) {
                    if (selectorType.startsWith(args[1])) completions.add(selectorType);
                }
            } else if (args.length == 3) {
                if (sender.hasPermission("terrainer.wand.others")) {
                    for (Player player : Bukkit.getOnlinePlayers()) {
                        if (player.getName().startsWith(args[2])) completions.add(player.getName());
                    }
                }
            }
        };
    }
}
