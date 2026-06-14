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

package com.epicnicity322.terrainer.core.event.terrain;

import com.epicnicity322.terrainer.core.event.SenderEvent;
import com.epicnicity322.terrainer.core.event.TerrainEvent;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * An event called right before a terrain is transferred to a new owner. Triggered by '/tr transfer' command.
 * <p>
 * The transfer guarantees that the owner has full authorization to own the terrain. The limit and permission checks
 * are always done beforehand unless the command sender has used the '--force' option. In which case you may check its
 * usage with {@link #isForced()}.
 */
public interface IUserTransferTerrainEvent<T> extends TerrainEvent, SenderEvent<T> {
    /**
     * The new owner of the terrain, null if it's console.
     * <p>
     * This is guaranteed to be non-null if the {@link #isForced()} value is true, since console always has
     * authorization to own terrains and the transfer doesn't need to be forced.
     */
    @Nullable UUID newOwner();

    /**
     * @return Whether the '--force' option was used when transferring the terrain, bypassing permission checks.
     */
    boolean isForced();
}
