package com.epicnicity322.terrainer.bukkit.command;

import com.epicnicity322.epicpluginlib.bukkit.command.Command;
import com.epicnicity322.epicpluginlib.bukkit.command.CommandRunnable;
import com.epicnicity322.terrainer.bukkit.TerrainerPlugin;
import com.epicnicity322.terrainer.bukkit.util.CommandUtil;
import com.epicnicity322.terrainer.core.Coordinate;
import com.epicnicity322.terrainer.core.config.Configurations;
import com.epicnicity322.terrainer.core.terrain.Terrain;
import com.epicnicity322.yamlhandler.Configuration;
import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A command that toggles the showing of border particles to the player.
 */
public class BordersCommand extends Command {
    private static final @NotNull ConcurrentHashMap<UUID, BukkitRunnable> viewers = new ConcurrentHashMap<>();
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
        stopShowingBorders(player);
        if (!player.hasPermission("terrainer.borders.show")) return;
        Configuration config = Configurations.CONFIG.getConfiguration();
        if (!config.getBoolean("Borders.Enabled").orElse(false)) return;
        if (viewers.size() >= config.getNumber("Borders.Max Viewing").orElse(20).intValue()) return;
        UUID world = player.getWorld().getUID();
        double yOffSet = config.getNumber("Borders.Y OffSet").orElse(0.5).doubleValue();

        BukkitRunnable runnable = new BukkitRunnable() {
            private @Nullable BukkitTask stopper;

            @Override
            public void run() {
                if (!player.isOnline() || !world.equals(player.getWorld().getUID())) {
                    stopShowingBorders(player);
                    return;
                }

                double y = player.getLocation().getY() + yOffSet;

                for (Terrain t : terrains) {
                    double finalY = y;
                    if (finalY > t.maxDiagonal().y() + 1) finalY = t.maxDiagonal().y() + 1;
                    else if (finalY < t.minDiagonal().y()) finalY = t.minDiagonal().y();

                    for (Coordinate coordinate : t.borders()) {
                        player.spawnParticle(particle, coordinate.x(), finalY, coordinate.z(), 0);
                    }
                }
            }

            @Override
            public synchronized @NotNull BukkitTask runTaskTimer(@NotNull Plugin plugin, long delay, long period) throws IllegalArgumentException, IllegalStateException {
                try {
                    return super.runTaskTimer(plugin, delay, period);
                } finally {
                    stopper = Bukkit.getScheduler().runTaskLater(plugin, () -> stopShowingBorders(player), config.getNumber("Borders.Time").orElse(200).longValue());
                }
            }

            @Override
            public synchronized @NotNull BukkitTask runTaskTimerAsynchronously(@NotNull Plugin plugin, long delay, long period) throws IllegalArgumentException, IllegalStateException {
                try {
                    return super.runTaskTimerAsynchronously(plugin, delay, period);
                } finally {
                    stopper = Bukkit.getScheduler().runTaskLater(plugin, () -> stopShowingBorders(player), config.getNumber("Borders.Time").orElse(200).longValue());
                }
            }

            @Override
            public synchronized void cancel() throws IllegalStateException {
                super.cancel();
                if (stopper != null) stopper.cancel();
            }
        };

        viewers.put(player.getUniqueId(), runnable);
        long frequency = config.getNumber("Borders.Frequency").orElse(5).longValue();

        if (config.getBoolean("Borders.Async").orElse(false)) {
            runnable.runTaskTimerAsynchronously(plugin, 0, frequency);
        } else {
            runnable.runTaskTimer(plugin, 0, frequency);
        }
    }

    public void stopShowingBorders(@NotNull Player player) {
        BukkitRunnable runnable = viewers.remove(player.getUniqueId());
        if (runnable == null) return;
        runnable.cancel();
    }

    @Override
    public void run(@NotNull String label, @NotNull CommandSender sender, @NotNull String[] args) {
        //TODO: Toggle border showing command.
    }
}
