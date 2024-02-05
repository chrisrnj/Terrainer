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

package com.epicnicity322.terrainer.core.event.terrain;

import com.epicnicity322.terrainer.core.config.Configurations;
import com.epicnicity322.terrainer.core.event.SenderEvent;
import com.epicnicity322.terrainer.core.event.TerrainEvent;
import org.jetbrains.annotations.NotNull;

/**
 * When a terrain is named, whether by creation or rename. This event will not be called if the name was in the
 * blacklist of terrain names.
 *
 * @see #isBlackListed(String)
 */
public interface IUserTerrainNameEvent<T> extends TerrainEvent, SenderEvent<T> {
    static boolean isBlackListed(@NotNull String name) {
        return Configurations.CONFIG.getConfiguration().getCollection("Black Listed Names", Object::toString).contains(name);
    }

    /**
     * The previous name of this terrain. If this event was called because of a creation, the previous name will be the
     * first characters of the terrain's {@link java.util.UUID}.
     * <p>
     * Cancelling the event will make so the terrain has this name.
     *
     * @return The name that was set on the terrain.
     */
    @NotNull String previousName();

    /**
     * @return The new name of the terrain.
     */
    @NotNull String newName();

    /**
     * The reason that caused this name event.
     *
     * @return Why the terrain was named/renamed.
     */
    @NotNull NameReason reason();

    enum NameReason {
        CREATION, RENAME
    }
}
