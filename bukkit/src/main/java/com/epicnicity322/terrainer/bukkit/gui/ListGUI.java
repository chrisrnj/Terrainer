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

package com.epicnicity322.terrainer.bukkit.gui;

import com.epicnicity322.epicpluginlib.bukkit.util.InventoryUtils;
import com.epicnicity322.epicpluginlib.core.util.ObjectUtils;
import com.epicnicity322.terrainer.bukkit.TerrainerPlugin;
import com.epicnicity322.terrainer.core.config.Configurations;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.HumanEntity;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.function.Consumer;

public abstract class ListGUI<T> {
    protected final @NotNull Inventory inventory;
    private final @NotNull HashMap<Integer, ArrayList<T>> pages;
    private final @NotNull HashMap<Integer, Consumer<InventoryClickEvent>> buttons;

    public ListGUI(@NotNull Collection<T> collection, @NotNull String title) {
        this(collection, title, 1);
    }

    public ListGUI(@NotNull Collection<T> collection, @NotNull String title, int startingPage) {
        pages = ObjectUtils.splitIntoPages(collection, 45);
        int initialCapacity;
        int inventorySize;

        // Determining the initial capacity of the button map and the size of the inventory based on the number of
        //entries in the collection.
        if (pages.size() == 1) {
            int pageSize = pages.get(1).size();
            initialCapacity = (int) (pageSize / 0.75) + 1;
            inventorySize = 9 + (pageSize % 9 == 0 ? pageSize : pageSize + (9 - (pageSize % 9)));
        } else {
            initialCapacity = (int) (47 / 0.75) + 1;
            inventorySize = 54;
        }

        //noinspection deprecation
        inventory = Bukkit.createInventory(null, inventorySize, title);
        buttons = new HashMap<>(initialCapacity);
        InventoryUtils.fill(Material.GLASS_PANE, inventory, 0, 8);
        populate(startingPage);
    }

    protected abstract @NotNull ItemStack item(@NotNull T obj);

    protected abstract @NotNull Consumer<InventoryClickEvent> event(@NotNull T obj);

    public final void populate(int page) {
        if (page < 1) page = 1;
        if (page > pages.size()) page = pages.size();

        buttons.clear();
        for (int i = 9; i < inventory.getSize(); ++i) {
            inventory.setItem(i, null);
        }

        if (page > 1) {
            inventory.setItem(0, InventoryUtils.getItemStack("List.GUI Items.Previous Page", Configurations.CONFIG.getConfiguration(), TerrainerPlugin.getLanguage(), Integer.toString(page - 1)));
            int previous = page - 1;
            buttons.put(0, event -> populate(previous));
        } else {
            InventoryUtils.fill(Material.GLASS_PANE, inventory, 0, 0);
        }
        if (page != pages.size()) {
            inventory.setItem(8, InventoryUtils.getItemStack("List.GUI Items.Next Page", Configurations.CONFIG.getConfiguration(), TerrainerPlugin.getLanguage(), Integer.toString(page + 1)));
            int next = page + 1;
            buttons.put(8, event -> populate(next));
        } else {
            InventoryUtils.fill(Material.GLASS_PANE, inventory, 8, 8);
        }

        int slot = 9;

        for (T obj : pages.get(page)) {
            inventory.setItem(slot, item(obj));
            buttons.put(slot++, event(obj));
        }
    }

    /**
     * Opens the list inventory to a player. This inventory should have one instance per player due to how the pages are
     * populated. Because if a player changes a page, other players could see the pages changing.
     *
     * @param player The player to open the inventory to.
     */
    public final void open(@NotNull HumanEntity player) {
        InventoryUtils.openInventory(inventory, buttons, player);
    }
}
