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

import com.epicnicity322.terrainer.core.WorldCoordinate;
import com.epicnicity322.terrainer.core.util.PlayerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * @param <O> Offline Player
 * @param <P> Online Player
 */
public interface PlaceholderFormatter<O, P extends O> {
    @NotNull
    String name();

    default @Nullable String formatPlaceholder(@Nullable O player, @NotNull String params) {
        return player == null ? formatOnlinePlaceholder(null, params) : (isOnline(player) ? this.formatOnlinePlaceholder((P) player, params) : null);
    }

    default @Nullable String formatOnlinePlaceholder(@Nullable P player, @NotNull String params) {
        return null;
    }

    default boolean isPlayerRelevant() {
        return true;
    }

    default @NotNull String suggestedSuffix() {
        return "";
    }

    boolean isOnline(@NotNull O offlinePlayer);

    @NotNull
    UUID uuid(@NotNull O offlinePlayer);

    @Nullable
    WorldCoordinate location(@NotNull O player);

    @Nullable
    UUID world(@NotNull O offlinePlayer);

    @Nullable
    UUID world(@NotNull String name);

    @NotNull
    PlayerUtil<P, ? super P> playerUtil();
}
