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
import com.epicnicity322.terrainer.bukkit.util.NMSHandler;
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
import org.bukkit.entity.Player;
import org.bukkit.entity.Slime;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

public final class ReflectionHook implements NMSHandler {
    private static final @NotNull Vec3D zero = new Vec3D(0, 0, 0);
    private static final @Nullable Method method_CraftWorld_getHandle;
    private static final @Nullable Method method_CraftEntity_getBukkitEntity = ReflectionUtil.getMethod(Entity.class, "getBukkitEntity");
    private static final @Nullable Method method_Entity_getDataWatcher = ReflectionUtil.findMethodByType(Entity.class, DataWatcher.class, false);
    private static final @Nullable Constructor<?> constructor_PacketPlayOutEntityMetadata;
    private static final boolean newPacketMetadataConstructor;
    private static final @Nullable EntityTypes<?> slimeEntityType = findSlimeEntityType();

    static {
        Class<?> class_CraftWorld = ReflectionUtil.getClass("CraftWorld", PackageType.CRAFTBUKKIT);
        if (class_CraftWorld == null) {
            method_CraftWorld_getHandle = null;
        } else {
            method_CraftWorld_getHandle = ReflectionUtil.getMethod(class_CraftWorld, "getHandle");
        }
        Constructor<?> oldConstructor = ReflectionUtil.getConstructor(PacketPlayOutEntityMetadata.class, int.class, DataWatcher.class, boolean.class);
        if (oldConstructor == null) {
            constructor_PacketPlayOutEntityMetadata = null;
            newPacketMetadataConstructor = ReflectionUtil.getConstructor(PacketPlayOutEntityMetadata.class, int.class, List.class) != null;
        } else {
            constructor_PacketPlayOutEntityMetadata = oldConstructor;
            newPacketMetadataConstructor = false;
        }
    }

    private static EntityTypes<?> findSlimeEntityType() {
        for (Field f : EntityTypes.class.getFields()) {
            if (f.getGenericType().getTypeName().equals("net.minecraft.world.entity.EntityTypes<net.minecraft.world.entity.monster.EntitySlime>")) {
                try {
                    return (EntityTypes<?>) f.get(null);
                } catch (Exception ignored) {
                }
                break;
            }
        }
        return null;
    }

    @Override
    public @NotNull PlayerUtil.SpawnedMarker spawnMarkerEntity(@NotNull Player player, int x, int y, int z) throws Throwable {
        assert method_CraftWorld_getHandle != null;
        assert slimeEntityType != null;
        EntitySlime slime = new EntitySlime((EntityTypes<? extends EntitySlime>) slimeEntityType, (World) method_CraftWorld_getHandle.invoke(player.getWorld()));
        assert method_CraftEntity_getBukkitEntity != null;
        Slime bukkitSlime = (Slime) method_CraftEntity_getBukkitEntity.invoke(slime);
        int id = bukkitSlime.getEntityId();

        bukkitSlime.setGlowing(true);
        bukkitSlime.setSize(2);
        bukkitSlime.setCollidable(false);
        bukkitSlime.setInvisible(true);

        ReflectionUtil.sendPacket(player, new PacketPlayOutSpawnEntity(id, bukkitSlime.getUniqueId(), x + 0.5, y, z + 0.5, 0, 0, slimeEntityType, 0, zero, 0));

        assert method_Entity_getDataWatcher != null;
        DataWatcher dataWatcher = (DataWatcher) method_Entity_getDataWatcher.invoke(slime);

        if (newPacketMetadataConstructor) {
            ReflectionUtil.sendPacket(player, new PacketPlayOutEntityMetadata(id, dataWatcher.c()));
        } else {
            assert constructor_PacketPlayOutEntityMetadata != null;
            ReflectionUtil.sendPacket(player, constructor_PacketPlayOutEntityMetadata.newInstance(id, dataWatcher, false));
        }
        return new PlayerUtil.SpawnedMarker(id, bukkitSlime.getUniqueId());
    }

    @Override
    public void killEntity(@NotNull Player player, int entityID) {
        PacketPlayOutEntityDestroy packet = new PacketPlayOutEntityDestroy(entityID);
        ReflectionUtil.sendPacket(player, packet);
    }
}
