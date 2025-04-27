/*
 * Terrainer - A minecraft terrain claiming protection plugin.
 * Copyright (C) 2025 Christiano Rangel
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

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.ForkJoinPool;
import java.util.function.BiConsumer;

/**
 * Utility class to provide feedback for long-running tasks by displaying a progress bar.
 * <p>
 * The class estimates the number of updates to provide based on the total number of tasks
 * and the available system parallelism. A printer function is triggered when progress
 * crosses the calculated step threshold.
 */
public final class LongTaskFeedback {
    private static final int PROGRESS_BAR_LENGTH = 20;
    private static final double UPDATE_SCALING_FACTOR = 82.07;
    private static final double PARALLELISM_OFFSET = 26.29;

    private final @NotNull BiConsumer<String, Long> printer;
    private final boolean print;
    private final long top;
    private final long step;
    private long counter = 0;

    /**
     * Constructs a new {@link LongTaskFeedback} instance for monitoring progress of a long-running task.
     *
     * @param top            The total number of items to be processed.
     * @param minimumUpdates The minimum number of progress updates required to enable output.
     *                       If the estimated number of updates falls below this threshold, output is disabled.
     * @param printer        A {@link BiConsumer} that accepts a string representing the visual progress bar
     *                       and the current count. This consumer is invoked on each progress update step.
     */
    public LongTaskFeedback(long top, int minimumUpdates, @NotNull BiConsumer<String, Long> printer) {
        this.top = top;

        int parallelism = ForkJoinPool.getCommonPoolParallelism();
        int updates = (int) Math.max(1, Math.log10(top) * (UPDATE_SCALING_FACTOR / (parallelism + PARALLELISM_OFFSET)));

        this.print = updates >= minimumUpdates;
        this.printer = printer;
        this.step = Math.max(1, top / updates);
    }

    /**
     * Atomically increments the internal counter and triggers a progress update if the step threshold is reached.
     */
    public void increment() {
        if (print) synchronized (this) {
            long current = ++counter;

            if (current == top || current % step == 0) printer.accept(render(current), current);
        }
    }

    private String render(long current) {
        int filled = (int) ((current * PROGRESS_BAR_LENGTH) / top);
        int empty = PROGRESS_BAR_LENGTH - filled;
        return "&a#".repeat(filled) + "&7-".repeat(empty);
    }
}
