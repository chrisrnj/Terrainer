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

import com.epicnicity322.epicpluginlib.bukkit.lang.MessageSender;
import com.epicnicity322.epicpluginlib.bukkit.util.InputGetterUtil;
import com.epicnicity322.epicpluginlib.bukkit.util.InventoryUtils;
import com.epicnicity322.epicpluginlib.core.logger.ConsoleLogger;
import com.epicnicity322.terrainer.bukkit.TerrainerPlugin;
import com.epicnicity322.terrainer.bukkit.event.flag.UserFlagSetEvent;
import com.epicnicity322.terrainer.bukkit.event.flag.UserFlagUnsetEvent;
import com.epicnicity322.terrainer.core.Terrainer;
import com.epicnicity322.terrainer.core.config.Configurations;
import com.epicnicity322.terrainer.core.flag.Flag;
import com.epicnicity322.terrainer.core.flag.FlagTransformException;
import com.epicnicity322.terrainer.core.flag.Flags;
import com.epicnicity322.terrainer.core.terrain.Terrain;
import com.epicnicity322.yamlhandler.Configuration;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.HumanEntity;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.ref.WeakReference;
import java.util.*;
import java.util.function.Consumer;
import java.util.regex.Pattern;

import static org.bukkit.event.inventory.ClickType.SHIFT_RIGHT;

public class FlagListGUI extends ListGUI<FlagListGUI.FlagEntry> {
    @SuppressWarnings("deprecation")
    private static final @NotNull Comparator<FlagEntry> flagNameComparator = Comparator.comparing(flagEntry -> ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', Configurations.FLAGS.getConfiguration().getString(flagEntry.flag().id() + ".Display Name").orElse(flagEntry.flag().id()))));
    private static final @NotNull Pattern loreLineBreaker = Pattern.compile("<line>|\\n");

    public FlagListGUI(@NotNull HumanEntity editor, @NotNull Terrain terrain, @Nullable UUID specificPlayer) {
        super(qualifiedFlags(terrain, editor, specificPlayer), TerrainerPlugin.getLanguage().getColored("Flags.Management GUI." + (specificPlayer == null ? "Default.Title" : "Specific.Title")).replace("<terrain>", terrain.name()).replace("<player>", TerrainerPlugin.getPlayerUtil().ownerName(specificPlayer)));
        inventory.setItem(4, InventoryUtils.getItemStack("Flags.Management GUI." + (specificPlayer == null ? "Default.Info Item" : "Specific.Info Item"), Configurations.CONFIG.getConfiguration(), TerrainerPlugin.getLanguage(), terrain.name(), TerrainerPlugin.getPlayerUtil().ownerName(specificPlayer)));
    }

    private static @NotNull ArrayList<FlagEntry> qualifiedFlags(@NotNull Terrain terrain, @NotNull HumanEntity editor, @Nullable UUID specificPlayer) {
        var terrainReference = new WeakReference<>(terrain);
        var qualified = new ArrayList<FlagEntry>();

        for (Flag<?> flag : Flags.values()) {
            if (editor.hasPermission(flag.editPermission())) {
                qualified.add(new FlagEntry(terrainReference, flag, specificPlayer));
            }
        }
        qualified.sort(flagNameComparator);
        return qualified;
    }

    private static <T> boolean putFlag(@NotNull Terrain terrain, @NotNull Flag<T> flag, @NotNull String input, @NotNull HumanEntity player, @NotNull String localized, @Nullable UUID memberId) {
        MessageSender lang = TerrainerPlugin.getLanguage();
        try {
            UserFlagSetEvent event = new UserFlagSetEvent(player, terrain, flag, input, true, memberId == null ? null : Bukkit.getOfflinePlayer(memberId));
            Bukkit.getPluginManager().callEvent(event);
            input = event.input();
            if (event.isCancelled()) return false;

            T data = flag.transformer().apply(input);

            try {
                input = flag.formatter().apply(data);
            } catch (Throwable ignored) {
            }

            if (memberId != null) {
                terrain.memberFlags().putFlag(memberId, flag, data);
                lang.send(player, lang.get("Flags.Specific.Set").replace("<flag>", localized).replace("<name>", terrain.name()).replace("<state>", input).replace("<who>", TerrainerPlugin.getPlayerUtil().ownerName(memberId)));
            } else {
                terrain.flags().putFlag(flag, data);
                lang.send(player, lang.get("Flags.Default.Set").replace("<flag>", localized).replace("<name>", terrain.name()).replace("<state>", input));
            }

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

    private static <T> boolean removeFlag(@NotNull Terrain terrain, @NotNull Flag<T> flag, @NotNull HumanEntity player, @NotNull String localized, @Nullable UUID memberId) {
        MessageSender lang = TerrainerPlugin.getLanguage();
        UserFlagUnsetEvent event = new UserFlagUnsetEvent(player, terrain, flag, true, memberId == null ? null : Bukkit.getOfflinePlayer(memberId));
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) return false;

        if (memberId != null) {
            terrain.memberFlags().removeFlag(memberId, flag);
            lang.send(player, lang.get("Flags.Specific.Unset").replace("<flag>", localized).replace("<name>", terrain.name()).replace("<who>", TerrainerPlugin.getPlayerUtil().ownerName(memberId)));
        } else {
            terrain.flags().removeFlag(flag);
            lang.send(player, lang.get("Flags.Default.Unset").replace("<flag>", localized).replace("<name>", terrain.name()));

            // Alerting unset to default value
            if (terrain.usesDefaultFlagValues()) {
                String formatted;
                try {
                    formatted = flag.formatter().apply(flag.defaultValue());
                } catch (Throwable t) {
                    formatted = flag.defaultValue().toString();
                }
                lang.send(player, lang.get("Flags.Default.Unset Alert").replace("<flag>", localized).replace("<name>", terrain.name()).replace("<state>", formatted));
            }
        }
        return true;
    }

    @Contract("null,_,_ -> null; !null,_,_ -> !null")
    private static @Nullable Number increaseOrDecreaseNumber(@Nullable Number current, @NotNull Flag<?> flag, boolean decrease) {
        if (current == null) return null;
        Number changeFactor = Configurations.FLAGS.getConfiguration().getNumber(flag.id() + ".Number Increase Factor").orElse(1);
        int multiplier = decrease ? -1 : 1;

        return switch (current) {
            case Integer i -> i + (multiplier * changeFactor.intValue());
            case Double d -> d + (multiplier * changeFactor.doubleValue());
            case Float f -> f + (multiplier * changeFactor.floatValue());
            case Long l -> l + (multiplier * changeFactor.longValue());
            case Short s -> s + (multiplier * changeFactor.shortValue());
            case Byte b -> b + (multiplier * changeFactor.byteValue());
            default -> current;
        };
    }

    @SuppressWarnings("deprecation")
    private static @NotNull ItemStack getInputItem(@NotNull String name) {
        ItemStack inputItem = InventoryUtils.getItemStack("Input.Anvil GUI", Configurations.CONFIG.getConfiguration(), TerrainerPlugin.getLanguage());

        if (!name.isEmpty() && inputItem.hasItemMeta()) {
            ItemMeta meta = inputItem.getItemMeta();
            meta.setDisplayName(name);
            inputItem.setItemMeta(meta);
        }
        return inputItem;
    }

    @Override
    protected @NotNull ItemStack item(@NotNull FlagEntry obj) {
        return item(obj.flag(), obj.terrain(), obj.specificPlayer());
    }

    @SuppressWarnings("deprecation")
    private <T> @NotNull ItemStack item(@NotNull Flag<T> flag, @NotNull WeakReference<Terrain> terrainReference, @Nullable UUID specificPlayer) {
        Terrain terrain = terrainReference.get();
        T data = terrain == null ? null : (specificPlayer == null ? terrain.flags().getData(flag) : terrain.memberFlags().getData(specificPlayer, flag));
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

        Configuration flags = Configurations.FLAGS.getConfiguration();

        Material material = Material.matchMaterial(flags.getString(flag.id() + ".Material").orElse("STONE"));
        if (material == null || !material.isItem()) material = Material.STONE;
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            item = new ItemStack(Material.STONE);
            meta = item.getItemMeta();
        }

        if (data instanceof Boolean b ? b : state != null) {
            meta.addEnchant(Enchantment.LURE, 1, true);
        }
        meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', flags.getString(flag.id() + ".Display Name").orElse(flag.id())));
        if (state == null) state = TerrainerPlugin.getLanguage().getColored("Flags.Undefined");
        meta.setLore(List.of(loreLineBreaker.split(ChatColor.translateAlternateColorCodes('&', flags.getString(flag.id() + ".Lore").orElse("").replace("<var0>", state)))));
        meta.addItemFlags(ItemFlag.values());
        item.setItemMeta(meta);
        return item;
    }

    @Override
    protected @NotNull Consumer<InventoryClickEvent> event(@NotNull FlagEntry flagEntry) {
        return event(flagEntry.flag(), flagEntry.terrain(), flagEntry.specificPlayer());
    }

    private <T> @NotNull Consumer<InventoryClickEvent> event(@NotNull Flag<T> flag, @NotNull WeakReference<Terrain> terrainReference, @Nullable UUID memberId) {
        return event -> {
            Terrain terrain = terrainReference.get();
            if (terrain == null) return;

            Inventory inventory = event.getInventory();
            int slot = event.getSlot();
            HumanEntity player = event.getWhoClicked();
            ClickType clickType = event.getClick();
            String flagName = Configurations.FLAGS.getConfiguration().getString(flag.id() + ".Display Name").orElse(flag.id());
            T currentData = memberId == null ? terrain.flags().getData(flag) : terrain.memberFlags().getData(memberId, flag);

            // Remove flag from terrain
            if (clickType == ClickType.RIGHT || clickType == ClickType.DROP) {
                if (removeFlag(terrain, flag, player, flagName, memberId)) {
                    updateItem(inventory, slot, flag, terrainReference, memberId);
                }
                return;
            }

            // Invert value of boolean flag
            if (Boolean.class.isAssignableFrom(flag.dataType())) {
                Boolean currentState = (currentState = (Boolean) currentData) == null || currentState;

                if (putFlag(terrain, flag, currentState ? "false" : "true", player, flagName, memberId)) {
                    updateItem(inventory, slot, flag, terrainReference, memberId);
                }
                return;
            }

            // Increase number of flag
            if (Number.class.isAssignableFrom(flag.dataType()) && (clickType == ClickType.SHIFT_LEFT || clickType == ClickType.SHIFT_RIGHT)) {
                boolean decrease = clickType == SHIFT_RIGHT;
                Number value = (Number) currentData;
                Number newValue = increaseOrDecreaseNumber(value, flag, decrease);
                if (Objects.equals(value, newValue)) return;

                if (putFlag(terrain, flag, newValue.toString(), player, flagName, memberId)) {
                    updateItem(inventory, slot, flag, terrainReference, memberId);
                }
                return;
            }

            // Ask for input otherwise
            Consumer<String> onInput = input -> {
                Terrain terrain1 = terrainReference.get();
                if (terrain1 == null) return;

                if (input.isBlank()) {
                    if (removeFlag(terrain1, flag, player, flagName, memberId)) {
                        updateItem(inventory, slot, flag, terrainReference, memberId);
                    }
                    return;
                }
                if (putFlag(terrain1, flag, input, player, flagName, memberId)) {
                    updateItem(inventory, slot, flag, terrainReference, memberId);
                }
            };

            boolean askAnvil = Configurations.CONFIG.getConfiguration().getBoolean("Input.Anvil GUI.Enabled").orElse(false);
            String previousInput;
            try {
                previousInput = currentData == null ? "" : flag.formatter().apply(currentData);
            } catch (Throwable t) {
                previousInput = "";
            }

            if (!askAnvil || !InputGetterUtil.askAnvilInput(player, getInputItem(previousInput), onInput)) { // Ask in chat if anvil is disabled
                long chatInterval = Configurations.CONFIG.getConfiguration().getNumber("Input.Chat Interval").orElse(200).longValue();

                MessageSender lang = TerrainerPlugin.getLanguage();
                lang.send(player, lang.get("Input.Ask").replace("<time>", Long.toString(chatInterval / 20)));
                InputGetterUtil.askChatInput(player, chatInterval, onInput);
            }
        };
    }

    private void updateItem(@NotNull Inventory inventory, int slot, @NotNull Flag<?> flag, @NotNull WeakReference<Terrain> terrain, @Nullable UUID specificPlayer) {
        if (!inventory.isEmpty()) inventory.setItem(slot, item(flag, terrain, specificPlayer));
    }

    protected record FlagEntry(@NotNull WeakReference<Terrain> terrain, @NotNull Flag<?> flag,
                               @Nullable UUID specificPlayer) {
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
