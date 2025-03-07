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

package com.epicnicity322.terrainer.core.event.flag;

import com.epicnicity322.terrainer.core.event.MemberFlagSetUnsetEvent;
import com.epicnicity322.terrainer.core.flag.Flag;
import com.epicnicity322.terrainer.core.terrain.WorldTerrain;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * When a flag is unset from a terrain by any means.
 *
 * @param <T> The data type of the flag being removed.
 */
public interface IFlagUnsetEvent<T> extends MemberFlagSetUnsetEvent {
    @Override
    @NotNull
    Flag<T> flag();

    /**
     * The new data of this flag. Could be a default value, or null if the terrain accepts null data.
     * <p>
     * The data of the flag after being <b>unset</b> will always be null if there is an {@link #affectedMember()}.
     *
     * @return The new data of this flag.
     * @see WorldTerrain#usesDefaultFlagValues()
     */
    @Nullable
    T data();
}
