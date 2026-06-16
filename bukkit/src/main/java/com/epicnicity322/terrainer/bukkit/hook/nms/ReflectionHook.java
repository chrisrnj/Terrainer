/*
 * Terrainer - A minecraft terrain claiming protection plugin.
 * Copyright (C) 2025-2026 Christiano Rangel
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
import com.epicnicity322.terrainer.bukkit.TerrainerPlugin;
import com.epicnicity322.terrainer.bukkit.hook.NMSHandler;
import com.epicnicity322.terrainer.bukkit.hook.geyser.GeyserHook;
import com.epicnicity322.terrainer.bukkit.hook.viaversion.ViaVersionHook;
import com.epicnicity322.terrainer.bukkit.util.BlockDisplayUtil;
import com.epicnicity322.terrainer.core.Terrainer;
import com.epicnicity322.terrainer.core.util.PlayerUtil;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Slime;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public final class ReflectionHook implements NMSHandler {
    private static final boolean available;
    private static final boolean hasBlockDisplays;
    private static final int blockDisplayProtocolVersion = 762;
    private static final Class<?> class_EntityType;
    private static final Class<?> class_Slime;
    private static final Class<?> class_BukkitBlockDisplay = ReflectionUtil.getClass("org.bukkit.entity.BlockDisplay");
    private static final Class<?> class_BlockDisplay = ReflectionUtil.getClass("net.minecraft.world.entity.Display$BlockDisplay");
    private static final Constructor<?> constructor_Slime;
    private static final Constructor<?> constructor_BlockDisplay;
    private static final Method method_CraftWorld_getHandle;
    private static final Method method_CraftEntity_getHandle;
    private static final Method method_Entity_getBukkitEntity;
    private static final Method method_Entity_getEntityData;
    private static final ClientboundAddEntityPacketAdapter clientboundAddEntityPacketAdapter;
    private static final ClientboundSetEntityDataPacketAdapter clientboundSetEntityDataPacketAdapter;
    private static final ClientboundRemoveEntitiesPacketAdapter clientboundRemoveEntitiesPacketAdapter;
    private static final Object blockDisplayType;
    private static final Object slimeEntityType;

    static {
        boolean available1 = false;
        boolean hasBlockDisplays1 = class_BukkitBlockDisplay != null && class_BlockDisplay != null;
        Class<?> class_EntityType1 = null;
        Class<?> class_Slime1 = null;
        Constructor<?> constructor_Slime1 = null;
        Constructor<?> constructor_BlockDisplay1 = null;
        Method method_CraftWorld_getHandle1 = null;
        Method method_CraftEntity_getHandle1 = null;
        Method method_Entity_getBukkitEntity1 = null;
        Method method_Entity_getEntityData1 = null;
        ClientboundAddEntityPacketAdapter clientboundAddEntityPacketAdapter1 = null;
        ClientboundSetEntityDataPacketAdapter clientboundSetEntityDataPacketAdapter1 = null;
        ClientboundRemoveEntitiesPacketAdapter clientboundRemoveEntitiesPacketAdapter1 = null;
        Object blockDisplayType1 = null;
        Object slimeEntityType1 = null;

        try {
            Class<?> class_CraftWorld = Objects.requireNonNull(ReflectionUtil.getClass("CraftWorld", PackageType.CRAFTBUKKIT));
            method_CraftWorld_getHandle1 = class_CraftWorld.getMethod("getHandle");
            Class<?> class_CraftEntity = Objects.requireNonNull(ReflectionUtil.getClass("CraftEntity", SubPackageType.ENTITY));
            method_CraftEntity_getHandle1 = class_CraftEntity.getMethod("getHandle");

            Class<?> class_Level = ReflectionUtil.getClass("net.minecraft.world.level.Level");
            if (class_Level == null) class_Level = Class.forName("net.minecraft.world.level.World");

            Class<?> class_Vec3 = ReflectionUtil.getClass("net.minecraft.world.phys.Vec3");
            if (class_Vec3 == null) class_Vec3 = Class.forName("net.minecraft.world.phys.Vec3D");
            Object vec3_zero = class_Vec3.getConstructor(double.class, double.class, double.class).newInstance(0d, 0d, 0d);

            Class<?> class_SynchedEntityData = ReflectionUtil.getClass("net.minecraft.network.syncher.SynchedEntityData");
            if (class_SynchedEntityData == null)
                class_SynchedEntityData = Class.forName("net.minecraft.network.syncher.DataWatcher");

            Method method_SynchedEntityData_getNonDefaultValues = ReflectionUtil.getMethod(class_SynchedEntityData, "getNonDefaultValues");
            if (method_SynchedEntityData_getNonDefaultValues == null) {
                method_SynchedEntityData_getNonDefaultValues = ReflectionUtil.getMethod(class_SynchedEntityData, "c");
                if (method_SynchedEntityData_getNonDefaultValues == null) {
                    method_SynchedEntityData_getNonDefaultValues = class_SynchedEntityData.getMethod("getAll");
                }
            }

            Class<?> class_Entity = Class.forName("net.minecraft.world.entity.Entity");
            method_Entity_getBukkitEntity1 = class_Entity.getMethod("getBukkitEntity");
            method_Entity_getEntityData1 = ReflectionUtil.getMethod(class_Entity, "getEntityData");
            if (method_Entity_getEntityData1 == null)
                method_Entity_getEntityData1 = Objects.requireNonNull(ReflectionUtil.findMethodByType(class_Entity, class_SynchedEntityData, false));

            class_EntityType1 = ReflectionUtil.getClass("net.minecraft.world.entity.EntityType");
            if (class_EntityType1 == null) class_EntityType1 = Class.forName("net.minecraft.world.entity.EntityTypes");

            class_Slime1 = ReflectionUtil.getClass("net.minecraft.world.entity.monster.Slime");
            if (class_Slime1 == null) class_Slime1 = Class.forName("net.minecraft.world.entity.monster.EntitySlime");
            constructor_Slime1 = class_Slime1.getConstructor(class_EntityType1, class_Level);

            Class<?> class_ClientboundAddEntityPacket = ReflectionUtil.getClass("net.minecraft.network.protocol.game.ClientboundAddEntityPacket");
            if (class_ClientboundAddEntityPacket == null) {
                class_ClientboundAddEntityPacket = Class.forName("net.minecraft.network.protocol.game.PacketPlayOutSpawnEntity");
            }

            Constructor<?> constructor_ClientboundAddEntityPacket = ReflectionUtil.getConstructor(class_ClientboundAddEntityPacket, int.class, UUID.class, double.class, double.class, double.class, float.class, float.class, class_EntityType1, int.class, class_Vec3, double.class);
            if (constructor_ClientboundAddEntityPacket != null) {
                clientboundAddEntityPacketAdapter1 = (entityId, uuid, x, y, z, type) -> constructor_ClientboundAddEntityPacket.newInstance(entityId, uuid, x, y, z, 0f, 0f, type, 0, vec3_zero, 0.0);
            } else {
                Constructor<?> constructor_ClientboundAddEntityPacket1 = class_ClientboundAddEntityPacket.getConstructor(int.class, UUID.class, double.class, double.class, double.class, float.class, float.class, class_EntityType1, int.class, class_Vec3);
                clientboundAddEntityPacketAdapter1 = (entityId, uuid, x, y, z, type) -> constructor_ClientboundAddEntityPacket1.newInstance(entityId, uuid, x, y, z, 0f, 0f, type, 0, vec3_zero);
            }

            Class<?> class_ClientboundSetEntityDataPacket = ReflectionUtil.getClass("net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket");
            if (class_ClientboundSetEntityDataPacket == null) {
                class_ClientboundSetEntityDataPacket = Class.forName("net.minecraft.network.protocol.game.PacketPlayOutEntityMetadata");
            }

            Constructor<?> constructor_ClientboundSetEntityDataPacket = ReflectionUtil.getConstructor(class_ClientboundSetEntityDataPacket, int.class, List.class);
            if (constructor_ClientboundSetEntityDataPacket != null) {
                Method finalMethod_SynchedEntityData_getNonDefaultValues = method_SynchedEntityData_getNonDefaultValues;
                clientboundSetEntityDataPacketAdapter1 = (entityId, entityData) -> constructor_ClientboundSetEntityDataPacket.newInstance(entityId, finalMethod_SynchedEntityData_getNonDefaultValues.invoke(entityData));
            } else {
                Constructor<?> constructor_ClientboundSetEntityDataPacket1 = class_ClientboundSetEntityDataPacket.getConstructor(int.class, class_SynchedEntityData, boolean.class);
                clientboundSetEntityDataPacketAdapter1 = (entityId, entityData) -> constructor_ClientboundSetEntityDataPacket1.newInstance(entityId, entityData, false);
            }

            Class<?> class_ClientboundRemoveEntitiesPacket = ReflectionUtil.getClass("net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket");
            if (class_ClientboundRemoveEntitiesPacket == null) {
                class_ClientboundRemoveEntitiesPacket = Class.forName("net.minecraft.network.protocol.game.PacketPlayOutEntityDestroy");
            }

            Constructor<?> constructor_ClientboundRemoveEntitiesPacket = ReflectionUtil.getConstructor(class_ClientboundRemoveEntitiesPacket, int[].class);
            if (constructor_ClientboundRemoveEntitiesPacket != null) {
                clientboundRemoveEntitiesPacketAdapter1 = (player, markers) -> ReflectionUtil.sendPacket(player, constructor_ClientboundRemoveEntitiesPacket.newInstance((Object) markers.stream().mapToInt(PlayerUtil.SpawnedMarker::entityID).toArray()));
            } else {
                Constructor<?> constructor_ClientboundRemoveEntitiesPacket1 = class_ClientboundRemoveEntitiesPacket.getConstructor(int.class);
                clientboundRemoveEntitiesPacketAdapter1 = (player, markers) -> ReflectionUtil.sendPackets(player, markers.stream().map(marker -> {
                            try {
                                return constructor_ClientboundRemoveEntitiesPacket1.newInstance(marker.entityID());
                            } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
                                throw new RuntimeException(e);
                            }
                        }
                ).toArray());
            }

            if (hasBlockDisplays1) {
                try {
                    constructor_BlockDisplay1 = class_BlockDisplay.getConstructor(class_EntityType1, class_Level);
                    blockDisplayType1 = Objects.requireNonNull(findEntityType(class_EntityType1.getName() + "<" + class_BlockDisplay.getName() + ">"));
                } catch (Exception e) {
                    Terrainer.logger().log("An unknown error happened while getting the constructor for " + class_BlockDisplay.getSimpleName() + "(" + class_EntityType1.getSimpleName() + ", " + class_Level.getSimpleName() + "):", ConsoleLogger.Level.ERROR);
                    e.printStackTrace();
                    hasBlockDisplays1 = false;
                }
            }

            slimeEntityType1 = Objects.requireNonNull(findEntityType(class_EntityType1.getName() + "<" + class_Slime1.getName() + ">"));

            available1 = true;
        } catch (Exception e) {
            Terrainer.logger().log("Unable to use reflection to load ReflectionHook.", ConsoleLogger.Level.ERROR);
            e.printStackTrace();
        }

        available = available1;
        hasBlockDisplays = hasBlockDisplays1;
        class_EntityType = class_EntityType1;
        class_Slime = class_Slime1;
        constructor_Slime = constructor_Slime1;
        constructor_BlockDisplay = constructor_BlockDisplay1;
        method_CraftWorld_getHandle = method_CraftWorld_getHandle1;
        method_CraftEntity_getHandle = method_CraftEntity_getHandle1;
        method_Entity_getBukkitEntity = method_Entity_getBukkitEntity1;
        method_Entity_getEntityData = method_Entity_getEntityData1;
        clientboundAddEntityPacketAdapter = clientboundAddEntityPacketAdapter1;
        clientboundSetEntityDataPacketAdapter = clientboundSetEntityDataPacketAdapter1;
        clientboundRemoveEntitiesPacketAdapter = clientboundRemoveEntitiesPacketAdapter1;
        slimeEntityType = slimeEntityType1;
        blockDisplayType = blockDisplayType1;

        if (!hasBlockDisplays) {
            Terrainer.logger().log("Block Displays are not available. Using Slime entities as markers for everyone.", ConsoleLogger.Level.WARN);
        }
    }

    /**
     * @return Whether ReflectionHook could find the classes correctly and markers/slime entities should work.
     */
    public static boolean isAvailable() {
        return available;
    }

    private static Object findEntityType(@NotNull String type) {
        for (Field f : class_EntityType.getFields()) {
            if (f.getGenericType().getTypeName().equals(type)) {
                try {
                    return f.get(null);
                } catch (Exception ignored) {
                }
                break;
            }
        }
        return null;
    }

    private static double center(double coordinate, @NotNull Object type) {
        // The specific transformation size of the block entity makes so its texture clips through other blocks.
        return type == blockDisplayType ? coordinate - 0.0005 : coordinate + 0.5;
    }

    private static boolean bedrockPlayer(@NotNull Player player) {
        return TerrainerPlugin.getGeyserHook() && GeyserHook.isBedrock(player.getUniqueId()) && ReflectionHookOptions.geyserOption;
    }

    private static boolean versionSupportsDisplays(@NotNull Player player) {
        if (TerrainerPlugin.getViaVersionHook() && ReflectionHookOptions.viaVersionOption) {
            return ViaVersionHook.getVersion(player) >= blockDisplayProtocolVersion;
        }
        return true;
    }

    @Override
    public @NotNull PlayerUtil.SpawnedMarker spawnMarkerEntity(@NotNull Player player, int x, int y, int z, boolean edge, boolean selection) throws Throwable {
        assert method_CraftWorld_getHandle != null;
        assert method_Entity_getBukkitEntity != null;
        assert method_Entity_getEntityData != null;
        BlockData blockType = selection ? edge ? ReflectionHookOptions.selectionEdgeBlock : ReflectionHookOptions.selectionBlock : edge ? ReflectionHookOptions.terrainEdgeBlock : ReflectionHookOptions.terrainBlock;
        Object type;
        org.bukkit.entity.Entity entity;
        Object nmsEntity;

        if (hasBlockDisplays && blockType.getMaterial().isBlock() && !bedrockPlayer(player) && versionSupportsDisplays(player)) {
            assert blockDisplayType != null;
            type = blockDisplayType;
            nmsEntity = constructor_BlockDisplay.newInstance(type, method_CraftWorld_getHandle.invoke(player.getWorld()));
            entity = BlockDisplayUtil.applyPropertiesToBlockDisplay((org.bukkit.entity.Entity) method_Entity_getBukkitEntity.invoke(nmsEntity), blockType, selection ? ReflectionHookOptions.selectionColor : ReflectionHookOptions.terrainColor);
        } else {
            assert slimeEntityType != null;
            type = slimeEntityType;
            Object slime = constructor_Slime.newInstance(type, method_CraftWorld_getHandle.invoke(player.getWorld()));
            Slime bukkitSlime = (Slime) method_Entity_getBukkitEntity.invoke(slime);
            bukkitSlime.setSize(2);
            bukkitSlime.setCollidable(false);
            if (!bedrockPlayer(player)) bukkitSlime.setInvisible(true);
            update(slime, selection, false, player);
            entity = bukkitSlime;
            nmsEntity = slime;
        }

        int id = entity.getEntityId();
        entity.setGlowing(true);

        Object addEntity = clientboundAddEntityPacketAdapter.instance(id, entity.getUniqueId(), center(x, type), type == slimeEntityType ? y : center(y, type), center(z, type), type);
        Object setEntityData = clientboundSetEntityDataPacketAdapter.instance(id, method_Entity_getEntityData.invoke(nmsEntity));

        ReflectionUtil.sendPackets(player, addEntity, setEntityData);

        return new PlayerUtil.SpawnedMarker(id, entity.getUniqueId(), entity);
    }

    @Override
    public void killEntities(@NotNull Player player, @NotNull Collection<PlayerUtil.SpawnedMarker> markers) throws InvocationTargetException, InstantiationException, IllegalAccessException {
        clientboundRemoveEntitiesPacketAdapter.send(player, markers);

        // Removing from team.
        Scoreboard board = Bukkit.getServer().getScoreboardManager().getMainScoreboard();
        Team selectionTeam = board.getTeam("TRselectionTeam");
        Team createdTeam = board.getTeam("TRcreatedTeam");
        Team terrainTeam = board.getTeam("TRterrainTeam");

        for (PlayerUtil.SpawnedMarker marker : markers) {
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

            team.setColor(Objects.requireNonNullElse(ChatColor.getByChar(Integer.toHexString((selection ? ReflectionHookOptions.selectionColor : created ? ReflectionHookOptions.terrainCreatedColor : ReflectionHookOptions.terrainColor).asRGB())), selection ? ChatColor.YELLOW : created ? ChatColor.GREEN : ChatColor.WHITE));
            team.addEntry(slime.getUniqueId().toString());
        } else if (class_BukkitBlockDisplay != null && class_BukkitBlockDisplay.isAssignableFrom(entity.getClass())) {
            Entity blockDisplay = (Entity) entity;
            BlockData block = BlockDisplayUtil.getBlock(blockDisplay);

            if (block.equals(ReflectionHookOptions.selectionBlock)) {
                BlockDisplayUtil.applyPropertiesToBlockDisplay(blockDisplay, ReflectionHookOptions.terrainBlock, ReflectionHookOptions.terrainCreatedColor);
            } else if (block.equals(ReflectionHookOptions.selectionEdgeBlock)) {
                BlockDisplayUtil.applyPropertiesToBlockDisplay(blockDisplay, ReflectionHookOptions.terrainEdgeBlock, ReflectionHookOptions.terrainCreatedColor);
            }

            Object setEntityData = clientboundSetEntityDataPacketAdapter.instance(blockDisplay.getEntityId(), method_Entity_getEntityData.invoke(method_CraftEntity_getHandle.invoke(entity)));

            ReflectionUtil.sendPacket(player, setEntityData);
        }
    }

    private interface ClientboundAddEntityPacketAdapter {
        Object instance(int entityId, UUID uuid, double x, double y, double z, Object type) throws InstantiationException, IllegalAccessException, InvocationTargetException;
    }

    private interface ClientboundSetEntityDataPacketAdapter {
        Object instance(int entityId, @NotNull Object entityData) throws InstantiationException, IllegalAccessException, InvocationTargetException;
    }

    private interface ClientboundRemoveEntitiesPacketAdapter {
        void send(@NotNull Player player, @NotNull Collection<PlayerUtil.SpawnedMarker> entities) throws InstantiationException, IllegalAccessException, InvocationTargetException;
    }
}
