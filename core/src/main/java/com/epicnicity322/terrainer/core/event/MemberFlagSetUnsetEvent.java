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
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * A flag event called when a flag is set or unset, whether to a specific player or to everyone in the terrain.
 */
public interface MemberFlagSetUnsetEvent extends FlagEvent, TerrainEvent {
    /**
     * @return The {@link UUID} of the player this flag is being specifically set to. {@code null} if the flag is being set globally on the terrain.
     * @see Terrain#memberFlags()
     */
    @Nullable UUID affectedMember();
}
