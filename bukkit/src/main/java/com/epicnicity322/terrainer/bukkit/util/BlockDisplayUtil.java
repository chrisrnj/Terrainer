/*
 * Terrainer - A minecraft terrain claiming protection plugin.
 * Copyright (C) 2026 Christiano Rangel
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

import org.bukkit.Color;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Entity;
import org.bukkit.util.Transformation;
import org.jetbrains.annotations.NotNull;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

public final class BlockDisplayUtil {
    private static final @NotNull Transformation transformation = new Transformation(new Vector3f(0, 0, 0), new AxisAngle4f(0, 0, 0, 0), new Vector3f(1.001f, 1.001f, 1.001f), new AxisAngle4f(0, 0, 0, 0));
    private static final @NotNull org.bukkit.entity.Display.Brightness brightness = new org.bukkit.entity.Display.Brightness(14, 14);

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
