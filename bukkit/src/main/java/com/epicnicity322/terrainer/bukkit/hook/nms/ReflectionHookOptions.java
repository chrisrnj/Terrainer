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

package com.epicnicity322.terrainer.bukkit.hook.nms;

import com.epicnicity322.terrainer.core.config.Configurations;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.block.data.BlockData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class ReflectionHookOptions {
    static boolean geyserOption = Configurations.CONFIG.config().getBoolean("Markers.Bedrock Players See Slime Entity").orElse(true);
    static @Nullable BlockData selectionBlock = Material.GLOWSTONE.createBlockData();
    static @Nullable BlockData selectionEdgeBlock = Material.GOLD_BLOCK.createBlockData();
    static @Nullable BlockData terrainBlock = Material.DIAMOND_BLOCK.createBlockData();
    static @Nullable BlockData terrainEdgeBlock = Material.GLASS.createBlockData();
    static @NotNull Color selectionColor = Color.YELLOW;
    static @NotNull Color terrainColor = Color.WHITE;
    static @NotNull Color terrainCreatedColor = Color.LIME;
    static boolean viaVersionOption = Configurations.CONFIG.config().getBoolean("Markers.Unsupported Version Players See Slime Entity").orElse(true);

    public static void setUnsupportedVersionPlayersSeeSlimeEntity(boolean value) {
        viaVersionOption = value;
    }

    public static void setBedrockPlayersSeeSlimeEntity(boolean value) {
        geyserOption = value;
    }

    public static void setMarkerBlocks(@Nullable Material selectionBlock, @Nullable Material selectionEdgeBlock, @Nullable Material terrainBlock, @Nullable Material terrainEdgeBlock) {
        if (selectionBlock == null) selectionBlock = Material.GLOWSTONE;
        if (selectionEdgeBlock == null) selectionEdgeBlock = Material.GOLD_BLOCK;
        if (terrainBlock == null) terrainBlock = Material.DIAMOND_BLOCK;
        if (terrainEdgeBlock == null) terrainEdgeBlock = Material.GLASS;

        ReflectionHookOptions.selectionBlock = selectionBlock.isBlock() ? selectionBlock.createBlockData() : null;
        ReflectionHookOptions.selectionEdgeBlock = selectionEdgeBlock.isBlock() ? selectionEdgeBlock.createBlockData() : null;
        ReflectionHookOptions.terrainBlock = terrainBlock.isBlock() ? terrainBlock.createBlockData() : null;
        ReflectionHookOptions.terrainEdgeBlock = terrainEdgeBlock.isBlock() ? terrainEdgeBlock.createBlockData() : null;
    }

    public static void setMarkerColors(int selection, int terrain, int created) {
        if (selection >> 24 != 0) selection = 0xFFFFFF;
        if (terrain >> 24 != 0) terrain = 0xFFFFFF;
        if (created >> 24 != 0) created = 0xFFFFFF;

        ReflectionHookOptions.selectionColor = Color.fromRGB(selection);
        ReflectionHookOptions.terrainColor = Color.fromRGB(terrain);
        ReflectionHookOptions.terrainCreatedColor = Color.fromRGB(created);
    }
}
