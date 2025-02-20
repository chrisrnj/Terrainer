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
import com.epicnicity322.epicpluginlib.core.logger.ConsoleLogger;
import com.epicnicity322.terrainer.bukkit.TerrainerPlugin;
import com.epicnicity322.terrainer.bukkit.event.terrain.*;
import com.epicnicity322.terrainer.bukkit.util.ToggleableListener;
import com.epicnicity322.terrainer.core.Terrainer;
import com.epicnicity322.terrainer.core.event.TerrainEnterLeaveEvent.EnterLeaveReason;
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
    public static final boolean asyncTeleport = ReflectionUtil.getMethod(Entity.class, "teleportAsync", Location.class) != null;
    static final @NotNull HashSet<UUID> ignoredPlayersTeleportEvent = new HashSet<>(4);
    static final @NotNull HashSet<UUID> ignoredPlayersDismountEvent = new HashSet<>(4);
    // Entity#getYaw and Entity#getPitch are only available on Paper.
    private static final boolean getYawAndPitchMethod = ReflectionUtil.getMethod(Entity.class, "getYaw") != null && ReflectionUtil.getMethod(Entity.class, "getPitch") != null;
    private static final @NotNull Vector zero = new Vector(0, 0, 0);
    private static @NotNull List<String> commandsOnEntryCancelled = Collections.emptyList();

    static {
        if (!asyncTeleport)
            Terrainer.logger().log("Unable to perform teleports asynchronously. Please use Paper to have better performance.", ConsoleLogger.Level.WARN);
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

    static void handlePassengerCarrier(@NotNull Entity vehicle, @NotNull Location from, @NotNull Location to) {
        int fromX = from.getBlockX(), fromY = from.getBlockY(), fromZ = from.getBlockZ();
        int toX = to.getBlockX(), toY = to.getBlockY(), toZ = to.getBlockZ();

        // Only full block moves are processed to save performance.
        if (fromX == toX && fromY == toY && fromZ == toZ) return;

        EnterLeaveReason reason = EnterLeaveReason.MOVE;
        Player pVehicle = vehicle instanceof Player player ? player : null;
        Set<Player> players = getPassengers(vehicle, null);

        if (pVehicle == null) {
            if (players == null) return;
        } else if (players == null) players = Collections.emptySet();

        UUID world = vehicle.getWorld().getUID();
        Set<Terrain> fromTerrains = TerrainManager.terrainsAt(world, fromX, fromY, fromZ);
        Set<Terrain> toTerrains = TerrainManager.terrainsAt(world, toX, toY, toZ);
        Set<Terrain> leftTerrains = new HashSet<>(fromTerrains);
        Set<Terrain> enteredTerrains = new HashSet<>(toTerrains);

        leftTerrains.removeIf(enteredTerrains::remove); // Removing the intersection of the sets, leaving only the actual left and entered terrains.

        if (!leftTerrains.isEmpty()) {
            if (pVehicle != null && callCanLeave(leftTerrains, fromTerrains, toTerrains, from, to, pVehicle, reason)) {
                cancelMovement(from, to, players, pVehicle, vehicle);
                return;
            }

            for (Player player : players) {
                if (callCanLeave(leftTerrains, fromTerrains, toTerrains, from, to, player, reason)) {
                    cancelMovement(from, to, players, pVehicle, vehicle);
                    return;
                }
            }
        }
        if (!enteredTerrains.isEmpty()) {
            if (pVehicle != null && callCanEnter(enteredTerrains, fromTerrains, toTerrains, from, to, pVehicle, reason)) {
                cancelMovement(from, to, players, pVehicle, vehicle);
                return;
            }

            for (Player player : players) {
                if (callCanEnter(enteredTerrains, fromTerrains, toTerrains, from, to, player, reason)) {
                    cancelMovement(from, to, players, pVehicle, vehicle);
                    return;
                }
            }
        }

        if (!leftTerrains.isEmpty()) {
            if (pVehicle != null) callLeave(leftTerrains, fromTerrains, toTerrains, from, to, pVehicle, reason);

            for (Player player : players) callLeave(leftTerrains, fromTerrains, toTerrains, from, to, player, reason);
        }
        if (!enteredTerrains.isEmpty()) {
            if (pVehicle != null) callEnter(enteredTerrains, fromTerrains, toTerrains, from, to, pVehicle, reason);

            for (Player player : players)
                callEnter(enteredTerrains, fromTerrains, toTerrains, from, to, player, reason);
        }
    }

    private static void cancelMovement(@NotNull Location from, @NotNull Location to, @NotNull Set<Player> players, @Nullable Player pVehicle, @NotNull Entity vehicle) {
        to.setX(from.getBlockX() + 0.5); // Using 0.5 offset to avoid teleport abuse and potential lag.
        to.setY(from.getBlockY());
        to.setZ(from.getBlockZ() + 0.5);

        // Removing the player from the passenger carrier while ignoring dismount event, then teleporting player to the cancelled location while ignoring teleport.
        for (Player player : players) {
            var pUID = player.getUniqueId();

            ignoredPlayersDismountEvent.add(pUID);
            Entity thisVehicle = player.getVehicle();
            if (thisVehicle != null) thisVehicle.removePassenger(player);

            Location tp;
            if (getYawAndPitchMethod) {
                tp = to.clone();
                tp.setYaw(player.getYaw());
                tp.setPitch(player.getPitch());
            } else tp = to;

            ignoredPlayersTeleportEvent.add(pUID);
            if (asyncTeleport) {
                player.teleportAsync(tp);
            } else {
                player.teleport(tp);
            }
        }

        vehicle.eject();

        // Teleporting the vehicle as well.
        if (pVehicle == null) {
            vehicle.setVelocity(zero);
            if (asyncTeleport) {
                vehicle.teleportAsync(to);
            } else {
                vehicle.teleport(to);
            }
        } else {
            ignoredPlayersTeleportEvent.add(vehicle.getUniqueId()); // Players automatically teleport to new "to" location.

            // If player is in a vehicle, dismount the player while ignoring dismount event and teleport the vehicle to the cancelled location.
            Entity vehiclesVehicle = pVehicle.getVehicle();
            if (vehiclesVehicle != null) {
                ignoredPlayersDismountEvent.add(pVehicle.getUniqueId());
                vehiclesVehicle.removePassenger(pVehicle);
                if (asyncTeleport) {
                    vehiclesVehicle.teleportAsync(to);
                } else {
                    vehiclesVehicle.teleport(to);
                }
            }
        }
    }

    static boolean handleFromTo(@NotNull Location from, @NotNull Location to, @NotNull UUID worldFrom, @NotNull UUID worldTo, @NotNull Player player, @NotNull EnterLeaveReason reason) {
        int fromX = from.getBlockX(), fromY = from.getBlockY(), fromZ = from.getBlockZ();
        int toX = to.getBlockX(), toY = to.getBlockY(), toZ = to.getBlockZ();

        // Only full block moves are processed to save performance.
        if (fromX == toX && fromY == toY && fromZ == toZ) return false;

        Set<Terrain> fromTerrains = TerrainManager.terrainsAt(worldFrom, fromX, fromY, fromZ);
        Set<Terrain> toTerrains = TerrainManager.terrainsAt(worldTo, toX, toY, toZ);
        Set<Terrain> leftTerrains = new HashSet<>(fromTerrains);
        Set<Terrain> enteredTerrains = new HashSet<>(toTerrains);

        leftTerrains.removeIf(enteredTerrains::remove); // Removing the intersection of the sets, leaving only the actual left and entered terrains.

        boolean cancel = callCanLeave(leftTerrains, fromTerrains, toTerrains, from, to, player, reason);
        if (cancel) return true;
        cancel = callCanEnter(enteredTerrains, fromTerrains, toTerrains, from, to, player, reason);
        if (cancel) return true;

        callLeave(leftTerrains, fromTerrains, toTerrains, from, to, player, reason);
        callEnter(enteredTerrains, fromTerrains, toTerrains, from, to, player, reason);
        return false;
    }

    private static boolean callCanLeave(@NotNull Set<Terrain> terrains, @Nullable Set<Terrain> fromTerrains, @Nullable Set<Terrain> toTerrains, @NotNull Location from, @NotNull Location to, @NotNull Player player, @NotNull EnterLeaveReason reason) {
        if (terrains.isEmpty()) return false;

        var canLeave = new TerrainCanLeaveEvent(from, to, player, terrains, fromTerrains, toTerrains, reason);
        Bukkit.getPluginManager().callEvent(canLeave);

        return canLeave.isCancelled();
    }

    private static boolean callCanEnter(@NotNull Set<Terrain> terrains, @Nullable Set<Terrain> fromTerrains, @Nullable Set<Terrain> toTerrains, @NotNull Location from, @NotNull Location to, @NotNull Player player, @NotNull EnterLeaveReason reason) {
        if (terrains.isEmpty()) return false;

        var canEnter = new TerrainCanEnterEvent(from, to, player, terrains, fromTerrains, toTerrains, reason);
        Bukkit.getPluginManager().callEvent(canEnter);

        return canEnter.isCancelled();
    }

    private static void callLeave(@NotNull Set<Terrain> terrains, @Nullable Set<Terrain> fromTerrains, @Nullable Set<Terrain> toTerrains, @NotNull Location from, @NotNull Location to, @NotNull Player player, @NotNull EnterLeaveReason reason) {
        if (terrains.isEmpty()) return;

        var leave = new TerrainLeaveEvent(from, to, player, terrains, fromTerrains, toTerrains, reason);
        Bukkit.getPluginManager().callEvent(leave);
    }

    private static void callEnter(@NotNull Set<Terrain> terrains, @Nullable Set<Terrain> fromTerrains, @Nullable Set<Terrain> toTerrains, @NotNull Location from, @NotNull Location to, @NotNull Player player, @NotNull EnterLeaveReason reason) {
        if (terrains.isEmpty()) return;

        var enter = new TerrainEnterEvent(from, to, player, terrains, fromTerrains, toTerrains, reason);
        Bukkit.getPluginManager().callEvent(enter);
    }

    @EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        Location from = event.getFrom(), to = event.getTo();
        Player player = event.getPlayer();
        handlePassengerCarrier(player, from, to);
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
        if (handleFromTo(from, to, from.getWorld().getUID(), to.getWorld().getUID(), event.getPlayer(), EnterLeaveReason.TELEPORT)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        Location from = player.getLocation(), to = event.getRespawnLocation();

        if (handleFromTo(from, to, player.getWorld().getUID(), to.getWorld().getUID(), player, EnterLeaveReason.RESPAWN)) {
            event.setRespawnLocation(from);
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        Location loc = player.getLocation();
        Set<Terrain> enteredTerrains = TerrainManager.terrainsAt(player.getWorld().getUID(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());

        if (callCanEnter(enteredTerrains, enteredTerrains, enteredTerrains, loc, loc, player, EnterLeaveReason.JOIN_SERVER)) {
            for (String command : commandsOnEntryCancelled) {
                TerrainerPlugin.getPlayerUtil().dispatchCommand(null, command.replace("%p", player.getName()));
            }
        } else {
            callEnter(enteredTerrains, enteredTerrains, enteredTerrains, loc, loc, player, EnterLeaveReason.JOIN_SERVER);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        Location loc = player.getLocation();
        Set<Terrain> leftTerrains = TerrainManager.terrainsAt(player.getWorld().getUID(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());

        // Can leave is not called for PlayerQuitEvent because it can't be cancelled.
        callLeave(leftTerrains, leftTerrains, leftTerrains, loc, loc, player, EnterLeaveReason.LEAVE_SERVER);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onAdd(TerrainAddEvent event) {
        Terrain terrain = event.terrain();
        World world = Bukkit.getWorld(terrain.world());
        if (world == null) return;
        Set<Terrain> singletonTerrain = Collections.singleton(terrain);

        for (Player player : world.getPlayers()) {
            Location loc = player.getLocation();
            if (!terrain.isWithin(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ())) continue;

            if (callCanEnter(singletonTerrain, null, null, loc, loc, player, EnterLeaveReason.CREATE)) {
                for (String command : commandsOnEntryCancelled) {
                    TerrainerPlugin.getPlayerUtil().dispatchCommand(null, command.replace("%p", player.getName()));
                }
            } else {
                callEnter(singletonTerrain, null, null, loc, loc, player, EnterLeaveReason.CREATE);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onRemove(TerrainRemoveEvent event) {
        Terrain terrain = event.terrain();
        World world = Bukkit.getWorld(terrain.world());
        if (world == null) return;
        Set<Terrain> singletonTerrain = Collections.singleton(terrain);

        for (Player player : world.getPlayers()) {
            Location loc = player.getLocation();
            if (!terrain.isWithin(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ())) continue;

            // Can leave is not called for TerrainRemoveEvent because it can't be cancelled.
            callLeave(singletonTerrain, null, null, loc, loc, player, EnterLeaveReason.REMOVE);
        }
    }

}
