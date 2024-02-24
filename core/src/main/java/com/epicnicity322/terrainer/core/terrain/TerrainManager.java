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

import com.epicnicity322.epicpluginlib.core.logger.ConsoleLogger;
import com.epicnicity322.epicpluginlib.core.util.PathUtils;
import com.epicnicity322.terrainer.core.Chunk;
import com.epicnicity322.terrainer.core.Terrainer;
import com.epicnicity322.terrainer.core.WorldChunk;
import com.epicnicity322.terrainer.core.WorldCoordinate;
import com.epicnicity322.terrainer.core.config.Configurations;
import com.epicnicity322.terrainer.core.event.flag.IFlagSetEvent;
import com.epicnicity322.terrainer.core.event.flag.IFlagUnsetEvent;
import com.epicnicity322.terrainer.core.event.terrain.ITerrainAddEvent;
import com.epicnicity322.terrainer.core.event.terrain.ITerrainRemoveEvent;
import com.google.common.collect.Iterables;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Function;
import java.util.stream.Stream;

public final class TerrainManager {
    /**
     * The folder where terrains are saved at. Terrain are saved in files with '.terrain' extension.
     */
    public static final @NotNull Path TERRAINS_FOLDER = Configurations.DATA_FOLDER.resolve("Terrains");
    /**
     * A comparator that sorts terrains by {@link Terrain#priority}, from higher priority to lower.
     */
    public static final @NotNull Comparator<Terrain> PRIORITY_COMPARATOR = Comparator.comparingInt(Terrain::priority).reversed().thenComparing(Terrain::id);

    /**
     * A map with the World's ID as key and a list of terrains in this world as value. This list is sorted based on the terrain's priority.
     */
    private static final @NotNull Map<UUID, TreeSet<Terrain>> registeredTerrains = new ConcurrentHashMap<>();
    /**
     * A map of chunks that have terrains in it.
     */
    private static final @NotNull Map<WorldChunk, TreeSet<Terrain>> chunks = new ConcurrentHashMap<>();
    /**
     * A dummy chunk used in chunks map as the one that holds global/huge terrains.
     */
    private static final @NotNull Chunk globalChunk = new Chunk(Integer.MAX_VALUE, Integer.MAX_VALUE);
    /**
     * A set with all the terrains to be deleted by the auto-saver.
     * Initial capacity of 4 because there usually isn't a ton of players deleting their terrains at the same time.
     */
    private static final @NotNull Set<UUID> terrainsToRemove = ConcurrentHashMap.newKeySet(4);

    // Usually there's only one listener for these events: the one to be used internally by Terrainer.
    private static final @NotNull ArrayList<Function<ITerrainAddEvent, Boolean>> onAddListeners = new ArrayList<>(2);
    private static final @NotNull ArrayList<Function<ITerrainRemoveEvent, Boolean>> onRemoveListeners = new ArrayList<>(2);
    private static final @NotNull ArrayList<Function<IFlagSetEvent<?>, Boolean>> onFlagSetListeners = new ArrayList<>(2);
    private static final @NotNull ArrayList<Function<IFlagUnsetEvent<?>, Boolean>> onFlagUnsetListeners = new ArrayList<>(2);

    private static final @NotNull ScheduledExecutorService autoSaveExecutor = Executors.newSingleThreadScheduledExecutor();
    private static volatile @Nullable ScheduledFuture<?> autoSave = null;

    private TerrainManager() {
    }

    /**
     * Registers a terrain object to {@link #allTerrains()}. Registered terrain objects are saved, loaded, and have their
     * protections enforced. They also are saved automatically once any changes to the instance are made.
     * <p>
     * If a different terrain instance with same {@link Terrain#id()} is present, it is removed and replaced by the
     * specified one.
     * <p>
     * The terrain is not added if it is already present in {@link #allTerrains()}, or if the {@link ITerrainAddEvent} was
     * cancelled.
     *
     * @param terrain The terrain to save.
     * @return Whether the terrain was added or not.
     */
    public static boolean add(@NotNull Terrain terrain) {
        if (addWithoutAutoSave(terrain)) {
            terrain.changed = true;
            loadAutoSave();
            return true;
        }
        return false;
    }

    /**
     * Adds the terrain to {@link #allTerrains()} without calling {@link #loadAutoSave()}.
     *
     * @param terrain The terrain to add.
     * @return Whether the terrain was added and {@link #loadAutoSave()} should be called.
     */
    private static synchronized boolean addWithoutAutoSave(@NotNull Terrain terrain) {
        Set<Terrain> worldTerrains = registeredTerrains.get(terrain.world);

        if (worldTerrains != null && worldTerrains.contains(terrain)) return false;
        // Calling add event. If it's cancelled, then the terrain should not be added, and false is returned.
        if (callOnAdd(terrain)) return false;

        // Events passed, terrain should be added.

        // Removing instance with the same ID if found.
        remove(terrain.id, false);

        // Adding new instance of terrain.
        registeredTerrains.computeIfAbsent(terrain.world, k -> new TreeSet<>(PRIORITY_COMPARATOR)).add(terrain);

        // Adding the instance to chunks map, so it can be found with #terrainsAt map.
        if (terrain.chunks.isEmpty()) { // Chunks are empty when the terrain is global/huge.
            chunks.computeIfAbsent(new WorldChunk(terrain.world, globalChunk), k -> new TreeSet<>(PRIORITY_COMPARATOR)).add(terrain);
        } else {
            terrain.chunks.forEach(chunk -> chunks.computeIfAbsent(new WorldChunk(terrain.world, chunk), k -> new TreeSet<>(PRIORITY_COMPARATOR)).add(terrain));
        }

        // Setting Terrain #save to true, so it's saved automatically.
        terrain.save = true;
        return true;
    }

    /**
     * Unregisters a terrain object. This makes so the terrain object is no longer saved automatically once changes are
     * made, also the saved file associated to this terrain's ID is deleted.
     * <p>
     * A terrain with the specified terrain's ID is removed, regardless if the provided terrain instance is not exactly
     * equal to the one in the list of registered terrains.
     * <p>
     * The terrain is not removed if the {@link ITerrainRemoveEvent} was cancelled.
     *
     * @param terrain The terrain to delete.
     * @return The terrain that was removed, null if the terrain could not be removed.
     */
    public static @Nullable Terrain remove(@NotNull Terrain terrain) {
        Terrain removed = remove(terrain.id);

        if (removed != null) {
            // The terrain that has this ID might be a different instance, making sure the terrain is set to not save.
            terrain.save = false;
            terrain.changed = true;
        }

        return removed;
    }

    /**
     * Unregisters a terrain with matching ID. If found, this makes so the terrain object is no longer saved
     * automatically once changes are made, also the saved file associated with this terrain's ID is deleted.
     * <p>
     * The terrain is not removed if the {@link ITerrainRemoveEvent} was cancelled.
     *
     * @param terrainID The ID of the terrain to delete.
     * @return The terrain that was removed, null if no terrain was found or removed.
     */
    public static @Nullable Terrain remove(@NotNull UUID terrainID) {
        return remove(terrainID, true);
    }

    private static @Nullable Terrain remove(@NotNull UUID terrainID, boolean callEvents) {
        Terrain found = terrainByID(terrainID);
        if (found == null) return null;
        // Calling remove event. If it's cancelled, then cancel the removal.
        if (callEvents && callOnRemove(found)) return null;

        found.save = false;
        found.changed = true;

        // Removing from registered terrains.
        UUID world = found.world;
        Set<Terrain> worldTerrains = registeredTerrains.get(world);
        worldTerrains.remove(found);
        if (worldTerrains.isEmpty()) registeredTerrains.remove(world);

        // Removing from chunk map.
        found.chunks.forEach(chunk -> {
            var worldChunk = new WorldChunk(found.world, chunk);
            Set<Terrain> chunkTerrains = chunks.get(worldChunk);
            chunkTerrains.remove(found);
            if (chunkTerrains.isEmpty()) chunks.remove(worldChunk);
        });

        // Removing from global terrains list.
        var globalWorldChunk = new WorldChunk(found.world, globalChunk);
        Set<Terrain> globalTerrains = chunks.get(globalWorldChunk);
        if (globalTerrains != null) {
            globalTerrains.remove(found);
            if (globalTerrains.isEmpty()) chunks.remove(globalWorldChunk);
        }

        if (callEvents) {
            // Adding this terrain's ID to be removed and loading the auto saver.
            terrainsToRemove.add(terrainID);
            loadAutoSave();
        }

        return found;
    }

    /**
     * Removes the terrain from registered terrains and chunks list and adds it again at the proper index, so the list
     * of terrains is always sorted based on {@link Terrain#priority}.
     *
     * @param terrain The terrain to update in the terrains list.
     */
    static void priorityUpdate(@NotNull Terrain terrain) {
        Set<Terrain> worldTerrains = registeredTerrains.get(terrain.world);
        if (worldTerrains == null) return;
        // If the list of world terrains contained the terrain, then add it again at sorted index.
        if (worldTerrains.remove(terrain)) worldTerrains.add(terrain);

        // Update in each chunk.
        if (terrain.chunks.isEmpty()) {
            Set<Terrain> globalTerrains = chunks.get(new WorldChunk(terrain.world, globalChunk));
            if (globalTerrains.remove(terrain)) globalTerrains.add(terrain);
        } else {
            terrain.chunks.forEach(chunk -> {
                Set<Terrain> chunkTerrains = chunks.get(new WorldChunk(terrain.world, chunk));
                if (chunkTerrains.remove(terrain)) chunkTerrains.add(terrain);
            });
        }
    }

    /**
     * Removes the terrain from the chunks it was registered in, then adds the terrain again in the new chunks it's
     * currently in.
     *
     * @param terrain        The terrain to update in the chunks map.
     * @param previousChunks The previous chunks this terrain was in.
     */
    static void chunkUpdate(@NotNull Terrain terrain, @NotNull Set<Chunk> previousChunks) {
        // Removing from previous chunks.
        if (previousChunks.isEmpty()) {
            var globalWorldChunk = new WorldChunk(terrain.world, globalChunk);
            Set<Terrain> globalTerrains = chunks.get(globalWorldChunk);
            globalTerrains.remove(terrain);
            if (globalTerrains.isEmpty()) chunks.remove(globalWorldChunk);
        } else {
            previousChunks.forEach(chunk -> {
                var worldChunk = new WorldChunk(terrain.world, chunk);
                Set<Terrain> chunkTerrains = chunks.get(worldChunk);
                chunkTerrains.remove(terrain);
                if (chunkTerrains.isEmpty()) chunks.remove(worldChunk);
            });
        }

        // Adding it again.
        if (terrain.chunks.isEmpty()) { // Chunks are empty when the terrain is global/huge.
            chunks.computeIfAbsent(new WorldChunk(terrain.world, globalChunk), k -> new TreeSet<>(PRIORITY_COMPARATOR)).add(terrain);
        } else {
            terrain.chunks.forEach(chunk -> chunks.computeIfAbsent(new WorldChunk(terrain.world, chunk), k -> new TreeSet<>(PRIORITY_COMPARATOR)).add(terrain));
        }
    }

    /**
     * Concat terrains from all worlds into a single iterable.
     *
     * @return An unmodifiable iterable of all currently loaded terrains.
     * @see #terrains(UUID) It is recommended to get terrains by world, for better performance.
     */
    public static @NotNull Iterable<Terrain> allTerrains() {
        return Iterables.unmodifiableIterable(Iterables.concat(registeredTerrains.values()));
    }

    /**
     * @return The amount of terrains that are currently registered.
     */
    public static long terrainsAmount() {
        return registeredTerrains.values().stream().mapToLong(TreeSet::size).sum();
    }

    /**
     * Gets the terrains of a specific world. The provided list has the terrains sorted based on {@link Terrain#priority}.
     *
     * @param world The world of the terrains.
     * @return An unmodifiable list with the terrains located in this world.
     */
    public static @NotNull Set<Terrain> terrains(@NotNull UUID world) {
        Set<Terrain> worldTerrains = registeredTerrains.get(world);
        if (worldTerrains == null) return Collections.emptySet();
        return Collections.unmodifiableSet(worldTerrains);
    }

    /**
     * Gets the terrain with matching ID from the list of registered terrains. The provided list has the terrains sorted based on {@link Terrain#priority}.
     *
     * @param id The ID of the terrain.
     * @return The terrain with matching ID or null if not found.
     */
    @Contract("null -> null")
    public static @Nullable Terrain terrainByID(@Nullable UUID id) {
        if (id == null) return null;

        // Looking for matching ID through all worlds.
        for (Set<Terrain> terrainList : registeredTerrains.values()) {
            for (Terrain terrain : terrainList) if (id.equals(terrain.id)) return terrain;
        }

        return null;
    }

    /**
     * Searches for terrains that have the provided coordinate within.
     * <p>
     * The provided list has the terrains sorted based on {@link #PRIORITY_COMPARATOR}.
     *
     * @param worldCoordinate The coordinate to get the terrains at.
     * @return A {@link Collections#emptyList()} if there are no terrains, or a mutable list with the terrains containing the location.
     */
    public static @NotNull Set<Terrain> terrainsAt(@NotNull WorldCoordinate worldCoordinate) {
        return terrainsAt(worldCoordinate.world(), (int) worldCoordinate.coordinate().x(), (int) worldCoordinate.coordinate().y(), (int) worldCoordinate.coordinate().z());
    }

    /**
     * Searches for terrains that have the provided coordinate within.
     * <p>
     * The provided list has the terrains sorted based on {@link #PRIORITY_COMPARATOR}.
     *
     * @param world The UUID of the world where the location resides.
     * @param x     The X coordinate of the block.
     * @param y     The Y coordinate of the block.
     * @param z     The Z coordinate of the block.
     * @return A {@link Collections#emptyList()} if there are no terrains, or a mutable list with the terrains containing the location.
     */
    public static @NotNull Set<Terrain> terrainsAt(@NotNull UUID world, int x, int y, int z) {
        Set<Terrain> chunkTerrains = chunks.get(new WorldChunk(world, Chunk.fromBlockCoordinates(x, z)));
        Set<Terrain> globalTerrains = chunks.get(new WorldChunk(world, globalChunk));

        Set<Terrain> terrainsAt = null; // The result of the search.

        if (chunkTerrains != null) {
            for (Terrain terrain : chunkTerrains)
                if (terrain.isWithin(x, y, z)) {
                    if (terrainsAt == null) terrainsAt = new TreeSet<>(PRIORITY_COMPARATOR);
                    terrainsAt.add(terrain);
                }
        }
        if (globalTerrains != null) {
            for (Terrain terrain : globalTerrains)
                if (terrain.isWithin(x, y, z)) {
                    if (terrainsAt == null) terrainsAt = new TreeSet<>(PRIORITY_COMPARATOR);
                    terrainsAt.add(terrain);
                }
        }

        return terrainsAt == null ? Collections.emptySet() : terrainsAt;
    }

    /**
     * Gets all terrains within the specified chunk. Some terrains might be so huge they are considered global, they are
     * included in the iterable as well.
     *
     * @param world  The UUID of the world where the chunk resides.
     * @param chunkX The X coordinate of the chunk.
     * @param chunkZ The Z coordinate of the chunk.
     * @return An unmodifiable iterable with all terrains that are in the chunk.
     */
    public static @NotNull Iterable<Terrain> terrainsAtChunk(@NotNull UUID world, int chunkX, int chunkZ) {
        return terrainsAtChunk(new WorldChunk(world, new Chunk(chunkX, chunkZ)));
    }

    /**
     * Gets all terrains within the specified chunk. Some terrains might be so huge they are considered global, they are
     * included in the iterable as well.
     *
     * @param worldChunk The chunk to get terrains at.
     * @return An unmodifiable iterable with all terrains that are in the chunk.
     */
    public static @NotNull Iterable<Terrain> terrainsAtChunk(@NotNull WorldChunk worldChunk) {
        Set<Terrain> chunkTerrains = chunks.get(worldChunk);
        if (chunkTerrains == null) chunkTerrains = Collections.emptySet();
        Set<Terrain> globalTerrains = chunks.get(new WorldChunk(worldChunk.world(), globalChunk));
        if (globalTerrains == null) globalTerrains = Collections.emptySet();
        return Iterables.unmodifiableIterable(Iterables.concat(chunkTerrains, globalTerrains));
    }

    /**
     * Get the terrains owned by a player.
     *
     * @param owner The UUID of the player to check if owns the terrain.
     * @return A mutable list with the terrains that have this player as owner.
     */
    public static @NotNull List<Terrain> terrainsOf(@Nullable UUID owner) {
        ArrayList<Terrain> terrainsOf = new ArrayList<>();
        for (Set<Terrain> terrainList : registeredTerrains.values()) {
            for (Terrain terrain : terrainList) if (Objects.equals(terrain.owner, owner)) terrainsOf.add(terrain);
        }
        return terrainsOf;
    }

    /**
     * Gets the highest priority terrain at the specified location that has the specified flag set with a value that's
     * not null.
     * <p>
     * There can be two terrains with same priority at the location with diverging flag data, this method returns the
     * first one found.
     *
     * @param flag            The flag to look for in the location.
     * @param worldCoordinate The coordinate to get the terrain at.
     * @return The terrain in the location that has this flag, null if not found.
     */
    public static @Nullable Terrain highestPriorityTerrainWithFlagAt(@NotNull Flag<?> flag, @NotNull WorldCoordinate worldCoordinate) {
        return highestPriorityTerrainWithFlagAt(flag, worldCoordinate.world(), (int) worldCoordinate.coordinate().x(), (int) worldCoordinate.coordinate().y(), (int) worldCoordinate.coordinate().z());
    }

    /**
     * Gets the highest priority terrain at the specified block location that has the specified flag set with a value that's
     * not null.
     * <p>
     * There can be two terrains with same priority at the location with diverging flag data, this method returns the
     * first one found.
     *
     * @param flag  The flag to look for in the location.
     * @param world The UUID of the world where the location resides.
     * @param x     The X coordinate of the block.
     * @param y     The Y coordinate of the block.
     * @param z     The Z coordinate of the block.
     * @return The terrain in the location that has this flag, null if not found.
     */
    public static @Nullable Terrain highestPriorityTerrainWithFlagAt(@NotNull Flag<?> flag, @NotNull UUID world, int x, int y, int z) {
        // Terrain list is sorted by priority.
        for (Terrain terrain : terrainsAt(world, x, y, z)) if (terrain.flags().getData(flag) != null) return terrain;
        return null;
    }

    /**
     * Determines whether a specific flag is allowed at a given location based on various conditions.
     * <p>
     * Conditions for flag allowance:
     * <ul>
     *     <li>Returns true if the location has no terrains with the specified flag.</li>
     *     <li>Returns true if the flag is explicitly allowed for the terrain with the highest priority at the location.</li>
     *     <li>Returns false if the flag is explicitly denied and the player lacks any relations to terrains with the same or higher priority as the terrain where the flag was denied.</li>
     * </ul>
     * <p>
     * A permission check for {@link Flag#bypassPermission()} should be made before calling this method to ensure the
     * player should have the flag tested.
     *
     * @param flag            The flag to be tested at the location.
     * @param player          The UUID of the player.
     * @param worldCoordinate The location where to test the flag.
     * @return {@code true} if the flag is allowed at the specified location; {@code false} otherwise.
     * @throws UnsupportedOperationException If terrainer is not loaded yet or {@link Terrainer#playerUtil()} is not set.
     */
    public static boolean isFlagAllowedAt(@NotNull Flag<Boolean> flag, @NotNull UUID player, @NotNull WorldCoordinate worldCoordinate) {
        return isFlagAllowedAt(flag, player, worldCoordinate.world(), (int) worldCoordinate.coordinate().x(), (int) worldCoordinate.coordinate().y(), (int) worldCoordinate.coordinate().z());
    }

    /**
     * Determines whether a specific flag is allowed at a given block location based on various conditions.
     * <p>
     * Conditions for flag allowance:
     * <ul>
     *     <li>Returns true if the location has no terrains with the specified flag.</li>
     *     <li>Returns true if the flag is explicitly allowed for the terrain with the highest priority at the location.</li>
     *     <li>Returns false if the flag is explicitly denied and the player lacks any relations to terrains with the same or higher priority as the terrain where the flag was denied.</li>
     * </ul>
     * <p>
     * A permission check for {@link Flag#bypassPermission()} should be made before calling this method to ensure the
     * player should have the flag tested.
     *
     * @param flag   The flag to be tested at the location.
     * @param player The UUID of the player.
     * @param world  The UUID of the world where the location resides.
     * @param x      The X coordinate of the block.
     * @param y      The Y coordinate of the block.
     * @param z      The Z coordinate of the block.
     * @return {@code true} if the flag is allowed at the specified location; {@code false} otherwise.
     * @throws UnsupportedOperationException If terrainer is not loaded yet or {@link Terrainer#playerUtil()} is not set.
     */
    public static boolean isFlagAllowedAt(@NotNull Flag<Boolean> flag, @NotNull UUID player, @NotNull UUID world, int x, int y, int z) {
        if (Terrainer.playerUtil() == null) {
            throw new UnsupportedOperationException("Terrainer is not loaded yet, player util could not be found.");
        }
        Integer foundPriority = null;

        // Terrain list is sorted by priority.
        for (Terrain terrain : terrainsAt(world, x, y, z)) {
            // If the flag was already found, check if the player has relations to terrains in the location which have the same priority.
            if (foundPriority != null && terrain.priority != foundPriority) return false;
            // If the player has any relations to terrains found at the location, return true.
            if (Terrainer.playerUtil().hasAnyRelations(player, terrain)) return true;
            if (foundPriority != null) continue;

            Boolean state = terrain.flags().getData(flag);

            if (state != null) {
                // State found as false. Continue loop to check for relations with terrains with same priority.
                if (!state) {
                    foundPriority = terrain.priority;
                    continue;
                }
                return true;
            }
        }
        return foundPriority == null;
    }

    /**
     * Adds a new or converts a saved global terrain for this world. This should be called only once per world, when the world is loaded.
     * <p>
     * If a terrain with this world's ID already exists, it's removed from the terrain list and added again as new
     * {@link WorldTerrain} instance, with the same data from flags, moderators, members, priority, etc.
     *
     * @param world The ID of the world to create the {@link WorldTerrain}
     * @param name  The name of the world to set as the terrain's name.
     */
    public static void loadWorld(@NotNull UUID world, @NotNull String name) {
        Terrain savedWorld = remove(world, false);

        // Converting saved world to WorldTerrain. If none was found, this means this is a new world, so creating new.
        if (savedWorld == null) {
            savedWorld = new WorldTerrain(world, name);
            if (!add(savedWorld)) return;
        } else {
            savedWorld = new WorldTerrain(savedWorld, name);
            if (!addWithoutAutoSave(savedWorld)) return;
        }

        alertDangerousFlagAllowed(savedWorld, Flags.EXPLOSION_DAMAGE);
        alertDangerousFlagAllowed(savedWorld, Flags.FIRE_DAMAGE);
        alertDangerousFlagAllowed(savedWorld, Flags.FIRE_SPREAD);
    }

    private static void alertDangerousFlagAllowed(@NotNull Terrain terrain, @NotNull Flag<Boolean> flag) {
        Boolean state = terrain.flags().getData(flag);
        if (state == null || state) {
            Terrainer.logger().log("World " + terrain.name() + " has flag &c" + flag.id() + "&r ALLOWED");
        }
    }

    /**
     * Reads all terrains from disk and registers them into the terrains list. No existing terrains are actually updated,
     * this only deserializes {@link Terrain} objects in {@link #TERRAINS_FOLDER} and adds them to the terrains list using
     * {@link TerrainManager#addWithoutAutoSave(Terrain)}.
     *
     * @see #save()
     */
    public static void load() throws IOException {
        if (Files.notExists(TERRAINS_FOLDER)) {
            Files.createDirectories(TERRAINS_FOLDER);
            return;
        }
        try (Stream<Path> terrainFiles = Files.list(TERRAINS_FOLDER)) {
            terrainFiles.filter(file -> file.toString().endsWith(".terrain")).forEach(terrainFile -> {
                try {
                    addWithoutAutoSave(Terrain.fromFile(terrainFile));
                } catch (Exception e) {
                    Terrainer.logger().log("Unable to read file '" + terrainFile.getFileName() + "' as a Terrain object:", ConsoleLogger.Level.ERROR);
                    e.printStackTrace();

                    // Terrain file is likely corrupted, adding .bak to the end of the file name to avoid trying to load it again next time.
                    Path newName = PathUtils.getUniquePath(terrainFile.getParent().resolve(terrainFile.getFileName().toString() + ".bak"));
                    Terrainer.logger().log("The file will be renamed to '" + newName.getFileName() + "'.", ConsoleLogger.Level.ERROR);
                    try {
                        Files.move(terrainFile, newName);
                    } catch (Exception e2) {
                        Terrainer.logger().log("Something went wrong while renaming the file.", ConsoleLogger.Level.ERROR);
                    }
                }
            });
        }
    }

    /**
     * Saves any terrains that had changes into disk.
     * <p>
     * Terrains are serialized into .terrain files in {@link #TERRAINS_FOLDER}.
     */
    public static void save() {
        Terrainer.logger().log("Saving terrain changes.");

        // Make sure autoSave is shutdown, so when this method is called on disable, the autoSave stops running.
        ScheduledFuture<?> autoSave1 = autoSave;
        if (autoSave1 != null) {
            autoSave1.cancel(true);
            autoSave = null;
        }

        for (UUID uuid : terrainsToRemove) {
            try {
                Files.deleteIfExists(TERRAINS_FOLDER.resolve(uuid + ".terrain"));
            } catch (IOException e) {
                Terrainer.logger().log("Unable to remove '" + uuid + "' terrain from " + TERRAINS_FOLDER.getFileName() + " folder:", ConsoleLogger.Level.ERROR);
                e.printStackTrace();
                Terrainer.logger().log("The terrain will still exist when the server restarts!", ConsoleLogger.Level.ERROR);
            }
        }
        terrainsToRemove.clear();

        // Saving changed terrains.
        for (Terrain terrain : allTerrains()) {
            if (!terrain.changed) continue;

            terrain.changed = false;

            Path path = TERRAINS_FOLDER.resolve(terrain.id + ".terrain");

            try {
                Files.deleteIfExists(path);
            } catch (Exception e) {
                Terrainer.logger().log("Error while deleting old terrain file '" + path.getFileName() + "':", ConsoleLogger.Level.ERROR);
                e.printStackTrace();
                Terrainer.logger().log("Changes were not saved for terrain '" + terrain.id + "' (" + terrain.name + ") and it will be reset when the server restarts.", ConsoleLogger.Level.ERROR);
                continue;
            }

            try {
                Terrain.toFile(path, terrain);
            } catch (Exception e) {
                Terrainer.logger().log("Error while saving terrain '" + terrain.id + "':", ConsoleLogger.Level.ERROR);
                e.printStackTrace();
                Terrainer.logger().log("The terrain '" + terrain.id + "' (" + terrain.name + ") was deleted, but could not be saved again. The terrain will not exist when the server restarts.", ConsoleLogger.Level.ERROR);
            }
        }
    }

    /**
     * In order to not make calls to IO all the time, the terrains are only saved after 10 minutes. This allows players
     * to edit the terrain as much as they want, and save all changes at once only after 10 minutes the first change was made.
     */
    static void loadAutoSave() {
        if (autoSave != null) return;
        autoSave = autoSaveExecutor.schedule(TerrainManager::save, 10, TimeUnit.MINUTES);
    }

    /**
     * Adds a function that will be applied once a terrain is added. The result of the function will be used to determine
     * if the addition of the terrain should be cancelled or not.
     *
     * @param onAdd The listener for TerrainAddEvent.
     * @apiNote You should use the platform's implementation of {@link ITerrainAddEvent} instead of this. This is only used internally to call the platform's event.
     */
    @ApiStatus.Internal
    public static void setOnTerrainAddListener(@NotNull Function<ITerrainAddEvent, Boolean> onAdd) {
        onAddListeners.add(onAdd);
    }

    /**
     * Adds a function that will be applied once the terrain is removed.
     * The result of the function will be used to determine if the removal of the terrain should be cancelled or not.
     *
     * @param onRemove The listener for TerrainRemoveEvent.
     * @apiNote You should use the platform's implementation of {@link ITerrainRemoveEvent} instead of this. This is only used internally to call the platform's event.
     */
    @ApiStatus.Internal
    public static void setOnTerrainRemoveListener(@NotNull Function<ITerrainRemoveEvent, Boolean> onRemove) {
        onRemoveListeners.add(onRemove);
    }

    /**
     * Adds a function that will be applied once a flag is added to a terrain by any means. The result of the function
     * will be used to determine if the addition of the flag should be cancelled or not.
     *
     * @param onFlagSet The listener for FlagSetEvent.
     * @apiNote You should use the platform's implementation of {@link IFlagSetEvent} instead of this. This is only used internally to call the platform's event.
     */
    @ApiStatus.Internal
    public static void setOnFlagSetListener(@NotNull Function<IFlagSetEvent<?>, Boolean> onFlagSet) {
        onFlagSetListeners.add(onFlagSet);
    }

    /**
     * Adds a function that will be applied once a flag is removed from a terrain by any means. The result of the function
     * will be used to determine if the removal of the flag should be cancelled or not.
     *
     * @param onFlagUnset The listener for FlagUnsetEvent.
     * @apiNote You should use the platform's implementation of {@link IFlagUnsetEvent} instead of this. This is only used internally to call the platform's event.
     */
    @ApiStatus.Internal
    public static void setOnFlagUnsetListener(@NotNull Function<IFlagUnsetEvent<?>, Boolean> onFlagUnset) {
        onFlagUnsetListeners.add(onFlagUnset);
    }

    private static boolean callOnAdd(@NotNull Terrain terrain) {
        var event = new ITerrainAddEvent() {
            @Override
            public @NotNull Terrain terrain() {
                return terrain;
            }
        };
        boolean cancel = false;
        for (Function<ITerrainAddEvent, Boolean> listener : onAddListeners) {
            try {
                if (listener.apply(event)) cancel = true;
            } catch (Throwable t) {
                Terrainer.logger().log("Unknown issue happened while calling TerrainAddEvent for terrain " + terrain.id + ":", ConsoleLogger.Level.WARN);
                t.printStackTrace();
            }
        }
        return cancel;
    }

    private static boolean callOnRemove(@NotNull Terrain terrain) {
        var event = new ITerrainRemoveEvent() {
            @Override
            public @NotNull Terrain terrain() {
                return terrain;
            }
        };
        boolean cancel = false;
        for (Function<ITerrainRemoveEvent, Boolean> listener : onRemoveListeners) {
            try {
                if (listener.apply(event)) cancel = true;
            } catch (Throwable t) {
                Terrainer.logger().log("Unknown issue happened while calling TerrainRemoveEvent for terrain " + terrain.id + ":", ConsoleLogger.Level.WARN);
                t.printStackTrace();
            }
        }
        return cancel;
    }

    static <T> FlagSetResult<T> callOnFlagSet(@NotNull Terrain terrain, @NotNull Flag<T> flag, @NotNull T data) {
        var event = new IFlagSetEvent<T>() {
            private @NotNull T newData = data;

            @Override
            public @NotNull Flag<T> flag() {
                return flag;
            }

            @NotNull
            @Override
            public T data() {
                return newData;
            }

            @Override
            public void setData(@NotNull T newData) {
                this.newData = newData;
            }

            @Override
            public @NotNull Terrain terrain() {
                return terrain;
            }
        };
        boolean cancel = false;
        for (Function<IFlagSetEvent<?>, Boolean> listener : onFlagSetListeners) {
            try {
                if (listener.apply(event)) cancel = true;
            } catch (Throwable t) {
                Terrainer.logger().log("Unknown issue happened while calling FlagSetEvent for terrain " + terrain.id + ":", ConsoleLogger.Level.WARN);
                t.printStackTrace();
            }
        }
        return new FlagSetResult<>(cancel, event.newData);
    }

    static <T> boolean callOnFlagUnset(@NotNull Terrain terrain, @NotNull Flag<T> flag) {
        var event = new IFlagUnsetEvent<T>() {
            @Override
            public @NotNull Flag<T> flag() {
                return flag;
            }

            @Override
            public @NotNull Terrain terrain() {
                return terrain;
            }
        };
        boolean cancel = false;
        for (Function<IFlagUnsetEvent<?>, Boolean> listener : onFlagUnsetListeners) {
            try {
                if (listener.apply(event)) cancel = true;
            } catch (Throwable t) {
                Terrainer.logger().log("Unknown issue happened while calling FlagUnsetEvent for terrain " + terrain.id + ":", ConsoleLogger.Level.WARN);
                t.printStackTrace();
            }
        }
        return cancel;
    }

    record FlagSetResult<T>(boolean cancel, T newData) {
    }
}
