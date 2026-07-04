/*
 * Terrainer - A minecraft terrain claiming protection plugin.
 * Copyright (C) 2026 Christiano Rangel
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
import com.epicnicity322.epicpluginlib.core.util.PathLocker;
import com.epicnicity322.epicpluginlib.core.util.PathUtils;
import com.epicnicity322.terrainer.core.Terrainer;
import com.epicnicity322.terrainer.core.config.Configurations;
import com.epicnicity322.terrainer.core.location.Coordinate;
import com.epicnicity322.yamlhandler.Configuration;
import com.epicnicity322.yamlhandler.ConfigurationSection;
import com.epicnicity322.yamlhandler.loaders.YamlConfigurationLoader;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * A class for saving, loading and deleting terrain files.
 */
final class TerrainStorageManager {
    private TerrainStorageManager() {
    }

    static void save(@NotNull Terrain terrain) {
        terrain.changed = false;
        currentStorageType().save.accept(terrain);
    }

    static @NotNull Terrain load(@NotNull Path path) {
        String fileName = path.getFileName().toString();

        if (fileName.endsWith(StorageType.YAML.extension)) {
            return StorageType.YAML.load.apply(path);
        } else if (fileName.endsWith(StorageType.SERIALIZED.extension)) {
            return StorageType.SERIALIZED.load.apply(path);
        } else {
            throw new UnsupportedOperationException("Unknown terrain file type '" + fileName + "'");
        }
    }

    static void delete(@NotNull UUID terrainId) throws IOException {
        try (PathLocker.LockToken ignore = PathLocker.lock(terrainFile(terrainId, currentStorageType().extension))) { // lock onto current saving method.
            // Files of all types should be deleted.
            Files.deleteIfExists(terrainFile(terrainId, StorageType.SERIALIZED.extension));
            Files.deleteIfExists(terrainFile(terrainId, StorageType.YAML.extension));
        }
    }

    static boolean isValidTerrainFile(@NotNull Path file) {
        String name = file.getFileName().toString();
        return (name.endsWith(StorageType.YAML.extension) || name.endsWith(StorageType.SERIALIZED.extension)) && Files.isRegularFile(file);
    }

    private static @NotNull Path terrainFile(@NotNull UUID terrainId, @NotNull String extension) {
        Path parent = TerrainManager.TERRAINS_FOLDER;
        try {
            if (!Files.isDirectory(parent)) PathUtils.deleteAll(parent);
            Files.createDirectories(parent);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return parent.resolve(terrainId + extension);
    }

    private static @NotNull StorageType currentStorageType() {
        try {
            return StorageType.valueOf(Configurations.CONFIG.config().getString("Storage Type").orElse(StorageType.YAML.name()).toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return StorageType.YAML;
        }
    }

    @Contract("null,_ -> null")
    private static HashMap<String, Object> deserializeFlagSection(@Nullable ConfigurationSection flagsSection, @NotNull UUID terrainId) {
        if (flagsSection == null) return null;

        Set<Map.Entry<String, Object>> nodes = flagsSection.getNodes().entrySet();
        HashMap<String, Object> flagMap = new HashMap<>((int) (nodes.size() / .75f) + 1);

        for (Map.Entry<String, Object> node : nodes) {
            if (!(node.getValue() instanceof ConfigurationSection flag)) continue;
            Optional<String> id = flag.getString("id");
            if (id.isEmpty()) continue;
            Optional<Object> data = flag.getObject("data");
            if (data.isEmpty()) continue;
            if (!(data.get() instanceof byte[])) continue;

            try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream((byte[]) data.get()))) {
                flagMap.put(id.get(), in.readObject());
            } catch (Exception e) {
                Terrainer.logger().log("Flag with id '" + id.get() + "' could not be added to terrain '" + terrainId + "' because an issue happened while loading the data. (Maybe because of a removed plugin?)", ConsoleLogger.Level.ERROR);
            }
        }

        return flagMap;
    }

    private static void serializeFlagsSection(@NotNull Terrain.FlagMap map, @NotNull ConfigurationSection section) {
        int count = 0;
        for (Map.Entry<String, Object> entry : map.view().entrySet()) {
            Object data = entry.getValue();
            if (data == null) continue;

            try (ByteArrayOutputStream baos = new ByteArrayOutputStream(); ObjectOutputStream out = new ObjectOutputStream(baos)) {
                out.writeObject(data);
                section.set("flags." + count + ".id", entry.getKey());
                section.set("flags." + count + ".data", baos.toByteArray());
                count++;
            } catch (Exception e) {
                Terrainer.logger().log("Unable to serialize flag with id '" + entry.getKey() + "':", ConsoleLogger.Level.ERROR);
                e.printStackTrace();
            }
        }
    }

    private enum StorageType {
        SERIALIZED(path -> {
            try (ObjectInputStream ois = new ObjectInputStream(Files.newInputStream(path))) {
                return (Terrain) ois.readObject();
            } catch (ClassNotFoundException | IOException e) {
                throw new RuntimeException(e);
            }
        }, terrain -> {
            Path path = terrainFile(terrain.id(), ".ser");

            try (PathLocker.LockToken ignore = PathLocker.lock(path); ObjectOutputStream oos = new ObjectOutputStream(Files.newOutputStream(path))) {
                oos.writeObject(terrain);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }, ".ser"), SQL(path -> { //TODO
            throw new UnsupportedOperationException("SQL storage is not implemented yet.");
        }, terrain -> {
            throw new UnsupportedOperationException("SQL storage is not implemented yet.");
        }, ".db"), YAML(path -> {
            try {
                Configuration terrain = new YamlConfigurationLoader().load(path);
                UUID terrainId = UUID.fromString(terrain.getString("id").orElseThrow());
                Coordinate min = new Coordinate(terrain.getNumber("diagonals.min-x").orElseThrow().doubleValue(), terrain.getNumber("diagonals.min-y").orElseThrow().doubleValue(), terrain.getNumber("diagonals.min-z").orElseThrow().doubleValue());
                Coordinate max = new Coordinate(terrain.getNumber("diagonals.max-x").orElseThrow().doubleValue(), terrain.getNumber("diagonals.max-y").orElseThrow().doubleValue(), terrain.getNumber("diagonals.max-z").orElseThrow().doubleValue());
                String owner = terrain.getString("owner").orElse(null);

                ArrayList<UUID> moderators = terrain.getCollection("moderators", obj -> {
                    try {
                        return UUID.fromString(obj.toString());
                    } catch (IllegalArgumentException ignored) {
                        return null;
                    }
                });
                moderators.removeIf(Objects::isNull);

                ArrayList<UUID> members = terrain.getCollection("members", obj -> {
                    try {
                        return UUID.fromString(obj.toString());
                    } catch (IllegalArgumentException ignored) {
                        return null;
                    }
                });
                members.removeIf(Objects::isNull);

                HashMap<String, Object> flagMap = deserializeFlagSection(terrain.getConfigurationSection("flags"), terrainId);

                ConfigurationSection memberFlagsSection = terrain.getConfigurationSection("member-flags");
                HashMap<UUID, HashMap<String, Object>> memberFlags = null;

                if (memberFlagsSection != null) {
                    Set<Map.Entry<String, Object>> nodes = memberFlagsSection.getNodes().entrySet();
                    memberFlags = new HashMap<>((int) (nodes.size() / .75f) + 1);

                    for (Map.Entry<String, Object> node : nodes) {
                        UUID memberID;
                        try {
                            memberID = UUID.fromString(node.getKey());
                        } catch (IllegalArgumentException ignored) {
                            continue;
                        }

                        if (!(node.getValue() instanceof ConfigurationSection flagsSection)) continue;
                        memberFlags.put(memberID, deserializeFlagSection(flagsSection, terrainId));
                    }
                }

                Terrain instance = new Terrain(min, max, UUID.fromString(terrain.getString("world").orElseThrow()), terrainId, terrain.getString("name").orElse(null), terrain.getString("description").orElse(null), terrain.getString("creation-date").map(ZonedDateTime::parse).orElseThrow(), owner == null ? null : UUID.fromString(owner), terrain.getNumber("priority").orElse(0).intValue(), moderators, members, flagMap, memberFlags);

                if (terrain.getString("type").orElse("").equals(WorldTerrain.class.getName())) {
                    return new WorldTerrain(instance, instance.name);
                } else {
                    return instance;
                }
            } catch (Exception e) {
                throw new IllegalArgumentException("The provided file is not a valid terrain file:", e);
            }
        }, terrain -> {
            Configuration config = new Configuration(new YamlConfigurationLoader());
            UUID id = terrain.id();
            UUID owner = terrain.owner();
            Coordinate maxDiagonal = terrain.maxDiagonal();
            Coordinate minDiagonal = terrain.minDiagonal();

            config.set("type", terrain.getClass().getName());
            config.set("id", id.toString());
            config.set("name", terrain.name());
            config.set("description", terrain.description);
            config.set("creation-date", terrain.creationDate().toString());
            config.set("world", terrain.world().toString());
            config.set("priority", terrain.priority());
            config.set("diagonals.max-x", maxDiagonal.x());
            config.set("diagonals.max-y", maxDiagonal.y());
            config.set("diagonals.max-z", maxDiagonal.z());
            config.set("diagonals.min-x", minDiagonal.x());
            config.set("diagonals.min-y", minDiagonal.y());
            config.set("diagonals.min-z", minDiagonal.z());
            config.set("owner", owner == null ? null : owner.toString());
            config.set("moderators", terrain.moderators().view().stream().map(Objects::toString).collect(Collectors.toList()));
            config.set("members", terrain.members().view().stream().map(Objects::toString).collect(Collectors.toList()));
            serializeFlagsSection(terrain.flags(), config);

            HashMap<UUID, Terrain.FlagMap> memberFlagsMap = terrain.memberFlags().map;
            if (memberFlagsMap != null) {
                ConfigurationSection memberFlagsSection = config.createSection("member-flags");

                memberFlagsMap.forEach((member, flagMap) -> {
                    ConfigurationSection memberSection = memberFlagsSection.createSection(member.toString());
                    serializeFlagsSection(flagMap, memberSection);
                });
            }

            Path path = terrainFile(id, ".yml");

            try (PathLocker.LockToken ignore = PathLocker.lock(path)) {
                try {
                    PathUtils.deleteAll(path);
                } catch (IOException e) {
                    Terrainer.logger().log("Error while deleting old terrain file of '" + id + "':", ConsoleLogger.Level.ERROR);
                    e.printStackTrace();
                    Terrainer.logger().log("Changes were not saved for terrain '" + id + "' (" + terrain.name + ") and it will be reset when the server restarts.", ConsoleLogger.Level.ERROR);
                    throw new RuntimeException(e);
                }
                try {
                    config.save(path);
                } catch (IOException e) {
                    Terrainer.logger().log("Error while saving terrain '" + id + "':", ConsoleLogger.Level.ERROR);
                    e.printStackTrace();
                    Terrainer.logger().log("The terrain '" + id + "' (" + terrain.name + ") was deleted, but could not be saved again. The terrain will not exist when the server restarts.", ConsoleLogger.Level.ERROR);
                    throw new RuntimeException(e);
                }
            }
        }, ".yml");

        private final @NotNull Function<Path, Terrain> load;
        private final @NotNull Consumer<Terrain> save;
        private final @NotNull String extension;

        StorageType(@NotNull Function<Path, Terrain> load, @NotNull Consumer<Terrain> save, @NotNull String extension) {
            this.load = load;
            this.save = save;
            this.extension = extension;
        }
    }
}
