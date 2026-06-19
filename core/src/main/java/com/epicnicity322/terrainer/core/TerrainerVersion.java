/*
 * Terrainer - A minecraft terrain claiming protection plugin.
 * Copyright (C) 2024-2026 Christiano Rangel
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

import org.apache.maven.artifact.versioning.ComparableVersion;
import org.jetbrains.annotations.NotNull;

public final class TerrainerVersion {
    public static final @NotNull String VERSION_STRING = "1.0";
    public static final @NotNull ComparableVersion VERSION = new ComparableVersion(VERSION_STRING);

    private TerrainerVersion() {
    }
}
