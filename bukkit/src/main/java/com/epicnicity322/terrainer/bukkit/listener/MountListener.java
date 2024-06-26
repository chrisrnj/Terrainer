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

import com.epicnicity322.terrainer.core.event.TerrainEnterLeaveEvent;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDismountEvent;
import org.bukkit.event.entity.EntityMountEvent;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A class for handling mount events in newer versions of bukkit.
 */
public final class MountListener implements Listener {
    private final @NotNull ProtectionsListener protectionsListener;
    private final @NotNull AtomicBoolean enterLeaveEvents;

    public MountListener(@NotNull ProtectionsListener protectionsListener, @NotNull AtomicBoolean enterLeaveEvents) {
        this.protectionsListener = protectionsListener;
        this.enterLeaveEvents = enterLeaveEvents;
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onMount(EntityMountEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        Entity mount = event.getMount();
        Location from = player.getLocation(), to = event.getMount().getLocation();
        UUID toWorld = mount.getWorld().getUID();

        // Checking mount protection.
        if (!protectionsListener.mount(toWorld, to.getBlockX(), to.getBlockY(), to.getBlockZ(), player)) {
            event.setCancelled(true);
            return;
        }

        // Calling enter/leave events for mount if enabled.
        if (!enterLeaveEvents.get()) return;
        if (EnterLeaveListener.handleFromTo(from, to, player.getWorld().getUID(), toWorld, player, TerrainEnterLeaveEvent.EnterLeaveReason.MOUNT)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onDismount(EntityDismountEvent event) {
        if (!enterLeaveEvents.get()) return;
        if (!(event.getEntity() instanceof Player player)) return;
        if (EnterLeaveListener.ignoredPlayersDismountEvent.remove(player.getUniqueId())) return;
        Location from = event.getDismounted().getLocation(), to = player.getLocation();

        if (EnterLeaveListener.handleFromTo(from, to, event.getDismounted().getWorld().getUID(), player.getWorld().getUID(), player, TerrainEnterLeaveEvent.EnterLeaveReason.DISMOUNT)) {
            EnterLeaveListener.ignoredPlayersTeleportEvent.add(player.getUniqueId());
            player.teleport(from);
        }
    }
}
