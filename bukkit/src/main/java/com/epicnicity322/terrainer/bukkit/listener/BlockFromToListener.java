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
import org.bukkit.event.block.BlockFromToEvent;
import org.jetbrains.annotations.NotNull;

/**
 * A listener for exclusive for block from to event.
 */
public final class BlockFromToListener extends ToggleableListener {
    private final @NotNull ProtectionsListener protectionsListener;

    public BlockFromToListener(@NotNull ProtectionsListener protectionsListener) {
        this.protectionsListener = protectionsListener;
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onLiquidFlow(BlockFromToEvent event) {
        Block from = event.getBlock();
        Block to = event.getToBlock();
        if (!protectionsListener.liquidFlow(to.getWorld().getUID(), to.getX(), to.getY(), to.getZ(), from.getX(), from.getY(), from.getZ()))
            event.setCancelled(true);
    }
}
