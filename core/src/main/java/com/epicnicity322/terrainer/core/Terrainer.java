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

package com.epicnicity322.terrainer.core;

import com.epicnicity322.epicpluginlib.core.lang.LanguageHolder;
import com.epicnicity322.epicpluginlib.core.logger.ConsoleLogger;
import com.epicnicity322.terrainer.core.config.Configurations;
import com.epicnicity322.terrainer.core.util.PlayerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public final class Terrainer {
    private static final @NotNull Path LAST_DISABLE_TIME = Configurations.DATA_FOLDER.resolve(".last");
    private static final @NotNull ScheduledExecutorService dailyTimerExecutor = Executors.newSingleThreadScheduledExecutor();
    private static @NotNull ConsoleLogger<?> logger = ConsoleLogger.simpleLogger("&8[&4Terrainer&8]&7 ");
    private static @NotNull LanguageHolder<?, ?> lang = LanguageHolder.simpleLanguage(() -> "", Configurations.LANG_EN_US.getDefaultConfiguration());
    private static @UnknownNullability PlayerUtil<?, ?> playerUtil = null;
    private static volatile @Nullable ScheduledFuture<?> dailyTimer;
    private static volatile @Nullable LocalDate timeBeforeStartingDailyTimer;

    private Terrainer() {
    }

    public static @NotNull LanguageHolder<?, ?> lang() {
        return lang;
    }

    public static void setLang(@NotNull LanguageHolder<?, ?> lang) {
        Terrainer.lang = lang;
    }

    public static @NotNull ConsoleLogger<?> logger() {
        return logger;
    }

    public static void setLogger(@NotNull ConsoleLogger<?> logger) {
        Terrainer.logger = logger;
    }

    public static @UnknownNullability PlayerUtil<?, ?> playerUtil() {
        return playerUtil;
    }

    public static void setPlayerUtil(@NotNull PlayerUtil<?, ?> playerUtil) {
        Terrainer.playerUtil = playerUtil;
    }

    public static synchronized void loadDailyTimer() {
        if (dailyTimer != null) return;

        boolean dayChangeSinceLastTime = false;

        if (Files.exists(LAST_DISABLE_TIME)) {
            try {
                ZonedDateTime lastDisableTime = ZonedDateTime.parse(Files.readString(LAST_DISABLE_TIME));
                dayChangeSinceLastTime = lastDisableTime.until(ZonedDateTime.now(), ChronoUnit.DAYS) >= 1;
            } catch (IOException | DateTimeParseException e) {
                logger.log("Failed to read the time when the plugin was last disabled.", ConsoleLogger.Level.ERROR);
                e.printStackTrace();
            }
        }

        Runnable dailyRunnable = dailyRunnable();

        if (dayChangeSinceLastTime) {
            logger.log("A day change has been detected since the plugin was last disabled. Performing Terrainer's daily tasks now.");
            dailyRunnable.run();
            return;
        }

        scheduleDailyRunnable();
    }

    private static long minutesUntilNextDay() {
        return LocalDateTime.now().until(LocalDateTime.of(LocalDate.now().plusDays(1), LocalTime.MIDNIGHT).plusMinutes(1), ChronoUnit.MINUTES);
    }

    private static @NotNull Runnable dailyRunnable() {
        return () -> {
            // Checking if a day has really passed. If not, run the scheduler again.
            LocalDate timeBeforeStartingDailyTimer1 = timeBeforeStartingDailyTimer;
            if (timeBeforeStartingDailyTimer1 != null && timeBeforeStartingDailyTimer1.until(LocalDate.now(), ChronoUnit.DAYS) <= 0) {
                logger.log("Timer ran before a day has really passed. Re-scheduling.", ConsoleLogger.Level.WARN);
                scheduleDailyRunnable();
                return;
            }

            logger.log("A day has passed!");

            // TODO: Daily tasks, such as: Taxing, Old Terrain Pruning, etc

            // Re-scheduling.
            scheduleDailyRunnable();
        };
    }

    private static synchronized void scheduleDailyRunnable() {
        ScheduledFuture<?> dailyTimer1 = dailyTimer;
        if (dailyTimer1 != null) dailyTimer1.cancel(true);

        timeBeforeStartingDailyTimer = LocalDate.now();
        dailyTimer = dailyTimerExecutor.schedule(dailyRunnable(), minutesUntilNextDay(), TimeUnit.MINUTES);
    }

    public static synchronized void stopDailyTimer() {
        ScheduledFuture<?> dailyTimer1 = dailyTimer;
        if (dailyTimer1 != null) {
            dailyTimer1.cancel(true);
            dailyTimer = null;
        }

        try {
            Files.deleteIfExists(LAST_DISABLE_TIME);
            Files.writeString(LAST_DISABLE_TIME, ZonedDateTime.now().toString(), StandardOpenOption.CREATE_NEW);
        } catch (IOException e) {
            logger.log("Failed to save the time the plugin was last disabled.", ConsoleLogger.Level.ERROR);
            e.printStackTrace();
        }
    }
}
