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
import com.epicnicity322.terrainer.core.event.SenderEvent;
import com.epicnicity322.terrainer.core.flag.Flag;
import org.jetbrains.annotations.NotNull;

/**
 * When a player sets a flag to a terrain using the Flag Management GUI or the command <u>/tr flag {@literal <flag> <inputArgs>}</u>.
 * <p>
 * The input is converted to the flag's data type using the flag's transformer {@link Flag#transformer()}.
 *
 * @param <T> The type of {@link #sender()}.
 */
public interface IUserFlagSetEvent<T> extends MemberFlagSetUnsetEvent, SenderEvent<T> {
    /**
     * @return The extra command arguments, if a command was used to add the flag to this terrain.
     */
    @NotNull
    String input();

    /**
     * Changes the provided input of the flag. This input will be used to generate the data of the flag.
     */
    void setInput(@NotNull String input);

    /**
     * @return Whether a GUI was used to edit this flag.
     */
    boolean isGui();
}
