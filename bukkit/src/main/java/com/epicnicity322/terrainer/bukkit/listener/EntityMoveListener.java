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
import io.papermc.paper.event.entity.EntityMoveEvent;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;

/**
 * A listener for exclusive for entity move events, available for PaperMC only.
 */
public final class EntityMoveListener extends ToggleableListener {
    // Fire enter/leave events if the entity is carrying a player.
    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onEntityMove(EntityMoveEvent event) {
        Location from = event.getFrom(), to = event.getTo();
        EnterLeaveListener.handlePassengerCarrier(event.getEntity(), from, to);
    }
}
