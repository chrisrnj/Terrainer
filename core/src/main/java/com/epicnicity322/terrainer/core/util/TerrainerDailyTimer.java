/*
 * Terrainer - A minecraft terrain claiming protection plugin.
 * Copyright (C) 2026 Christiano Rangel
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

package com.epicnicity322.terrainer.core.util;

import com.epicnicity322.epicpluginlib.core.logger.ConsoleLogger;
import com.epicnicity322.epicpluginlib.core.scheduler.Scheduled;
import com.epicnicity322.epicpluginlib.core.util.PathLocker;
import com.epicnicity322.epicpluginlib.core.util.PathUtils;
import com.epicnicity322.terrainer.core.Terrainer;
import com.epicnicity322.terrainer.core.config.Configurations;
import com.epicnicity322.terrainer.core.terrain.TerrainManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

public final class TerrainerDailyTimer {
    private static final @NotNull Path LAST_DISABLE_TIME = Configurations.DATA_FOLDER.resolve(".last");
    private static @Nullable Scheduled dailyTimer;
    private static volatile @Nullable LocalDate timeBeforeDailyTimerStarted;

    public static void loadDailyTimer() {
        if (Terrainer.playerUtil() == null) throw new IllegalStateException("Terrainer#playerUtil is not available.");

        synchronized (Terrainer.class) {
            if (dailyTimer != null) return;
        }

        boolean dayChangeSinceLastTime = false;

        try (PathLocker.LockToken ignored = PathLocker.lock(LAST_DISABLE_TIME)) {
            if (Files.exists(LAST_DISABLE_TIME)) {
                try {
                    ZonedDateTime lastDisableTime = ZonedDateTime.parse(Files.readString(LAST_DISABLE_TIME));
                    dayChangeSinceLastTime = lastDisableTime.until(ZonedDateTime.now(), ChronoUnit.DAYS) >= 1;
                    Files.deleteIfExists(LAST_DISABLE_TIME);
                } catch (IOException | DateTimeParseException e) {
                    Terrainer.logger().log("Failed to read the time when the plugin was last disabled.", ConsoleLogger.Level.ERROR);
                    e.printStackTrace();
                }
            }
        }

        Runnable dailyRunnable = dailyRunnable();

        if (dayChangeSinceLastTime) {
            Terrainer.logger().log("A day change has been detected since the plugin was last disabled. Performing Terrainer's daily tasks now.");
            dailyRunnable.run();
            return;
        }

        scheduleDailyRunnable();
    }

    public static void stopDailyTimer() {
        synchronized (Terrainer.class) {
            if (dailyTimer != null) {
                dailyTimer.cancel();
                dailyTimer = null;
            }
        }

        try (PathLocker.LockToken ignored = PathLocker.lock(LAST_DISABLE_TIME)) {
            Files.deleteIfExists(LAST_DISABLE_TIME);
            PathUtils.write(ZonedDateTime.now().toString(), LAST_DISABLE_TIME);
        } catch (IOException e) {
            Terrainer.logger().log("Failed to save the time the plugin was last disabled.", ConsoleLogger.Level.ERROR);
            e.printStackTrace();
        }
    }


    private static long secondsUntilNextDay() {
        return LocalDateTime.now().until(LocalDateTime.of(LocalDate.now().plusDays(1), LocalTime.MIDNIGHT).plusMinutes(1), ChronoUnit.SECONDS);
    }

    private static @NotNull Runnable dailyRunnable() {
        return () -> {
            // Checking if a day has really passed. If not, run the scheduler again.
            LocalDate timeBeforeDailyTimerStarted1 = timeBeforeDailyTimerStarted;
            if (timeBeforeDailyTimerStarted1 != null && timeBeforeDailyTimerStarted1.until(LocalDate.now(), ChronoUnit.DAYS) <= 0) {
                Terrainer.logger().log("Timer ran before a day has really passed. Re-scheduling.", ConsoleLogger.Level.WARN);
                scheduleDailyRunnable();
                return;
            }

            Terrainer.logger().log("A day has passed!");

            // TODO: Daily tasks, such as: collecting taxes

            TerrainManager.allTerrains().forEach(t -> {
                UUID owner = t.owner();
                if (owner == null) return;
// add last seen method to player util and prune terrains.
            });

            // Re-scheduling.
            scheduleDailyRunnable();
        };
    }

    private static synchronized void scheduleDailyRunnable() {
        if (dailyTimer != null) dailyTimer.cancel();

        timeBeforeDailyTimerStarted = LocalDate.now();
        dailyTimer = Terrainer.taskFactory().async().delayed(secondsUntilNextDay() * 20, t -> dailyRunnable().run());
    }
}
