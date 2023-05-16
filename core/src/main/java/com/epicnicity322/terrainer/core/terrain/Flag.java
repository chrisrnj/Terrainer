/*
 * Terrainer - A minecraft terrain claiming protection plugin.
 * Copyright (C) 2023 Christiano Rangel
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

package com.epicnicity322.terrainer.core.terrain;

import com.epicnicity322.terrainer.core.Terrainer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Pattern;

/**
 * Flags are used to change the way things behave within a terrain. All terrains can have flags assigned to them, and
 * these flags may hold serializable data, that's why flags must be static and not contain any terrain-specific data,
 * as more than one terrain might have the same {@link Flag} instance.
 * <p>
 * Flags can be edited through the Flag Management GUI or through the "/tr flag" command, as long as they are registered
 * with Flags#registerFlag.
 * <p>
 * The ID of the flag will be used to find the localized name of the flag, the description, and the material for
 * the Flag Management GUI in language and config, in the path "Flags.Values.FlagIDHere.Display4 Name",
 * "Flags.Values.FlagIDHere.Lore", and "Flags.Values.FlagIDHere.Material", respectfully.
 *
 * @param id           The ID of the flag, used in commands and to find localized names. Must match [a-zA-Z ] regex.
 * @param defaultValue The default value used in terrains where the flag is undefined.
 * @param transformer  The transformer to obtain the data object from the player's input.
 * @param formatter    The formatter to show to the player the current set data of this flag.
 * @param <T>          The type of data this flag can hold.
 */
public record Flag<T>(@NotNull String id, @NotNull T defaultValue, @NotNull Function<String, T> transformer,
                      @NotNull Function<T, String> formatter) {
    private static final @NotNull Pattern ALLOWED_FLAG_ID_REGEX = Pattern.compile("^[a-zA-Z ]+$");

    private static final @NotNull Pattern comma = Pattern.compile(",");
    private static final @NotNull Function<String, Boolean> booleanTransformer = input -> input.equalsIgnoreCase("true") || input.equalsIgnoreCase("allow");
    private static final @NotNull Function<String, String> stringTransformer = input -> input;
    private static final @NotNull Function<String, Set<String>> setTransformer = input -> Set.of(comma.split(input));
    private static final @NotNull Function<String, Integer> integerTransformer = input -> {
        try {
            return Integer.parseInt(input);
        } catch (NumberFormatException e) {
            return 0;
        }
    };
    private static final @NotNull Function<String, Map<String, String>> stringMapTransformer = input -> {
        String[] entries = comma.split(input);
        Map<String, String> map = new HashMap<>((int) (entries.length / 0.75) + 1);

        for (String s : entries) {
            int equalIndex = s.lastIndexOf('=');
            if (equalIndex == -1) {
                map.put(s, "");
            } else {
                map.put(s.substring(0, equalIndex), s.substring(equalIndex + 1));
            }
        }
        return map;
    };
    private static final @NotNull Function<String, Map<String, Integer>> integerMapTransformer = input -> {
        String[] entries = comma.split(input);
        Map<String, Integer> map = new HashMap<>((int) (entries.length / 0.75) + 1);

        for (String s : entries) {
            int equalIndex = s.lastIndexOf('=');
            if (equalIndex == -1) {
                map.put(s, 0);
            } else {
                map.put(s.substring(0, equalIndex), integerTransformer.apply(s.substring(equalIndex + 1)));
            }
        }
        return map;
    };
    private static final @NotNull Function<Boolean, String> booleanFormatter = bool -> Terrainer.lang().get(bool ? "Flags.Allow" : "Flags.Deny");

    /**
     * Creates a flag with default {@link #formatter()} transformer, which is essentially {@link Object#toString()}.
     *
     * @param id           The ID of the flag, used in commands and to find localized names. Must match [a-zA-Z ] regex.
     * @param defaultValue The default value used in terrains where the flag is undefined.
     * @param stringToData The transformer to obtain the data object from the player's input.
     * @throws IllegalArgumentException If the provided data type is not serializable, or if the ID does not match the [a-zA-Z ] regex.
     * @see #Flag(String, Object, Function, Function)
     */
    public Flag(@NotNull String id, @NotNull T defaultValue, @NotNull Function<String, T> stringToData) {
        this(id, defaultValue, stringToData, Object::toString);
    }

    /**
     * @throws IllegalArgumentException If the provided data type is not serializable, or if the ID does not match the [a-zA-Z ] regex.
     */
    public Flag {
        if (!Serializable.class.isAssignableFrom(defaultValue.getClass())) {
            throw new IllegalArgumentException("Flags must only contain serializable data types, and '" + defaultValue.getClass().getName() + "' is not serializable.");
        }
        if (!ALLOWED_FLAG_ID_REGEX.matcher(id).matches()) {
            throw new IllegalArgumentException("Flag IDs must follow the regex [a-zA-Z ]. ID provided: '" + id + "'");
        }
    }

    public static @NotNull Flag<Boolean> newBooleanFlag(@NotNull String id, boolean defaultValue) {
        return new Flag<>(id, defaultValue, booleanTransformer, booleanFormatter);
    }

    public static @NotNull Flag<Integer> newIntegerFlag(@NotNull String id, int defaultValue) {
        return new Flag<>(id, defaultValue, integerTransformer);
    }

    public static @NotNull Flag<String> newStringFlag(@NotNull String id, @NotNull String defaultValue) {
        return new Flag<>(id, defaultValue, stringTransformer);
    }

    public static @NotNull Flag<Set<String>> newSetFlag(@NotNull String id, @Nullable Set<String> defaultValue) {
        if (defaultValue == null) defaultValue = Collections.emptySet();
        return new Flag<>(id, defaultValue, setTransformer);
    }

    public static @NotNull Flag<Map<String, String>> newStringMapFlag(@NotNull String id, @Nullable Map<String, String> defaultValue) {
        if (defaultValue == null) defaultValue = Collections.emptyMap();
        return new Flag<>(id, defaultValue, stringMapTransformer);
    }

    public static @NotNull Flag<Map<String, Integer>> newIntegerMapFlag(@NotNull String id, @Nullable Map<String, Integer> defaultValue) {
        if (defaultValue == null) defaultValue = Collections.emptyMap();
        return new Flag<>(id, defaultValue, integerMapTransformer);
    }

    /**
     * Whether the provided object has the same {@link #id()}.
     *
     * @param o The object to compare.
     * @return true if the object is equal to this flag.
     */
    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Flag<?> flag = (Flag<?>) o;
        return id.equals(flag.id);
    }

    @Override
    public int hashCode() {
        return 31 * Objects.hash(id);
    }
}
