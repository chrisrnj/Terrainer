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

/**
 * An event that had a terrain involved.
 */
public interface TerrainEvent {
    /**
     * @return The terrain involved in this event.
     */
    @NotNull Terrain terrain();

    /**
     * Determine whether a player can enter or leave a terrain.
     */
    enum CanEnterLeave {
        /**
         * Allow the player to enter or leave.
         */
        ALLOW,
        /**
         * No verdict made, allow entry or leave.
         */
        DEFAULT,
        /**
         * Deny the player from entering or leaving.
         */
        DENY
    }

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
