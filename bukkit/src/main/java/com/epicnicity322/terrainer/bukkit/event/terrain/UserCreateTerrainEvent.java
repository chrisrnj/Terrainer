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

package com.epicnicity322.terrainer.bukkit.event.terrain;

import com.epicnicity322.terrainer.core.event.terrain.IUserCreateTerrainEvent;
import com.epicnicity322.terrainer.core.terrain.Terrain;
import org.bukkit.command.CommandSender;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * When a terrain is claimed or defined using commands.
 * <p>
 * Useful for adding flags if the terrain is being defined rather than claimed. Defined terrains must have protections
 * against everything.
 */
public class UserCreateTerrainEvent extends Event implements IUserCreateTerrainEvent<CommandSender> {
    private static final @NotNull HandlerList handlers = new HandlerList();
    private final @NotNull Terrain terrain;
    private final @NotNull CommandSender sender;
    private final boolean define;

    public UserCreateTerrainEvent(@NotNull Terrain terrain, @NotNull CommandSender sender, boolean define) {
        this.terrain = terrain;
        this.sender = sender;
        this.define = define;
    }

    public static @NotNull HandlerList getHandlerList() {
        return handlers;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return handlers;
    }

    @Override
    public @NotNull Terrain terrain() {
        return terrain;
    }

    @Override
    public @NotNull CommandSender sender() {
        return sender;
    }

    @Override
    public boolean isDefine() {
        return define;
    }
}
