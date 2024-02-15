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

package com.epicnicity322.terrainer.bukkit.util;

import com.epicnicity322.terrainer.core.util.ChunkUtil;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public final class BukkitChunkUtil extends ChunkUtil<Chunk> {
    @Override
    protected int x(@NotNull Chunk chunk) {
        return chunk.getX();
    }

    @Override
    protected int z(@NotNull Chunk chunk) {
        return chunk.getZ();
    }

    @Override
    protected @NotNull Chunk @NotNull [] loadedChunks(@NotNull UUID world) {
        World world1 = Bukkit.getWorld(world);
        if (world1 == null) return new Chunk[0];
        return world1.getLoadedChunks();
    }
}
