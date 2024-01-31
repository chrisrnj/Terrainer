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

import com.epicnicity322.terrainer.bukkit.event.terrain.TerrainAddEvent;
import com.epicnicity322.terrainer.bukkit.event.terrain.TerrainEnterEvent;
import com.epicnicity322.terrainer.bukkit.event.terrain.TerrainLeaveEvent;
import com.epicnicity322.terrainer.bukkit.event.terrain.TerrainRemoveEvent;
import com.epicnicity322.terrainer.core.event.TerrainEvent;
import com.epicnicity322.terrainer.core.terrain.Terrain;
import com.epicnicity322.terrainer.core.terrain.TerrainManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Vehicle;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDismountEvent;
import org.bukkit.event.entity.EntityMountEvent;
import org.bukkit.event.player.*;
import org.bukkit.event.vehicle.VehicleMoveEvent;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public final class EnterLeaveListener implements Listener {
    private static final @NotNull HashSet<UUID> ignoredPlayersTeleportEvent = new HashSet<>(4);
    private static final @NotNull Vector zero = new Vector(0, 0, 0);
    private static @NotNull List<String> commandsOnEntryCancelled = Collections.emptyList();

    public static void setCommandsOnEntryCancelled(@NotNull List<String> commandsOnEntryCancelled) {
        EnterLeaveListener.commandsOnEntryCancelled = commandsOnEntryCancelled;
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Location from = event.getFrom(), to = event.getTo();
        int fromX = from.getBlockX(), fromY = from.getBlockY(), fromZ = from.getBlockZ();
        int toX = to.getBlockX(), toY = to.getBlockY(), toZ = to.getBlockZ();

        // Only full block moves are processed to save performance.
        if (fromX == toX && fromY == toY && fromZ == toZ) return;

        Player player = event.getPlayer();
        UUID world = player.getWorld().getUID();
        boolean wasCancelled = event.isCancelled();

        for (Terrain terrain : TerrainManager.allTerrains()) {
            if (!terrain.world().equals(world)) continue;

            boolean inFrom = terrain.isWithin(fromX, fromY, fromZ), inTo = terrain.isWithin(toX, toY, toZ);

            if (inFrom && !inTo) {
                var leave = new TerrainLeaveEvent(from, to, player, terrain, TerrainEvent.EnterLeaveReason.MOVE, event.isCancelled());
                Bukkit.getPluginManager().callEvent(leave);
                if (leave.isCancelled()) event.setCancelled(true);
            } else if (inTo && !inFrom) {
                var enter = new TerrainEnterEvent(from, to, player, terrain, TerrainEvent.EnterLeaveReason.MOVE, event.isCancelled());
                Bukkit.getPluginManager().callEvent(enter);
                if (enter.isCancelled()) event.setCancelled(true);
            }
        }
        if (!wasCancelled && event.isCancelled()) {
            to.setX(from.getBlockX() + 0.5);
            to.setZ(from.getBlockZ() + 0.5);
            if (toY != fromY) to.setY(from.getBlockY());
            event.setCancelled(false);
            ignoredPlayersTeleportEvent.add(player.getUniqueId());
        }
    }

    @EventHandler
    public void onVehicleMove(VehicleMoveEvent event) {
        Location from = event.getFrom(), to = event.getTo();
        int fromX = from.getBlockX(), fromY = from.getBlockY(), fromZ = from.getBlockZ();
        int toX = to.getBlockX(), toY = to.getBlockY(), toZ = to.getBlockZ();

        // Only full block moves are processed to save performance.
        if (fromX == toX && fromY == toY && fromZ == toZ) return;

        // Getting the players to fire the event.
        Vehicle vehicle = event.getVehicle();
        List<Entity> passengers = vehicle.getPassengers();
        List<Player> players = null;

        for (Entity passenger : passengers) {
            if (passenger instanceof Player player) {
                if (players == null) players = new ArrayList<>(passengers.size());
                players.add(player);
            }
        }

        if (players == null) return;

        UUID world = vehicle.getWorld().getUID();
        boolean cancel = false;

        for (Terrain terrain : TerrainManager.allTerrains()) {
            if (!terrain.world().equals(world)) continue;

            boolean inFrom = terrain.isWithin(fromX, fromY, fromZ), inTo = terrain.isWithin(toX, toY, toZ);

            if (inFrom && !inTo) {
                for (Player player : players) {
                    var leave = new TerrainLeaveEvent(from, to, player, terrain, TerrainEvent.EnterLeaveReason.MOVE, cancel);
                    Bukkit.getPluginManager().callEvent(leave);
                    if (leave.isCancelled()) {
                        cancel = true;
                        vehicle.removePassenger(player);
                    }
                }
            } else if (inTo && !inFrom) {
                for (Player player : players) {
                    var enter = new TerrainEnterEvent(from, to, player, terrain, TerrainEvent.EnterLeaveReason.MOVE, cancel);
                    Bukkit.getPluginManager().callEvent(enter);
                    if (enter.isCancelled()) {
                        cancel = true;
                        vehicle.removePassenger(player);
                    }
                }
            }
        }

        if (cancel) vehicle.setVelocity(zero);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onTeleport(PlayerTeleportEvent event) {
        if (event.getCause() == PlayerTeleportEvent.TeleportCause.PLUGIN && ignoredPlayersTeleportEvent.remove(event.getPlayer().getUniqueId())) {
            return;
        }
        if (handleFromTo(event.getFrom(), event.getTo(), event.getPlayer(), TerrainEvent.EnterLeaveReason.TELEPORT, event.isCancelled())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        Location loc = player.getLocation();
        int x = loc.getBlockX(), y = loc.getBlockY(), z = loc.getBlockZ();
        UUID world = loc.getWorld().getUID();

        for (Terrain terrain : TerrainManager.allTerrains()) {
            if (!terrain.world().equals(world) || !terrain.isWithin(x, y, z)) continue;
            var enter = new TerrainEnterEvent(loc, loc, player, terrain, TerrainEvent.EnterLeaveReason.JOIN_SERVER, false);
            Bukkit.getPluginManager().callEvent(enter);
            if (enter.isCancelled()) {
                for (String command : commandsOnEntryCancelled) {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command.replace("%p", player.getName()).replace("%t", terrain.id().toString()));
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        Location loc = player.getLocation();
        int x = loc.getBlockX(), y = loc.getBlockY(), z = loc.getBlockZ();
        UUID world = loc.getWorld().getUID();

        for (Terrain terrain : TerrainManager.allTerrains()) {
            if (!terrain.world().equals(world) || !terrain.isWithin(x, y, z)) continue;
            var leave = new TerrainLeaveEvent(loc, loc, player, terrain, TerrainEvent.EnterLeaveReason.LEAVE_SERVER, false);
            Bukkit.getPluginManager().callEvent(leave);
            // if (enter.isCancelled()) { What are you gonna do, force the player to be online? }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onAdd(TerrainAddEvent event) {
        Terrain terrain = event.terrain();
        World world = Bukkit.getWorld(terrain.world());
        if (world == null) return;

        for (Player player : world.getPlayers()) {
            Location loc = player.getLocation();
            int x = loc.getBlockX(), y = loc.getBlockY(), z = loc.getBlockZ();
            if (!terrain.isWithin(x, y, z)) continue;

            var enter = new TerrainEnterEvent(loc, loc, player, terrain, TerrainEvent.EnterLeaveReason.CREATE, false);
            Bukkit.getPluginManager().callEvent(enter);
            if (enter.isCancelled()) {
                for (String command : commandsOnEntryCancelled) {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command.replace("%p", player.getName()).replace("%t", terrain.id().toString()));
                }
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onRemove(TerrainRemoveEvent event) {
        Terrain terrain = event.terrain();
        World world = Bukkit.getWorld(terrain.world());
        if (world == null) return;

        for (Player player : world.getPlayers()) {
            Location loc = player.getLocation();
            int x = loc.getBlockX(), y = loc.getBlockY(), z = loc.getBlockZ();
            if (!terrain.isWithin(x, y, z)) continue;

            var enter = new TerrainLeaveEvent(loc, loc, player, terrain, TerrainEvent.EnterLeaveReason.REMOVE, false);
            Bukkit.getPluginManager().callEvent(enter);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        if (handleFromTo(player.getLocation(), event.getRespawnLocation(), player, TerrainEvent.EnterLeaveReason.RESPAWN, false)) {
            event.setRespawnLocation(player.getLocation());
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onMount(EntityMountEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        Location from = player.getLocation(), to = event.getMount().getLocation();
        if (handleFromTo(from, to, player, TerrainEvent.EnterLeaveReason.MOUNT, event.isCancelled())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onDismount(EntityDismountEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        Location from = event.getDismounted().getLocation(), to = player.getLocation();
        if (handleFromTo(from, to, player, TerrainEvent.EnterLeaveReason.DISMOUNT, false)) {
            player.teleport(from);
        }
    }

    private boolean handleFromTo(@NotNull Location from, @NotNull Location to, @NotNull Player player, @NotNull TerrainEvent.EnterLeaveReason reason, boolean cancel) {
        int fromX = from.getBlockX(), fromY = from.getBlockY(), fromZ = from.getBlockZ();
        int toX = to.getBlockX(), toY = to.getBlockY(), toZ = to.getBlockZ();
        UUID fromWorld = from.getWorld().getUID();
        UUID toWorld = to.getWorld().getUID();

        for (Terrain terrain : TerrainManager.allTerrains()) {
            UUID world = terrain.world();
            boolean inFrom = world.equals(fromWorld) && terrain.isWithin(fromX, fromY, fromZ);
            boolean inTo = world.equals(toWorld) && terrain.isWithin(toX, toY, toZ);

            if (inFrom && !inTo) {
                var leave = new TerrainLeaveEvent(from, to, player, terrain, reason, cancel);
                Bukkit.getPluginManager().callEvent(leave);
                if (leave.isCancelled()) cancel = true;
            } else if (inTo && !inFrom) {
                var enter = new TerrainEnterEvent(from, to, player, terrain, reason, cancel);
                Bukkit.getPluginManager().callEvent(enter);
                if (enter.isCancelled()) cancel = true;
            }
        }

        return cancel;
    }
}
