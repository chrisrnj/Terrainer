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

package com.epicnicity322.terrainer.bukkit.listener;

import com.epicnicity322.epicpluginlib.bukkit.lang.MessageSender;
import com.epicnicity322.epicpluginlib.bukkit.util.InventoryUtils;
import com.epicnicity322.terrainer.bukkit.TerrainerPlugin;
import com.epicnicity322.terrainer.bukkit.command.InfoCommand;
import com.epicnicity322.terrainer.bukkit.util.BukkitPlayerUtil;
import com.epicnicity322.terrainer.core.Coordinate;
import com.epicnicity322.terrainer.core.WorldCoordinate;
import com.epicnicity322.terrainer.core.config.Configurations;
import com.epicnicity322.terrainer.core.terrain.TerrainManager;
import com.epicnicity322.terrainer.core.util.PlayerUtil;
import com.epicnicity322.yamlhandler.Configuration;
import org.bukkit.FluidCollisionMode;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

public final class SelectionListener implements Listener {
    private static @NotNull ItemStack selector = InventoryUtils.getItemStack("Selector Wand", Configurations.CONFIG.getConfiguration(), TerrainerPlugin.getLanguage());
    private static @NotNull ItemStack info = InventoryUtils.getItemStack("Info Wand", Configurations.CONFIG.getConfiguration(), TerrainerPlugin.getLanguage());
    private static boolean selectorUnique = true;
    private static boolean infoUnique = false;
    private static boolean leftAndRight = false;
    private static boolean cancelSelectorInteraction = true;
    private static boolean cancelInfoInteraction = true;
    private static boolean farSelection = true;
    private static int farSelectionDistance = 20;
    private final @NotNull NamespacedKey selectorWandKey;
    private final @NotNull NamespacedKey infoWandKey;
    private final @NotNull InfoCommand infoCommand;

    public SelectionListener(@NotNull NamespacedKey selectorWandKey, @NotNull NamespacedKey infoWandKey, @NotNull InfoCommand infoCommand) {
        this.selectorWandKey = selectorWandKey;
        this.infoWandKey = infoWandKey;
        this.infoCommand = infoCommand;
    }

    /**
     * The item used to select diagonals to claim a terrain.
     *
     * @return The selector wand item, as set in config.
     */
    public static @NotNull ItemStack getSelectorWand() {
        return selector;
    }

    /**
     * The item used to see information if there is a terrain on the block clicked.
     *
     * @return The info wand item, as set in config.
     */
    public static @NotNull ItemStack getInfoWand() {
        return info;
    }

    /**
     * Refresh the items to the current settings in {@link Configurations#CONFIG}.
     */
    @ApiStatus.Internal
    public static void reloadItems(@NotNull NamespacedKey selectorWandKey, @NotNull NamespacedKey infoWandKey) {
        Configuration config = Configurations.CONFIG.getConfiguration();
        selector = InventoryUtils.getItemStack("Selector Wand", config, TerrainerPlugin.getLanguage());
        selectorUnique = config.getBoolean("Selector Wand.Unique").orElse(true);
        if (selectorUnique) {
            ItemMeta meta = selector.getItemMeta();
            meta.getPersistentDataContainer().set(selectorWandKey, PersistentDataType.INTEGER, 1);
            selector.setItemMeta(meta);
        }
        leftAndRight = config.getBoolean("Selector Wand.Left And Right Click").orElse(false);
        info = InventoryUtils.getItemStack("Info Wand", config, TerrainerPlugin.getLanguage());
        infoUnique = config.getBoolean("Info Wand.Unique").orElse(false);
        cancelSelectorInteraction = config.getBoolean("Selector Wand.Cancel Interaction").orElse(true);
        cancelInfoInteraction = config.getBoolean("Info Wand.Cancel Interaction").orElse(false);
        farSelection = config.getBoolean("Selector Wand.Far Selection.Enabled").orElse(true);
        farSelectionDistance = config.getNumber("Selector Wand.Far Selection.Max Distance").orElse(20).intValue();
        if (infoUnique) {
            ItemMeta meta = info.getItemMeta();
            meta.getPersistentDataContainer().set(infoWandKey, PersistentDataType.INTEGER, 1);
            info.setItemMeta(meta);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onInteract(PlayerInteractEvent event) {
        ItemStack hand = event.getItem();
        if (hand == null) return;
        Action action = event.getAction();
        Player player = event.getPlayer();
        Block block = event.getClickedBlock();

        if (action == Action.PHYSICAL) return;
        boolean left = action == Action.LEFT_CLICK_AIR || action == Action.LEFT_CLICK_BLOCK;

        if (leftAndRight || !left) {
            if (isWand(hand, selector, selectorUnique, selectorWandKey) && player.hasPermission("terrainer.select.wand")) {
                if (cancelSelectorInteraction) event.setCancelled(true);
                if (block == null || block.isEmpty()) if (farSelection) {
                    block = player.getTargetBlockExact(farSelectionDistance, FluidCollisionMode.NEVER);
                    if (block == null) return;
                } else return;

                MessageSender lang = TerrainerPlugin.getLanguage();
                BukkitPlayerUtil util = TerrainerPlugin.getPlayerUtil();
                World world = block.getWorld();

                if (!player.hasPermission("terrainer.world." + world.getName().toLowerCase(Locale.ROOT))) {
                    lang.send(player, lang.get("Select.Error.World"));
                    return;
                }

                WorldCoordinate[] selections = PlayerUtil.selections(player.getUniqueId());

                // Clear if we are using only right clicks and both points are already selected.
                if (!leftAndRight && selections[0] != null && selections[1] != null) {
                    selections[0] = null;
                    selections[1] = null;
                    util.removeMarkers(player);
                }

                boolean first = leftAndRight ? left : selections[0] == null;
                int x = block.getX(), y = first ? Integer.MIN_VALUE : Integer.MAX_VALUE, z = block.getZ();

                selections[first ? 0 : 1] = new WorldCoordinate(world.getUID(), new Coordinate(x, y, z));

                // Showing markers.
                util.showMarkers(player, block.getY(), null);

                lang.send(player, lang.get("Select.Success." + (first ? "First" : "Second")).replace("<world>", block.getWorld().getName()).replace("<coord>", "X: " + x + ", Z: " + z));

                if (selections[0] != null && selections[1] != null) {
                    lang.send(player, lang.get("Select.Success.Suggest").replace("<label>", "tr"));
                }
            }
        }

        if (action != Action.RIGHT_CLICK_BLOCK || block == null) return;

        if (isWand(hand, info, infoUnique, infoWandKey) && player.hasPermission("terrainer.info.wand")) {
            if (cancelInfoInteraction) event.setCancelled(true);
            infoCommand.sendInfo(player, TerrainManager.terrainsAt(block.getWorld().getUID(), block.getX(), block.getY(), block.getZ()), block.getLocation());
        }
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onItemHeld(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        ItemStack hand = player.getInventory().getItem(event.getNewSlot());
        if (hand == null) return;

        if (isWand(hand, selector, selectorUnique, selectorWandKey)) {
            TerrainerPlugin.getPlayerUtil().showMarkers(player);
        }
    }

    private boolean isWand(@NotNull ItemStack hand, @NotNull ItemStack item, boolean unique, @NotNull NamespacedKey key) {
        if (unique) {
            ItemMeta meta = hand.getItemMeta();
            if (meta == null) return false;
            return meta.getPersistentDataContainer().has(key, PersistentDataType.INTEGER);
        } else {
            return hand.getType() == item.getType();
        }
    }
}
