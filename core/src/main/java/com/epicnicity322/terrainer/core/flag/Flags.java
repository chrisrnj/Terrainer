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

import com.epicnicity322.epicpluginlib.core.logger.ConsoleLogger;
import com.epicnicity322.epicpluginlib.core.util.PathUtils;
import com.epicnicity322.terrainer.core.Terrainer;
import com.epicnicity322.terrainer.core.config.Configurations;
import com.epicnicity322.yamlhandler.Configuration;
import com.epicnicity322.yamlhandler.ConfigurationSection;
import com.epicnicity322.yamlhandler.YamlConfigurationLoader;
import com.epicnicity322.yamlhandler.exceptions.InvalidConfigurationException;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Predicate;

/**
 * A class with default terrain flags. You may add
 */
public final class Flags {
    /**
     * Allows everyone to use anvils.
     */
    public static final @NotNull PlayerFlag<Boolean> ANVILS = PlayerFlag.newBooleanFlag("Anvils", false);
    /**
     * Allows everyone to use armor stands.
     */
    public static final @NotNull PlayerFlag<Boolean> ARMOR_STANDS = PlayerFlag.newBooleanFlag("Armor Stands", false);
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
    public static final @NotNull PlayerFlag<Boolean> BUILD = PlayerFlag.newBooleanFlag("Build", false);
    /**
     * Allows everyone to place and break boats.
     */
    public static final @NotNull PlayerFlag<Boolean> BUILD_BOATS = PlayerFlag.newBooleanFlag("Build Boats", false);
    /**
     * Allows everyone to place and break minecarts.
     */
    public static final @NotNull PlayerFlag<Boolean> BUILD_MINECARTS = PlayerFlag.newBooleanFlag("Build Minecarts", false);
    /**
     * Allows everyone to use buttons and levers.
     */
    public static final @NotNull PlayerFlag<Boolean> BUTTONS = PlayerFlag.newBooleanFlag("Buttons", false);
    /**
     * Allows everyone to use cauldrons.
     */
    public static final @NotNull PlayerFlag<Boolean> CAULDRONS = PlayerFlag.newBooleanFlag("Cauldrons", false);
    /**
     * Allows cauldrons to be filled with dripstone or rain, or to evaporate due to a dry biome.
     */
    public static final @NotNull Flag<Boolean> CAULDRONS_CHANGE_LEVEL_NATURALLY = Flag.newBooleanFlag("Cauldrons Change Level Naturally", true);
    /**
     * A list of commands, separated by comma, that are not allowed to be executed in the terrain. Add * to the list to make it a whitelist.
     */
    public static final @NotNull PlayerFlag<Set<String>> COMMAND_BLACKLIST = PlayerFlag.newSetFlag("Command Blacklist", null);
    /**
     * Allows everyone to open barrels, chests, hoppers, and other containers.
     */
    public static final @NotNull PlayerFlag<Boolean> CONTAINERS = PlayerFlag.newBooleanFlag("Containers", false);
    /**
     * Allows dispensers inside the terrain to fire.
     */
    public static final @NotNull PlayerFlag<Boolean> DISPENSERS = PlayerFlag.newBooleanFlag("Dispensers", true);
    /**
     * Allows everyone to open and close doors, trapdoors and gates.
     */
    public static final @NotNull PlayerFlag<Boolean> DOORS = PlayerFlag.newBooleanFlag("Doors", false);
    /**
     * Prevents players from eating food in terrains.
     */
    public static final @NotNull PlayerFlag<Boolean> EAT = PlayerFlag.newBooleanFlag("Eat", true);
    /**
     * Allows moderators to edit terrain flags, except for flags that control moderator permissions.
     */
    public static final @NotNull PlayerFlag<Boolean> EDIT_FLAGS = PlayerFlag.newBooleanFlag("Edit Flags", true);
    /**
     * Prevents players from harming enemy entities.
     */
    public static final @NotNull PlayerFlag<Boolean> ENEMY_HARM = PlayerFlag.newBooleanFlag("Enemy Harm", true);
    /**
     * Allows everyone to enter the terrain.
     */
    public static final @NotNull PlayerFlag<Boolean> ENTER = PlayerFlag.newBooleanFlag("Enter", true);
    /**
     * A list of commands to execute on console when a player enters the terrain. Use %p for the player's name, %t for the terrain's ID.
     */
    public static final @NotNull PlayerFlag<List<String>> ENTER_CONSOLE_COMMANDS = PlayerFlag.newListFlag("Enter Console Commands", null);
    /**
     * A list of commands to execute as the player when a player enters the terrain. Use %p for the player's name, %t for the terrain's ID.
     */
    public static final @NotNull PlayerFlag<List<String>> ENTER_PLAYER_COMMANDS = PlayerFlag.newListFlag("Enter Player Commands", null);
    /**
     * Allows everyone to enter vehicles.
     */
    public static final @NotNull PlayerFlag<Boolean> ENTER_VEHICLES = PlayerFlag.newBooleanFlag("Enter Vehicles", false);
    /**
     * Allows everyone to harm mobs.
     */
    public static final @NotNull PlayerFlag<Boolean> ENTITY_HARM = PlayerFlag.newBooleanFlag("Entity Harm", false);
    /**
     * Allows everyone to interact with entities, breed entities in a terrain.
     */
    public static final @NotNull PlayerFlag<Boolean> ENTITY_INTERACTIONS = PlayerFlag.newBooleanFlag("Entity Interactions", false);
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
    public static final @NotNull PlayerFlag<Boolean> FLY = PlayerFlag.newBooleanFlag("Fly", true);
    /**
     * Allows everyone to freeze water using frost walker enchanted boots.
     */
    public static final @NotNull PlayerFlag<Boolean> FROST_WALK = PlayerFlag.newBooleanFlag("Frost Walk", false);
    /**
     * Prevents players from gliding using elytras.
     */
    public static final @NotNull PlayerFlag<Boolean> GLIDE = PlayerFlag.newBooleanFlag("Glide", true);
    /**
     * Allows non-members interacting with any interactable block, such as beacons, dragon eggs, flower pots, jukeboxes,
     * noteblocks, respawn anchors etc.
     */
    public static final @NotNull PlayerFlag<Boolean> INTERACTIONS = PlayerFlag.newBooleanFlag("Interactions", false);
    /**
     * Allows everyone to drop items.
     */
    public static final @NotNull PlayerFlag<Boolean> ITEM_DROP = PlayerFlag.newBooleanFlag("Item Drop", false);
    /**
     * Allows everyone to rotate and place items on item frames. Taking items off requires {@link Flags#BUILD}.
     */
    public static final @NotNull PlayerFlag<Boolean> ITEM_FRAMES = PlayerFlag.newBooleanFlag("Item Frames", false);
    /**
     * Allows everyone to pick up items in a terrain.
     */
    public static final @NotNull PlayerFlag<Boolean> ITEM_PICKUP = PlayerFlag.newBooleanFlag("Item Pickup", false);
    /**
     * Allows players to pick up only the items they dropped themselves, such as in death or item drop event.
     */
    // TODO:
    public static final @NotNull PlayerFlag<Boolean> ITEM_PICKUP_OWN = PlayerFlag.newBooleanFlag("Item Pickup Own", false);
    /**
     * Allows leaf blocks to decay naturally.
     */
    public static final @NotNull Flag<Boolean> LEAF_DECAY = Flag.newBooleanFlag("Leaf Decay", true);
    /**
     * Allows everyone to leave the terrain.
     */
    public static final @NotNull PlayerFlag<Boolean> LEAVE = PlayerFlag.newBooleanFlag("Leave", true);
    /**
     * A list of commands to execute on console when a player leaves the terrain. Use %p for the player's name, %t for the terrain's ID.
     */
    public static final @NotNull PlayerFlag<List<String>> LEAVE_CONSOLE_COMMANDS = PlayerFlag.newListFlag("Leave Console Commands", null);
    /**
     * Sends a leave message to the player leaving the terrain. When this flag has no data, the default farewell message
     * in language is used.
     */
    public static final @NotNull PlayerFlag<String> LEAVE_MESSAGE = new PlayerFlag<>("Leave Message", "", input -> {
        int max = Configurations.CONFIG.getConfiguration().getNumber("Max Description Length").orElse(30).intValue();
        if (input.length() > max) {
            throw new FlagTransformException(Terrainer.lang().get("Description.Max").replace("<max>", Integer.toString(max)));
        }
        return input.trim();
    });
    /**
     * A list of commands to execute as the player when a player leaves the terrain. Use %p for the player's name, %t for the terrain's ID.
     */
    public static final @NotNull PlayerFlag<List<String>> LEAVE_PLAYER_COMMANDS = PlayerFlag.newListFlag("Leave Player Commands", null);
    /**
     * Allows everyone to use lighters such as fireballs or flint and steel.
     */
    public static final @NotNull PlayerFlag<Boolean> LIGHTERS = PlayerFlag.newBooleanFlag("Lighters", false);
    /**
     * Allows water and lava to flow.
     */
    public static final @NotNull Flag<Boolean> LIQUID_FLOW = Flag.newBooleanFlag("Liquid Flow", true);
    /**
     * Whether moderators can grant or revoke the moderation role of other players in a terrain.
     */
    public static final @NotNull PlayerFlag<Boolean> MANAGE_MODERATORS = PlayerFlag.newBooleanFlag("Manage Moderators", false);
    /**
     * Changes the location where enter and leave messages will be shown. By default, terrains send messages in the
     * "title", but the values "actionbar", "bossbar", "chat", and "none" are supported.
     */
    public static final @NotNull PlayerFlag<String> MESSAGE_LOCATION = new PlayerFlag<>("Message Location", "title", input -> {
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
     * Allows outside projectiles to land inside the terrain.
     */
    public static final @NotNull PlayerFlag<Boolean> OUTSIDE_PROJECTILES = PlayerFlag.newBooleanFlag("Outside Projectiles", false);
    /**
     * Allows pistons inside the terrain moving blocks.
     */
    public static final @NotNull Flag<Boolean> PISTONS = Flag.newBooleanFlag("Pistons", true);
    /**
     * Allows everyone to plant and break crops on farmland.
     */
    //TODO:
    public static final @NotNull PlayerFlag<Boolean> PLANT = PlayerFlag.newBooleanFlag("Plant", false);
    /**
     * Allows trees, crops, grass, etc. to grow naturally or from bone meal.
     */
    public static final @NotNull Flag<Boolean> PLANT_GROW = Flag.newBooleanFlag("Plant Grow", true);
    /**
     * Allows everyone to drink or throw potions in the terrain.
     */
    public static final @NotNull PlayerFlag<Boolean> POTIONS = PlayerFlag.newBooleanFlag("Potions", false);
    /**
     * Allows everyone to use preparing blocks, such as cartography tables, crafting tables, enchanting tables,
     * grindstones, looms, smithing tables, and stonecutters.
     */
    public static final @NotNull PlayerFlag<Boolean> PREPARE = PlayerFlag.newBooleanFlag("Prepare", true);
    /**
     * Allows everyone to press pressure plates.
     */
    public static final @NotNull PlayerFlag<Boolean> PRESSURE_PLATES = PlayerFlag.newBooleanFlag("Pressure Plates", false);
    /**
     * Allows everyone to shoot projectiles.
     */
    public static final @NotNull PlayerFlag<Boolean> PROJECTILES = PlayerFlag.newBooleanFlag("Projectiles", false);
    /**
     * Allows players to engage in combat.
     */
    public static final @NotNull PlayerFlag<Boolean> PVP = PlayerFlag.newBooleanFlag("PvP", false);
    /**
     * Show particles at the terrain's boundaries.
     */
    public static final @NotNull Flag<Boolean> SHOW_BORDERS = Flag.newBooleanFlag("Show Borders", true);
    /**
     * Allows everyone to right-click signs.
     */
    public static final @NotNull PlayerFlag<Boolean> SIGN_CLICK = PlayerFlag.newBooleanFlag("Sign Click", false);
    /**
     * Allows everyone to edit signs.
     */
    public static final @NotNull PlayerFlag<Boolean> SIGN_EDIT = PlayerFlag.newBooleanFlag("Sign Edit", false);
    /**
     * Allows spawners to spawn mobs.
     */
    public static final @NotNull Flag<Boolean> SPAWNERS = Flag.newBooleanFlag("Spawners", true);
    /**
     * Allows sponges to absorb water.
     */
    public static final @NotNull Flag<Boolean> SPONGES = Flag.newBooleanFlag("Sponges", true);
    /**
     * Allows everyone to use hoes on grass blocks.
     */
    //TODO:
    public static final @NotNull PlayerFlag<Boolean> TILL = PlayerFlag.newBooleanFlag("Till", false);
    /**
     * Allows everyone to trample farmland blocks.
     */
    public static final @NotNull PlayerFlag<Boolean> TRAMPLE = PlayerFlag.newBooleanFlag("Trample", false);
    /**
     * Do not freeze player's health and saturation.
     */
    public static final @NotNull PlayerFlag<Boolean> VULNERABILITY = PlayerFlag.newBooleanFlag("Vulnerability", true);

    private static @NotNull Predicate<String> effectChecker = effect -> true;

    /**
     * Gives the effects to the players in the terrain like a beacon. Upon leaving the terrain, the effects are removed.
     */
    @SuppressWarnings("unchecked")
    public static final @NotNull PlayerFlag<Map<String, Integer>> EFFECTS = new PlayerFlag<>("Effects", (Class<Map<String, Integer>>) (Class<?>) Map.class, Collections.emptyMap(), input -> {
        String[] entries = Flag.comma.split(input);
        HashMap<String, Integer> map = new HashMap<>((int) (entries.length / .75f) + 1);

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
    }, Flag.mapFormatter::apply);

    private static final @NotNull HashSet<Flag<?>> values = new HashSet<>(Set.of(ANVILS, ARMOR_STANDS, BLOCK_FORM,
            BLOCK_SPREAD, BUILD, BUILD_BOATS, BUILD_MINECARTS, BUTTONS, CAULDRONS, CAULDRONS_CHANGE_LEVEL_NATURALLY,
            COMMAND_BLACKLIST, CONTAINERS, DISPENSERS, DOORS, EAT, EFFECTS, ENEMY_HARM, ENTER, ENTER_CONSOLE_COMMANDS,
            ENTER_PLAYER_COMMANDS, ENTER_VEHICLES, ENTITY_HARM, ENTITY_INTERACTIONS, EXPLOSION_DAMAGE, FIRE_DAMAGE,
            FIRE_SPREAD, FLY, FROST_WALK, GLIDE, INTERACTIONS, ITEM_DROP, ITEM_FRAMES, ITEM_PICKUP, ITEM_PICKUP_OWN,
            LEAF_DECAY, LEAVE, LEAVE_CONSOLE_COMMANDS, LEAVE_MESSAGE, LEAVE_PLAYER_COMMANDS, LIGHTERS, LIQUID_FLOW,
            MESSAGE_LOCATION, MOB_SPAWN, EDIT_FLAGS, MANAGE_MODERATORS, OUTSIDE_DISPENSERS, OUTSIDE_PISTONS,
            OUTSIDE_PROJECTILES, PISTONS, PLANT, PLANT_GROW, POTIONS, PREPARE, PRESSURE_PLATES, PROJECTILES, PVP,
            SHOW_BORDERS, SIGN_CLICK, SIGN_EDIT, SPAWNERS, SPONGES, TILL, TRAMPLE, VULNERABILITY));
    private static final @NotNull Set<Flag<?>> unmodifiableValues = Collections.unmodifiableSet(values);

    private Flags() {
    }

    /**
     * @return An unmodifiable set of every flag available.
     */
    public static @NotNull Set<Flag<?>> values() {
        return unmodifiableValues;
    }

    /**
     * Registers a new flag into {@link #values()}. A registered flag can have its value set by the player using the
     * Flag Management GUI or a command.
     * <p>
     * This method will add the flag to the {@link Configurations#FLAGS} configuration with the provided values if it
     * isn't already present.
     * <p>
     * Ideally, this method should be called before Terrainer is enabled, so that the {@link Configurations#FLAGS} is
     * loaded with these values without requiring additional reloads.
     * <p>
     * The values can then be edited in the configuration to the user's liking, the paths the values will be available
     * are:
     * <ul>
     *     <li><code>{@link Flag#id()} + ".Display Name"</code></li>
     *     <li><code>{@link Flag#id()} + ".Lore"</code></li>
     *     <li><code>{@link Flag#id()} + ".Material"</code></li>
     *     <li><code>{@link Flag#id()} + ".Default"</code></li>
     *     <li><code>{@link Flag#id()} + ".Define Value"</code></li>
     * </ul>
     * <p>
     * This method is equivalent to calling {@link #registerFlag(Flag, String, String, String, Object)} with the
     * {@link Flag#id()} for display name, empty lore, WHITE_BANNER as material and no define value.
     *
     * @param flag The flag to register.
     * @param <T>  The type of data the flag holds.
     * @return The own flag that was added.
     */
    @Contract("_ -> param1")
    public static synchronized <T> @NotNull Flag<T> registerFlag(@NotNull Flag<T> flag) {
        return registerFlag(flag, flag.id(), "", "WHITE_BANNER", null);
    }

    /**
     * Registers a new flag into {@link #values()}. A registered flag can have its value set by the player using the
     * Flag Management GUI or a command.
     * <p>
     * This method will add the flag to the {@link Configurations#FLAGS} configuration with the provided values if it
     * isn't already present.
     * <p>
     * Ideally, this method should be called before Terrainer is enabled, so that the {@link Configurations#FLAGS} is
     * loaded with these values without requiring additional reloads.
     * <p>
     * The values can then be edited in the configuration to the user's liking, the paths the values will be available
     * are:
     * <ul>
     *     <li><code>{@link Flag#id()} + ".Display Name"</code></li>
     *     <li><code>{@link Flag#id()} + ".Lore"</code></li>
     *     <li><code>{@link Flag#id()} + ".Material"</code></li>
     *     <li><code>{@link Flag#id()} + ".Default"</code></li>
     *     <li><code>{@link Flag#id()} + ".Define Value"</code></li>
     * </ul>
     *
     * @param flag               The flag to register.
     * @param defaultDisplayName The display name this flag will have in messages and the Flag Management GUI.
     * @param defaultLore        The lore the item of flag will have in Flag Management GUI.
     * @param defaultMaterial    The material type the item of this flag will have in Flag Management GUI.
     * @param defineValue        The value to be set on terrains defined by '/tr define' command.
     * @param <T>                The type of data the flag holds.
     * @return The own flag that was added.
     */
    @Contract("_,_,_,_,_ -> param1")
    public static synchronized <T> @NotNull Flag<T> registerFlag(@NotNull Flag<T> flag, @NotNull String defaultDisplayName, @NotNull String defaultLore, @NotNull String defaultMaterial, @Nullable T defineValue) {
        values.add(flag);
        if (Configurations.FLAGS.getConfiguration().contains(flag.id())) return flag;

        try {
            ConfigurationSection defaultSection = Configurations.FLAGS.getDefaultConfiguration().createSection(flag.id());
            defaultSection.set("Default", flag.formatter().apply(flag.defaultValue()));
            if (defineValue != null) defaultSection.set("Define Value", flag.formatter().apply(defineValue));
            defaultSection.set("Material", defaultMaterial);
            defaultSection.set("Display Name", defaultDisplayName);
            defaultSection.set("Lore", defaultLore);

            Path flagsPath = Configurations.FLAGS.getPath();
            if (Files.exists(flagsPath)) {
                // Load flags file so that any editing the user did is not lost.
                Configuration flags = new YamlConfigurationLoader().load(flagsPath);

                ConfigurationSection section = flags.createSection(flag.id());
                section.set("Default", flag.formatter().apply(flag.defaultValue()));
                if (defineValue != null) section.set("Define Value", flag.formatter().apply(defineValue));
                section.set("Material", defaultMaterial);
                section.set("Display Name", defaultDisplayName);
                section.set("Lore", defaultLore);

                PathUtils.deleteAll(flagsPath);
                flags.save(flagsPath);
            }
        } catch (IOException | InvalidConfigurationException e) {
            Terrainer.logger().log("Unable to save flag '" + flag.id() + "' to flags.yml configuration:", ConsoleLogger.Level.ERROR);
            e.printStackTrace();
        }

        return flag;
    }

    /**
     * Looks for a flag that has a {@link Flag#commandFriendlyId()} matching the provided value.
     *
     * @param name The name of the flag to find.
     * @return The flag with matching id, if found.
     */
    public static @Nullable Flag<?> matchFlag(@NotNull String name) {
        name = name.replace('_', '-');

        for (Flag<?> f : values) {
            if (name.equalsIgnoreCase(f.commandFriendlyId())) return f;
        }
        return null;
    }

    /**
     * Sets the validator of valid potion effect names for {@link Flags#EFFECTS} flag.
     *
     * @param effectChecker The new effect checker.
     */
    @ApiStatus.Internal
    public static void setEffectChecker(@NotNull Predicate<String> effectChecker) {
        Flags.effectChecker = effectChecker;
    }

    /**
     * Sets all currently registered flags' default value to the one specified in {@link Configurations#FLAGS}.
     */
    public static void loadFlagDefaults() {
        for (Flag<?> f : values) resetFlagDefault(f);
    }

    private static <T> void resetFlagDefault(@NotNull Flag<T> flag) {
        Configurations.FLAGS.getConfiguration().getString(flag.id() + ".Default").ifPresent(s -> {
            try {
                flag.defaultValue = flag.transformer().apply(s);
            } catch (FlagTransformException e) {
                Terrainer.logger().log("Unable to load custom flag default for flag '" + flag.id() + "' - " + e.getMessage(), ConsoleLogger.Level.WARN);
                Terrainer.logger().log("The default value '" + flag.formatter().apply(flag.defaultValue()) + "' will be used.", ConsoleLogger.Level.WARN);
            }
        });
    }
}
