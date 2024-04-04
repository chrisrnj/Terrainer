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
import com.epicnicity322.terrainer.core.terrain.Flag;
import org.jetbrains.annotations.NotNull;

/**
 * When a flag is set by any means to a terrain.
 *
 * @param <T> The data type of the flag being set.
 */
public interface IFlagSetEvent<T> extends MemberFlagSetUnsetEvent {
    @Override
    @NotNull Flag<T> flag();

    /**
     * @return The data being set to the flag. Should never be null.
     */
    @NotNull T data();

    /**
     * Sets the data that this flag should be set to.
     *
     * @param data The data to set in this terrain.
     */
    void setData(@NotNull T data);
}
