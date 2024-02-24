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

import com.epicnicity322.epicpluginlib.bukkit.reflection.ReflectionUtil;
import com.epicnicity322.terrainer.bukkit.event.terrain.*;
import com.epicnicity322.terrainer.bukkit.util.ToggleableListener;
import com.epicnicity322.terrainer.core.Terrainer;
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
import org.bukkit.event.player.*;
import org.bukkit.event.vehicle.VehicleMoveEvent;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * A listener for enter leave (movement) events.
 */
public final class EnterLeaveListener extends ToggleableListener {
    static final @NotNull HashSet<UUID> ignoredPlayersTeleportEvent = new HashSet<>(4);
    static final @NotNull HashSet<UUID> ignoredPlayersDismountEvent = new HashSet<>(4);
    private static final boolean asyncTeleport = ReflectionUtil.getMethod(Entity.class, "teleportAsync", Location.class) != null;
    private static final @NotNull Vector zero = new Vector(0, 0, 0);
    private static @NotNull List<String> commandsOnEntryCancelled = Collections.emptyList();

    static {
        if (asyncTeleport) Terrainer.logger().log("Teleports will be done asynchronously.");
    }

    public static void setCommandsOnEntryCancelled(@NotNull List<String> commandsOnEntryCancelled) {
        EnterLeaveListener.commandsOnEntryCancelled = commandsOnEntryCancelled;
    }

    private static @Nullable Set<Player> getPassengers(@NotNull Entity entity, @Nullable Set<Player> players) {
        for (Entity passenger : entity.getPassengers()) {
            if (passenger instanceof Player player) {
                if (players == null) players = new HashSet<>();
                players.add(player);
            }
            players = getPassengers(passenger, players);
        }
        return players;
    }

    static boolean handlePassengerCarrier(@NotNull Entity vehicle, @NotNull Location from, @NotNull Location to) {
        int fromX = from.getBlockX(), fromY = from.getBlockY(), fromZ = from.getBlockZ();
        int toX = to.getBlockX(), toY = to.getBlockY(), toZ = to.getBlockZ();

        // Only full block moves are processed to save performance.
        if (fromX == toX && fromY == toY && fromZ == toZ) return false;

        Set<Player> players = getPassengers(vehicle, null);
        if (players == null) return false;

        TerrainEvent.EnterLeaveReason reason = TerrainEvent.EnterLeaveReason.MOVE;

        UUID world = vehicle.getWorld().getUID();
        Set<Terrain> fromTerrains = TerrainManager.terrainsAt(world, fromX, fromY, fromZ);
        Set<Terrain> toTerrains = TerrainManager.terrainsAt(world, toX, toY, toZ);
        boolean cancel = false;

        LinkedList<Terrain> callLeaveEvents = null;
        boolean leaveAllowed = false;

        terrainLoop:
        for (Terrain terrain : fromTerrains) {
            if (toTerrains.contains(terrain)) continue; // The terrain is in to, so the vehicle didn't really leave it.

            // The terrains to call leave event.
            if (callLeaveEvents == null) callLeaveEvents = new LinkedList<>();
            callLeaveEvents.add(terrain);

            // Leave is allowed, no need to call the event again, just adding the terrain to the list.
            if (leaveAllowed) continue;

            for (Player player : players) {
                var leave = new TerrainCanLeaveEvent(from, to, player, terrain, reason);

                Bukkit.getPluginManager().callEvent(leave);

                TerrainEvent.CanEnterLeave canLeave = leave.canLeave();
                if (canLeave == TerrainEvent.CanEnterLeave.DENY) {
                    cancel = true;
                    break terrainLoop;
                } else if (canLeave == TerrainEvent.CanEnterLeave.ALLOW) leaveAllowed = true;
            }
        }

        LinkedList<Terrain> callEnterEvents = null;
        boolean enterAllowed = false;

        terrainLoop:
        for (Terrain terrain : toTerrains) {
            if (fromTerrains.contains(terrain))
                continue; // The terrain was in from, so the vehicle didn't really enter it.

            // The terrains to call enter event.
            if (callEnterEvents == null) callEnterEvents = new LinkedList<>();
            callEnterEvents.add(terrain);

            // Enter is allowed, no need to call the event again, just adding the terrain to the list.
            if (enterAllowed) continue;

            for (Player player : players) {
                var enter = new TerrainCanEnterEvent(from, to, player, terrain, reason);

                Bukkit.getPluginManager().callEvent(enter);

                TerrainEvent.CanEnterLeave canEnter = enter.canEnter();
                if (canEnter == TerrainEvent.CanEnterLeave.DENY) {
                    cancel = true;
                    break terrainLoop;
                } else if (canEnter == TerrainEvent.CanEnterLeave.ALLOW) enterAllowed = true;
            }
        }

        if (!cancel) {
            // These events will only be called if the CanEnterLeave events were not cancelled.
            // Ideally they should only be called on MONITOR event priority, because they are for when players really entered/left
            // terrains, but that would require gathering the terrains again, and it would be bad for performance.
            if (callLeaveEvents != null) for (Terrain terrain : callLeaveEvents) {
                for (Player player : players) {
                    var leave = new TerrainLeaveEvent(from, to, player, terrain, reason);
                    Bukkit.getPluginManager().callEvent(leave);
                }
            }
            if (callEnterEvents != null) for (Terrain terrain : callEnterEvents) {
                for (Player player : players) {
                    var enter = new TerrainEnterEvent(from, to, player, terrain, reason);
                    Bukkit.getPluginManager().callEvent(enter);
                }
            }
        } else {
            vehicle.setVelocity(zero);
            to.setX(from.getBlockX() + 0.5);
            to.setY(from.getBlockY());
            to.setZ(from.getBlockZ() + 0.5);

            // Removing the player from the passenger carrier while ignoring dismount event, then teleporting player to the cancelled location while ignoring teleport.
            for (Player player : players) {
                var pUID = player.getUniqueId();

                ignoredPlayersDismountEvent.add(pUID);
                Entity pVehicle = player.getVehicle();
                if (pVehicle != null) pVehicle.removePassenger(player);

                Location tp = to.clone();
                tp.setYaw(player.getYaw());
                tp.setPitch(player.getPitch());

                ignoredPlayersTeleportEvent.add(pUID);
                if (asyncTeleport) {
                    player.teleportAsync(tp);
                } else {
                    player.teleport(tp);
                }
            }

            // Teleporting the vehicle as well.
            if (vehicle instanceof Player) ignoredPlayersTeleportEvent.add(vehicle.getUniqueId());
            if (asyncTeleport) {
                vehicle.teleportAsync(to);
            } else {
                vehicle.teleport(to);
            }
        }
        return cancel;
    }

    static boolean handleFromTo(@NotNull Location from, @NotNull Location to, @NotNull UUID worldFrom, @NotNull UUID worldTo, @NotNull Player player, @NotNull TerrainEvent.EnterLeaveReason reason) {
        int fromX = from.getBlockX(), fromY = from.getBlockY(), fromZ = from.getBlockZ();
        int toX = to.getBlockX(), toY = to.getBlockY(), toZ = to.getBlockZ();

        // Only full block moves are processed to save performance.
        if (fromX == toX && fromY == toY && fromZ == toZ) return false;

        Set<Terrain> fromTerrains = TerrainManager.terrainsAt(worldFrom, fromX, fromY, fromZ);
        Set<Terrain> toTerrains = TerrainManager.terrainsAt(worldTo, toX, toY, toZ);

        LinkedList<Terrain> callLeaveEvents = null;
        boolean leaveAllowed = false;

        for (Terrain terrain : fromTerrains) {
            if (toTerrains.contains(terrain)) continue; // The terrain is in to, so the player didn't really leave it.

            // The terrains to call leave event.
            if (callLeaveEvents == null) callLeaveEvents = new LinkedList<>();
            callLeaveEvents.add(terrain);

            // Leave is allowed, no need to call the event again, just adding the terrain to the list.
            if (leaveAllowed) continue;

            var leave = new TerrainCanLeaveEvent(from, to, player, terrain, reason);

            Bukkit.getPluginManager().callEvent(leave);

            if (leave.canLeave() == TerrainEvent.CanEnterLeave.DENY) return true;
            else if (leave.canLeave() == TerrainEvent.CanEnterLeave.ALLOW) leaveAllowed = true;
        }

        LinkedList<Terrain> callEnterEvents = null;
        boolean enterAllowed = false;

        for (Terrain terrain : toTerrains) {
            if (fromTerrains.contains(terrain))
                continue; // The terrain was in from, so the player didn't really enter it.

            // The terrains to call enter event.
            if (callEnterEvents == null) callEnterEvents = new LinkedList<>();
            callEnterEvents.add(terrain);

            // Enter is allowed, no need to call the event again, just adding the terrain to the list.
            if (enterAllowed) continue;

            var enter = new TerrainCanEnterEvent(from, to, player, terrain, reason);

            Bukkit.getPluginManager().callEvent(enter);

            if (enter.canEnter() == TerrainEvent.CanEnterLeave.DENY) return true;
            else if (enter.canEnter() == TerrainEvent.CanEnterLeave.ALLOW) enterAllowed = true;
        }

        // These events will only be called if the CanEnterLeave events were not cancelled.
        // Ideally they should only be called on MONITOR event priority, because they are for when players really entered/left
        // terrains, but that would require gathering the terrains again, and it would be bad for performance.
        if (callLeaveEvents != null) for (Terrain terrain : callLeaveEvents) {
            var leave = new TerrainLeaveEvent(from, to, player, terrain, reason);
            Bukkit.getPluginManager().callEvent(leave);
        }
        if (callEnterEvents != null) for (Terrain terrain : callEnterEvents) {
            var enter = new TerrainEnterEvent(from, to, player, terrain, reason);
            Bukkit.getPluginManager().callEvent(enter);
        }
        return false;
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        Location from = event.getFrom(), to = event.getTo();
        Player player = event.getPlayer();

        // Checking the player's passengers, if they have any.
        if (handlePassengerCarrier(player, from, to)) return;

        // Handling from to for player move event.
        UUID world = player.getWorld().getUID();
        boolean cancel = handleFromTo(from, to, world, world, player, TerrainEvent.EnterLeaveReason.MOVE);

        if (cancel) {
            to.setX(from.getBlockX() + 0.5);
            to.setY(from.getBlockY());
            to.setZ(from.getBlockZ() + 0.5);
            // The change of the TO location will count as teleport as soon as the event finishes being called.
            ignoredPlayersTeleportEvent.add(player.getUniqueId());

            // If player is in a vehicle, dismount the player while ignoring dismount event and teleport the vehicle to the cancelled location.
            Entity vehicle = player.getVehicle();
            if (vehicle != null) {
                ignoredPlayersDismountEvent.add(player.getUniqueId());
                vehicle.removePassenger(player);
                if (asyncTeleport) {
                    vehicle.teleportAsync(to);
                } else {
                    vehicle.teleport(to);
                }
            }
        }
    }

    @EventHandler
    public void onVehicleMove(VehicleMoveEvent event) {
        Location from = event.getFrom(), to = event.getTo();
        Vehicle vehicle = event.getVehicle();
        handlePassengerCarrier(vehicle, from, to);
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent event) {
        if (event.getCause() == PlayerTeleportEvent.TeleportCause.PLUGIN && ignoredPlayersTeleportEvent.remove(event.getPlayer().getUniqueId())) {
            return;
        }
        Location from = event.getFrom(), to = event.getTo();
        if (handleFromTo(from, to, from.getWorld().getUID(), to.getWorld().getUID(), event.getPlayer(), TerrainEvent.EnterLeaveReason.TELEPORT)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        Location loc = player.getLocation();
        int x = loc.getBlockX(), y = loc.getBlockY(), z = loc.getBlockZ();
        UUID world = player.getWorld().getUID();

        LinkedList<Terrain> callEnterEvents = null;
        boolean enterChecked = false;

        for (Terrain terrain : TerrainManager.terrainsAt(world, x, y, z)) {
            if (callEnterEvents == null) callEnterEvents = new LinkedList<>();
            callEnterEvents.add(terrain);
            if (enterChecked) continue;
            var enter = new TerrainCanEnterEvent(loc, loc, player, terrain, TerrainEvent.EnterLeaveReason.JOIN_SERVER);
            Bukkit.getPluginManager().callEvent(enter);
            if (enter.canEnter() == TerrainEvent.CanEnterLeave.DENY) {
                for (String command : commandsOnEntryCancelled) {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command.replace("%p", player.getName()).replace("%t", terrain.id().toString()));
                }
                // Even tho TerrainCanEnterEvent is denied, the TerrainEnterEvents should still be called, because
                // commands are only executed a tick after they're dispatched. So if the command teleported the player
                // outside the terrain by then, TerrainLeaveEvent should be called and everything should work as intended.
                // Setting enterChecked so further calls to TerrainCanEnterEvent are ignored and terrains are added to the
                // list of TerrainEnterEvent call.
                enterChecked = true;
            } else if (enter.canEnter() == TerrainEvent.CanEnterLeave.ALLOW) {
                enterChecked = true;
            }
        }

        if (callEnterEvents != null) for (Terrain terrain : callEnterEvents) {
            var enter = new TerrainEnterEvent(loc, loc, player, terrain, TerrainEvent.EnterLeaveReason.JOIN_SERVER);
            Bukkit.getPluginManager().callEvent(enter);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        Location loc = player.getLocation();
        int x = loc.getBlockX(), y = loc.getBlockY(), z = loc.getBlockZ();
        UUID world = player.getWorld().getUID();

        for (Terrain terrain : TerrainManager.terrainsAt(world, x, y, z)) {
            var leave = new TerrainLeaveEvent(loc, loc, player, terrain, TerrainEvent.EnterLeaveReason.LEAVE_SERVER);
            Bukkit.getPluginManager().callEvent(leave);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onAdd(TerrainAddEvent event) {
        Terrain terrain = event.terrain();
        World world = Bukkit.getWorld(terrain.world());
        if (world == null) return;

        LinkedList<Player> callEnterEvents = null;
        boolean enterChecked = false;

        for (Player player : world.getPlayers()) {
            Location loc = player.getLocation();
            int x = loc.getBlockX(), y = loc.getBlockY(), z = loc.getBlockZ();
            if (!terrain.isWithin(x, y, z)) continue;
            if (callEnterEvents == null) callEnterEvents = new LinkedList<>();
            callEnterEvents.add(player);
            if (enterChecked) continue;
            var enter = new TerrainCanEnterEvent(loc, loc, player, terrain, TerrainEvent.EnterLeaveReason.CREATE);
            Bukkit.getPluginManager().callEvent(enter);
            if (enter.canEnter() == TerrainEvent.CanEnterLeave.DENY) {
                for (String command : commandsOnEntryCancelled) {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command.replace("%p", player.getName()).replace("%t", terrain.id().toString()));
                }
                // Even tho TerrainCanEnterEvent is denied, the TerrainEnterEvents should still be called, because
                // commands are only executed a tick after they're dispatched. So if the command teleported the player
                // outside the terrain by then, TerrainLeaveEvent should be called and everything should work as intended.
                // Setting enterChecked so further calls to TerrainCanEnterEvent are ignored and players are added to the
                // list of TerrainEnterEvent call.
                enterChecked = true;
            } else if (enter.canEnter() == TerrainEvent.CanEnterLeave.ALLOW) {
                enterChecked = true;
            }
        }

        if (callEnterEvents != null) for (Player player : callEnterEvents) {
            var enter = new TerrainEnterEvent(player.getLocation(), player.getLocation(), player, terrain, TerrainEvent.EnterLeaveReason.CREATE);
            Bukkit.getPluginManager().callEvent(enter);
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

            var enter = new TerrainLeaveEvent(loc, loc, player, terrain, TerrainEvent.EnterLeaveReason.REMOVE);
            Bukkit.getPluginManager().callEvent(enter);
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        Location from = player.getLocation(), to = event.getRespawnLocation();
        if (handleFromTo(from, to, player.getWorld().getUID(), to.getWorld().getUID(), player, TerrainEvent.EnterLeaveReason.RESPAWN)) {
            event.setRespawnLocation(from);
        }
    }
}
