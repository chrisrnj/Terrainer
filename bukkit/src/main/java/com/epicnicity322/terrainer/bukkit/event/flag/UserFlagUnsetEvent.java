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

package com.epicnicity322.terrainer.bukkit.event.flag;

import com.epicnicity322.terrainer.core.event.flag.IUserFlagUnsetEvent;
import com.epicnicity322.terrainer.core.terrain.Flag;
import com.epicnicity322.terrainer.core.terrain.Terrain;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * When a player unsets a flag of a terrain using the Flag Management GUI or the command <u>/tr flag {@literal <flag>}</u>.
 */
public class UserFlagUnsetEvent extends Event implements IUserFlagUnsetEvent<CommandSender>, Cancellable {
    private static final @NotNull HandlerList handlers = new HandlerList();

    private final @NotNull CommandSender sender;
    private final @NotNull Terrain terrain;
    private final @NotNull Flag<?> flag;
    private final boolean gui;
    private boolean cancelled = false;

    public UserFlagUnsetEvent(@NotNull CommandSender sender, @NotNull Terrain terrain, @NotNull Flag<?> flag, boolean gui) {
        super(!Bukkit.isPrimaryThread());
        this.sender = sender;
        this.terrain = terrain;
        this.flag = flag;
        this.gui = gui;
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
    public @NotNull CommandSender sender() {
        return sender;
    }

    @Override
    public @NotNull Terrain terrain() {
        return terrain;
    }

    @Override
    public @NotNull Flag<?> flag() {
        return flag;
    }

    @Override
    public boolean isGui() {
        return gui;
    }
}
