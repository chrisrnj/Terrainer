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

import com.epicnicity322.epicpluginlib.core.logger.ConsoleLogger;
import com.epicnicity322.epicpluginlib.core.util.PathUtils;
import com.epicnicity322.terrainer.core.Terrainer;
import com.epicnicity322.terrainer.core.WorldCoordinate;
import com.epicnicity322.terrainer.core.config.Configurations;
import com.epicnicity322.terrainer.core.event.flag.IFlagSetEvent;
import com.epicnicity322.terrainer.core.event.flag.IFlagUnsetEvent;
import com.epicnicity322.terrainer.core.event.terrain.ITerrainAddEvent;
import com.epicnicity322.terrainer.core.event.terrain.ITerrainRemoveEvent;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Stream;

public final class TerrainManager {
    public static final @NotNull Path TERRAINS_FOLDER = Configurations.DATA_FOLDER.resolve("Terrains");
    private static final @NotNull HashSet<Terrain> terrains = new HashSet<>();
    private static final @NotNull Set<Terrain> unmodifiableTerrains = Collections.unmodifiableSet(terrains);
    private static final @NotNull HashMap<UUID, WorldCoordinate[]> selections = new HashMap<>();
    // Initial capacity of 4 because there usually isn't a ton of players deleting their terrains at the same
    // time.
    private static final @NotNull HashSet<UUID> terrainsToRemove = new HashSet<>(4);
    private static final @NotNull UUID consoleUUID = UUID.randomUUID();
    private static final @NotNull ArrayList<Function<ITerrainAddEvent, Boolean>> onAddListeners = new ArrayList<>(1);
    private static final @NotNull ArrayList<Function<ITerrainRemoveEvent, Boolean>> onRemoveListeners = new ArrayList<>(1);
    private static final @NotNull ArrayList<Function<IFlagSetEvent<?>, Boolean>> onFlagSetListeners = new ArrayList<>(1);
    private static final @NotNull ArrayList<Function<IFlagUnsetEvent<?>, Boolean>> onFlagUnsetListeners = new ArrayList<>(1);
    private static volatile boolean autoSaveRunning = false;

    private TerrainManager() {
    }

    /**
     * Registers a terrain object to {@link #terrains()}. Registered terrain objects are saved, loaded, and have their
     * protections enforced. They also are saved automatically once any changes to the instance are made.
     * <p>
     * If a different terrain instance with same {@link Terrain#id()} is present, it is removed and replaced by the
     * specified one.
     * <p>
     * The terrain is not added if it is already present in {@link #terrains()}, or if the {@link ITerrainAddEvent} was
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
     * Adds the terrain to {@link #terrains()} without calling {@link #loadAutoSave()}.
     *
     * @param terrain The terrain to add.
     * @return Whether the terrain was added and {@link #loadAutoSave()} should be called.
     */
    private static boolean addWithoutAutoSave(@NotNull Terrain terrain) {
        if (terrains.contains(terrain)) return false;
        // Calling add event. If it's cancelled, then the terrain should not be added, and false is returned.
        if (callOnAdd(terrain)) return false;

        // Removing instance with the same ID if found.
        Terrain found = getTerrainByID(terrain.id);
        if (found != null) {
            found.save = false;
            found.changed = true;
            terrains.remove(found);
        }

        // Adding new instance of terrain and setting Terrain#save to true, so it's saved automatically.
        terrains.add(terrain);
        terrain.save = true;
        return true;
    }

    /**
     * Unregisters a terrain object. This makes so the terrain object is no longer saved automatically once changes are
     * made, also the saved file associated to this terrain's ID is deleted.
     * <p>
     * A terrain with the same specified terrain's ID is removed, regardless if the provided terrain object is not
     * exactly equal to the one in {@link #terrains()} set.
     * <p>
     * The terrain is not removed if the {@link ITerrainRemoveEvent} was cancelled.
     *
     * @param terrain The terrain to delete.
     * @return Whether the terrain was removed or not.
     */
    public static boolean remove(@NotNull Terrain terrain) {
        Terrain found = getTerrainByID(terrain.id);
        if (found == null) return false;
        // Calling remove event. If it's cancelled, then cancel the removal.
        if (callOnRemove(terrain)) return false;

        terrain.save = false;
        terrain.changed = true;
        // Might be different object.
        found.save = false;
        found.changed = true;

        terrains.removeIf(t -> terrain.id.equals(t.id));
        terrainsToRemove.add(terrain.id);
        loadAutoSave();
        return true;
    }

    /**
     * Unregisters a terrain with matching ID. If found, this makes so the terrain object is no longer saved
     * automatically once changes are made, also the saved file associated with this terrain's ID is deleted.
     * <p>
     * The terrain is not removed if the {@link ITerrainRemoveEvent} was cancelled.
     *
     * @param terrainID The ID of the terrain to delete.
     * @return Whether the terrain was removed or not.
     */
    public static boolean remove(@NotNull UUID terrainID) {
        Terrain found = getTerrainByID(terrainID);
        if (found == null) return false;
        // Calling remove event. If it's cancelled, then cancel the removal.
        if (callOnRemove(found)) return false;

        found.save = false;
        found.changed = true;
        terrains.removeIf(t -> terrainID.equals(t.id));
        terrainsToRemove.add(terrainID);
        loadAutoSave();
        return true;
    }

    /**
     * @return An unmodifiable set of currently loaded terrains.
     */
    public static @NotNull Set<Terrain> terrains() {
        return unmodifiableTerrains;
    }

    /**
     * Gets the terrain with matching ID from {@link #terrains()}.
     *
     * @param id The ID of the terrain.
     * @return The terrain with matching ID or null if not found.
     */
    @Contract("null -> null")
    public static @Nullable Terrain getTerrainByID(@Nullable UUID id) {
        if (id == null) return null;
        for (Terrain terrain : terrains) {
            if (terrain.id.equals(id)) return terrain;
        }
        return null;
    }

    /**
     * Gets the terrains at a specific world.
     *
     * @param world The world of the terrains.
     * @return The terrains located in this world.
     */
    public static @NotNull HashSet<Terrain> getTerrainsAt(@NotNull UUID world) {
        var terrainsAt = new HashSet<Terrain>();
        for (Terrain terrain : terrains) {
            if (terrain.world.equals(world)) terrainsAt.add(terrain);
        }
        return terrainsAt;
    }

    /**
     * Gets the terrains at a specific location.
     *
     * @param world The world of the coordinates.
     * @param x     The X coordinate
     * @param y     The Y coordinate
     * @param z     The Z coordinate
     * @return The terrains containing the location.
     */
    public static @NotNull HashSet<Terrain> getTerrainsAt(@NotNull UUID world, double x, double y, double z) {
        var terrainsAt = new HashSet<Terrain>(4);
        for (Terrain terrain : terrains) {
            if (terrain.world.equals(world) && terrain.isWithin(x, y, z)) terrainsAt.add(terrain);
        }
        return terrainsAt;
    }

    /**
     * Gets the terrains at a specific location.
     *
     * @param worldCoordinate The coordinate to get terrains at.
     * @return The terrains containing the location.
     */
    public static @NotNull HashSet<Terrain> getTerrainsAt(@NotNull WorldCoordinate worldCoordinate) {
        var terrainsAt = new HashSet<Terrain>(4);
        for (Terrain terrain : terrains) {
            if (terrain.isWithin(worldCoordinate)) terrainsAt.add(terrain);
        }
        return terrainsAt;
    }

    /**
     * Get the terrains owned by a player.
     *
     * @param owner The UUID of the player to check if owns the terrain.
     * @return The terrains that have this player as an owner.
     */
    public static @NotNull HashSet<Terrain> getTerrainsOf(@Nullable UUID owner) {
        var terrainsOf = new HashSet<Terrain>();
        for (Terrain terrain : terrains) {
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
     * Loads all terrains from disk into {@link #terrains()} set. No existing terrains are actually updated, this only
     * deserializes {@link Terrain} objects in {@link #TERRAINS_FOLDER} and adds them to {@link #terrains()} set using
     * {@link Set#add(Object)}.
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

        synchronized (terrainsToRemove) {
            for (UUID uuid : terrainsToRemove) {
                try {
                    Files.deleteIfExists(TERRAINS_FOLDER.resolve(uuid + ".terrain"));
                } catch (IOException e) {
                    Terrainer.logger().log("Unable to remove '" + uuid + "' terrain from " + TERRAINS_FOLDER.getFileName() + " folder:", ConsoleLogger.Level.ERROR);
                    e.printStackTrace();
                }
            }
            terrainsToRemove.clear();
        }

        synchronized (terrains) {
            // Saving changed terrains.
            for (Terrain terrain : terrains) {
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
                Terrainer.logger().log("Failed to wait 10 minutes to load regions", ConsoleLogger.Level.WARN);
            }
            save();
            autoSaveRunning = false;
        }, "Terrain Remover Thread").start();
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
