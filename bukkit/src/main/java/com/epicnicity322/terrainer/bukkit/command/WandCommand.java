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
import com.epicnicity322.terrainer.bukkit.listener.SelectionListener;
import com.epicnicity322.terrainer.bukkit.util.CommandUtil;
import com.epicnicity322.terrainer.core.config.Configurations;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

public final class WandCommand extends Command {
    private final @NotNull NamespacedKey selectorWandKey;
    private final @NotNull NamespacedKey infoWandKey;

    public WandCommand(@NotNull NamespacedKey selectorWandKey, @NotNull NamespacedKey infoWandKey) {
        this.selectorWandKey = selectorWandKey;
        this.infoWandKey = infoWandKey;
    }

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
    public void run(@NotNull String label, @NotNull CommandSender sender, @NotNull String[] args) {
        MessageSender lang = TerrainerPlugin.getLanguage();
        boolean info = false;

        if (args.length > 1) {
            String infoType = lang.get("Commands.Wand.Info");
            String selectorType = lang.get("Commands.Wand.Selector");

            if (args[1].equalsIgnoreCase(infoType)) {
                info = true;
            } else if (!args[1].equalsIgnoreCase(selectorType)) {
                lang.send(sender, lang.get("Invalid Arguments.Error").replace("<label>", label).replace("<label2>", args[0]).replace("<args>", "<" + infoType + "|" + selectorType + "> " + lang.get("Invalid Arguments.Player Optional")));
                return;
            }
        }

        if (!sender.hasPermission("terrainer.wand." + (info ? "info" : "selector"))) {
            lang.send(sender, lang.get("General.No Permission"));
            return;
        }

        double price = Configurations.CONFIG.getConfiguration().getNumber((info ? "Info " : "Selector ") + "Wand.Price").orElse(50).doubleValue();
        Player player = sender instanceof Player p ? p : null;
        Player to = player;
        long currentTime = System.currentTimeMillis();

        if (player != null) {
            if (!sender.hasPermission("terrainer.wand." + (info ? "info" : "selector") + ".nocooldown")) {
                long cooldown = Configurations.CONFIG.getConfiguration().getNumber((info ? "Info " : "Selector ") + "Wand.Cooldown").orElse(0).longValue() * 1000;
                Long lastCall = player.getPersistentDataContainer().get(info ? infoWandKey : selectorWandKey, PersistentDataType.LONG);

                if (lastCall != null && cooldown != 0 && currentTime - lastCall <= cooldown) {
                    lang.send(sender, lang.get("Wand.Cooldown").replace("<remaining>", Long.toString((cooldown - (currentTime - lastCall)) / 1000)));
                    return;
                }
            }
            if (!sender.hasPermission("terrainer.wand." + (info ? "info" : "selector") + ".free")) {
                if (price != 0) {
                    if (TerrainerPlugin.getEconomyHandler() == null) {
                        lang.send(sender, lang.get("General.No Economy"));
                        return;
                    }
                    if (!TerrainerPlugin.getEconomyHandler().withdrawPlayer(to, price)) {
                        lang.send(sender, lang.get("General.Not Enough Money").replace("<value>", Double.toString(price)));
                        return;
                    }
                }
            } else {
                price = 0;
            }
        }

        ItemStack item = info ? SelectionListener.getInfoWand() : SelectionListener.getSelectorWand();
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

        if (player != null) {
            player.getPersistentDataContainer().set(info ? infoWandKey : selectorWandKey, PersistentDataType.LONG, currentTime);
        }

        if (!to.getInventory().addItem(item).isEmpty()) {
            to.getWorld().dropItem(to.getLocation(), item);
        }

        String itemName = item.getItemMeta().getDisplayName();

        if (given) {
            lang.send(sender, lang.get("Wand.Given").replace("<type>", itemName)
                    .replace("<player>", to.getName())
                    .replace("<price>", Double.toString(price)));
            lang.send(to, lang.get("Wand.Received").replace("<player>", sender.getName()).replace("<type>", itemName));
        } else {
            lang.send(sender, lang.get("Wand.Bought").replace("<type>", itemName)
                    .replace("<price>", Double.toString(price)));
        }
    }
}
