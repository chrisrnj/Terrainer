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

package com.epicnicity322.terrainer.bukkit.gui;

import com.epicnicity322.epicpluginlib.bukkit.lang.MessageSender;
import com.epicnicity322.epicpluginlib.bukkit.util.InventoryUtils;
import com.epicnicity322.epicpluginlib.core.logger.ConsoleLogger;
import com.epicnicity322.epicpluginlib.core.util.ObjectUtils;
import com.epicnicity322.terrainer.bukkit.TerrainerPlugin;
import com.epicnicity322.terrainer.bukkit.event.flag.UserFlagSetEvent;
import com.epicnicity322.terrainer.bukkit.event.flag.UserFlagUnsetEvent;
import com.epicnicity322.terrainer.bukkit.util.InputGetterUtil;
import com.epicnicity322.terrainer.core.Terrainer;
import com.epicnicity322.terrainer.core.config.Configurations;
import com.epicnicity322.terrainer.core.terrain.Flag;
import com.epicnicity322.terrainer.core.terrain.FlagTransformException;
import com.epicnicity322.terrainer.core.terrain.Flags;
import com.epicnicity322.terrainer.core.terrain.Terrain;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.HumanEntity;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Consumer;

public abstract class ListGUI<T> {
    private final @NotNull HashMap<Integer, ArrayList<T>> pages;
    private final @NotNull HashMap<Integer, Consumer<InventoryClickEvent>> buttons;
    private final @NotNull Inventory inventory;

    @SuppressWarnings("deprecation")
    public ListGUI(@NotNull Collection<T> collection, @NotNull String title) {
        pages = ObjectUtils.splitIntoPages(collection, 45);
        int initialCapacity;
        int inventorySize;

        // Determining the initial capacity of the buttons map and the size of the inventory based on the amount of
        //entries of the collection.
        if (pages.size() == 1) {
            int pageSize = pages.get(1).size();
            initialCapacity = (int) (pageSize / 0.75) + 1;
            inventorySize = 9 + (pageSize % 9 == 0 ? pageSize : pageSize + (9 - (pageSize % 9)));
        } else {
            initialCapacity = (int) (47 / 0.75) + 1;
            inventorySize = 54;
        }

        inventory = Bukkit.createInventory(null, inventorySize, title);
        buttons = new HashMap<>(initialCapacity);
        InventoryUtils.fill(Material.GLASS_PANE, inventory, 0, 8);
        populate(1);
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

    public static final class TerrainListGUI extends ListGUI<Terrain> {
        public TerrainListGUI(@NotNull Collection<Terrain> terrains, @NotNull String title) {
            super(terrains, title);
        }

        @Override
        protected @NotNull ItemStack item(@NotNull Terrain obj) {
            return InventoryUtils.getItemStack("Terrain List.GUI.Terrain Item", Configurations.CONFIG.getConfiguration(), TerrainerPlugin.getLanguage(), obj.name(), obj.id().toString(), obj.description(), Double.toString(obj.area()), TerrainerPlugin.getPlayerUtil().getOwnerName(obj.owner()));
        }

        @Override
        protected @NotNull Consumer<InventoryClickEvent> event(@NotNull Terrain obj) {
            return event -> {
            };
        }
    }

    public static final class FlagListGUI extends ListGUI<FlagListGUI.FlagEntry> {
        public FlagListGUI(@NotNull HumanEntity player, @NotNull Terrain terrain) {
            super(qualifiedFlags(terrain, player), TerrainerPlugin.getLanguage().getColored("Flags.Management GUI Title").replace("<terrain>", terrain.name()));
        }

        private static @NotNull TreeSet<FlagEntry> qualifiedFlags(@NotNull Terrain terrain, @NotNull HumanEntity player) {
            TreeSet<FlagEntry> qualified = new TreeSet<>(Comparator.comparing(entry -> entry.flag().id()));

            for (Flag<?> flag : Flags.values()) {
                if (player.hasPermission(Flags.findPermission(flag))) qualified.add(new FlagEntry(terrain, flag));
            }
            for (Flag<?> customFlag : Flags.customValues()) {
                if (player.hasPermission(Flags.findPermission(customFlag)))
                    qualified.add(new FlagEntry(terrain, customFlag));
            }
            return qualified;
        }

        @Override
        protected @NotNull ItemStack item(@NotNull FlagEntry obj) {
            return item(obj.flag, obj.terrain);
        }

        private <T> @NotNull ItemStack item(@NotNull Flag<T> flag, Terrain terrain) {
            MessageSender lang = TerrainerPlugin.getLanguage();
            T data = terrain.flags().getData(flag);
            String state = null;
            if (data != null) {
                try {
                    state = ChatColor.translateAlternateColorCodes('&', flag.formatter().apply(data));
                } catch (Throwable t) {
                    Terrainer.logger().log("Unable to find state of the flag '" + flag.id() + "':", ConsoleLogger.Level.ERROR);
                    t.printStackTrace();
                    state = "?";
                }
            }
            ItemStack item = InventoryUtils.getItemStack("Flags.Values." + flag.id(), Configurations.CONFIG.getConfiguration(), lang, data == null ? lang.getColored("Flags.Undefined") : state);
            if (Boolean.TRUE.equals(data) || state != null) {
                ItemMeta meta = item.getItemMeta();
                meta.addEnchant(Enchantment.DURABILITY, 1, true);
                item.setItemMeta(meta);
            }
            return item;
        }

        private <T> @Nullable String findState(@NotNull Flag<T> flag, T data) {

            if (data == null) return null;
            try {
                return ChatColor.translateAlternateColorCodes('&', flag.formatter().apply(data));
            } catch (Throwable t) {
                Terrainer.logger().log("Unable to find state of the flag '" + flag.id() + "':", ConsoleLogger.Level.ERROR);
                t.printStackTrace();
                return "?";
            }
        }

        @Override
        protected @NotNull Consumer<InventoryClickEvent> event(@NotNull FlagEntry t) {
            MessageSender lang = TerrainerPlugin.getLanguage();
            Terrain terrain = t.terrain();
            Flag<?> flag = t.flag();


            return event -> {
                HumanEntity player = event.getWhoClicked();
                String localized = lang.get("Flags.Values." + flag.id() + ".Display Name");

                if (event.getClick() == ClickType.RIGHT || event.getClick() == ClickType.DROP) {
                    UserFlagUnsetEvent e = new UserFlagUnsetEvent(player, terrain, flag, true);
                    Bukkit.getPluginManager().callEvent(e);
                    if (e.isCancelled()) return;

                    terrain.flags().removeFlag(flag);
                    lang.send(player, lang.get("Flags.Unset").replace("<flag>", localized).replace("<name>", terrain.name()));
                    event.getInventory().setItem(event.getSlot(), item(t));
                    return;
                }

                if (flag.defaultValue() instanceof Boolean) {
                    Flag<Boolean> conditionalFlag = (Flag<Boolean>) flag;
                    Boolean state = (state = terrain.flags().getData(conditionalFlag)) == null || state;

                    UserFlagSetEvent e = new UserFlagSetEvent(player, terrain, conditionalFlag, state ? "false" : "true", true);
                    Bukkit.getPluginManager().callEvent(e);
                    if (e.isCancelled()) return;

                    state = conditionalFlag.transformer().apply(e.input());
                    terrain.flags().putFlag(conditionalFlag, state);
                    lang.send(player, lang.get("Flags.Set").replace("<flag>", localized).replace("<name>", terrain.name()).replace("<state>", lang.get(state ? "Flags.Allow" : "Flags.Deny")));
                    event.getInventory().setItem(event.getSlot(), item(t));
                    return;
                }

                InputGetterUtil.input(player, input -> {
                    if (input.isBlank()) {
                        UserFlagUnsetEvent e = new UserFlagUnsetEvent(player, terrain, flag, true);
                        Bukkit.getPluginManager().callEvent(e);
                        if (e.isCancelled()) return;

                        terrain.flags().removeFlag(flag);
                        lang.send(player, lang.get("Flags.Unset").replace("<flag>", localized).replace("<name>", terrain.name()));
                        event.getInventory().setItem(event.getSlot(), item(t));
                        return;
                    }
                    UserFlagSetEvent e = new UserFlagSetEvent(player, terrain, flag, input, true);
                    Bukkit.getPluginManager().callEvent(e);
                    if (e.isCancelled()) return;

                    input = e.input();
                    if (putFlag(terrain, flag, input, player, localized)) {
                        event.getInventory().setItem(event.getSlot(), item(t));
                    }
                });
            };
        }

        // Can not use FlagMap#putFlag(Flag<T> flag, T obj) as FlagMap#putFlag(Flag<?> flag, ? obj), this method ensures
        // the transformer is of the same type.
        private <T> boolean putFlag(@NotNull Terrain terrain, @NotNull Flag<T> flag, @NotNull String input, @NotNull HumanEntity player, @NotNull String localized) {
            MessageSender lang = TerrainerPlugin.getLanguage();
            try {
                T data = flag.transformer().apply(input);

                terrain.flags().putFlag(flag, data);
                try {
                    input = flag.formatter().apply(data);
                } catch (Throwable ignored) {
                }
                lang.send(player, lang.get("Flags.Set").replace("<flag>", localized).replace("<name>", terrain.name()).replace("<state>", input));
                return true;
            } catch (FlagTransformException e) {
                lang.send(player, lang.get("Flags.Error.Default").replace("<flag>", lang.get("Flags.Values." + flag.id() + ".Display Name")).replace("<message>", e.getMessage()));
            } catch (Throwable t) {
                Terrainer.logger().log("Unable to parse input '" + input + "' as data for flag with ID '" + flag.id() + "':", ConsoleLogger.Level.ERROR);
                t.printStackTrace();
                lang.send(player, lang.get("Flags.Error.Unknown"));
            }
            return false;
        }

        private record FlagEntry(@NotNull Terrain terrain, @NotNull Flag<?> flag) {
            @Override
            public boolean equals(Object o) {
                if (this == o) return true;
                if (o == null || getClass() != o.getClass()) return false;
                FlagEntry flagEntry = (FlagEntry) o;
                return flag.id().equals(flagEntry.flag.id());
            }

            @Override
            public int hashCode() {
                return Objects.hash(flag.id());
            }
        }
    }
}
