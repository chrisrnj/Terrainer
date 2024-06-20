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
import com.epicnicity322.terrainer.core.Coordinate;
import com.epicnicity322.terrainer.core.Terrainer;
import com.epicnicity322.terrainer.core.WorldCoordinate;
import com.epicnicity322.terrainer.core.config.Configurations;
import com.epicnicity322.terrainer.core.util.PlayerUtil;
import com.epicnicity322.yamlhandler.Configuration;
import com.epicnicity322.yamlhandler.ConfigurationSection;
import com.epicnicity322.yamlhandler.YamlConfigurationLoader;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.nio.file.Path;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * A terrain is a protected cuboid area which not allowed players can not build or interact. Terrain objects are only
 * saved, loaded and have its flags enforced if they are added to a {@link TerrainManager}.
 */
public class Terrain implements Serializable {
    /**
     * The maximum amount of chunks a single terrain can have.
     */
    public static final int MAX_CHUNK_AMOUNT = 8398404;
    @Serial
    private static final long serialVersionUID = 7427059289387648133L;
    final @NotNull UUID world;
    final @NotNull UUID id;
    final @NotNull ZonedDateTime creationDate;
    final @NotNull PrivateSet<UUID> moderators;
    final @NotNull PrivateSet<UUID> members;
    final @NotNull FlagMap flags;
    final @NotNull MemberFlagMap memberFlags;
    @Nullable UUID owner;
    @NotNull Coordinate minDiagonal;
    @NotNull Coordinate maxDiagonal;
    @NotNull Set<Coordinate> borders;
    @NotNull Set<Chunk> chunks;
    @NotNull String name;
    @Nullable String description;
    int priority;
    /**
     * Whether this terrain should make a call to {@link TerrainManager#loadAutoSave()} everytime something changes.
     */
    transient volatile boolean save = false;
    /**
     * If the terrain was changed before it was last saved.
     */
    transient volatile boolean changed = false;

    /**
     * Constructor for creating a terrain object. Terrain objects are only saved, loaded and have its flags enforced if
     * they are added to a {@link TerrainManager}.
     * <p>
     * Diagonal coordinates have their min and max automatically calculated.
     * <p>
     * Moderators, members and flag collections are copied to avoid unsafe changes.
     * <p>
     * Flags with non {@link Serializable} objects are removed.
     *
     * @param first        The first diagonal of this terrain.
     * @param second       The second diagonal of this terrain.
     * @param world        The world this terrain is located.
     * @param id           The id of this terrain.
     * @param name         The name of this terrain, null to use the default name.
     * @param description  The description of this terrain, null to use the default description.
     * @param creationDate The date this terrain was created.
     * @param owner        The current owner of this terrain, null for CONSOLE.
     * @param moderators   The set of moderators of this terrain.
     * @param members      The set of members of this terrain.
     * @param flags        The set of active flags in this terrain.
     * @param memberFlags  The map of flags each specific player can have.
     */
    public Terrain(@NotNull Coordinate first, @NotNull Coordinate second, @NotNull UUID world, @NotNull UUID id, @Nullable String name, @Nullable String description, @NotNull ZonedDateTime creationDate, @Nullable UUID owner, int priority, @Nullable Collection<UUID> moderators, @Nullable Collection<UUID> members, @Nullable HashMap<String, Object> flags, @Nullable HashMap<UUID, HashMap<String, Object>> memberFlags) {
        this.minDiagonal = findMinMax(first, second, true);
        this.maxDiagonal = findMinMax(first, second, false);
        this.borders = findBorders(minDiagonal, maxDiagonal);
        this.chunks = findChunks(minDiagonal, maxDiagonal);
        this.name = name == null ? defaultName(id, owner) : name;
        this.id = id;
        this.world = world;
        this.owner = owner;
        this.priority = priority;
        this.moderators = new PrivateSet<>(moderators);
        this.members = new PrivateSet<>(members);
        this.description = description;
        this.creationDate = creationDate;
        this.flags = new FlagMap(flags);
        this.memberFlags = new MemberFlagMap(memberFlags);
    }

    /**
     * Constructor for creating a terrain object. Terrain objects are only saved, loaded and have its flags enforced if
     * they are added to a {@link TerrainManager}.
     * <p>
     * Diagonal coordinates have their min and max automatically calculated.
     * <p>
     * Moderators, members and flag collections are copied to avoid unsafe changes.
     * <p>
     * Flags with non {@link Serializable} objects are removed.
     *
     * @param first        The first diagonal of this terrain.
     * @param second       The second diagonal of this terrain.
     * @param world        The world this terrain is located.
     * @param id           The id of this terrain.
     * @param name         The name of this terrain, null to use the default name.
     * @param description  The description of this terrain, null to use the default description.
     * @param owner        The current owner of this terrain, null for CONSOLE.
     * @param creationDate The date this terrain was created.
     * @param moderators   The set of moderators of this terrain.
     * @param members      The set of members of this terrain.
     * @param flags        The set of active flags in this terrain.
     * @param memberFlags  The map of flags each specific player can have.
     */
    public Terrain(@NotNull Coordinate first, @NotNull Coordinate second, @NotNull UUID world, @NotNull UUID id, @Nullable String name, @Nullable String description, @Nullable UUID owner, @NotNull ZonedDateTime creationDate, int priority, @Nullable PrivateSet<UUID> moderators, @Nullable PrivateSet<UUID> members, @Nullable FlagMap flags, @Nullable MemberFlagMap memberFlags) {
        this.minDiagonal = findMinMax(first, second, true);
        this.maxDiagonal = findMinMax(first, second, false);
        this.borders = findBorders(minDiagonal, maxDiagonal);
        this.chunks = findChunks(minDiagonal, maxDiagonal);
        this.name = name == null ? defaultName(id, owner) : name;
        this.id = id;
        this.world = world;
        this.owner = owner;
        this.priority = priority;
        this.moderators = new PrivateSet<>(moderators == null ? null : moderators.set);
        this.members = new PrivateSet<>(members == null ? null : members.set);
        this.description = description;
        this.creationDate = creationDate;
        this.flags = new FlagMap(flags == null ? null : flags.map);
        this.memberFlags = new MemberFlagMap(memberFlags == null ? null : memberFlags.map, null);
    }

    /**
     * Constructor for creating a brand-new Terrain with random ID and {@link ZonedDateTime#now()} creation date.
     *
     * @param first  The first diagonal of this terrain.
     * @param second The second diagonal of this terrain.
     * @param world  The world this terrain is located.
     */
    public Terrain(@NotNull Coordinate first, @NotNull Coordinate second, @NotNull UUID world) {
        this(first, second, world, UUID.randomUUID(), null, null, ZonedDateTime.now(), null, 0, null, null, null, null);
    }

    /**
     * Makes an exact copy of a terrain. Terrain objects are only saved, loaded and have its flags enforced if they are
     * added to a {@link TerrainManager}, so any changes made to this copy will not be saved.
     *
     * @param terrain The terrain to make an unregistered copy of.
     */
    public Terrain(@NotNull Terrain terrain) {
        this(terrain.minDiagonal, terrain.maxDiagonal, terrain.world, terrain.id, terrain.name, terrain.description, terrain.owner, terrain.creationDate, terrain.priority, terrain.moderators, terrain.members, terrain.flags, terrain.memberFlags);
    }


    /**
     * Gets the name the specified terrain should have by default.
     * <p>
     * This will make checks to the {@link #owner()}'s other terrains, and name the terrain after their name plus a
     * number. If the terrain is owned by CONSOLE ({@link #owner()} is <code>null</code>), the default name is the
     * Time-low of the terrain's {@link #id()}.
     *
     * @return The default name of this terrain.
     */
    public static @NotNull String defaultName(@NotNull UUID terrainID, @Nullable UUID owner) {
        if (owner == null) return terrainID.toString().substring(0, terrainID.toString().indexOf('-'));

        List<Terrain> ownerTerrains = TerrainManager.terrainsOf(owner);
        PlayerUtil<?, ?> playerUtil = Terrainer.playerUtil();
        String nameFormat = Terrainer.lang().get("Create.Default Name").replace("<owner>", (playerUtil == null ? owner.toString() : playerUtil.getOwnerName(owner)));
        int size = 1;

        if (ownerTerrains.stream().anyMatch(t -> t.id.equals(terrainID))) {
            Set<String> ownerTerrainNames = ownerTerrains.stream().filter(t -> !t.id.equals(terrainID)).map(Terrain::name).collect(Collectors.toSet());
            // Find next available name.
            while (ownerTerrainNames.contains(nameFormat.replace("<number>", Integer.toString(size)))) size++;
        } else {
            size = ownerTerrains.size() + 1;
        }

        return nameFormat.replace("<number>", Integer.toString(size));
    }

    /**
     * Saves the terrain data in the specified file as YAML.
     * <p>
     * If the file already exists, it will be deleted and replaced. If the file does not exist, it will be created.
     *
     * @param path The path to save the terrain object.
     * @throws IOException If an input/output error happened while saving the terrain.
     */
    public static void toFile(@NotNull Path path, @NotNull Terrain terrain) throws IOException {
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
        serializeFlags(terrain.flags, config);
        if (terrain.memberFlags.map != null) {
            ConfigurationSection memberFlagsSection = config.createSection("member-flags");

            terrain.memberFlags.map.forEach((member, flagMap) -> {
                ConfigurationSection memberSection = memberFlagsSection.createSection(member.toString());
                serializeFlags(flagMap, memberSection);
            });
        }
        config.save(path);
    }

    /**
     * Attempts to get a terrain object from the YAML file in the path. The file must be a file that was previously saved
     * with {@link #toFile(Path, Terrain)}, or a YAML that provides every info about the terrain.
     *
     * @param path The file to get the terrain object from.
     * @return The terrain as previously saved in the path.
     * @throws IllegalArgumentException If the file is not a valid terrain object.
     */
    public static @NotNull Terrain fromFile(@NotNull Path path) {
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
    }

    private static void serializeFlags(@NotNull FlagMap map, @NotNull ConfigurationSection section) {
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

    @Contract("null,_ -> null")
    private static @Nullable HashMap<String, Object> deserializeFlagSection(@Nullable ConfigurationSection flagsSection, @NotNull UUID terrainId) {
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

    /**
     * Gets the min or max of two {@link Coordinate}s.
     *
     * @param first  The first coordinate to get the min or max points.
     * @param second The second coordinate to get the min or max points.
     * @param min    Whether to return the min or max coordinate points.
     * @return A min or max of two coordinates.
     */
    private static @NotNull Coordinate findMinMax(@NotNull Coordinate first, @NotNull Coordinate second, boolean min) {
        if (min) {
            return new Coordinate(Math.min(first.x(), second.x()), Math.min(first.y(), second.y()), Math.min(first.z(), second.z()));
        } else {
            return new Coordinate(Math.max(first.x(), second.x()), Math.max(first.y(), second.y()), Math.max(first.z(), second.z()));
        }
    }

    /**
     * Finds the borders of the terrain, used for showing particles.
     *
     * @param minDiagonal The min edge of the terrain.
     * @param maxDiagonal The max edge of the terrain.
     * @return An unmodifiable set with the coordinates of where border particles should spawn.
     */
    protected @NotNull Set<Coordinate> findBorders(@NotNull Coordinate minDiagonal, @NotNull Coordinate maxDiagonal) {
        Configuration config = Configurations.CONFIG.getConfiguration();

        if (!config.getBoolean("Borders.Enabled").orElse(false) || area() > config.getNumber("Borders.Max Area").orElse(2500).doubleValue()) {
            return Collections.emptySet();
        }

        double startX = minDiagonal.x(), endX = maxDiagonal.x() + 1d, startZ = minDiagonal.z(), endZ = maxDiagonal.z() + 1d;
        double borderAmount = ((endX - startX) + (endZ - startZ)) * 2;
        var border = new HashSet<Coordinate>((int) (borderAmount / .75f) + 1);

        for (double x = startX; x <= endX; ++x) {
            border.add(new Coordinate(x, 0, startZ));
            border.add(new Coordinate(x, 0, endZ));
        }

        for (double z = startZ; z <= endZ; ++z) {
            border.add(new Coordinate(startX, 0, z));
            border.add(new Coordinate(endX, 0, z));
        }

        return Collections.unmodifiableSet(border);
    }

    /**
     * Finds the chunks of this terrain, used for finding where the terrain is located.
     * <p>
     * This will be called on instantiation and when the method {@link #setDiagonals(Coordinate, Coordinate)} is called.
     *
     * @param minDiagonal The min edge of the terrain.
     * @param maxDiagonal The max edge of the terrain.
     * @return An unmodifiable set with all chunks where this terrain resides.
     * @implNote These chunks will be used for finding the terrain, so this should take into account terrains that are not rectangle shaped.
     * @implSpec When the amount of chunks is greater than {@link #MAX_CHUNK_AMOUNT}, an empty set should be returned.
     */
    protected @NotNull Set<Chunk> findChunks(@NotNull Coordinate minDiagonal, @NotNull Coordinate maxDiagonal) {
        // Converting block coordinates to chunk coordinates.
        int maxX = (int) maxDiagonal.x() >> 4;
        int minX = (int) minDiagonal.x() >> 4;
        int maxZ = (int) maxDiagonal.z() >> 4;
        int minZ = (int) minDiagonal.z() >> 4;

        long chunkAmount = ((maxX - minX + 1L) * (maxZ - minZ + 1L));

        if (chunkAmount > MAX_CHUNK_AMOUNT) return Collections.emptySet();

        var chunks = new HashSet<Chunk>((int) (chunkAmount / .75f) + 1);

        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                chunks.add(new Chunk(x, z));
            }
        }

        return Collections.unmodifiableSet(chunks);
    }

    /**
     * Updates this terrain in the list of terrain chunks, removing the instance from the previous chunks and adding in the new ones.
     *
     * @param previousChunks The chunks that this terrain had before being updated.
     */
    protected void chunkUpdate(@NotNull Set<Chunk> previousChunks) {
        // If this terrain is registered to save, then update in registered terrain chunks map.
        if (save && !previousChunks.equals(chunks)) TerrainManager.chunkUpdate(this, previousChunks);
    }

    /**
     * Updates this terrain in the list of registered terrains and chunks, removing this instance and adding it again,
     * in order to keep the list sorted by terrain priority.
     */
    protected void priorityUpdate() {
        // If this terrain is registered to save, then update the index in terrains list, so it remains sorted.
        if (save) TerrainManager.priorityUpdate(this);
    }

    /**
     * Sets this terrain as changed. If this terrain is marked to auto save, then {@link TerrainManager#loadAutoSave()}
     * will be called, to save this terrain's changes.
     * <p>
     * This should be called AFTER the property has changed. Redundant calls should be avoided, so the terrain saver is
     * only loaded if something has truly changed.
     */
    protected void markAsChanged() {
        changed = true;
        if (save) TerrainManager.loadAutoSave();
    }

    /**
     * @return The diagonal with min coordinates of this terrain.
     */
    public @NotNull Coordinate minDiagonal() {
        return minDiagonal;
    }

    /**
     * @return The diagonal with min coordinates of this terrain.
     */
    public @NotNull Coordinate maxDiagonal() {
        return maxDiagonal;
    }

    /**
     * Sets the min and max diagonal coordinates of this terrain.
     * <p>
     * Both min and max diagonals are updated by calling this method. This is to ensure both diagonals are truly
     * minimum and maximum.
     *
     * @param first  The new first diagonal of this terrain.
     * @param second The new second diagonal of this terrain.
     */
    public void setDiagonals(@NotNull Coordinate first, @NotNull Coordinate second) {
        Coordinate minDiagonal = findMinMax(first, second, true);
        Coordinate maxDiagonal = findMinMax(first, second, false);
        // Avoid unnecessary computations and don't mark terrain as changed.
        if (minDiagonal.equals(this.minDiagonal) && maxDiagonal.equals(this.maxDiagonal)) return;

        this.minDiagonal = minDiagonal;
        this.maxDiagonal = maxDiagonal;

        borders = findBorders(this.minDiagonal, this.maxDiagonal);

        Set<Chunk> previousChunks = chunks;
        chunks = findChunks(this.minDiagonal, this.maxDiagonal);
        chunkUpdate(previousChunks);

        markAsChanged();
    }

    /**
     * @return The bi-dimensional area of this terrain.
     */
    public double area() {
        return (maxDiagonal.x() - (minDiagonal.x() - 1)) * (maxDiagonal.z() - (minDiagonal.z() - 1));
    }

    /**
     * Gets the borders of this terrain.
     *
     * @return A set with the exact coordinates of the borders of the terrain.
     * Empty if borders are disabled or the terrain's area exceeds the limit.
     */
    public @NotNull Set<Coordinate> borders() {
        return borders;
    }

    public @NotNull Coordinate center() {
        return new Coordinate(minDiagonal.x() + ((maxDiagonal.x() - minDiagonal.x() + 1) / 2), minDiagonal.y() + ((maxDiagonal.y() - minDiagonal.y() + 1) / 2), minDiagonal.z() + ((maxDiagonal.z() - minDiagonal.z() + 1) / 2));
    }

    /**
     * Gets the chunks where this terrain resides.
     *
     * @return A set with the exact chunks of this terrain. Empty if this is a {@link WorldTerrain}.
     */
    public @NotNull Set<Chunk> chunks() {
        return chunks;
    }

    /**
     * @return The display name of this terrain.
     */
    public @NotNull String name() {
        return name;
    }

    /**
     * Sets the display name of this terrain.
     * <p>
     * Color codes are not formatted by this method, but it's recommended that you format all color codes of the name
     * parameter, as there is no guarantee the display name will be formatted in all messages sent to the player.
     *
     * @param name The display name of this terrain.
     * @see #defaultName(UUID, UUID)
     */
    public void setName(@NotNull String name) {
        if (name.equals(this.name)) return; // Don't mark terrain as changed.
        this.name = name;
        markAsChanged();
    }

    /**
     * The unique ID of this terrain. Unique IDs allow the terrain to have colored display names and more than one
     * terrain with the same name.
     */
    public @NotNull UUID id() {
        return id;
    }

    /**
     * @return The ID of the world this terrain is located.
     */
    public @NotNull UUID world() {
        return world;
    }

    /**
     * The owner of this terrain.
     * <p>
     * Owners have full control of the terrain. They can add moderators, add members, change the diagonals, change the
     * description, change flags of the terrain, and build.
     * <p>
     * The terrain can only have one owner, but they can pass the ownership to another player.
     *
     * @return The current owner of this terrain. Null if the terrain is owned by CONSOLE.
     */
    public @Nullable UUID owner() {
        return owner;
    }

    /**
     * Passes the ownership of the terrain to another player.
     *
     * @param owner The ID of the new owner, or null for CONSOLE.
     */
    public void setOwner(@Nullable UUID owner) {
        if (Objects.equals(this.owner, owner)) return; // Don't mark terrain as changed.
        this.owner = owner;
        markAsChanged();
    }

    /**
     * The priority this terrain should take over other terrains when enforcing flags.
     * <p>
     * Usually used when there's more than one terrain at a location.
     *
     * @return The priority of this terrain.
     */
    public int priority() {
        return priority;
    }

    /**
     * Sets this terrain's priority. The priority will be used when enforcing flags for multiple terrains.
     * <p>
     * If this terrain is registered, this method will update the terrain's index in the list of terrains by removing and
     * adding it again, in order to keep the list sorted by priority. So to avoid {@link ConcurrentModificationException},
     * it is not recommended to use this method while iterating through terrains using {@link TerrainManager#allTerrains()}
     * or {@link TerrainManager#terrains(UUID)}.
     *
     * @param priority The new priority of this terrain.
     */
    public void setPriority(int priority) {
        if (this.priority == priority) return; // Don't mark terrain as changed.
        this.priority = priority;
        priorityUpdate();
        markAsChanged();
    }

    /**
     * The moderators of the terrain.
     * <p>
     * Moderators can add members, change flags of the terrain, and build.
     *
     * @return The current moderators of the terrain.
     */
    public @NotNull PrivateSet<UUID> moderators() {
        return moderators;
    }

    /**
     * The members of the terrain.
     * <p>
     * Members can only build and interact with blocks on the terrain.
     * <p>
     * If you'd like to give members permission for just certain actions, check out {@link #memberFlags()}.
     *
     * @return The current members of the terrain.
     * @see #memberFlags()
     */
    public @NotNull PrivateSet<UUID> members() {
        return members;
    }

    /**
     * @return The description of the terrain.
     */
    public @NotNull String description() {
        if (description == null) return Terrainer.lang().get("Description.Default");
        return description;
    }

    /**
     * Sets the description of the terrain.
     * <p>
     * Color codes are not formatted by this method, but it's recommended that you format all color codes of the
     * description parameter, as there is no guarantee the description will be formatted in all messages sent to the
     * player.
     *
     * @param description The new description of this terrain, null to use the default one.
     */
    public void setDescription(@Nullable String description) {
        if (Objects.equals(this.description, description)) return; // Don't mark terrain as changed.
        this.description = description;
        markAsChanged();
    }

    /**
     * @return The creation date of this terrain.
     */
    public @NotNull ZonedDateTime creationDate() {
        return creationDate;
    }

    /**
     * Gets all active flags on this terrain.
     *
     * @return The active flags of this terrain.
     */
    public @NotNull FlagMap flags() {
        return flags;
    }

    /**
     * Gets the map for setting flags to specific members.
     * <p>
     * This map allows granting specific flags for members, rather than giving full build control using {@link #members()}.
     *
     * @return The per member flag map of this terrain.
     */
    public @NotNull MemberFlagMap memberFlags() {
        return memberFlags;
    }

    /**
     * Checks if the coordinate is in the same world and within this terrain's area.
     *
     * @param worldCoordinate The coordinate to check.
     * @return If the coordinate is in this terrain.
     */
    public boolean isWithin(@NotNull WorldCoordinate worldCoordinate) {
        if (!world.equals(worldCoordinate.world())) return false;
        Coordinate coordinate = worldCoordinate.coordinate();
        return coordinate.x() >= minDiagonal.x() && coordinate.x() <= maxDiagonal.x() && coordinate.y() >= minDiagonal.y() && coordinate.y() <= maxDiagonal.y() && coordinate.z() >= minDiagonal.z() && coordinate.z() <= maxDiagonal.z();
    }

    /**
     * Checks if the coordinate is within this terrain's area.
     *
     * @param x Coordinate point X.
     * @param y Coordinate point Y.
     * @param z Coordinate point Z.
     * @return If the coordinate is in this terrain.
     */
    public boolean isWithin(double x, double y, double z) {
        return x >= minDiagonal.x() && x <= maxDiagonal.x() && y >= minDiagonal.y() && y <= maxDiagonal.y() && z >= minDiagonal.z() && z <= maxDiagonal.z();
    }

    /**
     * Checks if the terrain is within the specified chunk coordinates.
     *
     * @param chunkX Chunk coordinate point X.
     * @param chunkZ Chunk coordinate point Z.
     * @return If the terrain is within the chunk.
     */
    public boolean isWithinChunk(int chunkX, int chunkZ) {
        int maxX = (int) maxDiagonal.x() >> 4;
        int minX = (int) minDiagonal.x() >> 4;
        int maxZ = (int) maxDiagonal.z() >> 4;
        int minZ = (int) minDiagonal.z() >> 4;
        return chunkX >= minX && chunkX <= maxX && chunkZ >= minZ && chunkZ <= maxZ;
    }

    /**
     * Checks if the specified terrain's diagonals overlap this terrain.
     *
     * @param terrain The terrain to check if contains this terrain within.
     * @return Whether this terrain is being overlapped by the other terrain.
     */
    public boolean isOverlapping(@NotNull Terrain terrain) {
        if (!world.equals(terrain.world) || terrain instanceof WorldTerrain) return false;

        Coordinate terrain2Min = terrain.minDiagonal;
        Coordinate terrain2Max = terrain.maxDiagonal;

        return minDiagonal.x() <= terrain2Max.x() && maxDiagonal.x() >= terrain2Min.x() && minDiagonal.y() <= terrain2Max.y() && maxDiagonal.y() >= terrain2Min.y() && minDiagonal.z() <= terrain2Max.z() && maxDiagonal.z() >= terrain2Min.z();
    }

    /**
     * @return Whether this implementation of Terrain uses the default values of the flags when they are undefined.
     */
    public boolean usesDefaultFlagValues() {
        return true;
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Terrain terrain = (Terrain) o;
        return id.equals(terrain.id) && world.equals(terrain.world) && minDiagonal.equals(terrain.minDiagonal) && maxDiagonal.equals(terrain.maxDiagonal) && Objects.equals(owner, terrain.owner) && creationDate.equals(terrain.creationDate) && name.equals(terrain.name) && Objects.equals(description, terrain.description) && priority == terrain.priority && moderators.equals(terrain.moderators) && members.equals(terrain.members) && flags.equals(terrain.flags) && memberFlags.equals(terrain.memberFlags);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, world, minDiagonal, maxDiagonal, owner, creationDate, priority, name, description, moderators, members, flags, memberFlags);
    }

    @Override
    public @NotNull String toString() {
        return "Terrain{" + "id=" + id + ", world=" + world + ", minDiagonal=" + minDiagonal + ", maxDiagonal=" + maxDiagonal + ", owner=" + owner + ", creationDate=" + creationDate + ", name='" + name + "', description='" + description + "', priority=" + priority + ", moderators=" + moderators + ", members=" + members + ", flags=" + flags + ", memberFlags=" + memberFlags + '}';
    }

    /**
     * Allows unloading empty sets from memory, and telling the terrain when a change is made.
     * To allow terrain auto-saving.
     *
     * @param <E> The elements in the set.
     */
    public final class PrivateSet<E> implements Serializable {
        // Usually there aren't many moderators or members in a terrain. Since this is all what PrivateSet is used for,
        // creating a HashSet that resizes only when there are 3 entries.
        private static final int INITIAL_CAPACITY = 4;
        @Serial
        private static final long serialVersionUID = -6990197981631016570L;

        @Nullable HashSet<E> set;
        private @Nullable Set<E> unmodifiableSet;

        private PrivateSet(@Nullable Collection<E> collection) {
            if (collection != null && !collection.isEmpty()) {
                this.set = new HashSet<>(collection);
                this.unmodifiableSet = Collections.unmodifiableSet(set);
            }
        }

        public boolean add(@NotNull E e) {
            if (set == null) {
                set = new HashSet<>(INITIAL_CAPACITY);
                unmodifiableSet = Collections.unmodifiableSet(set);
            }
            if (set.add(e)) {
                markAsChanged();
                return true;
            } else {
                return false;
            }
        }

        public boolean remove(@Nullable E e) {
            if (set == null) return false;
            try {
                if (set.remove(e)) {
                    markAsChanged();
                    return true;
                } else {
                    return false;
                }
            } finally {
                if (set.isEmpty()) {
                    set = null;
                    unmodifiableSet = null;
                }
            }
        }

        public boolean removeIf(@NotNull Predicate<E> filter) {
            if (set == null) return false;
            try {
                if (set.removeIf(filter)) {
                    markAsChanged();
                    return true;
                } else {
                    return false;
                }
            } finally {
                if (set.isEmpty()) {
                    set = null;
                    unmodifiableSet = null;
                }
            }
        }

        public void clear() {
            boolean hadSomething = set != null;
            set = null;
            unmodifiableSet = null;
            if (hadSomething) markAsChanged();
        }

        public @NotNull Set<E> view() {
            return Objects.requireNonNullElse(unmodifiableSet, Collections.emptySet());
        }

        @Override
        public boolean equals(@Nullable Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            PrivateSet<?> that = (PrivateSet<?>) o;
            return Objects.equals(set, that.set);
        }

        @Override
        public int hashCode() {
            return Objects.hash(set);
        }

        @Override
        public @NotNull String toString() {
            if (set == null) return "[]";
            return set.toString();
        }
    }

    /**
     * Helps to cast flag data objects, and tells the terrain when a change is made. To allow terrain auto-saving.
     * When the terrain has no flags, the map is unloaded from memory.
     */
    public final class FlagMap implements Serializable {
        private static final int INITIAL_CAPACITY = 8;
        @Serial
        private static final long serialVersionUID = -2028321748760154015L;

        @Nullable HashMap<String, Object> map;
        private @Nullable Map<String, Object> unmodifiableMap;

        private FlagMap(@Nullable HashMap<String, Object> map) {
            if (map != null && !map.isEmpty()) {
                this.map = new HashMap<>(map);
                this.map.values().removeIf(object -> !(object instanceof Serializable));
                unmodifiableMap = Collections.unmodifiableMap(this.map);
            }
        }

        /**
         * Adds data to a {@link Flag}. This data can be an extra property that changes the way the flag behaves on this
         * specific terrain. Like a farewell message, for example:
         * <pre>{@code
         *      Flag<String> farewellFlag = Flags.LEAVE_MESSAGE;
         *      terrain.flags().putFlag(farewellFlag, "Goodbye!");
         * }</pre>
         * Any saved data is serialized when the terrain is saved; then, when the terrain is loaded, it's cast back to
         * its original object. Therefore, all stored objects must implement {@link Serializable}.
         * <pre>{@code
         *      String leaveMessage = terrain.flags().getData(Flags.LEAVE_MESSAGE);
         *      // leaveMessage = "Goodbye!"
         * }</pre>
         * <p>
         * If a flag with the same ID is present, it's replaced by this instance and data.
         *
         * @param flag The flag to activate in this terrain.
         * @param data The additional properties of this flag.
         * @param <T>  The data type of the flag.
         * @return The previous associated data to this flag, null if there was no data or FlagSetEvent was cancelled.
         * Could be of a different data type if there was a flag with the same ID and different data type.
         * @throws IllegalArgumentException If the data object does not implement {@link Serializable}.
         */
        public <T> @Nullable Object putFlag(@NotNull Flag<T> flag, @NotNull T data) {
            return putFlag(flag, data, true);
        }

        private <T> @Nullable Object putFlag(@NotNull Flag<T> flag, @NotNull T data, boolean callEvents) {
            if (!(data instanceof Serializable))
                throw new IllegalArgumentException("Flags must only hold Serializable data.");

            if (callEvents) {
                TerrainManager.FlagSetResult<T> result = TerrainManager.callOnFlagSet(Terrain.this, flag, data, null);
                if (result.cancel()) return null;
                data = result.newData();
            }

            if (map == null) {
                map = new HashMap<>(INITIAL_CAPACITY);
                unmodifiableMap = Collections.unmodifiableMap(map);
            }
            try {
                return map.put(flag.id(), data);
            } finally {
                markAsChanged();
            }
        }

        /**
         * Gets the data associated to the {@link Flag}.
         * <p>
         * If this flag is present in the terrain, return the associated data to the flag.
         * If there is a flag with the same {@link Flag#id()} as the specified one, but different type,
         * then null is returned.
         *
         * @param flag The flag to get the data.
         * @param <T>  The data type of the flag.
         * @return The associated data to this flag. <code>null</code> (Or the default) if the terrain does not contain the flag.
         */
        public <T> @Nullable T getData(@NotNull Flag<T> flag) {
            return getData(flag, usesDefaultFlagValues());
        }

        @SuppressWarnings("unchecked")
        private <T> @Nullable T getData(@NotNull Flag<T> flag, boolean useDefault) {
            if (map == null) return useDefault ? flag.defaultValue() : null;
            Object data = map.get(flag.id());
            if (data == null) return useDefault ? flag.defaultValue() : null;
            if (flag.dataType().isAssignableFrom(data.getClass())) {
                return (T) data;
            } else {
                return useDefault ? flag.defaultValue() : null;
            }
        }

        /**
         * Removes a flag and its data from this terrain.
         *
         * @param flag The flag to remove.
         * @return The previous associated data to this flag or null if the FlagUnsetEvent was cancelled. Could be of a different data type.
         * @see #getAndRemoveFlag(Flag)
         */
        public @Nullable Object removeFlag(@NotNull Flag<?> flag) {
            return removeFlag(flag, true);
        }

        private @Nullable Object removeFlag(@NotNull Flag<?> flag, boolean callEvents) {
            if (map == null) return null;
            if (!map.containsKey(flag.id())) return null;
            if (callEvents && TerrainManager.callOnFlagUnset(Terrain.this, flag, null)) return null;

            try {
                return map.remove(flag.id());
            } finally {
                if (map.isEmpty()) {
                    map = null;
                    unmodifiableMap = null;
                }
                markAsChanged();
            }
        }

        /**
         * Removes a flag and its data from this terrain.
         *
         * @param flag The flag to remove.
         * @param <T>  The data type of the flag.
         * @return The previous associated data to this flag, if it had the same type as the specified flag. Always null if FlagUnsetEvent was cancelled.
         */
        @SuppressWarnings("unchecked")
        public @Nullable <T> T getAndRemoveFlag(@NotNull Flag<T> flag) {
            if (map == null) return null;
            if (!map.containsKey(flag.id())) return null;
            if (TerrainManager.callOnFlagUnset(Terrain.this, flag, null)) return null;

            try {
                Object previous = map.remove(flag.id());
                if (previous == null) return null;
                if (flag.dataType().isAssignableFrom(previous.getClass())) {
                    return (T) previous;
                } else {
                    return null;
                }
            } finally {
                if (map.isEmpty()) {
                    map = null;
                    unmodifiableMap = null;
                }
                markAsChanged();
            }
        }

        /**
         * @return An unmodifiable map of the flags IDs and associated objects.
         */
        public @NotNull Map<String, Object> view() {
            return Objects.requireNonNullElse(unmodifiableMap, Collections.emptyMap());
        }

        /**
         * Remove all flags and revert the map to default.
         *
         * @return Whether the map had any elements.
         */
        public boolean clear() {
            return clear(true, null);
        }

        private boolean clear(boolean callEvents, @Nullable UUID affectedMember) {
            if (!callEvents) {
                boolean wasEmpty = map == null || map.isEmpty();
                map = null;
                unmodifiableMap = null;
                if (!wasEmpty) markAsChanged();
                return wasEmpty;
            }

            if (map == null) return false;
            boolean anyRemoved = map.keySet().removeIf(flagID -> {
                Flag<?> f = Flags.matchFlag(flagID);
                if (f == null) return true;
                return !TerrainManager.callOnFlagUnset(Terrain.this, f, affectedMember);
            });

            if (map.isEmpty()) {
                map = null;
                unmodifiableMap = null;
            }
            if (anyRemoved) markAsChanged();
            return anyRemoved;
        }

        @Override
        public boolean equals(@Nullable Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            FlagMap that = (FlagMap) o;
            return Objects.equals(map, that.map);
        }

        @Override
        public int hashCode() {
            return Objects.hash(map);
        }

        @Override
        public @NotNull String toString() {
            if (map == null) return "{}";
            return map.toString();
        }
    }

    public final class MemberFlagMap implements Serializable {
        private static final int INITIAL_CAPACITY = 4;
        @Serial
        private static final long serialVersionUID = -6314163403738853033L;

        private @Nullable HashMap<UUID, FlagMap> map;

        private MemberFlagMap(@Nullable HashMap<UUID, HashMap<String, Object>> map) {
            if (map != null && !map.isEmpty()) {
                HashMap<UUID, FlagMap> thisMap = this.map = new HashMap<>((int) (map.size() / .75f) + 1);
                map.forEach(((member, flags) -> thisMap.put(member, new FlagMap(flags))));
            }
        }

        private MemberFlagMap(@Nullable HashMap<UUID, FlagMap> map, @Nullable Object ignoredDummy) {
            if (map != null && !map.isEmpty()) {
                HashMap<UUID, FlagMap> thisMap = this.map = new HashMap<>((int) (map.size() / .75f) + 1);
                map.forEach(((member, flags) -> thisMap.put(member, new FlagMap(flags.map))));
            }
        }

        /**
         * Maps a flag data to the specified member.
         *
         * @param member The member to set the flags of.
         * @param flag   The flag to map the data to.
         * @param data   The data to set to the flag.
         * @param <T>    The data type of the flag.
         * @return The previous value associated with the flag.
         * @throws IllegalArgumentException If the data object does not implement {@link Serializable}.
         */
        public <T> Object putFlag(@NotNull UUID member, @NotNull Flag<T> flag, T data) {
            if (!(data instanceof Serializable))
                throw new IllegalArgumentException("Flags must only hold Serializable data.");

            TerrainManager.FlagSetResult<T> result = TerrainManager.callOnFlagSet(Terrain.this, flag, data, member);
            if (result.cancel()) return null;
            data = result.newData();

            if (map == null) map = new HashMap<>(INITIAL_CAPACITY);
            return map.computeIfAbsent(member, k -> new FlagMap(null)).putFlag(flag, data, false);
        }

        /**
         * Gets the associated data to the flag of the specified member.
         *
         * @param member The member to get the flag's data from.
         * @param flag   The flag to get data from.
         * @param <T>    The data type of the flag.
         * @return The data of this flag, null if this member was not in the map, the flag was not set for this member, or the flag had a different {@link Flag#dataType()}
         */
        public <T> @Nullable T getData(@NotNull UUID member, @NotNull Flag<T> flag) {
            if (map == null) return null;
            FlagMap flagMap = map.get(member);
            if (flagMap == null) return null;
            return flagMap.getData(flag, false);
        }

        /**
         * Removes a member and all their flags from the map.
         *
         * @param member The member to remove from this map.
         * @return The previous flags of this member, null if the member was not in the member map.
         */
        public @Nullable Map<String, Object> removeMember(@NotNull UUID member) {
            if (map == null) return null;
            FlagMap flagMap = map.get(member);
            if (flagMap == null) return null;
            if (flagMap.clear(false, member)) markAsChanged();
            if (flagMap.view().isEmpty()) map.remove(member);
            if (map.isEmpty()) map = null;
            return flagMap.view();
        }

        /**
         * Removes a flag mapped to the specified member.
         *
         * @param member The member to remove the flag.
         * @param flag   The flag to remove from the member.
         * @return The previous data associated to the flag before removal, null if the member didn't have the flag or was not in the member map.
         */
        public @Nullable Object removeFlag(@NotNull UUID member, @NotNull Flag<?> flag) {
            if (map == null) return null;
            FlagMap flagMap = map.get(member);
            if (flagMap == null) return null;
            if (!flagMap.view().containsKey(flag.id())) return null;
            if (TerrainManager.callOnFlagUnset(Terrain.this, flag, member)) return null;

            try {
                return flagMap.removeFlag(flag, false);
            } finally {
                if (flagMap.view().isEmpty()) map.remove(member);
                if (map.isEmpty()) map = null;
                markAsChanged();
            }
        }

        /**
         * @param member The member to check if is in the map.
         * @return Whether this member has any flags set to them.
         */
        public boolean containsMember(@NotNull UUID member) {
            if (map == null) return false;
            return map.containsKey(member);
        }

        /**
         * Tests whether the specified member has data for the specified flag, and this data has the same {@link Flag#dataType()} as the specified flag.
         * <p>
         * This ignores whether the terrain has {@link Terrain#usesDefaultFlagValues()} true or false.
         *
         * @param member The member to check if it has the flag.
         * @param flag   The flag to check if it's in this members map.
         * @return Whether the member has the flag set.
         */
        public boolean containsFlag(@NotNull UUID member, @NotNull Flag<?> flag) {
            if (map == null) return false;
            FlagMap flagMap = map.get(member);
            if (flagMap == null) return false;
            Object data = flagMap.view().get(flag.id());
            if (data == null) return false;
            return flag.dataType().isAssignableFrom(data.getClass());
        }

        /**
         * Converts the internal {@link FlagMap} values to a map of flags represented by {@link String} and {@link Object}.
         *
         * @return An empty map if there are no member flags, or a clone of the member map with immutable flag values.
         */
        public @NotNull Map<UUID, Map<String, Object>> view() {
            if (map == null) return Collections.emptyMap();
            HashMap<UUID, Map<String, Object>> view = new HashMap<>((int) (map.size() / .75f) + 1);
            map.forEach((uuid, flagMap) -> view.put(uuid, flagMap.view()));
            return view;
        }

        @Override
        public boolean equals(@Nullable Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            MemberFlagMap that = (MemberFlagMap) o;
            return Objects.equals(map, that.map);
        }

        @Override
        public int hashCode() {
            return Objects.hash(map);
        }

        @Override
        public @NotNull String toString() {
            if (map == null) return "{}";
            return map.toString();
        }
    }
}
