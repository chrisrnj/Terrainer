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

import com.epicnicity322.terrainer.bukkit.TerrainerPlugin;
import com.epicnicity322.terrainer.core.event.terrain.ITerrainEnterEvent;
import com.epicnicity322.terrainer.core.terrain.Terrain;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class TerrainEnterEvent extends Event implements ITerrainEnterEvent<Location, Player>, Cancellable {
    private static final @NotNull HandlerList handlers = new TerrainerPlugin.EnterLeaveHandlerList();
    private final @NotNull Location from;
    private final @NotNull Location to;
    private final @NotNull Player player;
    private final @NotNull Terrain terrain;
    private final @NotNull EnterLeaveReason reason;
    private boolean cancelled;

    public TerrainEnterEvent(@NotNull Location from, @NotNull Location to, @NotNull Player player, @NotNull Terrain terrain, @NotNull EnterLeaveReason reason, boolean cancelled) {
        this.from = from;
        this.to = to;
        this.player = player;
        this.terrain = terrain;
        this.reason = reason;
        this.cancelled = cancelled;
    }

    public static @NotNull HandlerList getHandlerList() {
        return handlers;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return handlers;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    @Override
    public @NotNull Location from() {
        return from;
    }

    @Override
    public @NotNull Location to() {
        return to;
    }

    @Override
    public @NotNull EnterLeaveReason reason() {
        return reason;
    }

    @Override
    public @NotNull Player player() {
        return player;
    }

    @Override
    public @NotNull Terrain terrain() {
        return terrain;
    }
}
