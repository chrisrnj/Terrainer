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

import com.epicnicity322.terrainer.core.event.flag.IFlagSetEvent;
import com.epicnicity322.terrainer.core.terrain.Flag;
import com.epicnicity322.terrainer.core.terrain.Terrain;
import org.bukkit.Bukkit;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class FlagSetEvent<T> extends Event implements IFlagSetEvent<T>, Cancellable {
    private static final @NotNull HandlerList handlers = new HandlerList();
    private final @NotNull IFlagSetEvent<T> event;
    private boolean cancelled = false;

    public FlagSetEvent(@NotNull IFlagSetEvent<T> event) {
        super(!Bukkit.isPrimaryThread());
        this.event = event;
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
        return event.terrain();
    }

    @Override
    public @NotNull Flag<T> flag() {
        return event.flag();
    }

    @Override
    public @NotNull T data() {
        return event.data();
    }

    @Override
    public void setData(@NotNull T data) {
        event.setData(data);
    }

    @Override
    public @Nullable UUID affectedMember() {
        return event.affectedMember();
    }
}
