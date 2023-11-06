package com.epicnicity322.terrainer.bukkit.listener;

import com.epicnicity322.terrainer.bukkit.event.terrain.TerrainEnterEvent;
import com.epicnicity322.terrainer.bukkit.event.terrain.TerrainLeaveEvent;
import com.epicnicity322.terrainer.core.event.TerrainEvent;
import com.epicnicity322.terrainer.core.terrain.Terrain;
import com.epicnicity322.terrainer.core.terrain.TerrainManager;
import io.papermc.paper.event.entity.EntityMoveEvent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * A listener for events exclusive from PaperMC.
 */
public final class PaperListener implements Listener {
    // Fire enter/leave events if entity is a player, or if the entity is carrying a player.
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onEntityMove(EntityMoveEvent event) {
        Location from = event.getFrom(), to = event.getTo();
        int fromX = from.getBlockX(), fromY = from.getBlockY(), fromZ = from.getBlockZ();
        int toX = to.getBlockX(), toY = to.getBlockY(), toZ = to.getBlockZ();

        // Only full block moves are processed to save performance.
        if (fromX == toX && fromY == toY && fromZ == toZ) return;

        // Getting the players to fire the event.
        List<Entity> passengers = event.getEntity().getPassengers();
        List<Player> players = null;

        for (Entity passenger : passengers) {
            if (passenger instanceof Player player) {
                if (players == null) players = new ArrayList<>(passengers.size());
                players.add(player);
            }
        }

        if (players == null) return;

        UUID world = from.getWorld().getUID();

        for (Terrain terrain : TerrainManager.allTerrains()) {
            if (!terrain.world().equals(world)) continue;

            boolean inFrom = terrain.isWithin(fromX, fromY, fromZ), inTo = terrain.isWithin(toX, toY, toZ);

            if (inFrom && !inTo) {
                for (Player player : players) {
                    var leave = new TerrainLeaveEvent(from, to, player, terrain, TerrainEvent.EnterLeaveReason.MOVE, event.isCancelled());
                    Bukkit.getPluginManager().callEvent(leave);
                    if (leave.isCancelled()) event.setCancelled(true);
                }
            } else if (inTo && !inFrom) {
                for (Player player : players) {
                    var enter = new TerrainEnterEvent(from, to, player, terrain, TerrainEvent.EnterLeaveReason.MOVE, event.isCancelled());
                    Bukkit.getPluginManager().callEvent(enter);
                    if (enter.isCancelled()) event.setCancelled(true);
                }
            }
        }
    }
}
