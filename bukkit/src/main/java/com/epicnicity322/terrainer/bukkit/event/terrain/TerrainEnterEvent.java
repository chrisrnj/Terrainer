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

import com.epicnicity322.terrainer.core.event.terrain.ITerrainEnterEvent;
import com.epicnicity322.terrainer.core.terrain.Terrain;
import com.epicnicity322.terrainer.core.terrain.TerrainManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Set;

/**
 * When a player has entered a terrain.
 */
public class TerrainEnterEvent extends Event implements ITerrainEnterEvent<Location, Player>, Cancellable {
    private static final @NotNull HandlerList handlers = new HandlerList();
    private final @NotNull Location from;
    private final @NotNull Location to;
    private final @NotNull Player player;
    private final @NotNull Set<Terrain> terrains;
    private final @NotNull EnterLeaveReason reason;
    private @Nullable Set<Terrain> fromTerrains;
    private @Nullable Set<Terrain> toTerrains;
    private boolean cancelled = false;

    public TerrainEnterEvent(@NotNull Location from, @NotNull Location to, @NotNull Player player, @NotNull Set<Terrain> terrains, @Nullable Set<Terrain> fromTerrains, @Nullable Set<Terrain> toTerrains, @NotNull EnterLeaveReason reason) {
        super(!Bukkit.isPrimaryThread());
        this.from = from;
        this.to = to;
        this.player = player;
        this.terrains = terrains;
        this.fromTerrains = fromTerrains;
        this.toTerrains = toTerrains;
        this.reason = reason;
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
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }

    @Override
    public @NotNull Set<Terrain> terrains() {
        return Collections.unmodifiableSet(terrains);
    }

    @Override
    public @NotNull Set<Terrain> fromTerrains() {
        if (fromTerrains == null) {
            fromTerrains = TerrainManager.terrainsAt(from.getWorld().getUID(), from.getBlockX(), from.getBlockY(), from.getBlockZ());
        }
        return Collections.unmodifiableSet(fromTerrains);
    }

    @Override
    public @NotNull Set<Terrain> toTerrains() {
        if (toTerrains == null) {
            toTerrains = TerrainManager.terrainsAt(to.getWorld().getUID(), to.getBlockX(), to.getBlockY(), to.getBlockZ());
        }
        return Collections.unmodifiableSet(toTerrains);
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
}
