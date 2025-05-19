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

package com.epicnicity322.terrainer.bukkit.command;

import com.epicnicity322.epicpluginlib.bukkit.command.Command;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class TerrainerCommand extends Command {
    private static final @NotNull Pattern accents = Pattern.compile("\\p{M}");

    private static String removeDiacritics(@NotNull String text) {
        text = Normalizer.normalize(text, Normalizer.Form.NFD);
        return accents.matcher(text).replaceAll("");
    }

    @Override
    public void setAliases(@Nullable String @Nullable ... aliases) {
        int length;
        if (aliases == null || (length = aliases.length) < 1) return;

        // Copying array and removing diacritics.
        var aliasesList = Stream.of(aliases).filter(Objects::nonNull).collect(Collectors.toCollection(ArrayList::new));

        for (int i = 0; i < length; i++) {
            String alias = aliases[i];
            if (alias == null) continue;
            if (alias.equals(getName())) {
                aliasesList.remove(alias);
                continue;
            }
            String withoutDiacritics = removeDiacritics(alias);
            if (!withoutDiacritics.equals(alias)) aliasesList.add(withoutDiacritics);
        }

        super.setAliases(aliasesList.toArray(new String[0]));
    }

    public abstract void reloadCommand();
}
