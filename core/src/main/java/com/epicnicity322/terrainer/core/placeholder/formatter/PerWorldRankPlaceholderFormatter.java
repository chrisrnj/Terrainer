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

import java.util.UUID;

public interface PerWorldRankPlaceholderFormatter<O, P extends O> extends PriorityPlaceholderFormatter<O, P> {

    private @Nullable UUID world(@Nullable O player, @NotNull String params) {
        int namespaceIndex = params.indexOf(WorldPlaceholderFormatter.WORLD_NAMESPACE);

        if (namespaceIndex < 0) return player == null ? null : world(player);

        String world = params.substring(namespaceIndex + WorldPlaceholderFormatter.WORLD_NAMESPACE.length());
        int endingUnderline = world.indexOf('_');
        if (endingUnderline > 0) world = world.substring(0, endingUnderline);

        return world(world);
    }

    @Override
    @Nullable
    default String formatPlaceholder(@Nullable O player, @NotNull String params, int position) {
        return formatPlaceholder(player, params, world(player, params), position);
    }

    @Override
    @Nullable
    default String formatOnlinePlaceholder(@Nullable P player, @NotNull String params, int position) {
        return formatOnlinePlaceholder(player, params, world(player, params), position);
    }

    @Nullable
    default String formatPlaceholder(@Nullable O player, @NotNull String params, @Nullable UUID world, int position) {
        return player != null && isOnline(player) ? this.formatOnlinePlaceholder((P) player, params, world, position) : this.formatOnlinePlaceholder(null, params, world, position);
    }

    @Nullable
    default String formatOnlinePlaceholder(@Nullable P player, @NotNull String params, @Nullable UUID world, int position) {
        return null;
    }
}
