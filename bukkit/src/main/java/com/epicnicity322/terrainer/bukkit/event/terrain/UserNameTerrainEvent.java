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

import com.epicnicity322.terrainer.core.event.terrain.IUserNameTerrainEvent;
import com.epicnicity322.terrainer.core.terrain.Terrain;
import org.bukkit.command.CommandSender;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * When a terrain is named, whether by creation or rename. This event will not be called if the name was in the
 * blacklist of terrain names.
 *
 * @see IUserNameTerrainEvent#isBlackListed(String)
 */
// TODO: Call event
public class UserNameTerrainEvent extends Event implements Cancellable, IUserNameTerrainEvent<CommandSender> {
    private static final @NotNull HandlerList handlers = new HandlerList();
    private final @NotNull Terrain terrain;
    private final @NotNull CommandSender sender;
    private final @NotNull String previousName;
    private final @NotNull NameReason reason;
    private @NotNull String newName;
    private boolean cancelled = false;

    public UserNameTerrainEvent(@NotNull Terrain terrain, @NotNull CommandSender sender, @NotNull String previousName, @NotNull String newName, @NotNull NameReason reason) {
        this.terrain = terrain;
        this.sender = sender;
        this.previousName = previousName;
        this.newName = newName;
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
    public @NotNull CommandSender sender() {
        return sender;
    }

    @Override
    public @NotNull Terrain terrain() {
        return terrain;
    }

    @Override
    public @NotNull String previousName() {
        return previousName;
    }

    @Override
    public @NotNull String newName() {
        return newName;
    }

    public void setNewName(@NotNull String newName) {
        this.newName = newName;
    }

    @Override
    public @NotNull NameReason reason() {
        return reason;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Cancelling this event will result in the terrain having the {@link #previousName()} as name.
     *
     * @param cancel true if you wish to cancel this event
     */
    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }
}
