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

package com.epicnicity322.terrainer.bukkit.util;

import com.epicnicity322.epicpluginlib.bukkit.reflection.ReflectionUtil;
import com.epicnicity322.terrainer.bukkit.TerrainerPlugin;
import com.epicnicity322.terrainer.core.Terrainer;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import org.bukkit.entity.Entity;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A class for helping tasks run on Folia or Spigot servers.
 */
public final class TaskFactory {
    private static final boolean folia = ReflectionUtil.getClass("io.papermc.paper.threadedregions.RegionizedServer") != null;

    static {
        if (folia) Terrainer.logger().log("Folia detected! Tasks will run with regionized multithreading.");
    }

    private final @NotNull TerrainerPlugin plugin;

    public TaskFactory(@NotNull TerrainerPlugin plugin) {
        this.plugin = plugin;
    }

    public void runGlobalTask(@NotNull Runnable runnable) {
        if (folia) {
            plugin.getServer().getGlobalRegionScheduler().run(plugin, task -> runnable.run());
        } else {
            plugin.getServer().getScheduler().runTask(plugin, runnable);
        }
    }

    public void runGlobalAsyncTask(@NotNull Runnable runnable) {
        if (folia) {
            plugin.getServer().getAsyncScheduler().runNow(plugin, task -> runnable.run());
        } else {
            plugin.getServer().getScheduler().runTaskAsynchronously(plugin, runnable);
        }
    }

    public @Nullable CancellableTask runAtFixedRate(@NotNull Entity entity, long period, boolean async, @NotNull Runnable runnable, @NotNull Runnable retired) {
        if (period <= 0) period = 1;
        if (folia) {
            ScheduledTask scheduledTask = entity.getScheduler().runAtFixedRate(plugin, task -> runnable.run(), retired, 1, period);
            if (scheduledTask == null) return null;
            return scheduledTask::cancel;
        } else {
            BukkitTask bukkitTask;
            if (async) {
                bukkitTask = plugin.getServer().getScheduler().runTaskTimerAsynchronously(plugin, runnable, 1, period);
            } else {
                bukkitTask = plugin.getServer().getScheduler().runTaskTimer(plugin, runnable, 1, period);
            }
            return bukkitTask::cancel;
        }
    }

    public @Nullable CancellableTask runDelayed(@NotNull Entity entity, long interval, @NotNull Runnable runnable, @Nullable Runnable retired) {
        if (interval <= 0) interval = 1;
        if (folia) {
            ScheduledTask scheduledTask = entity.getScheduler().runDelayed(plugin, task -> runnable.run(), retired, interval);
            if (scheduledTask == null) return null;
            return scheduledTask::cancel;
        } else {
            BukkitTask bukkitTask = plugin.getServer().getScheduler().runTaskLater(plugin, runnable, interval);
            return bukkitTask::cancel;
        }
    }

    public interface CancellableTask {
        void cancel();
    }
}
