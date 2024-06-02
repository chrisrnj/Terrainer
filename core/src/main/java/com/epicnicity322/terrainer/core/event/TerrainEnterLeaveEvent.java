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

package com.epicnicity322.terrainer.core.event;

import com.epicnicity322.terrainer.core.terrain.Terrain;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

public interface TerrainEnterLeaveEvent<L> {
    /**
     * @return A set of terrains that the player entered/left.
     */
    @NotNull
    Set<Terrain> terrains();

    /**
     * @return A set of <b>all</b> terrains in {@link #from()} location.
     * @apiNote Useful for gathering data of <b>all</b> terrains in from location, without making extra queries for terrains.
     */
    @NotNull
    Set<Terrain> fromTerrains();

    /**
     * @return A set of <b>all</b> terrains in {@link #to()} location.
     * @apiNote Useful for gathering data of <b>all</b> terrains in to location, without making extra queries for terrains.
     */
    @NotNull
    Set<Terrain> toTerrains();

    /**
     * @return The location the player was before entering/leaving the terrain.
     */
    @NotNull
    L from();

    /**
     * @return The location the player entered/left the terrain.
     */
    @NotNull
    L to();

    /**
     * @return What caused the enter/leave event.
     */
    @NotNull
    EnterLeaveReason reason();

    /**
     * Reason why an enter or leave event is called.
     */
    enum EnterLeaveReason {
        /**
         * When a terrain is created where the player is located.
         */
        CREATE,
        /**
         * When a player dismounts an entity.
         */
        DISMOUNT,
        /**
         * When the player joins the server within a terrain.
         */
        JOIN_SERVER,
        /**
         * When the player leaves the server within a terrain.
         */
        LEAVE_SERVER,
        /**
         * When the player mounts an entity.
         */
        MOUNT,
        /**
         * When the player moves out of or into a terrain.
         */
        MOVE,
        /**
         * When the terrain is removed.
         */
        REMOVE,
        /**
         * When a player respawns in a terrain.
         */
        RESPAWN,
        /**
         * When the player teleports out of or into a terrain.
         */
        TELEPORT
    }
}
