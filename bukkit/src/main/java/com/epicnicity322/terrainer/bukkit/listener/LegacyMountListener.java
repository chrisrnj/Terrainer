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

import com.epicnicity322.terrainer.core.event.TerrainEvent;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;
import org.spigotmc.event.entity.EntityDismountEvent;
import org.spigotmc.event.entity.EntityMountEvent;

import java.util.UUID;

/**
 * A class for handling mount events before they were ported from spigot to bukkit.
 */
@SuppressWarnings({"UnstableApiUsage", "deprecation"})
public final class LegacyMountListener implements Listener {
    private final @NotNull ProtectionsListener protectionsListener;

    public LegacyMountListener(@NotNull ProtectionsListener protectionsListener) {
        this.protectionsListener = protectionsListener;
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onMount(EntityMountEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;

        Entity mount = event.getMount();
        Location from = player.getLocation(), to = event.getMount().getLocation();
        UUID toWorld = mount.getWorld().getUID();

        if (!protectionsListener.mount(toWorld, to.getBlockX(), to.getBlockY(), to.getBlockZ(), player)) {
            event.setCancelled(true);
            return;
        }

        if (EnterLeaveListener.handleFromTo(from, to, player.getWorld().getUID(), toWorld, player, TerrainEvent.EnterLeaveReason.MOUNT)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onDismount(EntityDismountEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (EnterLeaveListener.ignoredPlayersDismountEvent.remove(player.getUniqueId())) return;
        Location from = event.getDismounted().getLocation(), to = player.getLocation();
        if (EnterLeaveListener.handleFromTo(from, to, event.getDismounted().getWorld().getUID(), player.getWorld().getUID(), player, TerrainEvent.EnterLeaveReason.DISMOUNT)) {
            EnterLeaveListener.ignoredPlayersTeleportEvent.add(player.getUniqueId());
            player.teleport(from);
        }
    }
}
