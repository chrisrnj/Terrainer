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
import com.epicnicity322.terrainer.core.flag.Flag;
import com.epicnicity322.terrainer.core.flag.Flags;
import com.epicnicity322.terrainer.core.placeholder.formatter.TerrainPlaceholderFormatter;
import com.epicnicity322.terrainer.core.terrain.Terrain;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface IFlagDataPlaceholder<O, P extends O> extends TerrainPlaceholderFormatter<O, P> {
    @Override
    default @NotNull String name() {
        return "flag-data";
    }

    @Override
    @NotNull
    default String suggestedSuffix() {
        return "_<flag id>";
    }

    private @Nullable Flag<?> flag(@NotNull String params) {
        String namespace = name() + '_';
        int namespaceIndex = params.indexOf(namespace);
        if (namespaceIndex < 0) return null;
        String flagID = params.substring(namespaceIndex + namespace.length());
        int endingUnderline = flagID.indexOf('_');
        if (endingUnderline > 0) flagID = flagID.substring(0, endingUnderline);

        return Flags.matchFlag(flagID);
    }

    @Override
    default boolean terrainFilter(@NotNull Terrain terrain, @Nullable O player, @NotNull String params) {
        Flag<?> flag = flag(params);
        if (flag == null) return false;
        return (player != null && terrain.memberFlags().getData(uuid(player), flag) != null) || terrain.flags().getData(flag) != null;
    }

    @Override
    @Nullable
    default String formatPlaceholder(@Nullable O player, @NotNull String params, @Nullable Terrain terrain) {
        Flag<?> flag = flag(params);
        if (flag == null) return null; // Unknown flag.
        if (terrain == null) return Terrainer.lang().get("Placeholder Values.Flag Undefined");

        return getAndFormatData(player, terrain, flag);
    }

    private <T> @NotNull String getAndFormatData(@Nullable O player, @NotNull Terrain terrain, @NotNull Flag<T> flag) {
        T data = null;
        if (player != null) data = terrain.memberFlags().getData(uuid(player), flag);
        if (data == null) data = terrain.flags().getData(flag);

        return data == null ? Terrainer.lang().get("Placeholder Values.Flag Undefined") : flag.formatter().apply(data);
    }
}
