package com.epicnicity322.terrainer.bukkit.listener;

import com.epicnicity322.epicpluginlib.bukkit.lang.MessageSender;
import com.epicnicity322.epicpluginlib.bukkit.util.InventoryUtils;
import com.epicnicity322.terrainer.bukkit.TerrainerPlugin;
import com.epicnicity322.terrainer.bukkit.command.BordersCommand;
import com.epicnicity322.terrainer.bukkit.util.BukkitPlayerUtil;
import com.epicnicity322.terrainer.core.Coordinate;
import com.epicnicity322.terrainer.core.WorldCoordinate;
import com.epicnicity322.terrainer.core.config.Configurations;
import com.epicnicity322.terrainer.core.terrain.Terrain;
import com.epicnicity322.terrainer.core.terrain.TerrainManager;
import com.epicnicity322.terrainer.core.util.TerrainerUtil;
import com.epicnicity322.yamlhandler.Configuration;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.UUID;

public final class SelectionListener implements Listener {
    private static @NotNull ItemStack selector = InventoryUtils.getItemStack("Selector Wand", Configurations.CONFIG.getConfiguration(), TerrainerPlugin.getLanguage());
    private static @NotNull ItemStack info = InventoryUtils.getItemStack("Info Wand", Configurations.CONFIG.getConfiguration(), TerrainerPlugin.getLanguage());
    private static boolean selectorUnique = false;
    private static boolean infoUnique = false;
    private static boolean leftAndRight = false;
    private final @NotNull NamespacedKey selectorWandKey;
    private final @NotNull NamespacedKey infoWandKey;
    private final @NotNull BordersCommand bordersCommand;

    public SelectionListener(@NotNull NamespacedKey selectorWandKey, @NotNull NamespacedKey infoWandKey, @NotNull BordersCommand bordersCommand) {
        this.selectorWandKey = selectorWandKey;
        this.infoWandKey = infoWandKey;
        this.bordersCommand = bordersCommand;
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
        selectorUnique = config.getBoolean("Selector Wand.Unique").orElse(false);
        if (selectorUnique) {
            ItemMeta meta = selector.getItemMeta();
            meta.getPersistentDataContainer().set(selectorWandKey, PersistentDataType.INTEGER, 1);
            selector.setItemMeta(meta);
        }
        leftAndRight = config.getBoolean("Selector Wand.Left And Right Click").orElse(false);
        info = InventoryUtils.getItemStack("Info Wand", config, TerrainerPlugin.getLanguage());
        infoUnique = config.getBoolean("Info Wand.Unique").orElse(false);
        if (infoUnique) {
            ItemMeta meta = info.getItemMeta();
            meta.getPersistentDataContainer().set(infoWandKey, PersistentDataType.INTEGER, 1);
            info.setItemMeta(meta);
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onInteract(PlayerInteractEvent event) {
        ItemStack hand = event.getItem();
        if (hand == null) return;
        boolean left = true;

        if (event.getAction() == Action.RIGHT_CLICK_BLOCK) left = false;
        else if (!leftAndRight || event.getAction() != Action.LEFT_CLICK_BLOCK) return;

        Player player = event.getPlayer();
        Block block = event.getClickedBlock();
        if (block == null) return;

        if (!event.isCancelled() && isWand(hand, selector, selectorUnique, selectorWandKey) && player.hasPermission("terrainer.select.wand")) {
            if (Configurations.CONFIG.getConfiguration().getBoolean("Selector Wand.Cancel Interaction").orElse(false)) {
                event.setCancelled(true);
            }

            WorldCoordinate[] selections = TerrainManager.getSelection(player.getUniqueId());
            BukkitPlayerUtil util = TerrainerPlugin.getPlayerUtil();

            if (!leftAndRight && selections[0] != null && selections[1] != null) {
                selections[0] = null;
                selections[1] = null;
                util.removeMarker(player, true);
                util.removeMarker(player, false);
            }

            boolean first = leftAndRight ? left : selections[0] == null;
            int x = block.getX(), y = first ? Integer.MIN_VALUE : Integer.MAX_VALUE, z = block.getZ();
            MessageSender lang = TerrainerPlugin.getLanguage();

            selections[first ? 0 : 1] = new WorldCoordinate(block.getWorld().getUID(), new Coordinate(x, y, z));

            //Showing markers
            util.showMarker(player, first, x, block.getY(), z);
            WorldCoordinate other = selections[first ? 1 : 0];
            if (other != null) {
                int otherY = (int) other.coordinate().y();
                util.showMarker(player, !first, (int) other.coordinate().x(), otherY == (!first ? Integer.MIN_VALUE : Integer.MAX_VALUE) ? block.getY() : otherY, (int) other.coordinate().z());
            }

            lang.send(player, lang.get("Select.Success." + (first ? "First" : "Second")).replace("<world>", block.getWorld().getName()).replace("<coord>", "X: " + x + ", Z: " + z));

            if (selections[0] != null && selections[1] != null) {
                lang.send(player, lang.get("Select.Success.Suggest").replace("<label>", "tr"));
            }
        }

        if (!left && isWand(hand, info, infoUnique, infoWandKey) && player.hasPermission("terrainer.info.wand")) {
            if (Configurations.CONFIG.getConfiguration().getBoolean("Info Wand.Cancel Interaction").orElse(false)) {
                event.setCancelled(true);
            }
            sendInfo(player, block);
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

    private void sendInfo(@NotNull Player player, @NotNull Block block) {
        MessageSender lang = TerrainerPlugin.getLanguage();
        HashSet<Terrain> terrains = TerrainManager.getTerrainsAt(block.getWorld().getUID(), block.getX(), block.getY(), block.getZ());

        if (terrains.isEmpty()) {
            lang.send(player, lang.get("Info.Error.No Terrains"));
            return;
        }
        if (!player.hasPermission("terrainer.info.console")) {
            terrains.removeIf(t -> t.owner() == null);
        }
        if (!player.hasPermission("terrainer.info.others")) {
            UUID playerId = player.getUniqueId();
            terrains.removeIf(t -> !TerrainerPlugin.getPlayerUtil().hasAnyRelations(playerId, t));
        }
        if (terrains.isEmpty()) {
            lang.send(player, lang.get("Info.Error.No Relating Terrains"));
            return;
        }

        boolean showBorders = false;
        BukkitPlayerUtil util = TerrainerPlugin.getPlayerUtil();

        for (Terrain t : terrains) {
            World w = Bukkit.getWorld(t.world());
            String worldName = w == null ? t.world().toString() : w.getName();
            Coordinate min = t.minDiagonal();
            Coordinate max = t.maxDiagonal();
            if (!t.borders().isEmpty()) showBorders = true;

            lang.send(player, lang.get("Info.Text").replace("<name>", t.name()).replace("<id>", t.id().toString()).replace("<owner>", util.getOwnerName(t.owner())).replace("<desc>", t.description()).replace("<date>", t.creationDate().format(DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss"))).replace("<area>", Double.toString(t.area())).replace("<world>", worldName).replace("<x1>", Double.toString(min.x())).replace("<y1>", Double.toString(min.y())).replace("<z1>", Double.toString(min.z())).replace("<x2>", Double.toString(max.x())).replace("<y2>", Double.toString(max.y())).replace("<z2>", Double.toString(max.z())).replace("<mods>", TerrainerUtil.listToString(t.moderators().view(), util::getOwnerName)).replace("<members>", TerrainerUtil.listToString(t.members().view(), util::getOwnerName)).replace("<flags>", TerrainerUtil.listToString(t.flags().view().keySet(), id -> id)));
        }

        if (showBorders) bordersCommand.showBorders(player, terrains);
    }
}
