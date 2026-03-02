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

final class TerrainStorageManager {
    private TerrainStorageManager() {
    }

    static void save(@NotNull Terrain terrain) {
        currentStorageType().save.accept(terrain);
    }

    static @NotNull Terrain load(@NotNull Path path) {
        String fileName = path.getFileName().toString();

        if (fileName.endsWith(".terrain")) {
            return StorageType.FLAT_FILE.load.apply(path);
        } else if (fileName.endsWith(".yml")) {
            return StorageType.YAML.load.apply(path);
        } else {
            throw new UnsupportedOperationException("Unknown terrain file type '" + fileName + "'");
        }
    }

    static void delete(@NotNull Terrain terrain) throws IOException {
        Files.deleteIfExists(findPathForTerrain(terrain, ".terrain"));
        Files.deleteIfExists(findPathForTerrain(terrain, ".yml"));
    }

    static boolean isValidTerrainFile(@NotNull Path file) {
        String name = file.getFileName().toString();
        return (name.endsWith(".terrain") || name.endsWith(".yml")) && Files.isRegularFile(file);
    }

    private static @NotNull Path findPathForTerrain(@NotNull Terrain terrain, @NotNull String extension) {
        return TerrainManager.TERRAINS_FOLDER.resolve(terrain.id + extension);
    }

    private static @NotNull StorageType currentStorageType() {
        try {
            return StorageType.valueOf(Configurations.CONFIG.config().getString("Storage Type").orElse("YAML").toUpperCase(Locale.ROOT));
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

            try (ByteArrayInputStream bais = new ByteArrayInputStream((byte[]) data.get()); ObjectInputStream in = new ObjectInputStream(bais)) {
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
        FLAT_FILE(path -> {
            try (FileInputStream fis = new FileInputStream(path.toFile()); ObjectInputStream ois = new ObjectInputStream(fis)) {
                return (Terrain) ois.readObject();
            } catch (ClassNotFoundException | IOException e) {
                throw new RuntimeException(e);
            }
        }, terrain -> {
            try {
                Path path = findPathForTerrain(terrain, ".terrain");
                Path parent = path.getParent();
                if (parent != null) Files.createDirectories(parent);
                if (Files.notExists(path)) Files.createFile(path);

                try (FileOutputStream fos = new FileOutputStream(path.toFile()); ObjectOutputStream oos = new ObjectOutputStream(fos)) {
                    oos.writeObject(terrain);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }), //TODO:
        SQL(path -> {
            throw new UnsupportedOperationException("SQL storage is not implemented yet.");
        }, terrain -> {
            throw new UnsupportedOperationException("SQL storage is not implemented yet.");
        }), YAML(path -> {
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

                return new Terrain(min, max, UUID.fromString(terrain.getString("world").orElseThrow()), terrainId, terrain.getString("name").orElse(null), terrain.getString("description").orElse(null), terrain.getString("creation-date").map(ZonedDateTime::parse).orElseThrow(), owner == null ? null : UUID.fromString(owner), terrain.getNumber("priority").orElse(0).intValue(), moderators, members, flagMap, memberFlags);
            } catch (Exception e) {
                throw new IllegalArgumentException("The provided file is not a valid terrain file:", e);
            }
        }, terrain -> {
            try {
                Path path = findPathForTerrain(terrain, ".yml");
                PathUtils.deleteAll(path);
                Configuration config = new Configuration(new YamlConfigurationLoader());
                config.set("id", terrain.id.toString());
                config.set("name", terrain.name);
                config.set("description", terrain.description);
                config.set("creation-date", terrain.creationDate.toString());
                config.set("world", terrain.world.toString());
                config.set("priority", terrain.priority);
                config.set("diagonals.max-x", terrain.maxDiagonal.x());
                config.set("diagonals.max-y", terrain.maxDiagonal.y());
                config.set("diagonals.max-z", terrain.maxDiagonal.z());
                config.set("diagonals.min-x", terrain.minDiagonal.x());
                config.set("diagonals.min-y", terrain.minDiagonal.y());
                config.set("diagonals.min-z", terrain.minDiagonal.z());
                config.set("owner", terrain.owner == null ? null : terrain.owner.toString());
                config.set("moderators", terrain.moderators.view().stream().map(Objects::toString).collect(Collectors.toList()));
                config.set("members", terrain.members.view().stream().map(Objects::toString).collect(Collectors.toList()));
                serializeFlagsSection(terrain.flags, config);
                if (terrain.memberFlags.map != null) {
                    ConfigurationSection memberFlagsSection = config.createSection("member-flags");

                    terrain.memberFlags.map.forEach((member, flagMap) -> {
                        ConfigurationSection memberSection = memberFlagsSection.createSection(member.toString());
                        serializeFlagsSection(flagMap, memberSection);
                    });
                }
                config.save(path);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });

        private final @NotNull Function<Path, Terrain> load;
        private final @NotNull Consumer<Terrain> save;

        StorageType(@NotNull Function<Path, Terrain> load, @NotNull Consumer<Terrain> save) {
            this.load = load;
            this.save = save;
        }
    }
}
