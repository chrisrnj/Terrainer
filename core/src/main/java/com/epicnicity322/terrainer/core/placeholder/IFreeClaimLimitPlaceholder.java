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
import com.epicnicity322.terrainer.core.config.Configurations;
import com.epicnicity322.terrainer.core.placeholder.formatter.WorldPlaceholderFormatter;
import com.epicnicity322.terrainer.core.util.PlayerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public interface IFreeClaimLimitPlaceholder<O, P extends O> extends WorldPlaceholderFormatter<O, P> {
    @Override
    default @NotNull String name() {
        return "free-claimlimit";
    }

    @Override
    @Nullable
    default String formatOnlinePlaceholder(@Nullable P player, @NotNull String params, @Nullable UUID world) {
        if (world == null && Configurations.CONFIG.getConfiguration().getBoolean("Limits.Per World Claim Limit").orElse(false)) {
            return null;
        }

        PlayerUtil<P, ? super P> util;

        if (player == null || (util = playerUtil()).hasPermission(player, "terrainer.bypass.limit.claims")) {
            return Terrainer.lang().get("Placeholder Values.Infinite Limit");
        }

        return Integer.toString(util.claimLimit(player) - util.claimedTerrains(uuid(player), world));
    }
}
