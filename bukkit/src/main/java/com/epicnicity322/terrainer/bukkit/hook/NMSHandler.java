/*
 * Terrainer - A minecraft terrain claiming protection plugin.
 * Copyright (C) 2025 Christiano Rangel
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

package com.epicnicity322.terrainer.bukkit.hook;

import com.epicnicity322.terrainer.core.util.PlayerUtil;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public interface NMSHandler {
    /**
     * Spawns a marker entity at the location.
     *
     * @param player    The player to send the fake entity spawn packet to.
     * @param x         X coordinate.
     * @param y         Y coordinate.
     * @param z         Z coordinate.
     * @param edge      Whether this is the edge of the marker.
     * @param selection Whether this should be a selection or terrain marker.
     * @return The entity's IDs.
     */
    @NotNull
    PlayerUtil.SpawnedMarker spawnMarkerEntity(@NotNull Player player, int x, int y, int z, boolean edge, boolean selection) throws Throwable;

    /**
     * Kills an entity with the ID.
     *
     * @param player The player to send the entity remove packet to.
     * @param marker The marker entity to remove.
     */
    void killEntity(@NotNull Player player, @NotNull PlayerUtil.SpawnedMarker marker) throws Throwable;

    /**
     * Changes the color of a marker's glow effect.
     *
     * @param marker The marker to colorize.
     * @param player The player to colorize the marker to.
     */
    void updateSelectionMarkerToTerrainMarker(@NotNull PlayerUtil.SpawnedMarker marker, @NotNull Player player) throws Throwable;
}
