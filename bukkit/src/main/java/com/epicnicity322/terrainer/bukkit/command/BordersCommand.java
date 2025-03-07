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
import com.epicnicity322.terrainer.bukkit.TerrainerPlugin;
import com.epicnicity322.terrainer.bukkit.util.CommandUtil;
import com.epicnicity322.terrainer.bukkit.util.TaskFactory;
import com.epicnicity322.terrainer.core.config.Configurations;
import com.epicnicity322.terrainer.core.location.Coordinate;
import com.epicnicity322.terrainer.core.terrain.Terrain;
import com.epicnicity322.yamlhandler.Configuration;
import org.bukkit.Particle;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A command that toggles the showing of border particles to the player.
 */
public final class BordersCommand extends Command {
    private static final @NotNull ConcurrentHashMap<UUID, TaskFactory.CancellableTask> viewers = new ConcurrentHashMap<>();
    private final @NotNull TerrainerPlugin plugin;
    private @NotNull Particle particle = Particle.CLOUD;

    public BordersCommand(@NotNull TerrainerPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getName() {
        return "borders";
    }

    @Override
    public @NotNull String getPermission() {
        return "terrainer.borders";
    }

    @Override
    protected @NotNull CommandRunnable getNoPermissionRunnable() {
        return CommandUtil.noPermissionRunnable();
    }

    public void setParticle(@NotNull Particle particle) {
        this.particle = particle;
    }

    public void showBorders(@NotNull Player player, @NotNull Collection<Terrain> terrains) {
        UUID playerID = player.getUniqueId();
        stopShowingBorders(playerID);
        if (!player.hasPermission("terrainer.borders.show")) return;
        Configuration config = Configurations.CONFIG.getConfiguration();
        if (!config.getBoolean("Borders.Enabled").orElse(false)) return;

        synchronized (viewers) {
            if (viewers.size() >= config.getNumber("Borders.Max Viewing").orElse(20).intValue()) return;

            WeakHashMap<Terrain, Object> terrains1 = new WeakHashMap<>();
            terrains.forEach(t -> terrains1.put(t, null));
            UUID world = player.getWorld().getUID();
            long startTime = System.currentTimeMillis();

            Runnable particleRunnable = () -> {
                Configuration config1 = Configurations.CONFIG.getConfiguration();
                long time = config1.getNumber("Borders.Time").orElse(200).longValue() * 50; // A tick has 50ms.

                if (System.currentTimeMillis() - startTime >= time || !player.isOnline() || !world.equals(player.getWorld().getUID())) {
                    stopShowingBorders(playerID);
                    return;
                }

                double yOffSet = config1.getNumber("Borders.Y OffSet").orElse(0.5).doubleValue();
                double y = player.getLocation().getY() + yOffSet;

                for (Terrain t : terrains1.keySet()) {
                    double finalY = y;
                    if (finalY > t.maxDiagonal().y() + 1) finalY = t.maxDiagonal().y() + 1;
                    else if (finalY < t.minDiagonal().y()) finalY = t.minDiagonal().y();

                    for (Coordinate coordinate : t.borders()) {
                        player.spawnParticle(particle, coordinate.x(), finalY, coordinate.z(), 0);
                    }
                }
            };

            long frequency = config.getNumber("Borders.Frequency").orElse(5).longValue();
            TaskFactory.CancellableTask particleTask = plugin.getTaskFactory().runAtFixedRate(player, frequency, config.getBoolean("Borders.Async").orElse(false), particleRunnable, () -> stopShowingBorders(playerID));
            if (particleTask == null) return;
            viewers.put(playerID, particleTask);
        }
    }

    public void stopShowingBorders(@NotNull UUID player) {
        TaskFactory.CancellableTask particleTask = viewers.remove(player);
        if (particleTask != null) particleTask.cancel();
    }

    @Override
    public void run(@NotNull String label, @NotNull CommandSender sender, @NotNull String[] args) {
        //TODO: Toggle border showing command.
    }
}
