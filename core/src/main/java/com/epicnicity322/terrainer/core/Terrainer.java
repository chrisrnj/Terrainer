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
import org.jetbrains.annotations.NotNull;

public final class Terrainer {
    private static @NotNull ConsoleLogger<?> logger = ConsoleLogger.simpleLogger("[Terrainer] ");
    private static @NotNull LanguageHolder<?, ?> lang = LanguageHolder.simpleLanguage(() -> "", Configurations.LANG_EN_US.getDefaultConfiguration());

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
}
