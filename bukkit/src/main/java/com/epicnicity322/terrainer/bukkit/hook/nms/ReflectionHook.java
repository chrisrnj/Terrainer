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

package com.epicnicity322.terrainer.bukkit.hook.nms;

import com.epicnicity322.epicpluginlib.bukkit.reflection.ReflectionUtil;
import com.epicnicity322.epicpluginlib.bukkit.reflection.type.PackageType;
import com.epicnicity322.epicpluginlib.bukkit.reflection.type.SubPackageType;
import com.epicnicity322.epicpluginlib.core.logger.ConsoleLogger;
import com.epicnicity322.terrainer.bukkit.util.BlockDisplayUtil;
import com.epicnicity322.terrainer.bukkit.util.NMSHandler;
import com.epicnicity322.terrainer.core.Terrainer;
import com.epicnicity322.terrainer.core.util.PlayerUtil;
import net.minecraft.network.protocol.game.PacketPlayOutEntityDestroy;
import net.minecraft.network.protocol.game.PacketPlayOutEntityMetadata;
import net.minecraft.network.protocol.game.PacketPlayOutSpawnEntity;
import net.minecraft.network.syncher.DataWatcher;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.monster.EntitySlime;
import net.minecraft.world.level.World;
import net.minecraft.world.phys.Vec3D;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.entity.Slime;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public final class ReflectionHook implements NMSHandler {
    private static final @NotNull Vec3D zero = new Vec3D(0, 0, 0);
    private static final Method method_CraftWorld_getHandle;
    private static final Method method_CraftEntity_getHandle;
    private static final Method method_Entity_getBukkitEntity = ReflectionUtil.getMethod(Entity.class, "getBukkitEntity");
    private static final Method method_Entity_getDataWatcher = ReflectionUtil.findMethodByType(Entity.class, DataWatcher.class, false);
    private static final Constructor<?> constructor_PacketPlayOutEntityMetadata;
    private static final Constructor<?> constructor_PacketPlayOutSpawnEntity_Old = ReflectionUtil.getConstructor(PacketPlayOutSpawnEntity.class, int.class, UUID.class, double.class, double.class, double.class, float.class, float.class, EntityTypes.class, int.class, Vec3D.class);
    private static final Class<?> class_BlockDisplay = ReflectionUtil.getClass("org.bukkit.entity.BlockDisplay");
    private static final boolean hasBlockDisplays = class_BlockDisplay != null;
    private static final EntityTypes<?> blockDisplayType = hasBlockDisplays ? BlockDisplayUtil.findBlockDisplayType() : null;
    private static final boolean newPacketMetadataConstructor;
    private static final EntityTypes<?> slimeEntityType = findEntityType(EntityTypes.class.getName() + "<" + EntitySlime.class.getName() + ">");
    private static @NotNull BlockData selectionBlock = Material.GLOWSTONE.createBlockData();
    private static @NotNull BlockData selectionEdgeBlock = Material.GOLD_BLOCK.createBlockData();
    private static @NotNull BlockData terrainBlock = Material.DIAMOND_BLOCK.createBlockData();
    private static @NotNull BlockData terrainEdgeBlock = Material.GLASS.createBlockData();
    private static @NotNull Color selectionColor = Color.YELLOW;
    private static @NotNull Color terrainColor = Color.WHITE;
    private static @NotNull Color terrainCreatedColor = Color.LIME;

    static {
        Class<?> class_CraftWorld = ReflectionUtil.getClass("CraftWorld", PackageType.CRAFTBUKKIT);
        if (class_CraftWorld == null) {
            method_CraftWorld_getHandle = null;
        } else {
            method_CraftWorld_getHandle = ReflectionUtil.getMethod(class_CraftWorld, "getHandle");
        }
        Class<?> class_CraftEntity = ReflectionUtil.getClass("CraftEntity", SubPackageType.ENTITY);
        if (class_CraftEntity == null) {
            method_CraftEntity_getHandle = null;
        } else {
            method_CraftEntity_getHandle = ReflectionUtil.getMethod(class_CraftEntity, "getHandle");
        }
        Constructor<?> oldEntityMetadataConstructor = ReflectionUtil.getConstructor(PacketPlayOutEntityMetadata.class, int.class, DataWatcher.class, boolean.class);
        if (oldEntityMetadataConstructor == null) {
            constructor_PacketPlayOutEntityMetadata = null;
            newPacketMetadataConstructor = ReflectionUtil.getConstructor(PacketPlayOutEntityMetadata.class, int.class, List.class) != null;
        } else {
            constructor_PacketPlayOutEntityMetadata = oldEntityMetadataConstructor;
            newPacketMetadataConstructor = false;
        }

        if (!hasBlockDisplays) {
            Terrainer.logger().log("Block Displays are not available. Using Slime entities as markers.", ConsoleLogger.Level.WARN);
        }
    }

    public static void setMarkerBlocks(@Nullable Material selectionBlock, @Nullable Material selectionEdgeBlock, @Nullable Material terrainBlock, @Nullable Material terrainEdgeBlock) {
        if (selectionBlock == null) selectionBlock = Material.GLOWSTONE;
        if (selectionEdgeBlock == null) selectionEdgeBlock = Material.GOLD_BLOCK;
        if (terrainBlock == null) terrainBlock = Material.DIAMOND_BLOCK;
        if (terrainEdgeBlock == null) terrainEdgeBlock = Material.GLASS;

        ReflectionHook.selectionBlock = selectionBlock.createBlockData();
        ReflectionHook.selectionEdgeBlock = selectionEdgeBlock.createBlockData();
        ReflectionHook.terrainBlock = terrainBlock.createBlockData();
        ReflectionHook.terrainEdgeBlock = terrainEdgeBlock.createBlockData();
    }

    public static void setMarkerColors(int selection, int terrain, int created) {
        if (selection >> 24 != 0) selection = 0xFFFFFF;
        if (terrain >> 24 != 0) terrain = 0xFFFFFF;
        if (created >> 24 != 0) created = 0xFFFFFF;

        ReflectionHook.selectionColor = Color.fromRGB(selection);
        ReflectionHook.terrainColor = Color.fromRGB(terrain);
        ReflectionHook.terrainCreatedColor = Color.fromRGB(created);
    }

    private static EntityTypes<?> findEntityType(@NotNull String type) {
        for (Field f : EntityTypes.class.getFields()) {
            if (f.getGenericType().getTypeName().equals(type)) {
                try {
                    return (EntityTypes<?>) f.get(null);
                } catch (Exception ignored) {
                }
                break;
            }
        }
        return null;
    }

    private static double center(double coordinate, @NotNull EntityTypes<?> type) {
        // The specific transformation size of the block entity makes so its texture clips through other blocks.
        return type == blockDisplayType ? coordinate - 0.0005 : coordinate + 0.5;
    }

    @SuppressWarnings({"unchecked"})
    @Override
    public @NotNull PlayerUtil.SpawnedMarker spawnMarkerEntity(@NotNull Player player, int x, int y, int z, boolean edge, boolean selection) throws Throwable {
        assert method_CraftWorld_getHandle != null;
        assert method_Entity_getBukkitEntity != null;
        assert method_Entity_getDataWatcher != null;
        BlockData blockType = selection ? edge ? selectionEdgeBlock : selectionBlock : edge ? terrainEdgeBlock : terrainBlock;
        EntityTypes<?> type;
        org.bukkit.entity.Entity entity;
        Entity nmsEntity;

        if (hasBlockDisplays && !blockType.getMaterial().isAir()) {
            type = blockDisplayType;
            assert type != null;
            nmsEntity = BlockDisplayUtil.nmsBlockDisplay(type, (World) method_CraftWorld_getHandle.invoke(player.getWorld()));
            entity = BlockDisplayUtil.applyPropertiesToBlockDisplay((org.bukkit.entity.Entity) method_Entity_getBukkitEntity.invoke(nmsEntity), blockType, selection ? selectionColor : terrainColor);
        } else {
            type = slimeEntityType;
            assert type != null;
            EntitySlime slime = new EntitySlime((EntityTypes<? extends EntitySlime>) type, (World) method_CraftWorld_getHandle.invoke(player.getWorld()));
            Slime bukkitSlime = (Slime) method_Entity_getBukkitEntity.invoke(slime);
            bukkitSlime.setSize(2);
            bukkitSlime.setCollidable(false);
            bukkitSlime.setInvisible(true);
            update(slime, selection, false, player);
            entity = bukkitSlime;
            nmsEntity = slime;
        }

        int id = entity.getEntityId();
        entity.setGlowing(true);

        if (constructor_PacketPlayOutSpawnEntity_Old != null) {
            ReflectionUtil.sendPacket(player, constructor_PacketPlayOutSpawnEntity_Old.newInstance(id, entity.getUniqueId(), center(x, type), type == slimeEntityType ? y : center(y, type), center(z, type), 0, 0, type, 0, zero));
        } else {
            ReflectionUtil.sendPacket(player, new PacketPlayOutSpawnEntity(id, entity.getUniqueId(), center(x, type), type == slimeEntityType ? y : center(y, type), center(z, type), 0, 0, type, 0, zero, 0));
        }
        DataWatcher dataWatcher = (DataWatcher) method_Entity_getDataWatcher.invoke(nmsEntity);

        if (newPacketMetadataConstructor) {
            ReflectionUtil.sendPacket(player, new PacketPlayOutEntityMetadata(id, Objects.requireNonNull(dataWatcher.c())));
        } else {
            assert constructor_PacketPlayOutEntityMetadata != null;
            ReflectionUtil.sendPacket(player, constructor_PacketPlayOutEntityMetadata.newInstance(id, dataWatcher, false));
        }

        return new PlayerUtil.SpawnedMarker(id, entity.getUniqueId(), entity);
    }

    @Override
    public void killEntity(@NotNull Player player, @NotNull PlayerUtil.SpawnedMarker marker) {
        PacketPlayOutEntityDestroy packet = new PacketPlayOutEntityDestroy(marker.entityID());
        ReflectionUtil.sendPacket(player, packet);

        // Removing from team.
        Scoreboard board = Bukkit.getServer().getScoreboardManager().getMainScoreboard();
        Team selectionTeam = board.getTeam("TRselectionTeam");
        Team createdTeam = board.getTeam("TRcreatedTeam");
        Team terrainTeam = board.getTeam("TRterrainTeam");
        String uuid = marker.entityUUID().toString();

        if (selectionTeam != null) {
            selectionTeam.removeEntry(uuid);
            if (selectionTeam.getEntries().isEmpty()) selectionTeam.unregister();
        }
        if (createdTeam != null) {
            createdTeam.removeEntry(uuid);
            if (createdTeam.getEntries().isEmpty()) createdTeam.unregister();
        }
        if (terrainTeam != null) {
            terrainTeam.removeEntry(uuid);
            if (terrainTeam.getEntries().isEmpty()) terrainTeam.unregister();
        }
    }

    @Override
    public void updateSelectionMarkerToTerrainMarker(@NotNull PlayerUtil.SpawnedMarker marker, @NotNull Player player) throws Throwable {
        update(marker.markerEntity(), false, true, player);
    }

    @SuppressWarnings("deprecation")
    private void update(@NotNull Object entity, boolean selection, boolean created, @NotNull Player player) throws Throwable {
        if (entity instanceof Slime slime) {
            Scoreboard board = Bukkit.getServer().getScoreboardManager().getMainScoreboard();
            Team team = board.getTeam(selection ? "TRselectionTeam" : created ? "TRcreatedTeam" : "TRterrainTeam");
            if (team == null)
                team = board.registerNewTeam(selection ? "TRselectionTeam" : created ? "TRcreatedTeam" : "TRterrainTeam");

            team.setColor(Objects.requireNonNullElse(ChatColor.getByChar(Integer.toHexString((selection ? selectionColor : created ? terrainCreatedColor : terrainColor).asRGB())), selection ? ChatColor.YELLOW : created ? ChatColor.GREEN : ChatColor.WHITE));
            team.addEntry(slime.getUniqueId().toString());
        } else if (class_BlockDisplay != null && class_BlockDisplay.isAssignableFrom(entity.getClass())) {
            org.bukkit.entity.Entity blockDisplay = (org.bukkit.entity.Entity) entity;
            BlockData block = BlockDisplayUtil.getBlock(blockDisplay);

            if (block.equals(selectionBlock)) {
                BlockDisplayUtil.applyPropertiesToBlockDisplay(blockDisplay, terrainBlock, terrainCreatedColor);
            } else if (block.equals(selectionEdgeBlock)) {
                BlockDisplayUtil.applyPropertiesToBlockDisplay(blockDisplay, terrainEdgeBlock, terrainCreatedColor);
            }

            assert method_Entity_getDataWatcher != null;
            assert method_CraftEntity_getHandle != null;
            DataWatcher dataWatcher = (DataWatcher) method_Entity_getDataWatcher.invoke(method_CraftEntity_getHandle.invoke(entity));
            int entityId = blockDisplay.getEntityId();

            if (newPacketMetadataConstructor) {
                ReflectionUtil.sendPacket(player, new PacketPlayOutEntityMetadata(entityId, Objects.requireNonNull(dataWatcher.c())));
            } else {
                assert constructor_PacketPlayOutEntityMetadata != null;
                ReflectionUtil.sendPacket(player, constructor_PacketPlayOutEntityMetadata.newInstance(entityId, dataWatcher, false));
            }
        }
    }
}
