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

package com.epicnicity322.terrainer.core.placeholder.formatter;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface PriorityPlaceholderFormatter<O, P extends O> extends PlaceholderFormatter<O, P> {
    int DEFAULT_PRIORITY = Integer.MAX_VALUE;

    default @NotNull String namespace() {
        return "_priority_";
    }

    private int priority(@NotNull String params) {
        String namespace = namespace();
        int namespaceIndex = params.indexOf(namespace);
        if (namespaceIndex < 0) return DEFAULT_PRIORITY;
        String priority = params.substring(namespaceIndex + namespace.length());
        int endingUnderline = priority.indexOf('_');
        if (endingUnderline > 0) priority = priority.substring(0, endingUnderline);

        try {
            return Integer.parseInt(priority);
        } catch (NumberFormatException e) {
            return DEFAULT_PRIORITY;
        }
    }

    @Override
    @Nullable
    default String formatPlaceholder(@Nullable O player, @NotNull String params) {
        return formatPlaceholder(player, params, priority(params));
    }

    @Override
    @Nullable
    default String formatOnlinePlaceholder(@Nullable P player, @NotNull String params) {
        return formatOnlinePlaceholder(player, params, priority(params));
    }

    @Nullable
    default String formatPlaceholder(@Nullable O player, @NotNull String params, int priority) {
        return player != null && isOnline(player) ? this.formatOnlinePlaceholder((P) player, params, priority) : this.formatOnlinePlaceholder(null, params, priority);
    }

    @Nullable
    default String formatOnlinePlaceholder(@Nullable P player, @NotNull String params, int priority) {
        return null;
    }
}
