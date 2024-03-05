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
import com.epicnicity322.epicpluginlib.bukkit.reflection.ReflectionUtil;
import com.epicnicity322.terrainer.bukkit.TerrainerPlugin;
import com.epicnicity322.terrainer.bukkit.command.BordersCommand;
import com.epicnicity322.terrainer.bukkit.event.terrain.TerrainCanEnterEvent;
import com.epicnicity322.terrainer.bukkit.event.terrain.TerrainCanLeaveEvent;
import com.epicnicity322.terrainer.bukkit.event.terrain.TerrainEnterEvent;
import com.epicnicity322.terrainer.bukkit.event.terrain.TerrainLeaveEvent;
import com.epicnicity322.terrainer.bukkit.util.BlockStateToBlockMapping;
import com.epicnicity322.terrainer.bukkit.util.TaskFactory;
import com.epicnicity322.terrainer.core.Coordinate;
import com.epicnicity322.terrainer.core.config.Configurations;
import com.epicnicity322.terrainer.core.event.TerrainEvent;
import com.epicnicity322.terrainer.core.protection.Protections;
import com.epicnicity322.terrainer.core.terrain.Flag;
import com.epicnicity322.terrainer.core.terrain.Flags;
import com.epicnicity322.terrainer.core.terrain.Terrain;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.TextComponent;
import org.apache.commons.lang3.mutable.Mutable;
import org.bukkit.*;
import org.bukkit.block.*;
import org.bukkit.block.data.Directional;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.*;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.*;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.hanging.HangingBreakEvent;
import org.bukkit.event.hanging.HangingPlaceEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.*;
import org.bukkit.event.vehicle.VehicleDestroyEvent;
import org.bukkit.event.world.PortalCreateEvent;
import org.bukkit.event.world.StructureGrowEvent;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public final class ProtectionsListener extends Protections<Player, CommandSender, Material, Block, Entity> implements Listener {
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
        super(TerrainerPlugin.getPlayerUtil(), TerrainerPlugin.getLanguage());
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

    private static boolean isPrepareBlock(@NotNull Material type) {
        return switch (type) {
            case CARTOGRAPHY_TABLE, CRAFTING_TABLE, ENCHANTING_TABLE, GRINDSTONE, LOOM, SMITHING_TABLE, STONECUTTER ->
                    true;
            default -> false;
        };
    }

    private static @NotNull Location origin(@NotNull Entity entity) {
        if (getOriginMethod) {
            return entity.getOrigin() == null ? entity.getLocation() : entity.getOrigin();
        } else {
            return entity.getLocation();
        }
    }

    private static boolean tryingToEnterVehicle(@NotNull Material mainHand, @NotNull Material offHand, boolean sneaking) {
        return !sneaking && mainHand == Material.AIR && offHand == Material.AIR;
    }

    @Override
    @SuppressWarnings("deprecation")
    protected boolean isInteractable(@NotNull Material material) {
        return material.isInteractable();
    }

    @Override
    protected boolean isPlaceable(@Nullable Material material) {
        if (material == null) return false;
        if (material.isBlock()) return true;

        return switch (material) {
            case ARMOR_STAND, AXOLOTL_BUCKET, BUCKET, CHEST_MINECART, COD_BUCKET, COMMAND_BLOCK_MINECART, END_CRYSTAL, FURNACE_MINECART, GLOW_ITEM_FRAME, HOPPER_MINECART, ITEM_FRAME, LAVA_BUCKET, MINECART, PAINTING, POWDER_SNOW_BUCKET, PUFFERFISH_BUCKET, SALMON_BUCKET, STRING, TADPOLE_BUCKET, TNT_MINECART, TROPICAL_FISH_BUCKET, WATER_BUCKET ->
                    true;
            default -> material.name().endsWith("BOAT");
        };
    }

    @Override
    protected boolean isFire(@NotNull Material material) {
        return material == Material.FIRE || material == Material.SOUL_FIRE;
    }

    @Override
    protected boolean isBoat(@NotNull Entity entity) {
        return entity.getType() == EntityType.BOAT || entity.getType() == EntityType.CHEST_BOAT;
    }

    @Override
    protected boolean isPotion(@NotNull Entity entity) {
        return entity.getType() == EntityType.SPLASH_POTION;
    }

    @Override
    protected boolean isPlayer(@NotNull Entity entity) {
        return entity.getType() == EntityType.PLAYER;
    }

    @Override
    protected @NotNull UUID world(@NotNull Block block) {
        return block.getWorld().getUID();
    }

    @Override
    protected boolean isContainer(@NotNull Material material) {
        return switch (material) {
            case BARREL, BLAST_FURNACE, BREWING_STAND, CHEST, DISPENSER, DROPPER, FURNACE, HOPPER, SMOKER, TRAPPED_CHEST, SHULKER_BOX, BLACK_SHULKER_BOX, BLUE_SHULKER_BOX, BROWN_SHULKER_BOX, CYAN_SHULKER_BOX, GRAY_SHULKER_BOX, GREEN_SHULKER_BOX, LIGHT_BLUE_SHULKER_BOX, LIGHT_GRAY_SHULKER_BOX, LIME_SHULKER_BOX, MAGENTA_SHULKER_BOX, ORANGE_SHULKER_BOX, PINK_SHULKER_BOX, PURPLE_SHULKER_BOX, RED_SHULKER_BOX, WHITE_SHULKER_BOX, YELLOW_SHULKER_BOX ->
                    true;
            default -> false;
        };
    }

    @Override
    protected int x(@NotNull Block block) {
        return block.getX();
    }

    @Override
    protected int y(@NotNull Block block) {
        return block.getY();
    }

    @Override
    protected int z(@NotNull Block block) {
        return block.getZ();
    }

    @Override
    protected @NotNull Flag<Boolean> flagEntityPlaced(@NotNull Entity entity) {
        return switch (entity.getType()) {
            case MINECART, MINECART_CHEST, MINECART_HOPPER, MINECART_COMMAND, MINECART_FURNACE, MINECART_MOB_SPAWNER ->
                    Flags.BUILD_MINECARTS;
            case MINECART_TNT -> Flags.BUILD;
            case BOAT, CHEST_BOAT -> Flags.BUILD_BOATS;
            // ARMOR_STAND, ENDER_CRYSTAL
            default -> Flags.BUILD;
        };
    }

    @Override
    protected @NotNull Flag<Boolean> flagPhysicalInteraction(@NotNull Material material) {
        String typeName = material.name();
        int underscore = typeName.lastIndexOf('_');
        if (underscore != -1) typeName = typeName.substring(underscore + 1);

        return switch (typeName) {
            case "PLATE" -> Flags.PRESSURE_PLATES;
            case "FARMLAND" -> Flags.TRAMPLE;
            default -> Flags.INTERACTIONS;
        };
    }

    @Override
    protected @NotNull Flag<Boolean> flagInteractableBlock(@NotNull Material material, @Nullable Material hand) {
        if (isContainer(material)) return Flags.CONTAINERS;
        if (isPrepareBlock(material)) return Flags.PREPARE;

        String typeName = material.name();
        int underscore = typeName.lastIndexOf('_');
        if (underscore != -1) typeName = typeName.substring(underscore + 1);

        return switch (typeName) {
            case "LEVER", "BUTTON" -> Flags.BUTTONS;
            case "DOOR", "GATE", "TRAPDOOR" -> Flags.DOORS;
            case "SIGN" -> Flags.SIGN_CLICK;
            case "CAKE" ->
                    hand == Material.FLINT_AND_STEEL || hand == Material.FIRE_CHARGE ? Flags.LIGHTERS : Flags.EAT;
            case "ANVIL" -> Flags.ANVILS;
            case "CANDLE", "CAMPFIRE" -> Flags.LIGHTERS;
            case "CAULDRON" -> Flags.CAULDRONS;
            default -> Flags.INTERACTIONS;
        };
    }

    @Override
    protected @NotNull Flag<Boolean> flagItemUse(@NotNull Material block, @Nullable Material hand) {
        if (hand == null) return Flags.INTERACTIONS;

        return switch (hand) {
            case FLINT_AND_STEEL, FIRE_CHARGE -> Flags.LIGHTERS;
            case POTION, LINGERING_POTION, SPLASH_POTION -> Flags.POTIONS;
            default -> hand.isEdible() ? Flags.EAT : Flags.INTERACTIONS;
        };
    }

    @Override
    protected @NotNull Flag<Boolean> flagEntityInteraction(@NotNull Entity entity, @NotNull Material mainHand, @NotNull Material offHand, boolean sneaking) {
        return switch (entity.getType()) {
            case ARMOR_STAND -> Flags.ARMOR_STANDS;
            case GLOW_ITEM_FRAME, ITEM_FRAME -> Flags.ITEM_FRAMES;
            case MINECART_CHEST, MINECART_HOPPER, CHEST_BOAT -> Flags.CONTAINERS;
            case DONKEY, MULE, LLAMA, TRADER_LLAMA ->
                    ((ChestedHorse) entity).isCarryingChest() ? Flags.CONTAINERS : tryingToEnterVehicle(mainHand, offHand, sneaking) ? Flags.ENTER_VEHICLES : Flags.ENTITY_INTERACTIONS;
            case BOAT, MINECART -> Flags.ENTER_VEHICLES;
            case CAMEL, HORSE, SKELETON_HORSE, ZOMBIE_HORSE ->
                    tryingToEnterVehicle(mainHand, offHand, sneaking) ? Flags.ENTER_VEHICLES : Flags.ENTITY_INTERACTIONS;
            case PIG, STRIDER ->
                    (((Steerable) entity).hasSaddle() && mainHand == Material.AIR && offHand == Material.AIR) ? Flags.ENTER_VEHICLES : Flags.ENTITY_INTERACTIONS;
            default -> Flags.ENTITY_INTERACTIONS;
        };
    }

    @Override
    protected @NotNull Flag<Boolean> flagEntityHit(@NotNull Entity entity) {
        return switch (entity.getType()) {
            case ARMOR_STAND, ITEM_FRAME, GLOW_ITEM_FRAME, PAINTING -> Flags.BUILD;
            case CHEST_BOAT, MINECART_CHEST, MINECART_HOPPER -> Flags.CONTAINERS;
            case MINECART -> Flags.BUILD_MINECARTS;
            default -> {
                if (isEnemy(entity)) {
                    yield Flags.ENEMY_HARM;
                } else if (entity instanceof Mob) {
                    yield Flags.ENTITY_HARM;
                } else {
                    yield Flags.BUILD;
                }
            }
        };
    }

    @Override
    protected @Nullable Player entityOrShooterToPlayer(@NotNull Entity entity) {
        if (entity instanceof Player player) return player;
        if (entity instanceof Projectile proj && proj.getShooter() instanceof Player player) return player;
        return null;
    }

    @Override
    protected @NotNull Coordinate entityOrigin(@NotNull Entity entity) {
        Location loc = origin(entity);
        return new Coordinate(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (!blockBreak(block.getWorld().getUID(), block.getX(), block.getY(), block.getZ(), event.getPlayer(), block.getType()))
            event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onBucketFill(PlayerBucketFillEvent event) {
        Block block = event.getBlock();
        if (!bucketFill(block.getWorld().getUID(), block.getX(), block.getY(), block.getZ(), event.getPlayer()))
            event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onBucketEmpty(PlayerBucketEmptyEvent event) {
        Block block = event.getBlock();
        if (!bucketEmpty(block.getWorld().getUID(), block.getX(), block.getY(), block.getZ(), event.getPlayer()))
            event.setCancelled(true);
    }

    // Prevent block place if BUILD is false.
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent event) {
        Block block = event.getBlock();
        if (!blockPlace(block.getWorld().getUID(), block.getX(), block.getY(), block.getZ(), event.getPlayer(), block.getType()))
            event.setCancelled(true);
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
        Block block = event.getClickedBlock();
        if (block == null) return;
        UUID world = block.getWorld().getUID();
        Player player = event.getPlayer();
        Action action = event.getAction();

        if (action == Action.PHYSICAL) {
            if (!physicalInteract(world, block.getX(), block.getY(), block.getZ(), player, block.getType()))
                event.setCancelled(true);
            return;
        } else if (action != Action.RIGHT_CLICK_BLOCK) return;

        Material hand = event.getItem() == null ? null : event.getItem().getType();

        if (!rightClickInteract(world, block.getX(), block.getY(), block.getZ(), player, block.getType(), hand))
            event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onEntityPlace(EntityPlaceEvent event) {
        if (event.getPlayer() == null) return;
        Location loc = event.getEntity().getLocation();
        if (!placeEntity(event.getEntity().getWorld().getUID(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ(), event.getPlayer(), event.getEntity()))
            event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onInteractAtEntity(PlayerInteractAtEntityEvent event) {
        onInteractEntity(event);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onInteractEntity(PlayerInteractEntityEvent event) {
        Player player = event.getPlayer();
        Entity entity = event.getRightClicked();
        Location loc = entity.getLocation();
        if (!entityInteraction(entity.getWorld().getUID(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ(), player, entity, player.getInventory().getItemInMainHand().getType(), player.getInventory().getItemInOffHand().getType()))
            event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onItemPickup(EntityPickupItemEvent event) {
        if (event.getEntityType() != EntityType.PLAYER) return;
        Item item = event.getItem();
        Location loc = item.getLocation();
        if (!itemPickup(item.getWorld().getUID(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ(), (Player) event.getEntity()))
            event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onItemDrop(PlayerDropItemEvent event) {
        Item item = event.getItemDrop();
        Location loc = item.getLocation();
        if (!itemDrop(item.getWorld().getUID(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ(), event.getPlayer()))
            event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onLeafDecay(LeavesDecayEvent event) {
        Block block = event.getBlock();
        if (!leafDecay(block.getWorld().getUID(), block.getX(), block.getY(), block.getZ())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onInventoryOpen(InventoryOpenEvent event) {
        InventoryHolder holder = event.getInventory().getHolder();

        if (holder instanceof DoubleChest chest) {
            Chest left = (Chest) chest.getLeftSide();
            Chest right = (Chest) chest.getRightSide();
            if (left == null || right == null) return;
            if (!doubleChestOpen(left.getWorld().getUID(), left.getX(), left.getY(), left.getZ(), right.getX(), right.getY(), right.getZ(), (Player) event.getPlayer()))
                event.setCancelled(true);
        } else if (holder instanceof Container container) {
            if (!containerOpen(container.getWorld().getUID(), container.getX(), container.getY(), container.getZ(), (Player) event.getPlayer()))
                event.setCancelled(true);
        } else if (holder instanceof ChestedHorse entity) {
            Location loc = entity.getLocation();
            if (!containerOpen(entity.getWorld().getUID(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ(), (Player) event.getPlayer()))
                event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onEntityChangeBlock(EntityChangeBlockEvent event) {
        if (event.getEntityType() != EntityType.FALLING_BLOCK || event.getTo() == Material.AIR) return;
        Block block = event.getBlock();
        Location loc = origin(event.getEntity());
        if (!fallingBlockFall(block.getWorld().getUID(), block.getX(), block.getY(), block.getZ(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ()))
            event.setCancelled(true);
    }

    // Priority LOW to allow other plugins to add more blocks in LOWEST.
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onSpongeAbsorb(SpongeAbsorbEvent event) {
        // Converting BlockState list to Block.
        List<Block> blocks = BlockStateToBlockMapping.wrapBlockStates(event.getBlocks());
        Block sponge = event.getBlock();
        // If the sponge is outside a terrain and absorbs water inside, remove the blocks that are inside if flag BUILD is false.
        if (!spongeAbsorb(sponge.getWorld().getUID(), sponge.getX(), sponge.getY(), sponge.getZ(), blocks))
            event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPickupArrow(PlayerPickupArrowEvent event) {
        AbstractArrow arrow = event.getArrow();
        Location loc = arrow.getLocation();
        if (!pickupArrow(arrow.getWorld().getUID(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ(), event.getPlayer(), event.getFlyAtPlayer()))
            event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onBlockForm(BlockFormEvent event) {
        Block block = event.getBlock();
        if (!blockForm(block.getWorld().getUID(), block.getX(), block.getY(), block.getZ())) event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onBlockSpread(BlockSpreadEvent event) {
        Block from = event.getSource();
        Block to = event.getBlock();
        if (!blockSpread(to.getWorld().getUID(), to.getX(), to.getY(), to.getZ(), from.getX(), from.getY(), from.getZ(), event.getNewState().getType()))
            event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onBlockGrow(BlockGrowEvent event) {
        Block from = event.getBlock();
        BlockState to = event.getNewState();
        if (!blockGrow(from.getWorld().getUID(), to.getX(), to.getY(), to.getZ(), from.getX(), from.getY(), from.getZ()))
            event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onDamage(EntityDamageEvent event) {
        if (event.getEntityType() != EntityType.PLAYER) return;
        // Preventing checks for player damage being called twice.
        if (event instanceof EntityDamageByEntityEvent || event instanceof EntityDamageByBlockEvent) return;
        Entity player = event.getEntity();
        Location loc = player.getLocation();
        if (!playerDamage(player.getWorld().getUID(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ()))
            event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onDamageByBlock(EntityDamageByBlockEvent event) {
        Entity entity = event.getEntity();
        Location loc = entity.getLocation();

        if (event.getEntityType() == EntityType.PLAYER) {
            if (!playerDamage(entity.getWorld().getUID(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ()))
                event.setCancelled(true);
            return;
        }

        Block block = event.getDamager();
        if (block == null) return;
        if (!entityDamageByExplodingBlock(entity.getWorld().getUID(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ(), block.getX(), block.getY(), block.getZ(), entity))
            event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onDamageByEntity(EntityDamageByEntityEvent event) {
        Entity entity = event.getEntity();
        Location loc = entity.getLocation();
        Entity damager = event.getDamager();
        if (!entityDamageByEntity(entity.getWorld().getUID(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ(), entity, damager, event.getCause() == EntityDamageEvent.DamageCause.ENTITY_EXPLOSION))
            event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onHangingPlace(HangingPlaceEvent event) {
        if (event.getPlayer() == null) return;
        Entity placed = event.getEntity();
        Location loc = placed.getLocation();
        // Material could be ITEM_FRAME, GLOW_ITEM_FRAME or PAINTING, but block place only checks for fire, so it doesn't matter.
        if (!blockPlace(placed.getWorld().getUID(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ(), event.getPlayer(), Material.ITEM_FRAME))
            event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onHangingBreak(HangingBreakEvent event) {
        if (event.getCause() != HangingBreakEvent.RemoveCause.EXPLOSION) return;
        if (event instanceof HangingBreakByEntityEvent) return;
        Hanging hanging = event.getEntity();
        Location loc = hanging.getLocation();
        if (!explodeHanging(hanging.getWorld().getUID(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ()))
            event.setCancelled(true);
    }

    // For some reason is not called when broke by a TNT
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onHangingBreakByEntity(HangingBreakByEntityEvent event) {
        Hanging hanging = event.getEntity();
        Location loc = hanging.getLocation();
        if (!entityDamageByEntity(hanging.getWorld().getUID(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ(), hanging, event.getRemover(), event.getCause() == HangingBreakEvent.RemoveCause.EXPLOSION))
            event.setCancelled(true);
    }

    // Priority LOW to allow other plugins to add more blocks in LOWEST.
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onBlockExplode(BlockExplodeEvent event) {
        Block block = event.getBlock();
        BlockState state = event.getExplodedBlockState();
        if (state != null) {
            if (!blockExplode(block.getWorld().getUID(), state.getX(), state.getY(), state.getZ(), event.blockList()))
                event.setCancelled(true);
        } else {
            if (!blockExplode(block.getWorld().getUID(), block.getX(), block.getY(), block.getZ(), event.blockList()))
                event.setCancelled(true);
        }
    }

    // Priority LOW to allow other plugins to add more blocks in LOWEST.
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onEntityExplode(EntityExplodeEvent event) {
        Location loc = origin(event.getEntity());
        if (!entityExplode(event.getEntity().getWorld().getUID(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ(), event.blockList()))
            event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onBlockBurn(BlockBurnEvent event) {
        Block from = event.getIgnitingBlock();
        Block to = event.getBlock();
        UUID world = to.getWorld().getUID();

        if (from == null) {
            if (!blockBurn(world, to.getX(), to.getY(), to.getZ())) event.setCancelled(true);
        } else if (!blockBurn(world, to.getX(), to.getY(), to.getZ(), from.getX(), from.getY(), from.getZ()))
            event.setCancelled(true);
    }

    // Priority LOW to allow other plugins to add more blocks in LOWEST.
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onBlockFertilize(BlockFertilizeEvent event) {
        // Converting BlockState list to Block.
        List<Block> blocks = BlockStateToBlockMapping.wrapBlockStates(event.getBlocks());
        Block fertilized = event.getBlock();
        // If the fertilized is outside a terrain and grows blocks inside, remove the blocks that are inside if flag BUILD is false.
        if (!blockFertilize(fertilized.getWorld().getUID(), fertilized.getX(), fertilized.getY(), fertilized.getZ(), blocks))
            event.setCancelled(true);
    }

    // Priority LOW to allow other plugins to add more blocks in LOWEST.
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onStructureGrow(StructureGrowEvent event) {
        // Let BlockFertilizeEvent handle it.
        if (event.isFromBonemeal()) return;
        // Converting BlockState list to Block.
        List<Block> blocks = BlockStateToBlockMapping.wrapBlockStates(event.getBlocks());
        Location loc = event.getLocation();
        // If the sapling is outside a terrain and grows blocks inside, remove the blocks that are inside if flag BUILD is false.
        if (!structureGrow(event.getWorld().getUID(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ(), blocks))
            event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onEntityBlockForm(EntityBlockFormEvent event) {
        if (event.getEntity().getType() != EntityType.PLAYER) return;
        Block block = event.getBlock();
        if (!frostWalk(block.getWorld().getUID(), block.getX(), block.getY(), block.getZ(), (Player) event.getEntity()))
            event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onVehicleDestroy(VehicleDestroyEvent event) {
        Entity attacker = event.getAttacker();
        if (attacker == null || attacker.getType() != EntityType.PLAYER) return;
        Entity vehicle = event.getVehicle();
        Location loc = vehicle.getLocation();
        if (!vehicleDestroy(vehicle.getWorld().getUID(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ(), (Player) attacker, vehicle))
            event.setCancelled(true);
    }


    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onSignChange(SignChangeEvent event) {
        Block sign = event.getBlock();
        if (!signChange(sign.getWorld().getUID(), sign.getX(), sign.getY(), sign.getZ(), event.getPlayer()))
            event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onToggleFlight(PlayerToggleFlightEvent event) {
        if (!event.isFlying()) return;
        Player player = event.getPlayer();
        Location loc = player.getLocation();
        if (!startFlight(player.getWorld().getUID(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ(), player))
            player.setAllowFlight(false);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onToggleGlide(EntityToggleGlideEvent event) {
        if (!event.isGliding() || event.getEntityType() != EntityType.PLAYER) return;
        Player player = (Player) event.getEntity();
        Location loc = player.getLocation();
        if (!glide(player.getWorld().getUID(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ(), player))
            event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onProjectileLaunch(ProjectileLaunchEvent event) {
        Projectile proj = event.getEntity();
        Player shooter = proj.getShooter() instanceof Player p ? p : null;
        if (shooter == null) return;
        Location loc = proj.getLocation();
        if (!projectileLaunch(proj.getWorld().getUID(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ(), shooter, proj))
            event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onDispense(BlockDispenseEvent event) {
        Block block = event.getBlock();
        Block frontBlock = block.getRelative(((Directional) block.getBlockData()).getFacing());
        if (!dispenserDispense(block.getWorld().getUID(), frontBlock.getX(), frontBlock.getY(), frontBlock.getZ(), block.getX(), block.getY(), block.getZ()))
            event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onCauldronLevelChange(CauldronLevelChangeEvent event) {
        Block block = event.getBlock();

        switch (event.getReason()) {
            case UNKNOWN, EVAPORATE, NATURAL_FILL -> {
                if (!cauldronNaturallyChangeLevel(block.getWorld().getUID(), block.getX(), block.getY(), block.getZ())) {
                    event.setCancelled(true);
                }
            }
            case EXTINGUISH -> {
                Location from = origin(Objects.requireNonNull(event.getEntity()));
                if (!cauldronExtinguishEntity(block.getWorld().getUID(), block.getX(), block.getY(), block.getZ(), from.getBlockX(), from.getBlockY(), from.getBlockZ(), event.getEntity())) {
                    event.setCancelled(true);
                }
            }
            // Let interact event handle other cases.
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onEntityCombustByEntity(EntityCombustByEntityEvent event) {
        Entity entity = event.getEntity();
        Location loc = entity.getLocation();
        Location from = origin(event.getCombuster());

        if (!entityCombustByEntity(entity.getWorld().getUID(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ(), from.getBlockX(), from.getBlockY(), from.getBlockZ(), entity, event.getCombuster())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onBlockIgnite(BlockIgniteEvent event) {
        Player player = event.getPlayer();
        Entity ignitingEntity = event.getIgnitingEntity();
        Block block = event.getBlock();
        if (player == null && ignitingEntity != null) player = entityOrShooterToPlayer(ignitingEntity);

        if (player != null) {
            if (!playerBlockIgnite(block.getWorld().getUID(), block.getX(), block.getY(), block.getZ(), player)) {
                event.setCancelled(true);
            }
            return;
        }

        Block ignitingBlock = event.getIgnitingBlock();
        if (ignitingBlock != null) {
            if (!blockIgnite(block.getWorld().getUID(), block.getX(), block.getY(), block.getZ(), ignitingBlock.getX(), ignitingBlock.getY(), ignitingBlock.getZ())) {
                event.setCancelled(true);
            }
            return;
        }

        if (ignitingEntity != null) {
            Location loc = origin(ignitingEntity);
            if (!blockIgnite(block.getWorld().getUID(), block.getX(), block.getY(), block.getZ(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ())) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onProjectileHit(ProjectileHitEvent event) {
        Projectile projectile = event.getEntity();
        // Let potion events handle it
        if (projectile.getType() == EntityType.SPLASH_POTION) return;
        projectileHit(projectile, event);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPotionSplash(PotionSplashEvent event) {
        projectileHit(event.getEntity(), event);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onLingeringPotionSplash(LingeringPotionSplashEvent event) {
        projectileHit(event.getEntity(), event);
    }

    private void projectileHit(@NotNull Projectile projectile, @NotNull Cancellable event) {
        Player shooter = projectile.getShooter() instanceof Player p ? p : null;
        Location loc = projectile.getLocation();
        Location from = origin(projectile);
        if (!projectileHit(projectile.getWorld().getUID(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ(), from.getBlockX(), from.getBlockY(), from.getBlockZ(), projectile, shooter))
            event.setCancelled(true);
    }

    // Priority LOW to allow other plugins to change the food level in LOWEST.
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onFoodLevelChange(FoodLevelChangeEvent event) {
        HumanEntity human = event.getEntity();
        if (event.getFoodLevel() > human.getFoodLevel()) return;
        Location loc = human.getLocation();
        if (!playerFoodLeaveDecrease(human.getWorld().getUID(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ()))
            event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPortalCreate(PortalCreateEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!portalCreate(player, BlockStateToBlockMapping.wrapBlockStates(event.getBlocks())))
            event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onItemConsume(PlayerItemConsumeEvent event) {
        Player player = event.getPlayer();
        Location loc = player.getLocation();
        if (!itemConsume(player.getWorld().getUID(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ(), player, event.getItem().hasItemMeta() && event.getItem().getItemMeta() instanceof PotionMeta))
            event.setCancelled(true);
    }

    // Priority LOW to allow other plugins to change the command in LOWEST.
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onCommandPreprocess(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        Location loc = player.getLocation();
        if (!command(player.getWorld().getUID(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ(), player, event.getMessage()))
            event.setCancelled(true);
    }

    private boolean deny(Terrain terrain, Flag<Boolean> flag) {
        Boolean state = terrain.flags().getData(flag);
        return state != null && !state;
    }

    // TODO: Take member specific flags into account.
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void canEnter(TerrainCanEnterEvent event) {
        // Checking if canEnter was already set by a listener called before this one.
        if (event.canEnter() != TerrainEvent.CanEnterLeave.DEFAULT) return;
        Terrain terrain = event.terrain();
        Player player = event.player();
        Boolean enter = terrain.flags().getData(Flags.ENTER);
        if (enter == null) return;
        if (enter || player.hasPermission(Flags.ENTER.bypassPermission()) || TerrainerPlugin.getPlayerUtil().hasAnyRelations(player, terrain)) {
            event.setCanEnter(TerrainEvent.CanEnterLeave.ALLOW);
            return;
        }

        event.setCanEnter(TerrainEvent.CanEnterLeave.DENY);
        lang.send(player, lang.get("Protections." + Flags.ENTER.id()));
    }

    // TODO: Take member specific flags into account.
    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void canLeave(TerrainCanLeaveEvent event) {
        // Checking if canLeave was already set by a listener called before this one.
        if (event.canLeave() != TerrainEvent.CanEnterLeave.DEFAULT) return;
        Terrain terrain = event.terrain();
        Player player = event.player();
        Boolean leave = terrain.flags().getData(Flags.LEAVE);
        if (leave == null) return;
        if (leave || player.hasPermission(Flags.LEAVE.bypassPermission()) || TerrainerPlugin.getPlayerUtil().hasAnyRelations(player, terrain)) {
            event.setCanLeave(TerrainEvent.CanEnterLeave.ALLOW);
            return;
        }

        event.setCanLeave(TerrainEvent.CanEnterLeave.DENY);
        lang.send(player, lang.get("Protections." + Flags.LEAVE.id()));
    }

    // TODO: Take member specific flags into account.
    @SuppressWarnings("deprecation")
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void monitorOnEnter(TerrainEnterEvent event) {
        Terrain terrain = event.terrain();
        Player player = event.player();

        // Checking fly flag.
        if ((player.isFlying() || player.getAllowFlight()) && !player.hasPermission(Flags.FLY.bypassPermission())) {
            if (!TerrainerPlugin.getPlayerUtil().hasAnyRelations(player, terrain) && deny(terrain, Flags.FLY)) {
                // Setting tag on player to return flight when leaving the terrain.
                if (player.getAllowFlight()) {
                    String flyPermission = Configurations.CONFIG.getConfiguration().getString("Fly Permission").orElse("essentials.fly");
                    player.getPersistentDataContainer().set(resetFly, PersistentDataType.INTEGER, player.hasPermission(flyPermission) ? 1 : 0);
                }
                player.setAllowFlight(false);
                if (player.isFlying()) lang.send(player, lang.get("Protections." + Flags.FLY.id()));
            }
        }

        // Checking glide flag.
        if (player.isGliding() && !player.hasPermission(Flags.GLIDE.bypassPermission())) {
            if (!TerrainerPlugin.getPlayerUtil().hasAnyRelations(player, terrain) && deny(terrain, Flags.GLIDE)) {
                player.setGliding(false);
                lang.send(player, lang.get("Protections." + Flags.GLIDE.id()));
            }
        }

        // Applying effects of effects flag.
        Map<String, Integer> effects = terrain.flags().getData(Flags.EFFECTS);
        if (effects != null) {
            effects.forEach((effect, power) -> {
                PotionEffectType type = PotionEffectType.getByName(effect);
                if (type == null) return;
                player.addPotionEffect(new PotionEffect(type, Integer.MAX_VALUE, power, false, false));
            });
        }

        // Showing enter message.
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

        // Showing borders.
        if (!terrain.borders().isEmpty() && Boolean.TRUE.equals(terrain.flags().getData(Flags.SHOW_BORDERS)) && Configurations.CONFIG.getConfiguration().getBoolean("Borders.On Enter").orElse(false)) {
            bordersCommand.showBorders(player, Collections.singleton(terrain));
        }
    }

    // TODO: Take member specific flags into account.
    @SuppressWarnings("deprecation")
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void monitorOnLeave(TerrainLeaveEvent event) {
        Terrain terrain = event.terrain();
        Player player = event.player();

        // Returning the player's ability to fly.
        Integer returnFlight = player.getPersistentDataContainer().get(resetFly, PersistentDataType.INTEGER);
        if (returnFlight != null) {
            player.getPersistentDataContainer().remove(resetFly);

            String flyPermission = Configurations.CONFIG.getConfiguration().getString("Fly Permission").orElse("essentials.fly");

            // Checking fly permission only if the player had it before losing the flight.
            if (returnFlight == 0 || player.hasPermission(flyPermission)) player.setAllowFlight(true);
        }

        // Removing effects from effects flag.
        Map<String, Integer> effects = terrain.flags().getData(Flags.EFFECTS);
        if (effects != null) {
            effects.forEach((effect, power) -> {
                PotionEffectType type = PotionEffectType.getByName(effect);
                if (type == null) return;
                player.removePotionEffect(type);
            });
        }

        // Showing leave message.
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
        UUID playerID = player.getUniqueId();
        BossBarTask previous = bossBarTasks.get(playerID);
        BossBar bar;
        Mutable<TaskFactory.CancellableTask> barRemoverTask;

        // Creating or getting current bossbar of player.
        if (previous == null) {
            bar = Bukkit.createBossBar(message, BarColor.RED, BarStyle.SOLID);
            bar.addPlayer(player);
            barRemoverTask = new Mutable<>() {
                private @Nullable TaskFactory.CancellableTask task;

                @Override
                public @Nullable TaskFactory.CancellableTask getValue() {
                    return task;
                }

                @Override
                public void setValue(@Nullable TaskFactory.CancellableTask task) {
                    this.task = task;
                }
            };

            // Starting bar remover task.
            Runnable barRemoverRunnable = () -> {
                bossBarTasks.remove(playerID);
                bar.removePlayer(player);
            };
            barRemoverTask.setValue(plugin.getTaskFactory().runDelayed(player, 100, barRemoverRunnable, barRemoverRunnable));

            // Adding bar to current alive boss bars.
            if (barRemoverTask.getValue() != null) bossBarTasks.put(playerID, new BossBarTask(bar, barRemoverTask));
        } else {
            bar = previous.bar;
            barRemoverTask = previous.task;
            bar.setTitle(message);

            // Cancelling previous bar remover task and starting it again.
            if (barRemoverTask.getValue() != null) barRemoverTask.getValue().cancel();
            Runnable barRemoverRunnable = () -> {
                bossBarTasks.remove(playerID);
                bar.removePlayer(player);
            };
            barRemoverTask.setValue(plugin.getTaskFactory().runDelayed(player, 100, barRemoverRunnable, barRemoverRunnable));
        }
    }

    private record BossBarTask(@NotNull BossBar bar, @NotNull Mutable<TaskFactory.CancellableTask> task) {
    }
}
