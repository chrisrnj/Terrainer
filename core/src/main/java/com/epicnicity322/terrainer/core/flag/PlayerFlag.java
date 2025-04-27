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

import com.epicnicity322.terrainer.core.config.Configurations;
import com.epicnicity322.terrainer.core.terrain.Terrain;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

public class PlayerFlag<T> extends Flag<T> {
    public PlayerFlag(@NotNull String id, @NotNull Class<T> dataType, @NotNull T defaultValue, @NotNull String bypassPermission, @NotNull String editPermission, @NotNull Function<@NotNull String, @NotNull T> transformer, @NotNull Function<@NotNull T, @NotNull String> formatter) {
        super(id, dataType, defaultValue, bypassPermission, editPermission, transformer, formatter);
    }

    public PlayerFlag(@NotNull String id, @NotNull T defaultValue, @NotNull Function<String, T> transformer) {
        super(id, defaultValue, transformer);
    }

    public PlayerFlag(@NotNull String id, @NotNull Class<T> dataType, @NotNull T defaultValue, @NotNull Function<String, T> transformer, @NotNull Function<T, String> formatter) {
        super(id, dataType, defaultValue, transformer, formatter);
    }

    /**
     * Creates a new player flag with a boolean transformer and formatter.
     * <p>
     * The edit and bypass permissions will be, respectfully:
     * <ul>
     *     <li><code>"terrainer.flag." + {@link Flag#commandFriendlyId()}</code></li>
     *     <li><code>"terrainer.bypass." + {@link Flag#commandFriendlyId()}</code></li>
     * </ul>
     *
     * @param id           The ID of the flag to be used in commands and as options in configuration.
     * @param defaultValue The default value of this flag if no value is already present in {@link Configurations#FLAGS} at the path <code>{@link Flag#id()} + ".Default"</code>.<br> Default values will be used if the flag is unset in a terrain that has {@link Terrain#usesDefaultFlagValues()} true.
     * @return A player flag that accepts boolean objects.
     * @throws IllegalArgumentException If the ID does not match the [a-zA-Z ] regex.
     * @see PlayerFlag
     * @see PlayerFlag#PlayerFlag(String, Class, Object, Function, Function)
     */
    public static @NotNull PlayerFlag<Boolean> newBooleanFlag(@NotNull String id, boolean defaultValue) {
        return new PlayerFlag<>(id, Boolean.class, defaultValue, booleanTransformer, booleanFormatter);
    }

    /**
     * Creates a new player flag with an integer transformer and formatter.
     * <p>
     * The edit and bypass permissions will be, respectfully:
     * <ul>
     *     <li><code>"terrainer.flag." + {@link Flag#commandFriendlyId()}</code></li>
     *     <li><code>"terrainer.bypass." + {@link Flag#commandFriendlyId()}</code></li>
     * </ul>
     *
     * @param id           The ID of the flag to be used in commands and as options in configuration.
     * @param defaultValue The default value of this flag if no value is already present in {@link Configurations#FLAGS} at the path <code>{@link Flag#id()} + ".Default"</code>.<br> Default values will be used if the flag is unset in a terrain that has {@link Terrain#usesDefaultFlagValues()} true.
     * @return A player flag that accepts integer objects.
     * @throws IllegalArgumentException If the ID does not match the [a-zA-Z ] regex.
     * @see PlayerFlag
     * @see PlayerFlag#PlayerFlag(String, Class, Object, Function, Function)
     */
    public static @NotNull PlayerFlag<Integer> newIntegerFlag(@NotNull String id, int defaultValue) {
        return new PlayerFlag<>(id, defaultValue, integerTransformer);
    }

    /**
     * Creates a new player flag with a string transformer and formatter.
     * <p>
     * The edit and bypass permissions will be, respectfully:
     * <ul>
     *     <li><code>"terrainer.flag." + {@link Flag#commandFriendlyId()}</code></li>
     *     <li><code>"terrainer.bypass." + {@link Flag#commandFriendlyId()}</code></li>
     * </ul>
     *
     * @param id           The ID of the flag to be used in commands and as options in configuration.
     * @param defaultValue The default value of this flag if no value is already present in {@link Configurations#FLAGS} at the path <code>{@link Flag#id()} + ".Default"</code>.<br> Default values will be used if the flag is unset in a terrain that has {@link Terrain#usesDefaultFlagValues()} true.
     * @return A player flag that accepts string objects.
     * @throws IllegalArgumentException If the ID does not match the [a-zA-Z ] regex.
     * @see PlayerFlag
     * @see PlayerFlag#PlayerFlag(String, Class, Object, Function, Function)
     */
    public static @NotNull PlayerFlag<String> newStringFlag(@NotNull String id, @NotNull String defaultValue) {
        return new PlayerFlag<>(id, defaultValue, stringTransformer);
    }

    /**
     * Creates a new player flag with a string list transformer and formatter.
     * <p>
     * The edit and bypass permissions will be, respectfully:
     * <ul>
     *     <li><code>"terrainer.flag." + {@link Flag#commandFriendlyId()}</code></li>
     *     <li><code>"terrainer.bypass." + {@link Flag#commandFriendlyId()}</code></li>
     * </ul>
     *
     * @param id           The ID of the flag to be used in commands and as options in configuration.
     * @param defaultValue The default value of this flag if no value is already present in {@link Configurations#FLAGS} at the path <code>{@link Flag#id()} + ".Default"</code>.<br> Default values will be used if the flag is unset in a terrain that has {@link Terrain#usesDefaultFlagValues()} true.
     * @return A player flag that accepts string list objects.
     * @throws IllegalArgumentException If the ID does not match the [a-zA-Z ] regex.
     * @see PlayerFlag
     * @see PlayerFlag#PlayerFlag(String, Class, Object, Function, Function)
     */
    @SuppressWarnings("unchecked")
    public static @NotNull PlayerFlag<List<String>> newListFlag(@NotNull String id, @Nullable List<String> defaultValue) {
        if (defaultValue == null) defaultValue = Collections.emptyList();
        return new PlayerFlag<>(id, (Class<List<String>>) (Class<?>) List.class, defaultValue, listTransformer, collectionFormatter::apply);
    }

    /**
     * Creates a new player flag with a string set transformer and formatter.
     * <p>
     * The edit and bypass permissions will be, respectfully:
     * <ul>
     *     <li><code>"terrainer.flag." + {@link Flag#commandFriendlyId()}</code></li>
     *     <li><code>"terrainer.bypass." + {@link Flag#commandFriendlyId()}</code></li>
     * </ul>
     *
     * @param id           The ID of the flag to be used in commands and as options in configuration.
     * @param defaultValue The default value of this flag if no value is already present in {@link Configurations#FLAGS} at the path <code>{@link Flag#id()} + ".Default"</code>.<br> Default values will be used if the flag is unset in a terrain that has {@link Terrain#usesDefaultFlagValues()} true.
     * @return A player flag that accepts string set objects.
     * @throws IllegalArgumentException If the ID does not match the [a-zA-Z ] regex.
     * @see PlayerFlag
     * @see PlayerFlag#PlayerFlag(String, Class, Object, Function, Function)
     */
    @SuppressWarnings("unchecked")
    public static @NotNull PlayerFlag<Set<String>> newSetFlag(@NotNull String id, @Nullable Set<String> defaultValue) {
        if (defaultValue == null) defaultValue = Collections.emptySet();
        return new PlayerFlag<>(id, (Class<Set<String>>) (Class<?>) Set.class, defaultValue, setTransformer, collectionFormatter::apply);
    }

    /**
     * Creates a new player flag with a transformer and formatter of a map of string keys and string values.
     * <p>
     * The edit and bypass permissions will be, respectfully:
     * <ul>
     *     <li><code>"terrainer.flag." + {@link Flag#commandFriendlyId()}</code></li>
     *     <li><code>"terrainer.bypass." + {@link Flag#commandFriendlyId()}</code></li>
     * </ul>
     *
     * @param id           The ID of the flag to be used in commands and as options in configuration.
     * @param defaultValue The default value of this flag if no value is already present in {@link Configurations#FLAGS} at the path <code>{@link Flag#id()} + ".Default"</code>.<br> Default values will be used if the flag is unset in a terrain that has {@link Terrain#usesDefaultFlagValues()} true.
     * @return A player flag that accepts {@literal Map<String, String>} objects.
     * @throws IllegalArgumentException If the ID does not match the [a-zA-Z ] regex.
     * @see PlayerFlag
     * @see PlayerFlag#PlayerFlag(String, Class, Object, Function, Function)
     */
    @SuppressWarnings("unchecked")
    public static @NotNull PlayerFlag<Map<String, String>> newStringMapFlag(@NotNull String id, @Nullable Map<String, String> defaultValue) {
        if (defaultValue == null) defaultValue = Collections.emptyMap();
        return new PlayerFlag<>(id, (Class<Map<String, String>>) (Class<?>) Map.class, defaultValue, stringMapTransformer, mapFormatter::apply);
    }

    /**
     * Creates a new player flag with a transformer and formatter of a map of string keys and integer values.
     * <p>
     * The edit and bypass permissions will be, respectfully:
     * <ul>
     *     <li><code>"terrainer.flag." + {@link Flag#commandFriendlyId()}</code></li>
     *     <li><code>"terrainer.bypass." + {@link Flag#commandFriendlyId()}</code></li>
     * </ul>
     *
     * @param id           The ID of the flag to be used in commands and as options in configuration.
     * @param defaultValue The default value of this flag if no value is already present in {@link Configurations#FLAGS} at the path <code>{@link Flag#id()} + ".Default"</code>.<br> Default values will be used if the flag is unset in a terrain that has {@link Terrain#usesDefaultFlagValues()} true.
     * @return A player flag that accepts {@literal Map<String, Integer>} objects.
     * @throws IllegalArgumentException If the ID does not match the [a-zA-Z ] regex.
     * @see PlayerFlag
     * @see PlayerFlag#PlayerFlag(String, Class, Object, Function, Function)
     */
    @SuppressWarnings("unchecked")
    public static @NotNull PlayerFlag<Map<String, Integer>> newIntegerMapFlag(@NotNull String id, @Nullable Map<String, Integer> defaultValue) {
        if (defaultValue == null) defaultValue = Collections.emptyMap();
        return new PlayerFlag<>(id, (Class<Map<String, Integer>>) (Class<?>) Map.class, defaultValue, integerMapTransformer, mapFormatter::apply);
    }
}
