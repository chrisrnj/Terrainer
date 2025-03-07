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

package com.epicnicity322.terrainer.bukkit.command;

import com.epicnicity322.epicpluginlib.bukkit.command.Command;
import com.epicnicity322.epicpluginlib.bukkit.command.CommandRunnable;
import com.epicnicity322.epicpluginlib.bukkit.lang.MessageSender;
import com.epicnicity322.terrainer.bukkit.TerrainerPlugin;
import com.epicnicity322.terrainer.bukkit.listener.EnterLeaveListener;
import com.epicnicity322.terrainer.bukkit.util.CommandUtil;
import com.epicnicity322.terrainer.bukkit.util.TaskFactory;
import com.epicnicity322.terrainer.core.config.Configurations;
import com.epicnicity322.terrainer.core.location.Coordinate;
import com.epicnicity322.terrainer.core.terrain.Terrain;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public final class TeleportCommand extends Command implements Listener {
    private static final @NotNull HashMap<UUID, List<TaskFactory.CancellableTask>> teleportingPlayers = new HashMap<>();
    private final @NotNull TerrainerPlugin plugin;

    public TeleportCommand(@NotNull TerrainerPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getName() {
        return "teleport";
    }

    @Override
    public @NotNull String getPermission() {
        return "terrainer.teleport";
    }

    @Override
    protected @NotNull CommandRunnable getNoPermissionRunnable() {
        return CommandUtil.noPermissionRunnable();
    }

    @Override
    public void run(@NotNull String label, @NotNull CommandSender sender0, @NotNull String[] args0) {
        MessageSender lang = TerrainerPlugin.getLanguage();
        CommandUtil.findTerrain("terrainer.teleport.otherterrains", "terrainer.teleport.world", true, label, sender0, args0, lang.getColored("Teleport.Select"), commandArguments -> {
            String[] args = commandArguments.preceding();
            Terrain terrain = commandArguments.terrain();
            CommandSender sender = commandArguments.sender();
            World world = plugin.getServer().getWorld(terrain.world());

            if (world == null) {
                lang.send(sender, lang.get("Teleport.Error.World Not Found").replace("<terrain>", terrain.name()));
                return;
            }

            Player player;

            if (args.length > 0) {
                if (!sender.hasPermission("terrainer.teleport.otherplayers")) {
                    lang.send(sender, lang.get("Invalid Arguments.Error").replace("<label>", label).replace("<label2>", args0[0]).replace("<args>", lang.get("Invalid Arguments.Terrain Optional")));
                    return;
                }

                player = plugin.getServer().getPlayer(args[0]);
                if (player == null) {
                    lang.send(sender, lang.get("General.Player Not Found").replace("<value>", args[0]));
                    return;
                }
            } else if (sender instanceof Player p) {
                player = p;
            } else {
                lang.send(sender, lang.get("Invalid Arguments.Error").replace("<label>", label).replace("<label2>", args0[0]).replace("<args>", lang.get("Invalid Arguments.Player")));
                return;
            }

            Coordinate center = terrain.center();
            Location tpLoc = new Location(world, center.x(), world.getHighestBlockYAt((int) center.x(), (int) center.z()) + 1, center.z());
            boolean isWithinY = terrain.isWithin(center.x(), tpLoc.getBlockY(), center.z());

            int delay = Configurations.CONFIG.getConfiguration().getNumber("Teleport.Movement Check Delay").orElse(3).intValue();

            if (delay > 0 && sender == player && !sender.hasPermission("terrainer.teleport.nodelay")) {
                UUID pUID = player.getUniqueId();

                if (teleportingPlayers.containsKey(pUID)) {
                    lang.send(sender, lang.get("Teleport.Error.Already Teleporting"));
                    return;
                }

                var tasks = new ArrayList<TaskFactory.CancellableTask>(delay);
                int countdown = delay;

                lang.send(sender, lang.get("Teleport.Delay").replace("<delay>", Integer.toString(countdown--)));

                for (int i = 1; i < delay; i++) {
                    int j = countdown--;
                    tasks.add(plugin.getTaskFactory().runDelayed(player, i * 20L, () -> lang.send(sender, lang.get("Teleport.Delay").replace("<delay>", Integer.toString(j))), null));
                }

                String tName = terrain.name();
                tasks.add(plugin.getTaskFactory().runDelayed(player, delay * 20L, () -> {
                    teleportingPlayers.remove(pUID);
                    // Unregistering movement listener.
                    if (teleportingPlayers.isEmpty()) HandlerList.unregisterAll(this);
                    teleport(player, tpLoc, sender, tName, isWithinY);

                }, null));

                // Registering movement listener.
                if (teleportingPlayers.isEmpty()) plugin.getServer().getPluginManager().registerEvents(this, plugin);
                teleportingPlayers.put(pUID, tasks);
            } else {
                teleport(player, tpLoc, sender, terrain.name(), isWithinY);
            }
        });
    }

    private void teleport(@NotNull Player player, @NotNull Location location, @NotNull CommandSender sender, @NotNull String terrain, boolean isWithinY) {
        MessageSender lang = TerrainerPlugin.getLanguage();

        if (EnterLeaveListener.asyncTeleport) {
            player.teleportAsync(location).thenAccept(success -> {
                lang.send(player, lang.get("Teleport." + (success ? "Success" : "Error") + ".Default").replace("<terrain>", terrain));
                if (!isWithinY && success) lang.send(player, lang.get("Teleport.Above"));
                if (player != sender) {
                    lang.send(sender, lang.get("Teleport." + (success ? "Success" : "Error") + ".Other").replace("<terrain>", terrain).replace("<player>", player.getName()));
                }
            });
        } else {
            boolean success = player.teleport(location);
            lang.send(player, lang.get("Teleport." + (success ? "Success" : "Error") + ".Default").replace("<terrain>", terrain));
            if (!isWithinY && success) lang.send(player, lang.get("Teleport.Above"));
            if (player != sender) {
                lang.send(sender, lang.get("Teleport." + (success ? "Success" : "Error") + ".Other").replace("<terrain>", terrain).replace("<player>", player.getName()));
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onMove(PlayerMoveEvent event) {
        Location from = event.getFrom();
        Location to = event.getTo();

        // Only process full block movements.
        if (from.getBlockX() == to.getBlockX() && from.getBlockY() == to.getBlockY() && from.getBlockZ() == to.getBlockZ()) {
            return;
        }

        Player player = event.getPlayer();
        List<TaskFactory.CancellableTask> tasks = teleportingPlayers.remove(player.getUniqueId());

        if (tasks != null) {
            tasks.forEach(TaskFactory.CancellableTask::cancel);
            TerrainerPlugin.getLanguage().send(player, TerrainerPlugin.getLanguage().get("Teleport.Error.Moved"));

            // Unregistering movement listener.
            if (teleportingPlayers.isEmpty()) {
                HandlerList.unregisterAll(this);
            }
        }
    }
}
