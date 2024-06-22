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

import com.epicnicity322.terrainer.core.Terrainer;
import com.epicnicity322.terrainer.core.placeholder.formatter.TerrainPlaceholderFormatter;
import com.epicnicity322.terrainer.core.terrain.Terrain;
import com.epicnicity322.terrainer.core.terrain.WorldTerrain;
import com.epicnicity322.terrainer.core.util.TerrainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface ITerrainMembersPlaceholder<O, P extends O> extends TerrainPlaceholderFormatter<O, P> {
    @Override
    default @NotNull String name() {
        return "terrain-members";
    }

    @Override
    default boolean terrainFilter(@NotNull Terrain terrain, @Nullable O player, @NotNull String params) {
        return !(terrain instanceof WorldTerrain);
    }

    @Override
    @Nullable
    default String formatPlaceholder(@Nullable O player, @NotNull String params, @Nullable Terrain terrain) {
        if (terrain == null) return Terrainer.lang().get("Placeholder Values.Unknown Terrain");
        return TerrainerUtil.listToString(terrain.members().view(), uuid -> playerUtil().ownerName(uuid));
    }
}
