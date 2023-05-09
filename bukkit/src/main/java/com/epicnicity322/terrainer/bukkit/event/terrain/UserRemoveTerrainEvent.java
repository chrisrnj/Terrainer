/*
 * Terrainer - A minecraft terrain claiming protection plugin.
 * Copyright (C) 2023 Christiano Rangel
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

import com.epicnicity322.terrainer.core.event.terrain.IUserRemoveTerrainEvent;
import com.epicnicity322.terrainer.core.terrain.Terrain;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * When a terrain is removed by someone using commands.
 */
public class UserRemoveTerrainEvent extends Event implements IUserRemoveTerrainEvent<CommandSender> {
    private static final @NotNull HandlerList handlers = new HandlerList();
    private final @NotNull Terrain terrain;
    private final @NotNull World world;
    private final @NotNull CommandSender sender;

    public UserRemoveTerrainEvent(@NotNull Terrain terrain, @NotNull World world, @NotNull CommandSender sender) {
        this.terrain = terrain;
        this.world = world;
        this.sender = sender;
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

    public @NotNull World world() {
        return world;
    }
}
