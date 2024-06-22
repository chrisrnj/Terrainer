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

package com.epicnicity322.terrainer.core.terrain;

import com.epicnicity322.terrainer.core.Terrainer;
import com.epicnicity322.terrainer.core.config.Configurations;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * A class with default terrain flags. You may add
 */
public final class Flags {
    /**
     * Allows everyone to use anvils.
     */
    public static final @NotNull Flag<Boolean> ANVILS = Flag.newBooleanFlag("Anvils", false);
    /**
     * Allows everyone to use armor stands.
     */
    public static final @NotNull Flag<Boolean> ARMOR_STANDS = Flag.newBooleanFlag("Armor Stands", false);
    /**
     * Allows blocks such as ice, snow, cobblestone, obsidian, concrete to form.
     */
    public static final @NotNull Flag<Boolean> BLOCK_FORM = Flag.newBooleanFlag("Block Form", true);
    /**
     * Allows blocks such as grass, mushrooms and sculk to spread.
     */
    public static final @NotNull Flag<Boolean> BLOCK_SPREAD = Flag.newBooleanFlag("Block Spread", true);
    /**
     * Allows everyone to build in a terrain, even non-members.
     */
    public static final @NotNull Flag<Boolean> BUILD = Flag.newBooleanFlag("Build", false);
    /**
     * Allows everyone to place and break boats.
     */
    public static final @NotNull Flag<Boolean> BUILD_BOATS = Flag.newBooleanFlag("Build Boats", false);
    /**
     * Allows everyone to place and break minecarts.
     */
    public static final @NotNull Flag<Boolean> BUILD_MINECARTS = Flag.newBooleanFlag("Build Minecarts", false);
    /**
     * Allows everyone to use buttons and levers.
     */
    public static final @NotNull Flag<Boolean> BUTTONS = Flag.newBooleanFlag("Buttons", false);
    /**
     * Allows everyone to use cauldrons.
     */
    public static final @NotNull Flag<Boolean> CAULDRONS = Flag.newBooleanFlag("Cauldrons", false);
    /**
     * Allows cauldrons to be filled with dripstone or rain, or to evaporate due to a dry biome.
     */
    public static final @NotNull Flag<Boolean> CAULDRONS_CHANGE_LEVEL_NATURALLY = Flag.newBooleanFlag("Cauldrons Change Level Naturally", true);
    /**
     * A list of commands, separated by comma, that are not allowed to be executed in the terrain. Add * to the list to make it a whitelist.
     */
    public static final @NotNull Flag<Set<String>> COMMAND_BLACKLIST = Flag.newSetFlag("Command Blacklist", null);
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
     * Prevents players from eating food in terrains.
     */
    public static final @NotNull Flag<Boolean> EAT = Flag.newBooleanFlag("Eat", true);
    /**
     * Prevents players from harming enemy entities.
     */
    public static final @NotNull Flag<Boolean> ENEMY_HARM = Flag.newBooleanFlag("Enemy Harm", true);
    /**
     * Allows everyone to enter the terrain.
     */
    public static final @NotNull Flag<Boolean> ENTER = Flag.newBooleanFlag("Enter", true);
    /**
     * A list of commands to execute on console when a player enters the terrain. Use %p for the player's name, %t for the terrain's ID.
     */
    public static final @NotNull Flag<List<String>> ENTER_CONSOLE_COMMANDS = Flag.newListFlag("Enter Console Commands", null);
    /**
     * A list of commands to execute as the player when a player enters the terrain. Use %p for the player's name, %t for the terrain's ID.
     */
    public static final @NotNull Flag<List<String>> ENTER_PLAYER_COMMANDS = Flag.newListFlag("Enter Player Commands", null);
    /**
     * Allows everyone to enter vehicles.
     */
    public static final @NotNull Flag<Boolean> ENTER_VEHICLES = Flag.newBooleanFlag("Enter Vehicles", false);
    /**
     * Allows everyone to harm mobs.
     */
    public static final @NotNull Flag<Boolean> ENTITY_HARM = Flag.newBooleanFlag("Entity Harm", false);
    /**
     * Allows everyone to interact with entities, breed entities in a terrain.
     */
    public static final @NotNull Flag<Boolean> ENTITY_INTERACTIONS = Flag.newBooleanFlag("Entity Interactions", false);
    /**
     * Allows blocks being damaged by explosives.
     */
    public static final @NotNull Flag<Boolean> EXPLOSION_DAMAGE = Flag.newBooleanFlag("Explosion Damage", true);
    /**
     * Allows blocks being burned by fire.
     */
    public static final @NotNull Flag<Boolean> FIRE_DAMAGE = Flag.newBooleanFlag("Fire Damage", true);
    /**
     * Allows fire to spread.
     */
    public static final @NotNull Flag<Boolean> FIRE_SPREAD = Flag.newBooleanFlag("Fire Spread", true);
    /**
     * Prevents players from flying.
     */
    public static final @NotNull Flag<Boolean> FLY = Flag.newBooleanFlag("Fly", true);
    /**
     * Allows everyone to freeze water using frost walker enchanted boots.
     */
    public static final @NotNull Flag<Boolean> FROST_WALK = Flag.newBooleanFlag("Frost Walk", false);
    /**
     * Prevents players from gliding using elytras.
     */
    public static final @NotNull Flag<Boolean> GLIDE = Flag.newBooleanFlag("Glide", true);
    /**
     * Allows non-members interacting with any interactable block, such as beacons, dragon eggs, flower pots, jukeboxes,
     * noteblocks, respawn anchors etc.
     */
    public static final @NotNull Flag<Boolean> INTERACTIONS = Flag.newBooleanFlag("Interactions", false);
    /**
     * Allows everyone to drop items.
     */
    public static final @NotNull Flag<Boolean> ITEM_DROP = Flag.newBooleanFlag("Item Drop", false);
    /**
     * Allows everyone to rotate and place items on item frames. Taking items off requires {@link Flags#BUILD}.
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
     * A list of commands to execute on console when a player leaves the terrain. Use %p for the player's name, %t for the terrain's ID.
     */
    public static final @NotNull Flag<List<String>> LEAVE_CONSOLE_COMMANDS = Flag.newListFlag("Leave Console Commands", null);
    /**
     * Sends a leave message to the player leaving the terrain. When this flag has no data, the default farewell message
     * in language is used.
     */
    public static final @NotNull Flag<String> LEAVE_MESSAGE = new Flag<>("Leave Message", "", input -> {
        int max = Configurations.CONFIG.getConfiguration().getNumber("Max Description Length").orElse(30).intValue();
        if (input.length() > max) {
            throw new FlagTransformException(Terrainer.lang().get("Description.Max").replace("<max>", Integer.toString(max)));
        }
        return input.trim();
    });
    /**
     * A list of commands to execute as the player when a player leaves the terrain. Use %p for the player's name, %t for the terrain's ID.
     */
    public static final @NotNull Flag<List<String>> LEAVE_PLAYER_COMMANDS = Flag.newListFlag("Leave Player Commands", null);
    /**
     * Allows everyone to use lighters such as fireballs or flint and steel.
     */
    public static final @NotNull Flag<Boolean> LIGHTERS = Flag.newBooleanFlag("Lighters", false);
    /**
     * Allows water and lava to flow.
     */
    public static final @NotNull Flag<Boolean> LIQUID_FLOW = Flag.newBooleanFlag("Liquid Flow", true);
    /**
     * Changes the location where enter and leave messages will be shown. By default, terrains send messages in the
     * "title", but the values "actionbar", "bossbar", "chat", and "none" are supported.
     */
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
     * Allows moderators to edit terrain flags, except for flags that control moderator permissions.
     */
    public static final @NotNull Flag<Boolean> MODS_CAN_EDIT_FLAGS = Flag.newBooleanFlag("Mods Can Edit Flags", true);
    /**
     * Whether moderators can grant or revoke the moderation role of other players in a terrain.
     */
    public static final @NotNull Flag<Boolean> MODS_CAN_MANAGE_MODS = Flag.newBooleanFlag("Mods Can Manage Mods", false);
    /**
     * Allows dispensers outside the terrain firing into the inside.
     */
    public static final @NotNull Flag<Boolean> OUTSIDE_DISPENSERS = Flag.newBooleanFlag("Outside Dispensers", false);
    /**
     * Allows pistons outside the terrain moving blocks inside the terrain.
     */
    public static final @NotNull Flag<Boolean> OUTSIDE_PISTONS = Flag.newBooleanFlag("Outside Pistons", false);
    /**
     * Allows outside projectiles to land inside the terrain.
     */
    public static final @NotNull Flag<Boolean> OUTSIDE_PROJECTILES = Flag.newBooleanFlag("Outside Projectiles", false);
    /**
     * Allows pistons inside the terrain moving blocks.
     */
    public static final @NotNull Flag<Boolean> PISTONS = Flag.newBooleanFlag("Pistons", true);
    /**
     * Allows everyone to plant and break crops on farmland.
     */
    //TODO:
    public static final @NotNull Flag<Boolean> PLANT = Flag.newBooleanFlag("Plant", false);
    /**
     * Allows trees, crops, grass, etc. to grow naturally or from bone meal.
     */
    public static final @NotNull Flag<Boolean> PLANT_GROW = Flag.newBooleanFlag("Plant Grow", true);
    /**
     * Allows everyone to use hoes on grass blocks.
     */
    //TODO:
    public static final @NotNull Flag<Boolean> PLOW = Flag.newBooleanFlag("Plow", false);
    /**
     * Allows everyone to drink or throw potions in the terrain.
     */
    public static final @NotNull Flag<Boolean> POTIONS = Flag.newBooleanFlag("Potions", false);
    /**
     * Allows everyone to use preparing blocks, such as cartography tables, crafting tables, enchanting tables,
     * grindstones, looms, smithing tables, and stonecutters.
     */
    public static final @NotNull Flag<Boolean> PREPARE = Flag.newBooleanFlag("Prepare", true);
    /**
     * Allows everyone to press pressure plates.
     */
    public static final @NotNull Flag<Boolean> PRESSURE_PLATES = Flag.newBooleanFlag("Pressure Plates", false);
    /**
     * Allows everyone to shoot projectiles.
     */
    public static final @NotNull Flag<Boolean> PROJECTILES = Flag.newBooleanFlag("Projectiles", false);
    /**
     * Allows players to engage in combat.
     */
    public static final @NotNull Flag<Boolean> PVP = Flag.newBooleanFlag("PvP", false);
    /**
     * Show particles at the terrain's boundaries.
     */
    public static final @NotNull Flag<Boolean> SHOW_BORDERS = Flag.newBooleanFlag("Show Borders", true);
    /**
     * Allows everyone to right-click signs.
     */
    public static final @NotNull Flag<Boolean> SIGN_CLICK = Flag.newBooleanFlag("Sign Click", false);
    /**
     * Allows everyone to edit signs.
     */
    public static final @NotNull Flag<Boolean> SIGN_EDIT = Flag.newBooleanFlag("Sign Edit", false);
    /**
     * Allows spawners to spawn mobs.
     */
    public static final @NotNull Flag<Boolean> SPAWNERS = Flag.newBooleanFlag("Spawners", true);
    /**
     * Allows sponges to absorb water.
     */
    public static final @NotNull Flag<Boolean> SPONGES = Flag.newBooleanFlag("Sponges", true);
    /**
     * Allows everyone to trample farmland blocks.
     */
    public static final @NotNull Flag<Boolean> TRAMPLE = Flag.newBooleanFlag("Trample", false);
    /**
     * Do not freeze player's health and saturation.
     */
    public static final @NotNull Flag<Boolean> VULNERABILITY = Flag.newBooleanFlag("Vulnerability", true);

    private static final @NotNull HashSet<Flag<?>> customValues = new HashSet<>();

    private static @NotNull Predicate<String> effectChecker = effect -> true;

    private static final @NotNull Function<String, Map<String, Integer>> effectTransformer = input -> {
        String[] entries = input.split(",");
        HashMap<String, Integer> map = new HashMap<>((int) (entries.length / 0.75) + 1);

        for (String s : entries) {
            int equalIndex = s.lastIndexOf('=');
            String effect;
            if (equalIndex == -1) {
                effect = s;
                if (!effectChecker.test(effect))
                    throw new FlagTransformException(Terrainer.lang().get("Flags.Error.Unknown Effect").replace("<value>", effect));
                map.put(effect, 0);
            } else {
                effect = s.substring(0, equalIndex);
                if (!effectChecker.test(effect))
                    throw new FlagTransformException(Terrainer.lang().get("Flags.Error.Unknown Effect").replace("<value>", effect));
                String level = s.substring(equalIndex + 1);
                try {
                    map.put(effect, Integer.parseInt(level));
                } catch (NumberFormatException e) {
                    throw new FlagTransformException(Terrainer.lang().get("General.Not A Number").replace("<value>", level));
                }
            }
        }
        return map;
    };

    /**
     * Gives the effects to the players in the terrain like a beacon. Upon leaving the terrain, the effects are removed.
     */
    @SuppressWarnings("unchecked")
    public static final @NotNull Flag<Map<String, Integer>> EFFECTS = new Flag<>("Effects", (Class<Map<String, Integer>>) (Class<?>) Map.class, Collections.emptyMap(), effectTransformer, Map::toString);

    private static final @NotNull Set<Flag<?>> values = Set.of(ANVILS, ARMOR_STANDS, BLOCK_FORM, BLOCK_SPREAD, BUILD, BUILD_BOATS, BUILD_MINECARTS, BUTTONS, CAULDRONS, CAULDRONS_CHANGE_LEVEL_NATURALLY, COMMAND_BLACKLIST, CONTAINERS, DISPENSERS, DOORS, EAT, EFFECTS, ENEMY_HARM, ENTER, ENTER_CONSOLE_COMMANDS, ENTER_PLAYER_COMMANDS, ENTER_VEHICLES, ENTITY_HARM, ENTITY_INTERACTIONS, EXPLOSION_DAMAGE, FIRE_DAMAGE, FIRE_SPREAD, FLY, FROST_WALK, GLIDE, INTERACTIONS, ITEM_DROP, ITEM_FRAMES, ITEM_PICKUP, LEAF_DECAY, LEAVE, LEAVE_CONSOLE_COMMANDS, LEAVE_MESSAGE, LEAVE_PLAYER_COMMANDS, LIGHTERS, LIQUID_FLOW, MESSAGE_LOCATION, MOB_SPAWN, MODS_CAN_EDIT_FLAGS, MODS_CAN_MANAGE_MODS, OUTSIDE_DISPENSERS, OUTSIDE_PISTONS, OUTSIDE_PROJECTILES, PISTONS, PLANT, PLANT_GROW, PLOW, POTIONS, PREPARE, PRESSURE_PLATES, PROJECTILES, PVP, SHOW_BORDERS, SIGN_CLICK, SIGN_EDIT, SPAWNERS, SPONGES, TRAMPLE, VULNERABILITY);

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
     * Sets the checker of valid potion effect names for {@link Flags#EFFECTS} flag.
     *
     * @param effectChecker The new effect checker.
     */
    public static void setEffectChecker(@NotNull Predicate<String> effectChecker) {
        Flags.effectChecker = effectChecker;
    }
}
