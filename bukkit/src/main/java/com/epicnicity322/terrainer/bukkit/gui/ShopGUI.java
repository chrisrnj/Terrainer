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

package com.epicnicity322.terrainer.bukkit.gui;

import com.epicnicity322.epicpluginlib.bukkit.lang.MessageSender;
import com.epicnicity322.epicpluginlib.bukkit.util.InventoryUtils;
import com.epicnicity322.terrainer.bukkit.TerrainerPlugin;
import com.epicnicity322.terrainer.bukkit.util.BukkitPlayerUtil;
import com.epicnicity322.terrainer.core.config.Configurations;
import com.epicnicity322.yamlhandler.Configuration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.function.Consumer;

public class ShopGUI {
    private static final int @NotNull [] slots = new int[]{10, 13, 16, 28, 31, 34};

    /**
     * Creates and opens a GUI that allows the player to buy more blocks and claims. The GUI changes depending on the
     * player because the prices change depending on how many times the player buys.
     *
     * @param player The player to open the inventory to.
     */
    public ShopGUI(@NotNull Player player) {
        MessageSender lang = TerrainerPlugin.getLanguage();
        Configuration config = Configurations.CONFIG.getConfiguration();

        if (TerrainerPlugin.getEconomyHandler() == null) {
            lang.send(player, lang.get("General.No Economy"));
            return;
        }

        boolean claims = config.getBoolean("Shop.Claims.Enabled").orElse(false) && player.hasPermission("terrainer.shop.claims");
        boolean blocks = config.getBoolean("Shop.Blocks.Enabled").orElse(false) && player.hasPermission("terrainer.shop.blocks");

        Inventory inventory = Bukkit.createInventory(player, claims && blocks ? 45 : 27, lang.getColored("Shop.Title"));
        HashMap<Integer, Consumer<InventoryClickEvent>> buttons = new HashMap<>((int) ((claims && blocks ? 6 : 3) / 0.75) + 1);

        int slot = 0;

        if (claims) {
            setItem(inventory, slots[slot++], true, "1", buttons, player);
            setItem(inventory, slots[slot++], true, "2", buttons, player);
            setItem(inventory, slots[slot++], true, "3", buttons, player);
        }
        if (blocks) {
            setItem(inventory, slots[slot++], false, "1", buttons, player);
            setItem(inventory, slots[slot++], false, "2", buttons, player);
            setItem(inventory, slots[slot], false, "3", buttons, player);
        }

        if (slot == 0) {
            lang.send(player, lang.get("Shop.Error.Disabled"));
            return;
        }

        InventoryUtils.fill(Material.GRAY_STAINED_GLASS_PANE, inventory, 0, 8);
        if (slot == 5) {
            InventoryUtils.fill(Material.GRAY_STAINED_GLASS_PANE, inventory, 36, 44);
        } else {
            InventoryUtils.fill(Material.GRAY_STAINED_GLASS_PANE, inventory, 18, 26);
        }
        InventoryUtils.openInventory(inventory, buttons, player);
    }

    private void setItem(@NotNull Inventory inventory, int slot, boolean claims, @NotNull String option, @NotNull HashMap<Integer, Consumer<InventoryClickEvent>> buttons, @NotNull Player player) {
        MessageSender lang = TerrainerPlugin.getLanguage();
        Configuration config = Configurations.CONFIG.getConfiguration();
        String section = "Shop." + (claims ? "Claims" : "Blocks") + ".Option " + option;

        double price = config.getNumber(section + ".Price").orElse(0).doubleValue();
        int amount = config.getNumber(section + ".Amount").orElse(0).intValue();

        inventory.setItem(slot, InventoryUtils.getItemStack(section, config, lang, Integer.toString(amount), Double.toString(price + inflation(player, claims, amount))));
        buttons.put(slot, event -> {
            Player whoCLicked = (Player) event.getWhoClicked();
            // Inflation needs to be calculated again because changes might have been made before the player clicked the button.
            double finalPrice = price + inflation(player, claims, amount);

            assert TerrainerPlugin.getEconomyHandler() != null;
            if (TerrainerPlugin.getEconomyHandler().withdrawPlayer(whoCLicked, finalPrice)) {
                BukkitPlayerUtil util = TerrainerPlugin.getPlayerUtil();

                if (claims) {
                    util.setAdditionalMaxClaimLimit(whoCLicked, util.getAdditionalMaxClaimLimit(whoCLicked) + amount);
                } else {
                    util.setAdditionalMaxBlockLimit(whoCLicked, util.getAdditionalMaxBlockLimit(whoCLicked) + amount);
                }

                lang.send(whoCLicked, lang.get("Shop.Success." + (claims ? "Claims" : "Blocks")).replace("<amount>", Integer.toString(amount)).replace("<price>", Double.toString(finalPrice)));
                // Close because a new inventory with new inflation prices needs to be calculated.
                whoCLicked.closeInventory();
            } else {
                lang.send(whoCLicked, lang.get("General.Not Enough Money").replace("<value>", Double.toString(finalPrice)));
            }
        });
    }

    private double inflation(@NotNull Player player, boolean claims, int amount) {
        Configuration config = Configurations.CONFIG.getConfiguration();
        String section = "Shop." + (claims ? "Claims" : "Blocks") + ".Inflation.";

        if (!config.getBoolean(section + "Enabled").orElse(false)) return 0;
        double divide = config.getNumber(section + "Divide").orElse(1).doubleValue();
        if (divide < 1) divide = 1;
        double multiplier = config.getNumber(section + "Multiplier").orElse(0).doubleValue();

        double inflation = ((claims ? TerrainerPlugin.getPlayerUtil().getAdditionalMaxClaimLimit(player) : TerrainerPlugin.getPlayerUtil().getAdditionalMaxBlockLimit(player)) / divide) * multiplier;
        return (inflation * amount) / divide;
    }
}
