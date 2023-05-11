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
import com.epicnicity322.terrainer.core.config.Configurations;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * A class with default terrain flags. You may add
 */
public final class Flags {
    /**
     * Allows everyone to use armor stands.
     */
    public static final @NotNull Flag<Boolean> ARMOR_STANDS = Flag.newBooleanFlag("Armor Stands", false);
    /**
     * Allows everyone to build in a terrain, even non-members.
     */
    public static final @NotNull Flag<Boolean> BUILD = Flag.newBooleanFlag("Build", false);
    /**
     * Allows everyone to use buttons and levers.
     */
    public static final @NotNull Flag<Boolean> BUTTONS = Flag.newBooleanFlag("Buttons", false);
    /**
     * Allows everyone to open barrels, chests, hoppers, and other containers.
     */
    public static final @NotNull Flag<Boolean> CONTAINERS = Flag.newBooleanFlag("Containers", false);
    /**
     * Allows dispensers inside the terrain to fire.
     */
    public static final @NotNull Flag<Boolean> DISPENSERS = Flag.newBooleanFlag("Dispensers", true);
    /**
     * Allows everyone to open and close doors, trapdoors and gates.
     */
    public static final @NotNull Flag<Boolean> DOORS = Flag.newBooleanFlag("Doors", false);
    /**
     * Gives the effects to the players in the terrain like a beacon. Upon leaving the terrain, the effects are removed.
     */
    //TODO:
    public static final @NotNull Flag<Map<String, Integer>> EFFECTS = Flag.newIntegerMapFlag("Effects", null);
    /**
     * Prevents players from harming enemy entities.
     */
    public static final @NotNull Flag<Boolean> ENEMY_HARM = Flag.newBooleanFlag("Enemy Harm", true);
    /**
     * Allows everyone to enter vehicles.
     */
    public static final @NotNull Flag<Boolean> ENTER_VEHICLES = Flag.newBooleanFlag("Enter Vehicles", false);
    /**
     * Allows everyone to harm mobs.
     */
    public static final @NotNull Flag<Boolean> ENTITY_HARM = Flag.newBooleanFlag("Entity Harm", false);
    /**
     * Allows everyone to interact with entities, breed entities and use armor stands in a terrain.
     */
    public static final @NotNull Flag<Boolean> ENTITY_INTERACTIONS = Flag.newBooleanFlag("Entity Interactions", false);
    /**
     * Allows everyone to enter the terrain.
     */
    public static final @NotNull Flag<Boolean> ENTER = Flag.newBooleanFlag("Enter", true);
    /**
     * Allows blocks being damaged by explosives.
     */
    public static final @NotNull Flag<Boolean> EXPLOSION_DAMAGE = Flag.newBooleanFlag("Explosion Damage", true);
    /**
     * Allows everyone to trample farmland blocks.
     */
    public static final @NotNull Flag<Boolean> FARMLAND_TRAMPLE = Flag.newBooleanFlag("Farmland Trample", false);
    /**
     * Allows blocks being burned by fire.
     */
    public static final @NotNull Flag<Boolean> FIRE_DAMAGE = Flag.newBooleanFlag("Fire Damage", true);
    /**
     * Allows everyone to freeze water using frost walker enchanted boots.
     */
    public static final @NotNull Flag<Boolean> FROST_WALK = Flag.newBooleanFlag("Frost Walk", false);
    /**
     * Allows non-members interacting in the terrain with any item or any block (for example: flint and steel, hoes, repeaters etc).
     */
    public static final @NotNull Flag<Boolean> INTERACTIONS = Flag.newBooleanFlag("Interactions", false);
    /**
     * Allows everyone to drop items.
     */
    public static final @NotNull Flag<Boolean> ITEM_DROP = Flag.newBooleanFlag("Item Drop", false);
    /**
     * Allows everyone to use item frames.
     */
    public static final @NotNull Flag<Boolean> ITEM_FRAMES = Flag.newBooleanFlag("Item Frames", false);
    /**
     * Allows everyone to pick up items in a terrain.
     */
    public static final @NotNull Flag<Boolean> ITEM_PICKUP = Flag.newBooleanFlag("Item Pickup", false);
    /**
     * Allows leaf blocks to decay naturally.
     */
    public static final @NotNull Flag<Boolean> LEAF_DECAY = Flag.newBooleanFlag("Leaf Decay", true);
    /**
     * Allows everyone to leave the terrain.
     */
    public static final @NotNull Flag<Boolean> LEAVE = Flag.newBooleanFlag("Leave", true);
    /**
     * Sends a leave message to the player leaving the terrain. When this flag has no data, the default farewell message
     * in language is used.
     */
    //TODO:
    public static final @NotNull Flag<String> LEAVE_MESSAGE = new Flag<>("Leave Message", "", input -> {
        int max = Configurations.CONFIG.getConfiguration().getNumber("Max Description Length").orElse(30).intValue();
        if (input.length() > max) {
            throw new FlagTransformException(Terrainer.lang().get("Description.Max").replace("<max>", Integer.toString(max)));
        }
        return input;
    });
    /**
     * Allows water and lava to flow.
     */
    public static final @NotNull Flag<Boolean> LIQUID_FLOW = Flag.newBooleanFlag("Liquid Flow", true);
    /**
     * Changes the location where enter and leave messages will be shown. By default, terrains send messages in the
     * "title", but the values "actionbar", "bossbar", "chat", and "none" are supported.
     */
    //TODO:
    // # Supports actionbar, bossbar, chat, title and none.
    public static final @NotNull Flag<String> MESSAGE_LOCATION = new Flag<>("Message Location", "title", input -> {
        input = input.toLowerCase(Locale.ROOT);
        return switch (input) {
            case "actionbar", "bossbar", "chat", "none", "title" -> input;
            default -> throw new FlagTransformException(Terrainer.lang().get("Flags.Error.Message Location"));
        };
    });
    /**
     * Allows mobs spawning naturally.
     */
    public static final @NotNull Flag<Boolean> MOB_SPAWN = Flag.newBooleanFlag("Mob Spawn", true);
    /**
     * Allows dispensers outside the terrain firing into the inside.
     */
    public static final @NotNull Flag<Boolean> OUTSIDE_DISPENSERS = Flag.newBooleanFlag("Outside Dispensers", false);
    /**
     * Allows pistons outside the terrain moving blocks inside the terrain.
     */
    public static final @NotNull Flag<Boolean> OUTSIDE_PISTONS = Flag.newBooleanFlag("Outside Pistons", false);
    /**
     * Allows pistons inside the terrain moving blocks.
     */
    public static final @NotNull Flag<Boolean> PISTONS = Flag.newBooleanFlag("Pistons", true);
    /**
     * Allows everyone to press pressure plates.
     */
    public static final @NotNull Flag<Boolean> PRESSURE_PLATES = Flag.newBooleanFlag("Pressure Plates", false);
    /**
     * Allows players to engage in combat.
     */
    public static final @NotNull Flag<Boolean> PVP = Flag.newBooleanFlag("PvP", false);
    /**
     * Allows everyone to right-click signs.
     */
    public static final @NotNull Flag<Boolean> SIGN_CLICK = Flag.newBooleanFlag("Sign Click", false);
    /**
     * Allows everyone to edit signs.
     */
    //TODO:
    public static final @NotNull Flag<Boolean> SIGN_EDIT = Flag.newBooleanFlag("Sign Edit", false);
    /**
     * Allows spawners to spawn mobs.
     */
    public static final @NotNull Flag<Boolean> SPAWNERS = Flag.newBooleanFlag("Spawners", true);
    /**
     * Do not freeze player's health and saturation.
     */
    public static final @NotNull Flag<Boolean> VULNERABILITY = Flag.newBooleanFlag("Vulnerability", true);

    //private static final @NotNull Pattern space = Pattern.compile(" ");
    private static final @NotNull HashSet<Flag<?>> customValues = new HashSet<>();
    private static final @NotNull Set<Flag<?>> values = Set.of(ARMOR_STANDS, BUILD, BUTTONS, CONTAINERS, DISPENSERS,
            DOORS, EFFECTS, ENEMY_HARM, ENTER, ENTER_VEHICLES, ENTITY_HARM, ENTITY_INTERACTIONS, EXPLOSION_DAMAGE,
            FARMLAND_TRAMPLE, FIRE_DAMAGE, INTERACTIONS, ITEM_DROP, ITEM_FRAMES, ITEM_PICKUP, LEAF_DECAY, LEAVE,
            LEAVE_MESSAGE, LIQUID_FLOW, MESSAGE_LOCATION, MOB_SPAWN, OUTSIDE_DISPENSERS, OUTSIDE_PISTONS, PISTONS,
            PRESSURE_PLATES, PVP, SIGN_CLICK, SIGN_EDIT, SPAWNERS, VULNERABILITY);

    private Flags() {
    }

    /**
     * @return An unmodifiable set of every default flag available.
     */
    public static @NotNull Set<Flag<?>> values() {
        return values;
    }

    /**
     * Returns a mutable set that contains every third party flag. You may edit this set to add or remove third-party
     * flags.
     * <p>
     * Players will be able to add and edit these flags either through the Flag Management GUI, or commands if they have
     * the flag's permission. If the flag holds serializable objects, the event FlagInputEvent will be called, providing
     * the player's input string.
     * <p>
     * Display name and description of flags will be taken from Terrainer's language file.
     *
     * @return A set of registered third-party flags.
     * @see #findPermission(Flag)
     * @see Flag
     */
    public static @NotNull HashSet<Flag<?>> customValues() {
        return customValues;
    }

    /**
     * Finds a flag by name by looking through the {@link #values()} and {@link #customValues()}.
     *
     * @param name The name of the flag to find.
     * @return The flag with matching id, if found.
     */
    public static @Nullable Flag<?> matchFlag(@NotNull String name) {
        name = name.replace('_', '-');

        for (Flag<?> f : values) {
            if (name.equalsIgnoreCase(f.id().replace(' ', '-'))) {
                return f;
            }
        }
        for (Flag<?> f : customValues) {
            if (name.equalsIgnoreCase(f.id().replace(' ', '-'))) {
                return f;
            }
        }
        return null;
    }

    /**
     * Finds the permission of a flag. Flag permissions are just the ID on lower case with no spaces, plus the prefix
     * "terrainer.flag.".
     *
     * @param flag The flag to get the permission of.
     * @return The permission of this flag.
     */
    public static @NotNull String findPermission(@NotNull Flag<?> flag) {
        return "terrainer.flag." + flag.id().toLowerCase().replace(" ", "");
    }
}
