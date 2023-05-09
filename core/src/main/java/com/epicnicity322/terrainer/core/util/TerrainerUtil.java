/*
 * Terrainer - A minecraft terrain claiming protection plugin.
 * Copyright (C) 2023 Christiano Rangel
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

package com.epicnicity322.terrainer.core.util;

import com.epicnicity322.terrainer.core.Terrainer;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.function.Function;

public final class TerrainerUtil {
    private TerrainerUtil() {
    }

    public static <T> @NotNull String listToString(@NotNull Collection<T> list, @NotNull Function<T, String> name, @NotNull String and, @NotNull String none) {
        if (list.isEmpty()) return Terrainer.lang().get("");
        StringBuilder formatted = new StringBuilder();
        int size = list.size();
        int i = 1;
        for (T t : list) {
            formatted.append(name.apply(t));
            if (i + 1 == size) {
                formatted.append(", ").append(and).append(" ");
            } else if (i != size) {
                formatted.append(", ");
            }
            i++;
        }
        formatted.append('.');
        return formatted.toString();
    }
}
