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

package com.epicnicity322.terrainer.bukkit.listener;

import com.epicnicity322.terrainer.bukkit.util.ToggleableListener;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Collections;

/**
 * A listener for exclusive for piston events.
 */
public class PistonListener extends ToggleableListener {
    private final @NotNull ProtectionsListener protectionsListener;

    public PistonListener(@NotNull ProtectionsListener protectionsListener) {
        this.protectionsListener = protectionsListener;
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPistonExtend(BlockPistonExtendEvent event) {
        Block piston = event.getBlock();
        Collection<Block> movedBlocks = event.getBlocks().isEmpty() ? Collections.singleton(piston) : event.getBlocks();
        if (!protectionsListener.pistonExtend(piston.getWorld().getUID(), movedBlocks, b -> b.getRelative(event.getDirection())))
            event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onPistonRetract(BlockPistonRetractEvent event) {
        if (event.getBlocks().isEmpty()) return;
        Block piston = event.getBlock();
        if (!protectionsListener.pistonRetract(piston.getWorld().getUID(), piston.getX(), piston.getY(), piston.getZ(), event.getBlocks()))
            event.setCancelled(true);
    }
}
