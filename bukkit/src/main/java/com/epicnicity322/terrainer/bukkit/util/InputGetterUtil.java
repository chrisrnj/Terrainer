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

package com.epicnicity322.terrainer.bukkit.util;

import com.epicnicity322.epicpluginlib.bukkit.lang.MessageSender;
import com.epicnicity322.epicpluginlib.bukkit.reflection.ReflectionUtil;
import com.epicnicity322.epicpluginlib.bukkit.util.InventoryUtils;
import com.epicnicity322.epicpluginlib.core.logger.ConsoleLogger;
import com.epicnicity322.terrainer.bukkit.TerrainerPlugin;
import com.epicnicity322.terrainer.core.Terrainer;
import com.epicnicity322.terrainer.core.config.Configurations;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.UUID;
import java.util.function.Consumer;

public final class InputGetterUtil {
    private static final @NotNull HashMap<UUID, WaitingInput> inputListeningPlayers = new HashMap<>();
    private static final boolean hasOpenAnvil = ReflectionUtil.getMethod(HumanEntity.class, "openAnvil", Location.class, boolean.class) != null;
    private static final @NotNull Listener chatListener = new Listener() {
        @EventHandler(priority = EventPriority.LOWEST)
        @SuppressWarnings("deprecation")
        public void onChat(AsyncPlayerChatEvent event) {
            WaitingInput waiting = inputListeningPlayers.remove(event.getPlayer().getUniqueId());
            if (inputListeningPlayers.isEmpty()) HandlerList.unregisterAll(this);
            if (waiting == null) return;

            event.setCancelled(true);
            waiting.task().cancel();
            String message = event.getMessage();

            try {
                TerrainerPlugin.getLanguage().send(event.getPlayer(), TerrainerPlugin.getLanguage().get("Input.Submitted").replace("<input>", message));
                waiting.onInput.accept(message);
            } catch (Throwable t) {
                TerrainerPlugin.getLanguage().send(event.getPlayer(), TerrainerPlugin.getLanguage().get("Input.Error"));
                Terrainer.logger().log("Unable to accept input '" + message + "' for InputGetterUtil#input(HumanEntity, Consumer<String>) method:", ConsoleLogger.Level.ERROR);
                t.printStackTrace();
            }
        }
    };

    private InputGetterUtil() {
    }

    public static void input(@NotNull HumanEntity player, @NotNull Consumer<String> onInput) {
        if (!openAnvil(player, onInput)) askInChat(player, onInput);
    }

    @SuppressWarnings("deprecation")
    private static boolean openAnvil(@NotNull HumanEntity player, @NotNull Consumer<String> onInput) {
        if (!hasOpenAnvil || !Configurations.CONFIG.getConfiguration().getBoolean("Input.Anvil GUI.Enabled").orElse(false)) {
            return false;
        }

        InventoryView view = player.openAnvil(null, true);
        if (view == null) return false;
        HashMap<Integer, Consumer<InventoryClickEvent>> buttons = new HashMap<>();

        view.setItem(0, InventoryUtils.getItemStack("Input.Anvil GUI", Configurations.CONFIG.getConfiguration(), TerrainerPlugin.getLanguage()));
        buttons.put(2, event -> {
            ItemStack item = event.getCurrentItem();
            ItemMeta meta = item == null ? null : item.getItemMeta();
            String input = meta == null ? "" : meta.getDisplayName();
            try {
                onInput.accept(input);
            } catch (Throwable t) {
                Terrainer.logger().log("Unable to accept input '" + input + "'for InputGetterUtil#input(HumanEntity, Consumer<String>) method:", ConsoleLogger.Level.ERROR);
                t.printStackTrace();
            }
        });
        InventoryUtils.openInventory(view.getTopInventory(), buttons, player, event -> {
            ItemStack item = event.getInventory().getItem(2);
            ItemMeta meta = item == null ? null : item.getItemMeta();
            String input = meta == null ? "" : meta.getDisplayName();
            try {
                onInput.accept(input);
            } catch (Throwable t) {
                Terrainer.logger().log("Unable to accept input '" + input + "' for InputGetterUtil#input(HumanEntity, Consumer<String>) method:", ConsoleLogger.Level.ERROR);
                t.printStackTrace();
            }
        });
        return true;
    }

    private static void askInChat(@NotNull HumanEntity player, @NotNull Consumer<String> onInput) {
        player.closeInventory();
        MessageSender lang = TerrainerPlugin.getLanguage();
        Plugin plugin = Bukkit.getPluginManager().getPlugin("Terrainer");

        if (plugin == null) {
            Terrainer.logger().log("Unable to start input getting in chat because Terrainer is not enabled or could not be found.", ConsoleLogger.Level.ERROR);
            return;
        }

        UUID id = player.getUniqueId();
        long interval = Configurations.CONFIG.getConfiguration().getNumber("Input.Chat Interval").orElse(200).longValue();

        lang.send(player, lang.get("Input.Ask").replace("<time>", Double.toString(Math.floor(interval / 20.0))));
        inputListeningPlayers.put(id, new WaitingInput(onInput, Bukkit.getScheduler().runTaskLater(plugin, () -> {
            inputListeningPlayers.remove(id);
            Player onlinePlayer = Bukkit.getPlayer(id);
            if (onlinePlayer == null) return;
            try {
                TerrainerPlugin.getLanguage().send(onlinePlayer, TerrainerPlugin.getLanguage().get("Input.Success").replace("<input>", ""));
                onInput.accept("");
            } catch (Throwable t) {
                Terrainer.logger().log("Unable to accept input '' for InputGetterUtil#input(HumanEntity, Consumer<String>) method:", ConsoleLogger.Level.ERROR);
                t.printStackTrace();
            }
        }, interval)));
        Bukkit.getPluginManager().registerEvents(chatListener, plugin);
    }

    private record WaitingInput(@NotNull Consumer<String> onInput, @NotNull BukkitTask task) {
    }
}
