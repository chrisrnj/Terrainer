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

package com.epicnicity322.terrainer.bukkit.event.flag;

import com.epicnicity322.terrainer.core.event.flag.IFlagUnsetEvent;
import com.epicnicity322.terrainer.core.flag.Flag;
import com.epicnicity322.terrainer.core.terrain.Terrain;
import org.bukkit.Bukkit;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class FlagUnsetEvent<T> extends Event implements IFlagUnsetEvent<T>, Cancellable {
    private static final @NotNull HandlerList handlers = new HandlerList();
    private final @NotNull Terrain terrain;
    private final @NotNull Flag<T> flag;
    private final @Nullable UUID affectedMember;
    private final @Nullable T data;
    private boolean cancelled = false;

    public FlagUnsetEvent(@NotNull IFlagUnsetEvent<T> event) {
        this(event.terrain(), event.flag(), event.affectedMember(), event.data());
    }

    public FlagUnsetEvent(@NotNull Terrain terrain, @NotNull Flag<T> flag, @Nullable UUID affectedMember, @Nullable T data) {
        super(!Bukkit.isPrimaryThread());
        this.terrain = terrain;
        this.flag = flag;
        this.affectedMember = affectedMember;
        this.data = data;
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
    public @NotNull Terrain terrain() {
        return terrain;
    }

    @Override
    public @NotNull Flag<T> flag() {
        return flag;
    }

    @Override
    public @Nullable T data() {
        return data;
    }

    @Override
    public @Nullable UUID affectedMember() {
        return affectedMember;
    }
}
