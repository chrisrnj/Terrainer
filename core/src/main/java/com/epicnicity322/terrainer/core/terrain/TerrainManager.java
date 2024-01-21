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
import com.epicnicity322.terrainer.core.Terrainer;
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
import java.io.Serial;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Stream;

public final class TerrainManager {
    public static final @NotNull Path TERRAINS_FOLDER = Configurations.DATA_FOLDER.resolve("Terrains");
    /**
     * A map with the World's ID as key and a list of terrains in this world as value. This list is sorted based on the minDiagonal's coordinate ID.
     */
    private static final @NotNull Map<UUID, List<Terrain>> terrains = new ConcurrentHashMap<>();
    /**
     * The selected diagonals of players. Key as the player's ID and value as an array with size 2 containing the diagonals.
     */
    private static final @NotNull HashMap<UUID, WorldCoordinate[]> selections = new HashMap<>();
    /**
     * A set with all the terrains to be deleted by the auto-saver.
     * Initial capacity of 4 because there usually isn't a ton of players deleting their terrains at the same time.
     */
    private static final @NotNull Set<UUID> terrainsToRemove = ConcurrentHashMap.newKeySet(4);
    /**
     * The ID to use in the selection map as placeholder for the console player.
     */
    private static final @NotNull UUID consoleUUID = UUID.randomUUID();

    // Usually there's only one listener for these events: the one to be used internally by Terrainer.
    private static final @NotNull ArrayList<Function<ITerrainAddEvent, Boolean>> onAddListeners = new ArrayList<>(2);
    private static final @NotNull ArrayList<Function<ITerrainRemoveEvent, Boolean>> onRemoveListeners = new ArrayList<>(2);
    private static final @NotNull ArrayList<Function<IFlagSetEvent<?>, Boolean>> onFlagSetListeners = new ArrayList<>(2);
    private static final @NotNull ArrayList<Function<IFlagUnsetEvent<?>, Boolean>> onFlagUnsetListeners = new ArrayList<>(2);

    private static volatile boolean autoSaveRunning = false;

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
    private static boolean addWithoutAutoSave(@NotNull Terrain terrain) {
        List<Terrain> worldTerrains = terrains.get(terrain.world);

        if (worldTerrains != null && worldTerrains.contains(terrain)) return false;
        // Calling add event. If it's cancelled, then the terrain should not be added, and false is returned.
        if (callOnAdd(terrain)) return false;

        // Events passed, terrain should be added.

        if (worldTerrains == null) {
            // Creating a new list for this world, terrains are sorted based on priority.
            worldTerrains = Collections.synchronizedList(new SortedList<>((c1, c2) -> -Integer.compare(c1.priority, c2.priority)));
            terrains.put(terrain.world, worldTerrains);
        }

        // Removing instance with the same ID if found.
        remove(terrain.id, false);

        // Adding new instance of terrain and setting Terrain#save to true, so it's saved automatically.
        worldTerrains.add(terrain);
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
        Terrain found = getTerrainByID(terrainID);
        if (found == null) return null;
        // Calling remove event. If it's cancelled, then cancel the removal.
        if (callEvents && callOnRemove(found)) return null;

        found.save = false;
        found.changed = true;

        // Removing from registered terrains.
        UUID world = found.world;
        List<Terrain> worldTerrains = terrains.get(world);
        worldTerrains.remove(found);
        if (worldTerrains.isEmpty()) terrains.remove(world);

        if (callEvents) {
            // Adding this terrain's ID to be removed and loading the auto saver.
            terrainsToRemove.add(terrainID);
            loadAutoSave();
        }

        return found;
    }

    /**
     * Removes the terrain from terrains list and adds it again at the proper index, so the list of terrains is always
     * sorted based on {@link Terrain#priority}.
     *
     * @param terrain The terrain to update in the terrains list.
     */
    static void update(@NotNull Terrain terrain) {
        List<Terrain> worldTerrains = terrains.get(terrain.world);
        if (worldTerrains == null) return;

        // If the list of world terrains contained the terrain, then add it again at sorted index.
        if (worldTerrains.remove(terrain)) {
            worldTerrains.add(terrain);
        }
    }

    /**
     * Concat terrains from all worlds into a single iterable.
     *
     * @return An unmodifiable iterable of all currently loaded terrains.
     * @see #terrains(UUID) It is recommended to get terrains by world, for better performance.
     */
    public static @NotNull Iterable<Terrain> allTerrains() {
        return Iterables.unmodifiableIterable(Iterables.concat(terrains.values()));
    }

    /**
     * Gets the terrains of a specific world. The provided list has the terrains sorted based on {@link Terrain#priority()}.
     *
     * @param world The world of the terrains.
     * @return An unmodifiable list with the terrains located in this world.
     */
    public static @NotNull List<Terrain> terrains(@NotNull UUID world) {
        List<Terrain> worldTerrains = terrains.get(world);
        if (worldTerrains == null) return Collections.emptyList();
        return Collections.unmodifiableList(worldTerrains);
    }

    /**
     * Gets the terrain with matching ID from the list of registered terrains.
     *
     * @param id The ID of the terrain.
     * @return The terrain with matching ID or null if not found.
     */
    @Contract("null -> null")
    public static @Nullable Terrain getTerrainByID(@Nullable UUID id) {
        if (id == null) return null;

        // Looking for matching ID through all worlds.
        for (Terrain terrain : allTerrains()) {
            if (id.equals(terrain.id)) return terrain;
        }

        return null;
    }

    /**
     * Runs through all terrains in this world and adds the ones that have the coordinate within to a new {@link ArrayList}.
     * The provided list has the terrains sorted based on {@link Terrain#priority()}.
     *
     * @param world The world of the coordinates.
     * @param x     The X coordinate.
     * @param y     The Y coordinate.
     * @param z     The Z coordinate.
     * @return A mutable list with the terrains containing the location.
     */
    public static @NotNull List<Terrain> getTerrainsAt(@NotNull UUID world, double x, double y, double z) {
        List<Terrain> worldTerrains = terrains.get(world);
        if (worldTerrains == null) return Collections.emptyList();

        // Terrains are sorted based on priority. They are iterated over and the ones that have the coordinate within are added.
        ArrayList<Terrain> terrainsAt = new ArrayList<>();

        //TODO: binary search
        for (Terrain terrain : worldTerrains) {
            if (terrain.isWithin(x, y, z)) terrainsAt.add(terrain);
        }

        return terrainsAt;
    }

    /**
     * Runs through all terrains in this world and adds the ones that have the coordinate within to a new {@link ArrayList}.
     * The provided list has the terrains sorted based on {@link Terrain#priority()}.
     *
     * @param worldCoordinate The coordinate to get terrains at.
     * @return A mutable list with the terrains containing the location.
     */
    public static @NotNull List<Terrain> getTerrainsAt(@NotNull WorldCoordinate worldCoordinate) {
        return getTerrainsAt(worldCoordinate.world(), worldCoordinate.coordinate().x(), worldCoordinate.coordinate().y(), worldCoordinate.coordinate().z());
    }

    /**
     * Get the terrains owned by a player. The provided list has the terrains sorted based on {@link Terrain#priority()}.
     *
     * @param owner The UUID of the player to check if owns the terrain.
     * @return A mutable list with the terrains that have this player as owner.
     */
    public static @NotNull List<Terrain> getTerrainsOf(@Nullable UUID owner) {
        ArrayList<Terrain> terrainsOf = new ArrayList<>();
        for (Terrain terrain : allTerrains()) {
            if (Objects.equals(terrain.owner, owner)) terrainsOf.add(terrain);
        }
        return terrainsOf;
    }

    /**
     * Gets a mutable array with length 2, with the marked coordinate diagonals this player made.
     * <p>
     * Players can select either through command or the selection wand.
     *
     * @param player The UUID of the player to get selections from, or null to get from CONSOLE.
     * @return The selected coordinates of this player.
     */
    public static @Nullable WorldCoordinate @NotNull [] getSelection(@Nullable UUID player) {
        if (player == null) player = consoleUUID;
        return selections.computeIfAbsent(player, k -> new WorldCoordinate[2]);
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

        for (UUID uuid : terrainsToRemove) {
            try {
                Files.deleteIfExists(TERRAINS_FOLDER.resolve(uuid + ".terrain"));
            } catch (IOException e) {
                Terrainer.logger().log("Unable to remove '" + uuid + "' terrain from " + TERRAINS_FOLDER.getFileName() + " folder:", ConsoleLogger.Level.ERROR);
                e.printStackTrace();
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
                continue;
            }

            try {
                Terrain.toFile(path, terrain);
            } catch (Exception e) {
                Terrainer.logger().log("Error while saving terrain '" + terrain.id + "':", ConsoleLogger.Level.ERROR);
                e.printStackTrace();
            }
        }
    }

    /**
     * In order to not make calls to IO all the time, the terrains are only saved after 10 minutes. This allows players
     * to edit the terrain as much as they want, and save all changes at once only after 10 minutes the first change was made.
     */
    static void loadAutoSave() {
        if (autoSaveRunning) return;
        autoSaveRunning = true;
        new Thread(() -> {
            try {
                Thread.sleep(600000);
            } catch (InterruptedException e) {
                Terrainer.logger().log("Failed to wait 10 minutes to save terrains", ConsoleLogger.Level.WARN);
            }
            save();
            autoSaveRunning = false;
        }, "Terrain Saver Thread").start();
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

    private static final class SortedList<E> extends ArrayList<E> {
        @Serial
        private static final long serialVersionUID = -1378407811134218600L;

        private final Comparator<E> comparator;

        public SortedList(Comparator<E> comparator) {
            this.comparator = comparator;
        }

        @Override
        public boolean add(E e) {
            int index = Collections.binarySearch(this, e, comparator);
            if (index < 0) index = -index - 1;
            add(index, e);
            return true;
        }
    }
}
