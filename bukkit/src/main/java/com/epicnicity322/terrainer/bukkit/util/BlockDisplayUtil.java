package com.epicnicity322.terrainer.bukkit.util;

import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.level.World;
import org.bukkit.Color;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Entity;
import org.bukkit.util.Transformation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

import java.lang.reflect.Field;

public final class BlockDisplayUtil {
    private static final @NotNull Transformation transformation = new Transformation(new Vector3f(0, 0, 0), new AxisAngle4f(0, 0, 0, 0), new Vector3f(1.001f, 1.001f, 1.001f), new AxisAngle4f(0, 0, 0, 0));
    private static final @NotNull org.bukkit.entity.Display.Brightness brightness = new org.bukkit.entity.Display.Brightness(14, 14);

    public static @Nullable EntityTypes<?> findBlockDisplayType() {
        String type = EntityTypes.class.getName() + "<" + Display.BlockDisplay.class.getName() + ">";

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

    public static @NotNull net.minecraft.world.entity.Entity nmsBlockDisplay(@NotNull EntityTypes<?> type, @NotNull World world) {
        return new Display.BlockDisplay(type, world);
    }

    public static @NotNull Entity applyPropertiesToBlockDisplay(@NotNull Entity blockDisplay, @NotNull BlockData blockType, @NotNull Color color) {
        BlockDisplay bukkitDisplay = (BlockDisplay) blockDisplay;
        bukkitDisplay.setTransformation(transformation);
        bukkitDisplay.setBrightness(brightness);
        bukkitDisplay.setBlock(blockType);
        bukkitDisplay.setGlowColorOverride(color);
        return bukkitDisplay;
    }

    public static @NotNull BlockData getBlock(@NotNull Entity blockDisplay) {
        return ((BlockDisplay) blockDisplay).getBlock();
    }
}
