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

package com.epicnicity322.terrainer.core.event.terrain;

import com.epicnicity322.terrainer.core.event.PlayerEvent;
import com.epicnicity322.terrainer.core.event.TerrainEvent;
import org.jetbrains.annotations.NotNull;

/**
 * An event that's called solely to determine whether a player can leave a terrain.
 *
 * @param <L> The location class of the platform.
 * @param <P> The player class of the platform.
 */
public interface ITerrainCanLeaveEvent<L, P> extends TerrainEvent, PlayerEvent<P> {
    /**
     * @return The location the player was before leaving the terrain.
     */
    @NotNull L from();

    /**
     * @return The location the player left the terrain.
     */
    @NotNull L to();

    /**
     * What caused the leave event.
     * <p>
     * Will never be {@link EnterLeaveReason#LEAVE_SERVER} or {@link EnterLeaveReason#REMOVE}.
     *
     * @return What caused the leave event.
     */
    @NotNull EnterLeaveReason reason();

    /**
     * @return Whether the player is allowed to leave the terrain.
     * @see com.epicnicity322.terrainer.core.event.TerrainEvent.CanEnterLeave
     */
    @NotNull CanEnterLeave canLeave();

    void setCanLeave(@NotNull CanEnterLeave canLeave);
}
