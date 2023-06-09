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

package com.epicnicity322.terrainer.bukkit.listener;

import com.epicnicity322.epicpluginlib.bukkit.lang.MessageSender;
import com.epicnicity322.epicpluginlib.bukkit.reflection.ReflectionUtil;
import com.epicnicity322.terrainer.bukkit.TerrainerPlugin;
import com.epicnicity322.terrainer.bukkit.command.BordersCommand;
import com.epicnicity322.terrainer.bukkit.event.terrain.TerrainEnterEvent;
import com.epicnicity322.terrainer.bukkit.event.terrain.TerrainLeaveEvent;
import com.epicnicity322.terrainer.core.config.Configurations;
import com.epicnicity322.terrainer.core.terrain.Flag;
import com.epicnicity322.terrainer.core.terrain.Flags;
import com.epicnicity322.terrainer.core.terrain.Terrain;
import com.epicnicity322.terrainer.core.terrain.TerrainManager;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.apache.commons.lang3.mutable.Mutable;
import org.bukkit.*;
import org.bukkit.block.*;
import org.bukkit.block.data.Directional;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.*;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.*;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.player.*;
import org.bukkit.event.vehicle.VehicleDestroyEvent;
import org.bukkit.event.world.StructureGrowEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spigotmc.event.entity.EntityMountEvent;

import java.util.*;

public final class ProtectionsListener implements Listener {
    // Enemy interface was added in 1.19.3.
    private static final @Nullable Class<?> enemyInterface = ReflectionUtil.getClass("org.bukkit.entity.Enemy");
    // Paper
    private static final boolean getOriginMethod = ReflectionUtil.getMethod(Entity.class, "getOrigin") != null;
    private static final @NotNull HashMap<UUID, BossBarTask> bossBarTasks = new HashMap<>();
    private final @NotNull MessageSender lang = TerrainerPlugin.getLanguage();
    private final @NotNull TerrainerPlugin plugin;
    private final @NotNull BordersCommand bordersCommand;
    private final @NotNull NamespacedKey resetFly;

    public ProtectionsListener(@NotNull TerrainerPlugin plugin, @NotNull BordersCommand bordersCommand) {
        this.plugin = plugin;
        this.bordersCommand = bordersCommand;
        resetFly = new NamespacedKey(plugin, "reset-fly-on-leave");
    }

    /**
     * Entities that fight back the players.
     */
    private static boolean isEnemy(@NotNull Entity entity) {
        if (enemyInterface != null) {
            return enemyInterface.isAssignableFrom(entity.getClass()) || entity instanceof Wolf wolf && wolf.isAngry();
        } else {
            return switch (entity.getType()) {
                case BLAZE, CAVE_SPIDER, CREEPER, DROWNED, ELDER_GUARDIAN, ENDER_DRAGON, ENDERMAN, ENDERMITE, EVOKER, GHAST, GIANT, GUARDIAN, HOGLIN, HUSK, ILLUSIONER, MAGMA_CUBE, PHANTOM, PIGLIN, PIGLIN_BRUTE, PILLAGER, RAVAGER, SHULKER, SILVERFISH, SKELETON, SLIME, SPIDER, STRAY, VEX, VINDICATOR, WARDEN, WITCH, WITHER, WITHER_SKELETON, ZOGLIN, ZOMBIE, ZOMBIE_VILLAGER, ZOMBIFIED_PIGLIN ->
                        true;
                default -> entity instanceof Wolf wolf && wolf.isAngry();
            };
        }
    }

    /**
     * Items that when you use, they place a block.
     */
    private static boolean isBuildingItem(@NotNull Material type) {
        if (type.isBlock()) return true;

        return switch (type) {
            case ARMOR_STAND, GLOW_ITEM_FRAME, ITEM_FRAME, PAINTING, STRING, BUCKET, AXOLOTL_BUCKET, COD_BUCKET, LAVA_BUCKET, POWDER_SNOW_BUCKET, PUFFERFISH_BUCKET, SALMON_BUCKET, TADPOLE_BUCKET, TROPICAL_FISH_BUCKET, WATER_BUCKET, CHEST_MINECART, COMMAND_BLOCK_MINECART, FURNACE_MINECART, HOPPER_MINECART, TNT_MINECART ->
                    true;
            default -> type.name().endsWith("BOAT");
        };
    }

    private static boolean isContainer(@NotNull Material type) {
        return switch (type) {
            case BARREL, BLAST_FURNACE, BREWING_STAND, CHEST, DISPENSER, DROPPER, FURNACE, HOPPER, SHULKER_BOX, SMOKER, TRAPPED_CHEST ->
                    true;
            default -> false;
        };
    }

    private static boolean isPrepareBlock(@NotNull Material type) {
        return switch (type) {
            case CARTOGRAPHY_TABLE, CRAFTING_TABLE, ENCHANTING_TABLE, GRINDSTONE, LOOM, SMITHING_TABLE, STONECUTTER ->
                    true;
            default -> false;
        };
    }

    private static @NotNull Location getOrigin(@NotNull Entity entity) {
        if (getOriginMethod) {
            return entity.getOrigin() == null ? entity.getLocation() : entity.getOrigin();
        } else {
            return entity.getLocation();
        }
    }

    private static boolean deny(@NotNull Terrain terrain, @NotNull Flag<Boolean> flag) {
        Boolean state;
        return (state = terrain.flags().getData(flag)) != null && !state;
    }

    /**
     * Cancels the event and notifies the player if all conditions meet:
     * <li>The location is within a terrain;</li>
     * <li>The player has no relations (not a member) to the terrain;</li>
     * <li>The terrain does not have the specified flag.</li>
     */
    private static boolean handleProtection(@NotNull Cancellable event, @NotNull Entity player, @NotNull Location loc, @NotNull Flag<Boolean> flag, @Nullable String message) {
        int x = loc.getBlockX();
        int y = loc.getBlockY();
        int z = loc.getBlockZ();
        UUID world = loc.getWorld().getUID();

        for (Terrain terrain : TerrainManager.terrains()) {
            if (!terrain.world().equals(world) || !terrain.isWithin(x, y, z)) continue;
            if (!TerrainerPlugin.getPlayerUtil().hasAnyRelations(player.getUniqueId(), terrain) && deny(terrain, flag)) {
                event.setCancelled(true);
                if (message != null) {
                    MessageSender lang = TerrainerPlugin.getLanguage();
                    lang.send(player, lang.get(message));
                }
                return true;
            }
        }
        return false;
    }

    /**
     * Cancels the event if the provided flag is set to deny in a terrain found in the location.
     */
    private static void handleProtection(@NotNull Cancellable event, @NotNull UUID world, int x, int y, int z, @NotNull Flag<Boolean> flag) {
        for (Terrain terrain : TerrainManager.terrains()) {
            if (!terrain.world().equals(world) || !terrain.isWithin(x, y, z)) continue;
            if (deny(terrain, flag)) {
                event.setCancelled(true);
                break;
            }
        }
    }

    /**
     * Called everytime a player receives vulnerability.
     */
    private static boolean handleVulnerability(@NotNull EntityDamageEvent event, @Nullable Entity pvp) {
        if (event.getEntityType() != EntityType.PLAYER) return false;

        Location loc = event.getEntity().getLocation();
        int x = loc.getBlockX();
        int y = loc.getBlockY();
        int z = loc.getBlockZ();
        UUID world = loc.getWorld().getUID();

        for (Terrain terrain : TerrainManager.terrains()) {
            if (!terrain.world().equals(world) || !terrain.isWithin(x, y, z)) continue;
            if (deny(terrain, Flags.VULNERABILITY)) {
                event.setCancelled(true);
                return true;
            } else if (pvp != null && deny(terrain, Flags.PVP)) {
                MessageSender lang = TerrainerPlugin.getLanguage();
                lang.send(pvp, lang.get("Protections.PvP"));
                event.setCancelled(true);
                return true;
            }
        }
        return true;
    }

    /**
     * Removes blocks from explosion if all conditions meet:
     * <li>The exploded blocks are within a terrain;</li>
     * <li>The terrain has {@link Flags#EXPLOSION_DAMAGE} flag OR the origin is outside the terrain and the terrain
     * does not have {@link Flags#BUILD}</li>
     */
    private static void handleExplosion(@NotNull Location origin, @NotNull List<Block> exploded) {
        int x = origin.getBlockX();
        int y = origin.getBlockY();
        int z = origin.getBlockZ();
        UUID world = origin.getWorld().getUID();

        for (Terrain terrain : TerrainManager.terrains()) {
            if (!terrain.world().equals(world)) continue;
            if (deny(terrain, Flags.EXPLOSION_DAMAGE) || (!terrain.isWithin(x, y, z) && deny(terrain, Flags.BUILD))) {
                exploded.removeIf(block -> terrain.isWithin(block.getX(), block.getY(), block.getZ()));
            }
        }
    }

    /**
     * Checks if the victim is within a terrain and if the terrain has specific flags to allow or prevent the victim
     * from being damaged by the explosion. The following scenarios are considered:
     *
     * <li>If the victim is not a mob, it will only be damaged if the terrain does not have {@link Flags#EXPLOSION_DAMAGE} flag.</li>
     * <li>If the explosion happened outside the terrain, the entity will be damaged only if the terrain has a flag that
     * allows it, such as {@link Flags#ARMOR_STANDS}, {@link Flags#BUILD}, {@link Flags#CONTAINERS},
     * {@link Flags#ENTITY_HARM}, or {@link Flags#ITEM_FRAMES}, depending on the entity type. Conversely, the entity
     * will not be damaged if the terrain has a flag that disallows it, such as {@link Flags#ENEMY_HARM}.</li>
     */
    private static void handleEntityExplosion(@NotNull Cancellable event, @NotNull Location damager, @NotNull Entity victim) {
        Location loc = victim.getLocation();
        int x = loc.getBlockX();
        int y = loc.getBlockY();
        int z = loc.getBlockZ();
        int originX = damager.getBlockX();
        int originY = damager.getBlockY();
        int originZ = damager.getBlockZ();
        UUID world = loc.getWorld().getUID();

        Flag<Boolean> flag = null;
        boolean build = false;
        boolean mob = false;

        for (Terrain terrain : TerrainManager.terrains()) {
            if (!terrain.world().equals(world) || !terrain.isWithin(x, y, z)) continue;
            if (flag == null) {
                switch (victim.getType()) {
                    case ARMOR_STAND -> {
                        flag = Flags.ARMOR_STANDS;
                        build = true;
                    }
                    case MINECART_CHEST, MINECART_HOPPER -> {
                        flag = Flags.CONTAINERS;
                        build = true;
                    }
                    default -> {
                        if (isEnemy(victim)) {
                            flag = Flags.ENEMY_HARM;
                            mob = true;
                        } else if (victim instanceof Mob) {
                            flag = Flags.ENTITY_HARM;
                            mob = true;
                        } else {
                            flag = Flags.BUILD;
                        }
                    }
                }
            }

            if (terrain.isWithin(originX, originY, originZ)) {
                if (!mob && deny(terrain, Flags.EXPLOSION_DAMAGE)) {
                    event.setCancelled(true);
                    return;
                }
            } else {
                if ((!mob && deny(terrain, Flags.EXPLOSION_DAMAGE)) || deny(terrain, flag) || (build && deny(terrain, Flags.BUILD))) {
                    event.setCancelled(true);
                    return;
                }
            }
        }
    }

    // Prevent block break if BUILD is false, if it's a container, prevent if BUILD or CONTAINERS is false.
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        if (isContainer(block.getType())) {
            if (!player.hasPermission("terrainer.bypass.build") || !player.hasPermission("terrainer.bypass.containers")) {
                UUID world = block.getWorld().getUID();

                for (Terrain terrain : TerrainManager.terrains()) {
                    if (!terrain.world().equals(world) || !terrain.isWithin(block.getX(), block.getY(), block.getZ()))
                        continue;
                    if (!TerrainerPlugin.getPlayerUtil().hasAnyRelations(player.getUniqueId(), terrain) && (deny(terrain, Flags.CONTAINERS) || deny(terrain, Flags.CONTAINERS))) {
                        event.setCancelled(true);
                        lang.send(player, lang.get("Protections.Containers"));
                        return;
                    }
                }
            }
        } else {
            if (player.hasPermission("terrainer.bypass.build")) return;
            handleProtection(event, player, block.getLocation(), Flags.BUILD, "Protections.Build");
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onBucketFill(PlayerBucketFillEvent event) {
        if (event.getPlayer().hasPermission("terrainer.bypass.build")) return;
        handleProtection(event, event.getPlayer(), event.getBlockClicked().getLocation(), Flags.BUILD, "Protections.Build");
    }

    // Prevent block place if BUILD is false.
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        if (event.getPlayer().hasPermission("terrainer.bypass.build")) return;
        handleProtection(event, event.getPlayer(), event.getBlock().getLocation(), Flags.BUILD, "Protections.Build");
    }

    // Prevent interactions of:
    // Pressure Plates if PRESSURE_PLATES is false.
    // Farmland if FARMLAND_TRAMPLE is false.
    // Levers and buttons if BUTTONS is false.
    // Doors, gates, trapdoors if DOORS is false.
    // Signs if SIGN_CLICK is false.
    // Containers if CONTAINERS is false.
    // Double chests if any of the ends are within a terrain and CONTAINERS is false.
    // Any non interactable block if the item in hand is a building block and BUILD is false.
    // Any other interactable block if the item in hand is not a building block and INTERACTIONS is false.
    @EventHandler(priority = EventPriority.LOWEST)
    public void onInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        Action action = event.getAction();
        Block block = event.getClickedBlock();
        if (block == null) return;
        Material type = block.getType();

        Flag<Boolean> flag = null;
        String message = null;

        if (action == Action.PHYSICAL) {
            UUID world = block.getWorld().getUID();

            for (Terrain terrain : TerrainManager.terrains()) {
                if (!terrain.world().equals(world) || !terrain.isWithin(block.getX(), block.getY(), block.getZ())) {
                    continue;
                }
                if (flag == null) {
                    if (player.hasPermission("terrainer.bypass.interactions")) return;
                    String typeName = type.name();
                    int underscore = typeName.lastIndexOf('_');
                    if (underscore != -1) typeName = typeName.substring(underscore + 1);
                    switch (typeName) {
                        case "PLATE" -> {
                            flag = Flags.PRESSURE_PLATES;
                            message = "Protections.Pressure Plates";
                        }
                        case "FARMLAND" -> {
                            flag = Flags.TRAMPLE;
                            message = "Protections.Farmland Trampling";
                        }
                        default -> {
                            flag = Flags.INTERACTIONS;
                            message = "Protections.Interactions";
                        }
                    }
                }
                if (!TerrainerPlugin.getPlayerUtil().hasAnyRelations(player, terrain) && deny(terrain, flag)) {
                    event.setCancelled(true);
                    lang.send(player, lang.get(message));
                    return;
                }
            }
            return;
        } else if (action != Action.RIGHT_CLICK_BLOCK) return;

        if (type == Material.CHEST || type == Material.TRAPPED_CHEST) {
            if (((Chest) block.getState()).getInventory().getHolder() instanceof DoubleChest doubleChest) {
                Location side = null;
                Chest left = (Chest) doubleChest.getLeftSide();
                Chest right = (Chest) doubleChest.getRightSide();
                Location loc = block.getLocation();

                // Getting the other side of the chest.
                if (left != null && loc.equals(left.getLocation())) {
                    if (right != null) side = right.getLocation();
                } else if (right != null && loc.equals(right.getLocation())) {
                    if (left != null) side = left.getLocation();
                }

                if (side != null) {
                    if (player.hasPermission("terrainer.bypass.containers")) return;
                    if (handleProtection(event, player, side, Flags.CONTAINERS, "Protections.Containers")) {
                        return;
                    }
                }
            }
        }

        UUID world = block.getWorld().getUID();
        Block relative = block.getRelative(event.getBlockFace());

        for (Terrain terrain : TerrainManager.terrains()) {
            if (!terrain.world().equals(world) || (!terrain.isWithin(block.getX(), block.getY(), block.getZ()) && !terrain.isWithin(relative.getX(), relative.getY(), relative.getZ()))) {
                continue;
            }
            if (flag == null) {
                String typeName = type.name();
                int underscore = typeName.lastIndexOf('_');
                if (underscore != -1) typeName = typeName.substring(underscore + 1);
                switch (typeName) {
                    case "LEVER", "BUTTON" -> {
                        if (player.hasPermission("terrainer.bypass.interactions")) return;
                        flag = Flags.BUTTONS;
                        message = "Protections.Buttons";
                    }
                    case "DOOR", "GATE", "TRAPDOOR" -> {
                        if (player.hasPermission("terrainer.bypass.interactions")) return;
                        flag = Flags.DOORS;
                        message = "Protections.Doors";
                    }
                    case "SIGN" -> {
                        if (player.hasPermission("terrainer.bypass.interactions")) return;
                        flag = Flags.SIGN_CLICK;
                        message = "Protections.Sign Click";
                    }
                    case "BARREL", "CHEST", "FURNACE", "STAND", "DISPENSER", "DROPPER", "HOPPER", /*SHULKER_*/"BOX", "SMOKER" -> {
                        if (player.hasPermission("terrainer.bypass.containers")) return;
                        flag = Flags.CONTAINERS;
                        message = "Protections.Containers";
                    }
                    case "RAIL" -> {
                        ItemStack hand = event.getItem();
                        if (hand == null) return;
                        if (hand.getType() == Material.MINECART) {
                            if (player.hasPermission("terrainer.bypass.build")) return;
                            flag = Flags.BUILD_VEHICLES;
                            message = "Protections.Build Vehicles";
                        } else if (isBuildingItem(hand.getType())) {
                            if (player.hasPermission("terrainer.bypass.build")) return;
                            flag = Flags.BUILD;
                            message = "Protections.Build";
                        } else {
                            if (player.hasPermission("terrainer.bypass.interactions")) return;
                            flag = Flags.INTERACTIONS;
                            message = "Protections.Interactions";
                        }
                    }
                    default -> {
                        if (!type.isInteractable()) {
                            ItemStack hand = event.getItem();
                            if (hand == null) return;
                            if (isBuildingItem(hand.getType())) {
                                if (player.hasPermission("terrainer.bypass.build")) return;
                                flag = Flags.BUILD;
                                message = "Protections.Build";
                            } else {
                                if (player.hasPermission("terrainer.bypass.interactions")) return;
                                flag = Flags.INTERACTIONS;
                                message = "Protections.Interactions";
                            }
                        } else {
                            if (player.hasPermission("terrainer.bypass.interactions")) return;
                            flag = Flags.INTERACTIONS;
                            message = "Protections.Interactions";
                        }
                    }
                }
            }
            if (!TerrainerPlugin.getPlayerUtil().hasAnyRelations(player, terrain) && deny(terrain, flag)) {
                event.setCancelled(true);
                lang.send(player, lang.get(message));
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPistonExtend(BlockPistonExtendEvent event) {
        Block piston = event.getBlock();
        Location loc = piston.getLocation();
        UUID world = loc.getWorld().getUID();
        List<Block> blocks = event.getBlocks();
        BlockFace dir = event.getDirection();

        if (blocks.isEmpty()) {
            Block block = piston.getRelative(dir);

            for (Terrain terrain : TerrainManager.terrains()) {
                // If an outside piston is pushing blocks into a terrain.
                if (!terrain.world().equals(world)) continue;
                boolean pistonIn = terrain.isWithin(piston.getX(), piston.getY(), piston.getZ());
                boolean blockIn = terrain.isWithin(block.getX(), block.getY(), block.getZ());
                if (((pistonIn || blockIn) && deny(terrain, Flags.PISTONS)) || (!pistonIn && blockIn && deny(terrain, Flags.OUTSIDE_PISTONS))) {
                    event.setCancelled(true);
                    return;
                }
            }
        } else {
            for (Terrain terrain : TerrainManager.terrains()) {
                if (!terrain.world().equals(world)) continue;
                boolean pistonIn = terrain.isWithin(piston.getX(), piston.getY(), piston.getZ());

                if (pistonIn && deny(terrain, Flags.PISTONS)) {
                    event.setCancelled(true);
                    return;
                }

                boolean blockIn = false;
                for (Block b : blocks) {
                    Block block = b.getRelative(dir);
                    if (terrain.isWithin(block.getX(), block.getY(), block.getZ())) {
                        blockIn = true;
                        break;
                    }
                }

                if ((blockIn && deny(terrain, Flags.PISTONS)) || (!pistonIn && blockIn && deny(terrain, Flags.OUTSIDE_PISTONS))) {
                    event.setCancelled(true);
                    return;
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPistonRetract(BlockPistonRetractEvent event) {
        List<Block> blocks = event.getBlocks();
        if (blocks.isEmpty()) return;
        Block piston = event.getBlock();
        Location loc = piston.getLocation();
        UUID world = loc.getWorld().getUID();

        for (Terrain terrain : TerrainManager.terrains()) {
            if (!terrain.world().equals(world)) continue;
            boolean pistonIn = terrain.isWithin(piston.getX(), piston.getY(), piston.getZ());
            boolean pistonsState = deny(terrain, Flags.PISTONS);

            if (pistonIn && pistonsState) {
                event.setCancelled(true);
                return;
            }

            boolean blockIn = false;
            for (Block b : blocks) {
                if (terrain.isWithin(b.getX(), b.getY(), b.getZ())) {
                    blockIn = true;
                    break;
                }
            }

            if ((blockIn && pistonsState) || (!pistonIn && blockIn && deny(terrain, Flags.OUTSIDE_PISTONS))) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (event instanceof EntityDamageByEntityEvent || event instanceof EntityDamageByBlockEvent) return;
        handleVulnerability(event, null);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onDamage(EntityDamageByBlockEvent event) {
        if (handleVulnerability(event, null)) return;
        if (event.getDamager() == null) return;
        handleEntityExplosion(event, event.getDamager().getLocation(), event.getEntity());
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        Entity damager = event.getDamager();
        Entity victim = event.getEntity();

        // If damager is an explosive, test for entity explosions.
        // If damager is not a player, or it's a projectile and the shooter is not a player, return.
        if (damager.getType() == EntityType.CREEPER) {
            if (handleVulnerability(event, null)) return;
            handleEntityExplosion(event, damager.getLocation(), victim);
            return;
        } else if (damager instanceof Explosive) {
            if (handleVulnerability(event, null)) return;
            handleEntityExplosion(event, getOrigin(damager), victim);
            return;
        } else if (!(damager instanceof Player)) {
            if (damager instanceof Projectile projectile) {
                if (projectile.getShooter() instanceof Player player) damager = player;
                else {
                    handleVulnerability(event, null);
                    return;
                }
            } else {
                handleVulnerability(event, null);
                return;
            }
        }

        if (handleVulnerability(event, damager)) return;

        Location loc = victim.getLocation();
        int x = loc.getBlockX();
        int y = loc.getBlockY();
        int z = loc.getBlockZ();
        UUID world = loc.getWorld().getUID();

        Flag<Boolean> flag1 = null;
        Flag<Boolean> flag2 = null;
        String message = null;

        for (Terrain terrain : TerrainManager.terrains()) {
            if (!terrain.world().equals(world) || !terrain.isWithin(x, y, z)) continue;
            if (flag1 == null) {
                switch (event.getEntityType()) {
                    case ARMOR_STAND -> {
                        if (damager.hasPermission("terrainer.bypass.armorstands") && damager.hasPermission("terrainer.bypass.build")) {
                            return;
                        }
                        flag1 = Flags.ARMOR_STANDS;
                        flag2 = Flags.BUILD;
                        message = "Protections.Armor Stands";
                    }
                    case GLOW_ITEM_FRAME, ITEM_FRAME -> {
                        // Item Frames only call EntityDamageByEntity event if they have an item on them. If there's no item,
                        //HangingBreakByEntityEvent is called instead.
                        if (damager.hasPermission("terrainer.bypass.itemframes")) return;
                        flag1 = Flags.ITEM_FRAMES;
                        message = "Protections.Item Frames";
                    }
                    case CHEST_BOAT, MINECART_CHEST, MINECART_HOPPER -> {
                        if (damager.hasPermission("terrainer.bypass.containers") && damager.hasPermission("terrainer.bypass.build")) {
                            return;
                        }
                        flag1 = Flags.CONTAINERS;
                        flag2 = Flags.BUILD;
                        message = "Protections.Containers";
                    }
                    case DONKEY -> {
                        if (damager.hasPermission("terrainer.bypass.containers") && damager.hasPermission("terrainer.bypass.harm")) {
                            return;
                        }
                        flag1 = Flags.CONTAINERS;
                        flag2 = Flags.ENTITY_HARM;
                        message = "Protections.Containers";
                    }
                    default -> {
                        if (isEnemy(victim)) {
                            if (damager.hasPermission("terrainer.bypass.harm")) return;
                            flag1 = Flags.ENEMY_HARM;
                            message = "Protections.Enemy Harm";
                        } else if (victim instanceof Mob) {
                            if (damager.hasPermission("terrainer.bypass.harm")) return;
                            flag1 = Flags.ENTITY_HARM;
                            message = "Protections.Harm";
                        } else {
                            if (damager.hasPermission("terrainer.bypass.build")) return;
                            flag1 = Flags.BUILD;
                            message = "Protections.Build";
                        }
                    }
                }
            }
            if (!TerrainerPlugin.getPlayerUtil().hasAnyRelations(damager.getUniqueId(), terrain) && (deny(terrain, flag1) || (flag2 != null && deny(terrain, flag2)))) {
                event.setCancelled(true);
                lang.send(damager, lang.get(message));
                return;
            }
        }
    }

    //TODO: Fix breaking by explosions
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onHangingBreak(HangingBreakByEntityEvent event) {
        Entity victim = event.getEntity();
        Entity remover = event.getRemover();
        if (remover == null) return;
        if (remover.hasPermission("terrainer.bypass.build")) return;

        if (remover.getType() == EntityType.CREEPER) {
            handleEntityExplosion(event, remover.getLocation(), victim);
            return;
        } else if (remover instanceof Explosive) {
            handleEntityExplosion(event, getOrigin(remover), victim);
            return;
        } else if (!(remover instanceof Player)) {
            if (remover instanceof Projectile projectile) {
                if (projectile.getShooter() instanceof Player player) remover = player;
                else return;
            } else return;
        }

        handleProtection(event, remover, victim.getLocation(), Flags.BUILD, "Protections.Build");
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onInteractAtEntity(PlayerInteractAtEntityEvent event) {
        onInteractEntity(event);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onInteractEntity(PlayerInteractEntityEvent event) {
        Player player = event.getPlayer();
        Location loc = event.getRightClicked().getLocation();
        int x = loc.getBlockX();
        int y = loc.getBlockY();
        int z = loc.getBlockZ();
        UUID world = loc.getWorld().getUID();

        Flag<Boolean> flag1 = null;
        Flag<Boolean> flag2 = null;
        String message = null;

        for (Terrain terrain : TerrainManager.terrains()) {
            if (!terrain.world().equals(world) || !terrain.isWithin(x, y, z)) continue;
            if (flag1 == null) {
                switch (event.getRightClicked().getType()) {
                    case ARMOR_STAND -> {
                        if (player.hasPermission("terrainer.bypass.armorstands")) return;
                        flag1 = Flags.ARMOR_STANDS;
                        message = "Protections.Armor Stands";
                    }
                    case GLOW_ITEM_FRAME, ITEM_FRAME -> {
                        if (player.hasPermission("terrainer.bypass.itemframes")) return;
                        flag1 = Flags.ITEM_FRAMES;
                        message = "Protections.Item Frames";
                    }
                    case MINECART_CHEST, MINECART_HOPPER -> {
                        if (player.hasPermission("terrainer.bypass.containers")) return;
                        flag1 = Flags.CONTAINERS;
                        message = "Protections.Containers";
                    }
                    case CHEST_BOAT, DONKEY -> {
                        flag1 = Flags.CONTAINERS;
                        flag2 = Flags.ENTER_VEHICLES;
                        message = "Protections.Containers";
                    }
                    case BOAT, MINECART -> {
                        if (player.hasPermission("terrainer.bypass.entervehicles")) return;
                        flag1 = Flags.ENTER_VEHICLES;
                        message = "Protections.Enter Vehicles";
                    }
                    case CAMEL, HORSE, LLAMA, MULE, PIG, SKELETON_HORSE, STRIDER, TRADER_LLAMA, ZOMBIE_HORSE -> {
                        if (player.getInventory().getItemInMainHand().getType() == Material.AIR && player.getInventory().getItemInOffHand().getType() == Material.AIR) {
                            if (player.hasPermission("terrainer.bypass.entervehicles")) return;
                            flag1 = Flags.ENTER_VEHICLES;
                            message = "Protections.Enter Vehicles";
                        } else {
                            if (player.hasPermission("terrainer.bypass.entityinteractions")) return;
                            flag1 = Flags.ENTITY_INTERACTIONS;
                            message = "Protections.Entity Interactions";
                        }
                    }
                    default -> {
                        if (player.hasPermission("terrainer.bypass.entityinteractions")) return;
                        flag1 = Flags.ENTITY_INTERACTIONS;
                        message = "Protections.Entity Interactions";
                    }
                }
            }
            if (!TerrainerPlugin.getPlayerUtil().hasAnyRelations(player.getUniqueId(), terrain) && (deny(terrain, flag1) || (flag2 != null && deny(terrain, flag2)))) {
                event.setCancelled(true);
                lang.send(player, lang.get(message));
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        Flag<Boolean> flag;

        switch (event.getSpawnReason()) {
            case NATURAL, DEFAULT, NETHER_PORTAL, RAID, REINFORCEMENTS, DUPLICATION, ENDER_PEARL, JOCKEY, MOUNT, PATROL, TRAP, SILVERFISH_BLOCK ->
                    flag = Flags.MOB_SPAWN;
            case SPAWNER -> flag = Flags.SPAWNERS;
            default -> {
                return;
            }
        }

        Location loc = event.getLocation();
        handleProtection(event, loc.getWorld().getUID(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ(), flag);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onItemPickup(EntityPickupItemEvent event) {
        Entity entity = event.getEntity();
        if (entity.getType() != EntityType.PLAYER || entity.hasPermission("terrainer.bypass.pickup")) return;
        handleProtection(event, entity, event.getItem().getLocation(), Flags.ITEM_PICKUP, "Protections.Pickup");
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onItemDrop(PlayerDropItemEvent event) {
        if (event.getPlayer().hasPermission("terrainer.bypass.drop")) return;
        handleProtection(event, event.getPlayer(), event.getItemDrop().getLocation(), Flags.ITEM_DROP, "Protections.Drop");
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onLiquidFlow(BlockFromToEvent event) {
        Block from = event.getBlock();
        Block to = event.getToBlock();
        UUID world = to.getWorld().getUID();

        for (Terrain terrain : TerrainManager.terrains()) {
            if (!terrain.world().equals(world) || !terrain.isWithin(to.getX(), to.getY(), to.getZ())) continue;
            if (deny(terrain, Flags.LIQUID_FLOW) || (!terrain.isWithin(from.getX(), from.getY(), from.getZ()) && deny(terrain, Flags.BUILD))) {
                event.setCancelled(true);
                break;
            }
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onBlockExplode(BlockExplodeEvent event) {
        handleExplosion(event.getBlock().getLocation(), event.blockList());
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onEntityExplode(EntityExplodeEvent event) {
        handleExplosion(getOrigin(event.getEntity()), event.blockList());
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onBlockBurn(BlockBurnEvent event) {
        Block from = event.getIgnitingBlock();
        Block to = event.getBlock();
        UUID world = to.getWorld().getUID();

        if (from == null) {
            handleProtection(event, world, to.getX(), to.getY(), to.getZ(), Flags.FIRE_DAMAGE);
        } else {
            for (Terrain terrain : TerrainManager.terrains()) {
                if (!terrain.world().equals(world) || !terrain.isWithin(to.getX(), to.getY(), to.getZ())) continue;

                if (deny(terrain, Flags.FIRE_DAMAGE) || (!terrain.isWithin(from.getX(), from.getY(), from.getZ()) && deny(terrain, Flags.BUILD))) {
                    event.setCancelled(true);
                    break;
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onBlockSpread(BlockSpreadEvent event) {
        Block from = event.getSource();
        Block to = event.getBlock();
        UUID world = to.getWorld().getUID();

        for (Terrain terrain : TerrainManager.terrains()) {
            if (!terrain.world().equals(world) || !terrain.isWithin(to.getX(), to.getY(), to.getZ())) continue;

            if (!terrain.isWithin(from.getX(), from.getY(), from.getZ()) && deny(terrain, Flags.BUILD)) {
                event.setCancelled(true);
                break;
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onLeafDecay(LeavesDecayEvent event) {
        Block block = event.getBlock();
        handleProtection(event, block.getWorld().getUID(), block.getX(), block.getY(), block.getZ(), Flags.LEAF_DECAY);
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onStructureGrow(StructureGrowEvent event) {
        List<BlockState> blocks = event.getBlocks();
        Location loc = event.getLocation();
        int x = loc.getBlockX();
        int y = loc.getBlockY();
        int z = loc.getBlockZ();
        UUID world = event.getWorld().getUID();

        for (Terrain terrain : TerrainManager.terrains()) {
            if (!terrain.world().equals(world) || terrain.isWithin(x, y, z)) continue;
            if (deny(terrain, Flags.BUILD)) {
                blocks.removeIf(block -> terrain.isWithin(block.getX(), block.getY(), block.getZ()));
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onEntityBlockForm(EntityBlockFormEvent event) {
        Entity entity = event.getEntity();
        if (entity.getType() != EntityType.PLAYER) return;
        if (entity.hasPermission("terrainer.bypass.frostwalk")) return;
        handleProtection(event, entity, event.getBlock().getLocation(), Flags.FROST_WALK, null);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onEntityChangeBlock(EntityChangeBlockEvent event) {
        if (event.getEntityType() != EntityType.FALLING_BLOCK) return;
        Block block = event.getBlock();
        Location loc = getOrigin(event.getEntity());
        int x = loc.getBlockX();
        int y = loc.getBlockY();
        int z = loc.getBlockZ();
        UUID world = block.getWorld().getUID();

        for (Terrain terrain : TerrainManager.terrains()) {
            if (!terrain.world().equals(world) || !terrain.isWithin(block.getX(), block.getY(), block.getZ())) continue;
            if (!terrain.isWithin(x, y, z) && deny(terrain, Flags.BUILD)) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onEnter(TerrainEnterEvent event) {
        Terrain terrain = event.terrain();
        Player player = event.player();

        if (!player.hasPermission("terrainer.bypass.enter")) {
            if (!TerrainerPlugin.getPlayerUtil().hasAnyRelations(player, terrain) && deny(terrain, Flags.ENTER)) {
                event.setCancelled(true);
                lang.send(player, lang.get("Protections.Enter"));
                return;
            }
        }
        if ((player.isFlying() || player.getAllowFlight()) && !player.hasPermission("terrainer.bypass.fly")) {
            if (!TerrainerPlugin.getPlayerUtil().hasAnyRelations(player, terrain) && deny(terrain, Flags.FLY)) {
                if (player.getAllowFlight()) {
                    player.getPersistentDataContainer().set(resetFly, PersistentDataType.INTEGER, 1);
                }
                player.setAllowFlight(false);
                lang.send(player, lang.get("Protections.Fly"));
            }
        }
        if (player.isGliding() && !player.hasPermission("terrainer.bypass.glide")) {
            if (!TerrainerPlugin.getPlayerUtil().hasAnyRelations(player, terrain) && deny(terrain, Flags.GLIDE)) {
                event.setCancelled(true);
                lang.send(player, lang.get("Protections.Glide"));
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onLeave(TerrainLeaveEvent event) {
        Terrain terrain = event.terrain();
        Player player = event.player();

        if (player.hasPermission("terrainer.bypass.leave")) return;
        if (!TerrainerPlugin.getPlayerUtil().hasAnyRelations(player, terrain) && deny(terrain, Flags.LEAVE)) {
            event.setCancelled(true);
            lang.send(player, lang.get("Protections.Leave"));
            return;
        }

        if (player.getPersistentDataContainer().has(resetFly, PersistentDataType.INTEGER)) {
            player.getPersistentDataContainer().remove(resetFly);
            player.setAllowFlight(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onMount(EntityMountEvent event) {
        if (event.getEntity().hasPermission("terrainer.bypass.entervehicles")) return;
        handleProtection(event, event.getEntity(), event.getMount().getLocation(), Flags.ENTER_VEHICLES, "Protections.Enter Vehicles");
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onDispense(BlockDispenseEvent event) {
        Block block = event.getBlock();
        Block frontBlock = block.getRelative(((Directional) block.getBlockData()).getFacing());
        UUID world = block.getWorld().getUID();

        for (Terrain terrain : TerrainManager.terrains()) {
            if (!terrain.world().equals(world)) continue;

            boolean dispenserIn = terrain.isWithin(block.getX(), block.getY(), block.getZ());
            boolean frontIn = terrain.isWithin(frontBlock.getX(), frontBlock.getY(), frontBlock.getZ());

            if ((dispenserIn && deny(terrain, Flags.DISPENSERS)) || (!dispenserIn && frontIn && deny(terrain, Flags.OUTSIDE_DISPENSERS))) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onVehicleDestroy(VehicleDestroyEvent event) {
        //TODO: check if attacker is explosive.
        Entity attacker = event.getAttacker();
        if (attacker == null || attacker.getType() != EntityType.PLAYER) return;
        if (attacker.hasPermission("terrainer.bypass.build")) return;
        handleProtection(event, attacker, event.getVehicle().getLocation(), Flags.BUILD_VEHICLES, "Protections.Build Vehicles");
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onProjectileHit(ProjectileHitEvent event) {
        if (!getOriginMethod) return;

        Projectile projectile = event.getEntity();
        Location originLoc = projectile.getOrigin();
        if (originLoc == null) return;
        Player shooter = projectile.getShooter() instanceof Player p ? p : null;
        if (shooter != null && shooter.hasPermission("terrainer.bypass.outsideprojectiles")) return;

        int originX = originLoc.getBlockX(), originY = originLoc.getBlockY(), originZ = originLoc.getBlockZ();
        Location hitLoc = event.getHitEntity() == null ? Objects.requireNonNull(event.getHitBlock()).getLocation() : event.getHitEntity().getLocation();
        int x = hitLoc.getBlockX(), y = hitLoc.getBlockY(), z = hitLoc.getBlockZ();
        UUID world = projectile.getWorld().getUID();

        for (Terrain terrain : TerrainManager.terrains()) {
            if (!terrain.world().equals(world)) continue;

            if (!terrain.isWithin(originX, originY, originZ) && terrain.isWithin(x, y, z) && deny(terrain, Flags.OUTSIDE_PROJECTILES)) {
                if (shooter != null) lang.send(shooter, lang.get("Protections.Projectiles"));
                event.setCancelled(true);
                projectile.remove();
                return;
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onProjectileLaunch(ProjectileLaunchEvent event) {
        Player shooter = event.getEntity().getShooter() instanceof Player p ? p : null;
        if (shooter == null) {
            Location loc = event.getLocation();
            handleProtection(event, event.getEntity().getWorld().getUID(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ(), Flags.PROJECTILES);
        } else {
            if (shooter.hasPermission("terrainer.bypass.projectiles")) return;
            handleProtection(event, shooter, event.getLocation(), Flags.PROJECTILES, "Protections.Projectiles");
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onSignChange(SignChangeEvent event) {
        if (event.getPlayer().hasPermission("terrainer.bypass.signedit")) return;
        handleProtection(event, event.getPlayer(), event.getBlock().getLocation(), Flags.SIGN_EDIT, "Protections.Sign Edit");
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onToggleFlight(PlayerToggleFlightEvent event) {
        if (!event.isFlying()) return;
        Player player = event.getPlayer();
        if (player.hasPermission("terrainer.bypass.fly")) return;
        if (handleProtection(event, player, player.getLocation(), Flags.FLY, "Protections.Fly")) {
            if (player.getAllowFlight()) {
                player.getPersistentDataContainer().set(resetFly, PersistentDataType.INTEGER, 1);
            }
            player.setAllowFlight(false);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onToggleGlide(EntityToggleGlideEvent event) {
        if (!event.isGliding() || event.getEntityType() != EntityType.PLAYER) return;
        Entity player = event.getEntity();
        if (player.hasPermission("terrainer.bypass.glide")) return;
        handleProtection(event, player, player.getLocation(), Flags.GLIDE, "Protections.Glide");
    }

    @SuppressWarnings("deprecation")
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void monitorOnEnter(TerrainEnterEvent event) {
        Terrain terrain = event.terrain();
        Player player = event.player();

        Map<String, Integer> effects = terrain.flags().getData(Flags.EFFECTS);
        if (effects != null) {
            effects.forEach((effect, power) -> {
                PotionEffectType type = PotionEffectType.getByName(effect);
                if (type == null) return;
                player.addPotionEffect(new PotionEffect(type, Integer.MAX_VALUE, power, false, false));
            });
        }

        String messageLocation = terrain.flags().getData(Flags.MESSAGE_LOCATION);
        if (messageLocation != null && !(messageLocation = messageLocation.toLowerCase(Locale.ROOT)).equals("none")) {
            String message = ChatColor.translateAlternateColorCodes('&', lang.get("Enter Leave Messages Format").replace("<name>", terrain.name()).replace("<message>", terrain.description()));
            switch (messageLocation) {
                case "actionbar" ->
                        player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(message));
                case "bossbar" -> sendBar(message, player);
                case "chat" -> lang.send(player, message);
                case "title" ->
                        player.sendTitle(ChatColor.GOLD + ChatColor.translateAlternateColorCodes('&', terrain.name()), ChatColor.GRAY + ChatColor.translateAlternateColorCodes('&', terrain.description()));
            }
        }

        if (!terrain.borders().isEmpty() && Configurations.CONFIG.getConfiguration().getBoolean("Borders.On Enter").orElse(false)) {
            bordersCommand.showBorders(player, Collections.singleton(terrain));
        }
    }

    @SuppressWarnings("deprecation")
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void monitorOnLeave(TerrainLeaveEvent event) {
        Terrain terrain = event.terrain();
        Player player = event.player();

        Map<String, Integer> effects = terrain.flags().getData(Flags.EFFECTS);
        if (effects != null) {
            effects.forEach((effect, power) -> {
                PotionEffectType type = PotionEffectType.getByName(effect);
                if (type == null) return;
                player.removePotionEffect(type);
            });
        }

        String messageLocation = terrain.flags().getData(Flags.MESSAGE_LOCATION);
        if (messageLocation != null && !(messageLocation = messageLocation.toLowerCase(Locale.ROOT)).equals("none")) {
            String leaveMessage = terrain.flags().getData(Flags.LEAVE_MESSAGE);
            if (leaveMessage != null && !leaveMessage.isEmpty()) {
                String message = ChatColor.translateAlternateColorCodes('&', lang.get("Enter Leave Messages Format").replace("<name>", terrain.name()).replace("<message>", leaveMessage));
                switch (messageLocation) {
                    case "actionbar" ->
                            player.spigot().sendMessage(ChatMessageType.ACTION_BAR, TextComponent.fromLegacyText(message));
                    case "bossbar" -> sendBar(message, player);
                    case "chat" -> lang.send(player, message);
                    case "title" ->
                            player.sendTitle(ChatColor.GOLD + ChatColor.translateAlternateColorCodes('&', terrain.name()), ChatColor.GRAY + ChatColor.translateAlternateColorCodes('&', leaveMessage));
                }
            }
        }
    }

    private void sendBar(@NotNull String message, @NotNull Player player) {
        UUID playerId = player.getUniqueId();
        BossBarTask previous = bossBarTasks.get(playerId);

        if (previous == null) {
            BossBar bar = Bukkit.createBossBar(message, BarColor.RED, BarStyle.SOLID);
            Mutable<BukkitTask> task = new Mutable<>() {
                private @NotNull BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    bossBarTasks.remove(playerId);
                    bar.removePlayer(player);
                }, 100);

                @Override
                public BukkitTask getValue() {
                    return task;
                }

                @Override
                public void setValue(@NotNull BukkitTask task) {
                    this.task = task;
                }
            };
            previous = new BossBarTask(bar, task);
            bossBarTasks.put(player.getUniqueId(), previous);
            bar.addPlayer(player);
        } else {
            BossBar bar = previous.bar;
            bar.setTitle(message);
            previous.task.getValue().cancel();
            previous.task.setValue(Bukkit.getScheduler().runTaskLater(plugin, () -> {
                bossBarTasks.remove(playerId);
                bar.removePlayer(player);
            }, 100));
        }
    }

    private record BossBarTask(@NotNull BossBar bar, @NotNull Mutable<BukkitTask> task) {
    }
}
