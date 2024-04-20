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
import com.epicnicity322.terrainer.core.placeholder.formatter.PriorityPlaceholderFormatter;
import com.epicnicity322.terrainer.core.terrain.Terrain;
import com.epicnicity322.terrainer.core.terrain.TerrainManager;
import com.epicnicity322.terrainer.core.util.PlayerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.StreamSupport;

public interface ITopAssociatedTerrainsPlaceholder<O, P extends O> extends PriorityPlaceholderFormatter<O, P> {
    static @NotNull Optional<UUID> getTop(int topRank, Function<UUID, Long> scoreGetter) {
        return StreamSupport.stream(TerrainManager.allTerrains().spliterator(), false).map(Terrain::owner).filter(Objects::nonNull).distinct().sorted(Comparator.comparingLong(scoreGetter::apply).reversed()).skip(Math.max(topRank - 1, 0)).findFirst();
    }

    @Override
    default @NotNull String name() {
        return "top-associated-terrains";
    }

    @Override
    default int defaultPriority() {
        return 1;
    }

    @Override
    @NotNull
    default String namespace() {
        return "top-associated-terrains_";
    }

    @Override
    @Nullable
    default String formatPlaceholder(@Nullable O player, @NotNull String params, int priority) {
        PlayerUtil<P, ? super P> playerUtil = playerUtil();
        return getTop(priority, uuid -> StreamSupport.stream(TerrainManager.allTerrains().spliterator(), false).filter(t -> IAssociatedTerrainsPlaceholder.hasAnyRelations(uuid, t)).count()).map(playerUtil::getOwnerName).orElseGet(() -> Terrainer.lang().get("Placeholder Values.No One Top"));
    }

    @Override
    default boolean isPlayerRelevant() {
        return false;
    }
}
