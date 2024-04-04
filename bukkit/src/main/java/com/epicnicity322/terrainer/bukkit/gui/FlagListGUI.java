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
import com.epicnicity322.epicpluginlib.bukkit.util.InputGetterUtil;
import com.epicnicity322.epicpluginlib.bukkit.util.InventoryUtils;
import com.epicnicity322.epicpluginlib.core.logger.ConsoleLogger;
import com.epicnicity322.terrainer.bukkit.TerrainerPlugin;
import com.epicnicity322.terrainer.bukkit.event.flag.UserFlagSetEvent;
import com.epicnicity322.terrainer.bukkit.event.flag.UserFlagUnsetEvent;
import com.epicnicity322.terrainer.core.Terrainer;
import com.epicnicity322.terrainer.core.config.Configurations;
import com.epicnicity322.terrainer.core.terrain.Flag;
import com.epicnicity322.terrainer.core.terrain.FlagTransformException;
import com.epicnicity322.terrainer.core.terrain.Flags;
import com.epicnicity322.terrainer.core.terrain.Terrain;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.HumanEntity;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.Comparator;
import java.util.Objects;
import java.util.TreeSet;
import java.util.function.Consumer;

public class FlagListGUI extends ListGUI<FlagListGUI.FlagEntry> {
    public FlagListGUI(@NotNull HumanEntity player, @NotNull Terrain terrain) {
        super(qualifiedFlags(terrain, player), TerrainerPlugin.getLanguage().getColored("Flags.Management GUI Title").replace("<terrain>", terrain.name()));
    }

    private static @NotNull TreeSet<FlagEntry> qualifiedFlags(@NotNull Terrain terrain, @NotNull HumanEntity player) {
        TreeSet<FlagEntry> qualified = new TreeSet<>(Comparator.comparing(entry -> entry.flag().id()));

        for (Flag<?> flag : Flags.values()) {
            if (player.hasPermission(flag.editPermission())) qualified.add(new FlagEntry(terrain, flag));
        }
        for (Flag<?> customFlag : Flags.customValues()) {
            if (player.hasPermission(customFlag.editPermission()))
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
                state = flag.formatter().apply(data);
                if (state != null) state = ChatColor.translateAlternateColorCodes('&', state);
            } catch (Throwable t) {
                Terrainer.logger().log("Unable to find state of the flag '" + flag.id() + "':", ConsoleLogger.Level.ERROR);
                t.printStackTrace();
                state = "?";
            }
        }
        ItemStack item = InventoryUtils.getItemStack("Flags.Values." + flag.id(), Configurations.CONFIG.getConfiguration(), lang, state == null ? lang.getColored("Flags.Undefined") : state);
        if (data instanceof Boolean b ? b : state != null) {
            ItemMeta meta = item.getItemMeta();
            meta.addEnchant(Enchantment.DURABILITY, 1, true);
            item.setItemMeta(meta);
        }
        return item;
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
                UserFlagUnsetEvent e = new UserFlagUnsetEvent(player, terrain, flag, true, null);
                Bukkit.getPluginManager().callEvent(e);
                if (e.isCancelled()) return;

                terrain.flags().removeFlag(flag);
                lang.send(player, lang.get("Flags.Unset").replace("<flag>", localized).replace("<name>", terrain.name()));
                event.getInventory().setItem(event.getSlot(), item(t));
                return;
            }

            if (Boolean.class.isAssignableFrom(flag.dataType())) {
                Flag<Boolean> conditionalFlag = (Flag<Boolean>) flag;
                Boolean state = (state = terrain.flags().getData(conditionalFlag)) == null || state;

                UserFlagSetEvent e = new UserFlagSetEvent(player, terrain, conditionalFlag, state ? "false" : "true", true, null);
                Bukkit.getPluginManager().callEvent(e);
                if (e.isCancelled()) return;

                state = conditionalFlag.transformer().apply(e.input());
                terrain.flags().putFlag(conditionalFlag, state);
                lang.send(player, lang.get("Flags.Set").replace("<flag>", localized).replace("<name>", terrain.name()).replace("<state>", lang.get(state ? "Flags.Allow" : "Flags.Deny")));
                event.getInventory().setItem(event.getSlot(), item(t));
                return;
            }

            Consumer<String> onInput = input -> {
                if (input.isBlank()) {
                    UserFlagUnsetEvent e = new UserFlagUnsetEvent(player, terrain, flag, true, null);
                    Bukkit.getPluginManager().callEvent(e);
                    if (e.isCancelled()) return;

                    terrain.flags().removeFlag(flag);
                    lang.send(player, lang.get("Flags.Unset").replace("<flag>", localized).replace("<name>", terrain.name()));
                    event.getInventory().setItem(event.getSlot(), item(t));
                    return;
                }
                UserFlagSetEvent e = new UserFlagSetEvent(player, terrain, flag, input, true, null);
                Bukkit.getPluginManager().callEvent(e);
                if (e.isCancelled()) return;

                input = e.input();
                if (putFlag(terrain, flag, input, player, localized)) {
                    event.getInventory().setItem(event.getSlot(), item(t));
                }
            };

            boolean anvil = Configurations.CONFIG.getConfiguration().getBoolean("Input.Anvil GUI.Enabled").orElse(false);

            // TODO: Input starts with previous set value.
            if (!anvil || !InputGetterUtil.askAnvilInput(player, InventoryUtils.getItemStack("Input.Anvil GUI", Configurations.CONFIG.getConfiguration(), TerrainerPlugin.getLanguage()), onInput)) {
                // Anvil could not be open, asking for input in chat.
                long chatInterval = Configurations.CONFIG.getConfiguration().getNumber("Input.Chat Interval").orElse(200).longValue();

                lang.send(player, lang.get("Input.Ask").replace("<time>", Long.toString(chatInterval / 20)));
                InputGetterUtil.askChatInput(player, chatInterval, onInput);
            }
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

    protected record FlagEntry(@NotNull Terrain terrain, @NotNull Flag<?> flag) {
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
