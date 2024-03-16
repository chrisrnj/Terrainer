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

package com.epicnicity322.terrainer.core.placeholder.formatter;

import com.epicnicity322.terrainer.core.WorldCoordinate;
import com.epicnicity322.terrainer.core.terrain.Terrain;
import com.epicnicity322.terrainer.core.terrain.TerrainManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import java.util.stream.StreamSupport;

public interface TerrainPlaceholderFormatter<O, P extends O> extends PriorityPlaceholderFormatter<O, P> {
    String TERRAIN_NAMESPACE = "_terrain_";

    default boolean terrainFilter(@NotNull Terrain terrain, @Nullable O player, @NotNull String params) {
        return true;
    }

    private @Nullable Terrain terrain(@Nullable O player, @NotNull String params, int priority) {
        int terrainIndex = params.indexOf(TERRAIN_NAMESPACE);

        if (terrainIndex < 0) {
            // Getting terrain at location.
            if (player == null) return null; // Console
            WorldCoordinate loc = location(player);
            if (loc == null) return null;
            return TerrainManager.terrainsAt(loc).stream().filter(t -> t.priority() <= priority).filter(t -> terrainFilter(t, player, params)).findFirst().orElse(null);
        }

        String terrain = params.substring(terrainIndex + TERRAIN_NAMESPACE.length());
        int endingUnderline = terrain.indexOf('_');
        if (endingUnderline > 0) terrain = terrain.substring(0, endingUnderline);
        if (terrain.isEmpty()) return null;

        try {
            // Getting terrain by ID.
            return TerrainManager.terrainByID(UUID.fromString(terrain));
        } catch (IllegalArgumentException e) {
            // Getting terrain by name.
            String finalTerrain = terrain;
            return StreamSupport.stream(TerrainManager.allTerrains().spliterator(), false)
                    .filter(t -> t.name().equals(finalTerrain))
                    .filter(t -> t.priority() <= priority)
                    .filter(t -> terrainFilter(t, player, params))
                    .min(TerrainManager.PRIORITY_COMPARATOR).orElse(null);
        }
    }

    @Override
    @Nullable
    default String formatPlaceholder(@Nullable O player, @NotNull String params, int priority) {
        return formatPlaceholder(player, params, terrain(player, params, priority));
    }

    @Override
    @Nullable
    default String formatOnlinePlaceholder(@Nullable P player, @NotNull String params, int priority) {
        return formatOnlinePlaceholder(player, params, terrain(player, params, priority));
    }

    @Nullable
    default String formatPlaceholder(@Nullable O player, @NotNull String params, @Nullable Terrain terrain) {
        return player != null && isOnline(player) ? this.formatOnlinePlaceholder((P) player, params, terrain) : this.formatOnlinePlaceholder(null, params, terrain);
    }

    @Nullable
    default String formatOnlinePlaceholder(@Nullable P player, @NotNull String params, @Nullable Terrain terrain) {
        return null;
    }
}
