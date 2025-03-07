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

package com.epicnicity322.terrainer.core.flag;

import org.jetbrains.annotations.NotNull;

import java.io.Serial;

/**
 * Exception thrown when the flag's data type could not be transformed from a String using the {@link Flag#transformer()}.
 * <p>
 * The message of this exception will be sent to the player, informing why the flag could not be applied to the terrain.
 */
public class FlagTransformException extends RuntimeException {
    @Serial
    private static final long serialVersionUID = -4350695619649389840L;

    public FlagTransformException(@NotNull String message) {
        super(message);
    }
}
