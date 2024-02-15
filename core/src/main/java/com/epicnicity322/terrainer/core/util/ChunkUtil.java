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

package com.epicnicity322.terrainer.core.util;

import com.epicnicity322.terrainer.core.Chunk;
import com.epicnicity322.terrainer.core.terrain.Terrain;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedList;
import java.util.UUID;

public abstract class ChunkUtil<C> {
    protected abstract int x(@NotNull C chunk);

    protected abstract int z(@NotNull C chunk);

    protected abstract @NotNull C @NotNull [] loadedChunks(@NotNull UUID world);

    /**
     * Tests whether a terrain is within any loaded chunk.
     *
     * @param terrain The terrain to test.
     * @return Whether this terrain is/should be active.
     */
    public @NotNull LinkedList<Chunk> loadedChunks(@NotNull Terrain terrain) {
        var list = new LinkedList<Chunk>();
        for (C chunk : loadedChunks(terrain.world())) {
            int x = x(chunk), z = z(chunk);
            if (terrain.isWithinChunk(x, z)) list.add(new Chunk(x, z));
        }
        return list;
    }
}
