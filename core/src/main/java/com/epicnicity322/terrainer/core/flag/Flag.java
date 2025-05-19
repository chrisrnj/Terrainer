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

package com.epicnicity322.terrainer.core.flag;

import com.epicnicity322.epicpluginlib.core.util.StringUtils;
import com.epicnicity322.terrainer.core.Terrainer;
import com.epicnicity322.terrainer.core.config.Configurations;
import com.epicnicity322.terrainer.core.terrain.Terrain;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Function;
import java.util.regex.Pattern;

/**
 * Flags are used to change the way things behave within a terrain. All terrains can have flags assigned to them, and
 * these flags may hold serializable data. Therefore, flag instances must be static and not contain any terrain-specific
 * data, as more than one terrain might have the same {@link Flag} instance.
 * <p>
 * Flags can be edited through the Flag Management GUI or the "/tr flag" command, provided they are registered in the
 * set of flags {@link Flags#registerFlag(Flag, String, String, String, Object)}.
 *
 * @param <T> The type of data this flag can hold.
 */
public class Flag<T> {
    public static final @NotNull Pattern ALLOWED_FLAG_ID_REGEX = Pattern.compile("^[a-zA-Z ]+$");

    static final @NotNull Pattern comma = Pattern.compile(",(?! )");
    static final @NotNull Function<Map<?, ?>, String> mapFormatter = m -> {
        StringBuilder builder = new StringBuilder();
        for (Map.Entry<?, ?> e : m.entrySet()) builder.append(e.getKey()).append('=').append(e.getValue()).append(',');
        if (builder.isEmpty()) return "";
        return builder.substring(0, builder.length() - 1);
    };
    static final @NotNull Function<Boolean, String> booleanFormatter = bool -> Terrainer.lang().get(bool ? "Flags.Allow" : "Flags.Deny");
    static final @NotNull Function<Collection<?>, String> collectionFormatter = col -> {
        StringBuilder builder = new StringBuilder();
        for (Object o : col) builder.append(o.toString()).append(',');
        if (builder.isEmpty()) return "";
        return builder.substring(0, builder.length() - 1);
    };
    static final @NotNull Function<String, Boolean> booleanTransformer = input -> {
        if (input.equalsIgnoreCase("true") || input.equalsIgnoreCase(Terrainer.lang().get("Commands.Flag.Allow"))) {
            return true;
        } else if (!input.equalsIgnoreCase("false") && !input.equalsIgnoreCase(Terrainer.lang().get("Commands.Flag.Deny"))) {
            throw new FlagTransformException(Terrainer.lang().get("Flags.Error.Boolean"));
        }
        return false;
    };
    static final @NotNull Function<String, String> stringTransformer = input -> input;
    static final @NotNull Function<String, Set<String>> setTransformer = input -> Set.of(comma.split(input));
    static final @NotNull Function<String, List<String>> listTransformer = input -> List.of(comma.split(input));
    static final @NotNull Function<String, Integer> integerTransformer = input -> {
        if (!StringUtils.isNumeric(input)) return 0;
        try {
            return Integer.parseInt(input);
        } catch (NumberFormatException e) {
            return 0;
        }
    };
    static final @NotNull Function<String, Map<String, String>> stringMapTransformer = input -> {
        String[] entries = comma.split(input);
        HashMap<String, String> map = new HashMap<>((int) (entries.length / .75f) + 1);

        for (String s : entries) {
            int equalIndex = s.indexOf('=');
            if (equalIndex == -1) {
                map.put(s, "");
            } else {
                map.put(s.substring(0, equalIndex), s.substring(equalIndex + 1));
            }
        }
        return Collections.unmodifiableMap(map);
    };
    static final @NotNull Function<String, Map<String, Integer>> integerMapTransformer = input -> {
        String[] entries = comma.split(input);
        HashMap<String, Integer> map = new HashMap<>((int) (entries.length / .75f) + 1);

        for (String s : entries) {
            int equalIndex = s.lastIndexOf('=');
            if (equalIndex == -1) {
                map.put(s, 0);
            } else {
                map.put(s.substring(0, equalIndex), integerTransformer.apply(s.substring(equalIndex + 1)));
            }
        }
        return Collections.unmodifiableMap(map);
    };

    private final @NotNull String id;
    private final @NotNull Class<T> dataType;
    private final @NotNull String bypassPermission;
    private final @NotNull String editPermission;
    private final @NotNull Function<@NotNull String, @NotNull T> transformer;
    private final @NotNull Function<@NotNull T, @NotNull String> formatter;
    @NotNull T defaultValue;

    /**
     * Creates a flag to be used in terrains as an identifier, transformer and formatter of data.
     * <p>
     * The provided default value will be used in case a default is misconfigured or not present for this flag's ID in
     * the {@link Configurations#FLAGS} configuration at the path: <code>{@link Flag#id()} + ".Default"</code>.
     *
     * @param id               The ID of the flag. Must match the [a-zA-Z ] regex.
     * @param dataType         The type of data to be saved by this flag.
     * @param defaultValue     The default value used in terrains where the flag is undefined.
     * @param bypassPermission The permission to bypass a flag's protection.
     * @param editPermission   The permission to edit the flag's value through commands or the Flag Management GUI.
     * @param transformer      The function to obtain the data object from the player's input.
     * @param formatter        The function to display the current set data of this flag to the player.
     * @throws IllegalArgumentException If the ID does not match the [a-zA-Z ] regex.
     * @see Flag
     */
    public Flag(@NotNull String id, @NotNull Class<T> dataType, @NotNull T defaultValue,
                @NotNull String bypassPermission, @NotNull String editPermission,
                @NotNull Function<@NotNull String, @NotNull T> transformer,
                @NotNull Function<@NotNull T, @NotNull String> formatter) {
        if (!ALLOWED_FLAG_ID_REGEX.matcher(id).matches()) {
            throw new IllegalArgumentException("Flag IDs must follow the regex [a-zA-Z ]. ID provided: '" + id + "'");
        }

        this.id = id;
        this.dataType = dataType;
        this.defaultValue = defaultValue;
        this.bypassPermission = bypassPermission;
        this.editPermission = editPermission;
        this.transformer = transformer;
        this.formatter = formatter;
    }

    /**
     * Creates a flag with a default {@link #formatter()} function ({@link Object#toString()}),
     * a default bypass permission (<code>"terrainer.bypass." + {@link Flag#commandFriendlyId()}</code>), and a default
     * edit permission (<code>"terrainer.flag." + {@link Flag#commandFriendlyId()}</code>).
     * <p>
     * The provided default value will be used in case a default is misconfigured or not present for this flag's ID in
     * the {@link Configurations#FLAGS} configuration at the path: <code>{@link Flag#id()} + ".Default"</code>.
     *
     * @param id           The ID of the flag. Must match the [a-zA-Z ] regex.
     * @param defaultValue The default value used in terrains where the flag is undefined.
     * @param transformer  The function to obtain the data object from the player's input.
     * @throws IllegalArgumentException If the ID does not match the [a-zA-Z ] regex.
     * @see #Flag(String, Class, Object, String, String, Function, Function)
     */
    @SuppressWarnings("unchecked")
    public Flag(@NotNull String id, @NotNull T defaultValue, @NotNull Function<String, T> transformer) {
        this(id, (Class<T>) defaultValue.getClass(), defaultValue, transformer, Object::toString);
    }

    /**
     * Creates a flag with a default bypass permission (<code>"terrainer.bypass." + {@link Flag#commandFriendlyId()}</code>),
     * and a default edit permission (<code>"terrainer.flag." + {@link Flag#commandFriendlyId()}</code>).
     * <p>
     * The provided default value will be used in case a default is misconfigured or not present for this flag's ID in
     * the {@link Configurations#FLAGS} configuration at the path: <code>{@link Flag#id()} + ".Default"</code>.
     *
     * @param id           The ID of the flag. Must match the [a-zA-Z ] regex.
     * @param dataType     The type of data to be saved by this flag.
     * @param defaultValue The default value used in terrains where the flag is undefined.
     * @param transformer  The function to obtain the data object from the player's input.
     * @param formatter    The function to display the current set data of this flag to the player.
     * @throws IllegalArgumentException If the ID does not match the [a-zA-Z ] regex.
     * @see #Flag(String, Class, Object, String, String, Function, Function)
     */
    public Flag(@NotNull String id, @NotNull Class<T> dataType, @NotNull T defaultValue, @NotNull Function<String, T> transformer, @NotNull Function<T, String> formatter) {
        this(id, dataType, defaultValue, findBypassPermission(id), findEditPermission(id), transformer, formatter);
    }

    /**
     * Finds the permission to assign a flag. Flag edit permissions are just the ID on lower case with no spaces, plus
     * the prefix "terrainer.flag.".
     *
     * @param flagID The ID to use in the edit permission.
     * @return The permission to edit this flag.
     */
    private static @NotNull String findEditPermission(@NotNull String flagID) {
        return "terrainer.flag." + flagID.toLowerCase().replace(' ', '-');
    }

    /**
     * Finds the permission to bypass a flag's protection. Flag bypass permissions are just the ID on lower case with no
     * spaces, plus the prefix "terrainer.bypass.".
     *
     * @param flagID The ID to use in the bypass permission.
     * @return The permission of this flag.
     */
    private static @NotNull String findBypassPermission(@NotNull String flagID) {
        return "terrainer.bypass." + flagID.toLowerCase().replace(' ', '-');
    }

    /**
     * Creates a new flag with a boolean transformer and formatter.
     * <p>
     * The edit and bypass permissions will be, respectfully:
     * <ul>
     *     <li><code>"terrainer.flag." + {@link Flag#commandFriendlyId()}</code></li>
     *     <li><code>"terrainer.bypass." + {@link Flag#commandFriendlyId()}</code></li>
     * </ul>
     *
     * @param id           The ID of the flag to be used in commands and as options in configuration.
     * @param defaultValue The default value of this flag if no value is already present in {@link Configurations#FLAGS} at the path <code>{@link Flag#id()} + ".Default"</code>.<br> Default values will be used if the flag is unset in a terrain that has {@link Terrain#usesDefaultFlagValues()} true.
     * @return A flag that accepts boolean objects.
     * @throws IllegalArgumentException If the ID does not match the [a-zA-Z ] regex.
     * @see Flag
     * @see Flag#Flag(String, Class, Object, Function, Function)
     */
    public static @NotNull Flag<Boolean> newBooleanFlag(@NotNull String id, boolean defaultValue) {
        return new Flag<>(id, Boolean.class, defaultValue, booleanTransformer, booleanFormatter);
    }

    /**
     * Creates a new flag with an integer transformer and formatter.
     * <p>
     * The edit and bypass permissions will be, respectfully:
     * <ul>
     *     <li><code>"terrainer.flag." + {@link Flag#commandFriendlyId()}</code></li>
     *     <li><code>"terrainer.bypass." + {@link Flag#commandFriendlyId()}</code></li>
     * </ul>
     *
     * @param id           The ID of the flag to be used in commands and as options in configuration.
     * @param defaultValue The default value of this flag if no value is already present in {@link Configurations#FLAGS} at the path <code>{@link Flag#id()} + ".Default"</code>.<br> Default values will be used if the flag is unset in a terrain that has {@link Terrain#usesDefaultFlagValues()} true.
     * @return A flag that accepts integer objects.
     * @throws IllegalArgumentException If the ID does not match the [a-zA-Z ] regex.
     * @see Flag
     * @see Flag#Flag(String, Class, Object, Function, Function)
     */
    public static @NotNull Flag<Integer> newIntegerFlag(@NotNull String id, int defaultValue) {
        return new Flag<>(id, defaultValue, integerTransformer);
    }

    /**
     * Creates a new flag with a string transformer and formatter.
     * <p>
     * The edit and bypass permissions will be, respectfully:
     * <ul>
     *     <li><code>"terrainer.flag." + {@link Flag#commandFriendlyId()}</code></li>
     *     <li><code>"terrainer.bypass." + {@link Flag#commandFriendlyId()}</code></li>
     * </ul>
     *
     * @param id           The ID of the flag to be used in commands and as options in configuration.
     * @param defaultValue The default value of this flag if no value is already present in {@link Configurations#FLAGS} at the path <code>{@link Flag#id()} + ".Default"</code>.<br> Default values will be used if the flag is unset in a terrain that has {@link Terrain#usesDefaultFlagValues()} true.
     * @return A flag that accepts string objects.
     * @throws IllegalArgumentException If the ID does not match the [a-zA-Z ] regex.
     * @see Flag
     * @see Flag#Flag(String, Class, Object, Function, Function)
     */
    public static @NotNull Flag<String> newStringFlag(@NotNull String id, @NotNull String defaultValue) {
        return new Flag<>(id, defaultValue, stringTransformer);
    }

    /**
     * Creates a new flag with a string list transformer and formatter.
     * <p>
     * The edit and bypass permissions will be, respectfully:
     * <ul>
     *     <li><code>"terrainer.flag." + {@link Flag#commandFriendlyId()}</code></li>
     *     <li><code>"terrainer.bypass." + {@link Flag#commandFriendlyId()}</code></li>
     * </ul>
     *
     * @param id           The ID of the flag to be used in commands and as options in configuration.
     * @param defaultValue The default value of this flag if no value is already present in {@link Configurations#FLAGS} at the path <code>{@link Flag#id()} + ".Default"</code>.<br> Default values will be used if the flag is unset in a terrain that has {@link Terrain#usesDefaultFlagValues()} true.
     * @return A flag that accepts string list objects.
     * @throws IllegalArgumentException If the ID does not match the [a-zA-Z ] regex.
     * @see Flag
     * @see Flag#Flag(String, Class, Object, Function, Function)
     */
    @SuppressWarnings("unchecked")
    public static @NotNull Flag<List<String>> newListFlag(@NotNull String id, @Nullable List<String> defaultValue) {
        if (defaultValue == null) defaultValue = Collections.emptyList();
        return new Flag<>(id, (Class<List<String>>) (Class<?>) List.class, defaultValue, listTransformer, collectionFormatter::apply);
    }

    /**
     * Creates a new flag with a string set transformer and formatter.
     * <p>
     * The edit and bypass permissions will be, respectfully:
     * <ul>
     *     <li><code>"terrainer.flag." + {@link Flag#commandFriendlyId()}</code></li>
     *     <li><code>"terrainer.bypass." + {@link Flag#commandFriendlyId()}</code></li>
     * </ul>
     *
     * @param id           The ID of the flag to be used in commands and as options in configuration.
     * @param defaultValue The default value of this flag if no value is already present in {@link Configurations#FLAGS} at the path <code>{@link Flag#id()} + ".Default"</code>.<br> Default values will be used if the flag is unset in a terrain that has {@link Terrain#usesDefaultFlagValues()} true.
     * @return A flag that accepts string set objects.
     * @throws IllegalArgumentException If the ID does not match the [a-zA-Z ] regex.
     * @see Flag
     * @see Flag#Flag(String, Class, Object, Function, Function)
     */
    @SuppressWarnings("unchecked")
    public static @NotNull Flag<Set<String>> newSetFlag(@NotNull String id, @Nullable Set<String> defaultValue) {
        if (defaultValue == null) defaultValue = Collections.emptySet();
        return new Flag<>(id, (Class<Set<String>>) (Class<?>) Set.class, defaultValue, setTransformer, collectionFormatter::apply);
    }

    /**
     * Creates a new flag with a transformer and formatter of a map of string keys and string values.
     * <p>
     * The edit and bypass permissions will be, respectfully:
     * <ul>
     *     <li><code>"terrainer.flag." + {@link Flag#commandFriendlyId()}</code></li>
     *     <li><code>"terrainer.bypass." + {@link Flag#commandFriendlyId()}</code></li>
     * </ul>
     *
     * @param id           The ID of the flag to be used in commands and as options in configuration.
     * @param defaultValue The default value of this flag if no value is already present in {@link Configurations#FLAGS} at the path <code>{@link Flag#id()} + ".Default"</code>.<br> Default values will be used if the flag is unset in a terrain that has {@link Terrain#usesDefaultFlagValues()} true.
     * @return A flag that accepts {@literal Map<String, String>} objects.
     * @throws IllegalArgumentException If the ID does not match the [a-zA-Z ] regex.
     * @see Flag
     * @see Flag#Flag(String, Class, Object, Function, Function)
     */
    @SuppressWarnings("unchecked")
    public static @NotNull Flag<Map<String, String>> newStringMapFlag(@NotNull String id, @Nullable Map<String, String> defaultValue) {
        if (defaultValue == null) defaultValue = Collections.emptyMap();
        return new Flag<>(id, (Class<Map<String, String>>) (Class<?>) Map.class, defaultValue, stringMapTransformer, mapFormatter::apply);
    }

    /**
     * Creates a new flag with a transformer and formatter of a map of string keys and integer values.
     * <p>
     * The edit and bypass permissions will be, respectfully:
     * <ul>
     *     <li><code>"terrainer.flag." + {@link Flag#commandFriendlyId()}</code></li>
     *     <li><code>"terrainer.bypass." + {@link Flag#commandFriendlyId()}</code></li>
     * </ul>
     *
     * @param id           The ID of the flag to be used in commands and as options in configuration.
     * @param defaultValue The default value of this flag if no value is already present in {@link Configurations#FLAGS} at the path <code>{@link Flag#id()} + ".Default"</code>.<br> Default values will be used if the flag is unset in a terrain that has {@link Terrain#usesDefaultFlagValues()} true.
     * @return A flag that accepts {@literal Map<String, Integer>} objects.
     * @throws IllegalArgumentException If the ID does not match the [a-zA-Z ] regex.
     * @see Flag
     * @see Flag#Flag(String, Class, Object, Function, Function)
     */
    @SuppressWarnings("unchecked")
    public static @NotNull Flag<Map<String, Integer>> newIntegerMapFlag(@NotNull String id, @Nullable Map<String, Integer> defaultValue) {
        if (defaultValue == null) defaultValue = Collections.emptyMap();
        return new Flag<>(id, (Class<Map<String, Integer>>) (Class<?>) Map.class, defaultValue, integerMapTransformer, mapFormatter::apply);
    }

    public @NotNull String id() {
        return id;
    }

    public @NotNull Class<T> dataType() {
        return dataType;
    }

    public @NotNull T defaultValue() {
        return defaultValue;
    }

    public @NotNull String bypassPermission() {
        return bypassPermission;
    }

    public @NotNull String editPermission() {
        return editPermission;
    }

    public @NotNull Function<@NotNull String, @NotNull T> transformer() {
        return transformer;
    }

    public @NotNull Function<@NotNull T, @NotNull String> formatter() {
        return formatter;
    }

    /**
     * The ID to be used in commands and permissions.
     *
     * @return The {@link #id()} with spaces replaced by "-".
     */
    public @NotNull String commandFriendlyId() {
        return id.toLowerCase(Locale.ROOT).replace(' ', '-');
    }

    /**
     * Whether the provided object is a flag with the same {@link #id()}.
     *
     * @param o The object to compare.
     * @return true if the object is a flag with same ID.
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

    @Override
    public String toString() {
        return id;
    }
}