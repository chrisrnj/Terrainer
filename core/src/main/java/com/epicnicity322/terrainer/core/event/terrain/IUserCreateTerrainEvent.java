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

import com.epicnicity322.terrainer.core.event.SenderEvent;
import com.epicnicity322.terrainer.core.flag.Flags;

/**
 * When a terrain is claimed or defined using commands.
 */
public interface IUserCreateTerrainEvent<T> extends ITerrainAddEvent, SenderEvent<T> {
    /**
     * Whether the terrain is being defined rather than claimed.
     * <p>
     * Terrains created with the 'define' command must be protected from everything, for example: explosion damage.
     * <p>
     * When claimed, terrains do not have any flag set unless specified in config, to ensure default behavior, but
     * defined terrains must have flags such as {@link Flags#EXPLOSION_DAMAGE} set to denied, to protect against explosion
     * damages to blocks within.
     *
     * @return If the define command is being used.
     */
    boolean isDefine();
}
