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

package com.epicnicity322.terrainer.core.placeholder;

import com.epicnicity322.terrainer.core.placeholder.formatter.PlaceholderFormatter;
import com.epicnicity322.terrainer.core.terrain.TerrainManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.UUID;
import java.util.stream.StreamSupport;

public interface IOwningTerrainsAmountPlaceholder<O, P extends O> extends PlaceholderFormatter<O, P> {
    @Override
    @NotNull
    default String name() {
        return "owning-terrains-amount";
    }

    @Override
    @Nullable
    default String formatPlaceholder(@Nullable O player, @NotNull String params) {
        UUID id = player == null ? null : uuid(player);
        return Long.toString(StreamSupport.stream(TerrainManager.allTerrains().spliterator(), false).filter(t -> Objects.equals(id, t.owner())).count());
    }
}
