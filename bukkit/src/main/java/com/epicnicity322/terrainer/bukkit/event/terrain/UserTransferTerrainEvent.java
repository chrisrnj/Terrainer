/*
 * Terrainer - A minecraft terrain claiming protection plugin.
 * Copyright (C) 2026 Christiano Rangel
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

import com.epicnicity322.terrainer.core.event.terrain.IUserTransferTerrainEvent;
import com.epicnicity322.terrainer.core.terrain.Terrain;
import org.bukkit.command.CommandSender;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NonNull;

import java.lang.ref.WeakReference;
import java.util.UUID;

/**
 * An event called right before a terrain is transferred to a new owner. Triggered by '/tr transfer' command.
 * <p>
 * The transfer guarantees that the owner has full authorization to own the terrain. The limit and permission checks
 * are always done beforehand unless the command sender has used the '--force' option. In which case you may check its
 * usage with {@link #isForced()}.
 */
public class UserTransferTerrainEvent extends Event implements IUserTransferTerrainEvent<WeakReference<CommandSender>>, Cancellable {
    private static final @NotNull HandlerList handlers = new HandlerList();
    private final @NotNull Terrain terrain;
    private final @NotNull WeakReference<CommandSender> sender;
    private final boolean forced;
    private final @Nullable UUID newOwner;
    private boolean cancelled = false;

    public UserTransferTerrainEvent(@NotNull Terrain terrain, @NotNull WeakReference<CommandSender> sender, boolean forced, @Nullable UUID newOwner) {
        this.terrain = terrain;
        this.sender = sender;
        this.forced = forced;
        this.newOwner = newOwner;
    }

    public static @NotNull HandlerList getHandlerList() {
        return handlers;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return handlers;
    }

    @Override
    public @Nullable UUID newOwner() {
        return newOwner;
    }

    @Override
    public boolean isForced() {
        return forced;
    }

    @Override
    public @NonNull WeakReference<CommandSender> sender() {
        return sender;
    }

    @Override
    public @NotNull Terrain terrain() {
        return terrain;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }
}
