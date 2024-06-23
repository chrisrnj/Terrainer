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

package com.epicnicity322.terrainer.core.util;

import com.epicnicity322.epicpluginlib.core.lang.LanguageHolder;
import com.epicnicity322.epicpluginlib.core.logger.ConsoleLogger;
import com.epicnicity322.terrainer.core.Coordinate;
import com.epicnicity322.terrainer.core.Terrainer;
import com.epicnicity322.terrainer.core.WorldChunk;
import com.epicnicity322.terrainer.core.WorldCoordinate;
import com.epicnicity322.terrainer.core.config.Configurations;
import com.epicnicity322.terrainer.core.terrain.Terrain;
import com.epicnicity322.terrainer.core.terrain.TerrainManager;
import com.epicnicity322.terrainer.core.terrain.WorldTerrain;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;

import java.lang.ref.WeakReference;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * All sorts of utility methods for claiming and testing terrains.
 *
 * @param <P> The player type of the platform.
 * @param <R> The message recipient type the platform, used for the {@link LanguageHolder}.
 */
public abstract class PlayerUtil<P extends R, R> {
    protected static final @NotNull HashMap<UUID, HashMap<SpawnedMarker, Boolean>> markers = new HashMap<>();
    /**
     * The selected diagonals of players. Key as the player's ID and value as an array with size 2 containing the diagonals.
     */
    private static final @NotNull HashMap<UUID, WorldCoordinate[]> selections = new HashMap<>();
    /**
     * The terrains being resized.
     */
    private static final @NotNull HashMap<UUID, WeakReference<Terrain>> resizingTerrains = new HashMap<>();
    /**
     * The ID to use in the selection map as placeholder for the console player.
     */
    private static final @NotNull UUID consoleUUID = UUID.randomUUID();

    private final @NotNull LanguageHolder<?, R> lang;
    /**
     * The default permission based block limits set up on config.
     */
    private final @NotNull TreeSet<Map.Entry<String, Long>> defaultBlockLimits;
    /**
     * The default permission based claim limits set up on config.
     */
    private final @NotNull TreeSet<Map.Entry<String, Integer>> defaultClaimLimits;
    private final @NotNull AtomicBoolean nestedTerrainsCountTowardsBlockLimit;
    private final @NotNull AtomicBoolean perWorldBlockLimit;
    private final @NotNull AtomicBoolean perWorldClaimLimit;
    private final @NotNull AtomicBoolean sumIfTheresMultipleBlockLimitPermissions;
    private final @NotNull AtomicBoolean sumIfTheresMultipleClaimLimitPermissions;

    protected PlayerUtil(@NotNull LanguageHolder<?, R> lang, @NotNull TreeSet<Map.Entry<String, Long>> defaultBlockLimits, @NotNull TreeSet<Map.Entry<String, Integer>> defaultClaimLimits, @NotNull AtomicBoolean nestedTerrainsCountTowardsBlockLimit, @NotNull AtomicBoolean perWorldBlockLimit, @NotNull AtomicBoolean perWorldClaimLimit, @NotNull AtomicBoolean sumIfTheresMultipleBlockLimitPermissions, @NotNull AtomicBoolean sumIfTheresMultipleClaimLimitPermissions) {
        this.lang = lang;
        this.defaultBlockLimits = defaultBlockLimits;
        this.defaultClaimLimits = defaultClaimLimits;
        this.perWorldBlockLimit = perWorldBlockLimit;
        this.perWorldClaimLimit = perWorldClaimLimit;
        this.nestedTerrainsCountTowardsBlockLimit = nestedTerrainsCountTowardsBlockLimit;
        this.sumIfTheresMultipleBlockLimitPermissions = sumIfTheresMultipleBlockLimitPermissions;
        this.sumIfTheresMultipleClaimLimitPermissions = sumIfTheresMultipleClaimLimitPermissions;
    }

    /**
     * Gets a mutable array with length 2, with the marked coordinate diagonals this player made.
     * <p>
     * Players can select either through command or the selection wand.
     *
     * @param player The UUID of the player to get selections from, or null to get from CONSOLE.
     * @return The selected coordinates of this player.
     */
    public static @Nullable WorldCoordinate @NotNull [] selections(@Nullable UUID player) {
        if (player == null) player = consoleUUID;
        return selections.computeIfAbsent(player, k -> new WorldCoordinate[2]);
    }

    /**
     * Gets the terrain the player is currently resizing. The new diagonals will be determined by the selections of the
     * player obtained by {@link #selections(UUID)}.
     *
     * @param player The UUID of the player to get the resizing terrain, or null to get from CONSOLE.
     * @return The terrain the player is resizing, null if the player is not resizing.
     */
    public static @Nullable Terrain currentlyResizing(@Nullable UUID player) {
        if (player == null) player = consoleUUID;

        WeakReference<Terrain> terrainReference = resizingTerrains.get(player);
        if (terrainReference == null) return null;

        Terrain terrain = terrainReference.get();
        if (terrain == null) resizingTerrains.remove(player);

        return terrain;
    }

    /**
     * Sets a terrain to be resized by the player. Their selections will be set to the terrain's diagonals, and they will
     * be able to assign new diagonals to this existing terrain.
     *
     * @param player  The UUID of the player to set the resizing terrain, or null to set as CONSOLE.
     * @param terrain The terrain the player will resize, null to remove a currently resizing.
     */
    public static void setCurrentlyResizing(@Nullable UUID player, @Nullable Terrain terrain) {
        if (terrain == null) {
            resizingTerrains.remove(player == null ? consoleUUID : player);
        } else {
            resizingTerrains.put(player == null ? consoleUUID : player, new WeakReference<>(terrain));
        }
    }

    public abstract boolean canFly(@NotNull P player);

    public abstract void setCanFly(@NotNull P player, boolean canFly);

    public abstract boolean shouldResetFly(@NotNull P player);

    public abstract void setResetFly(@NotNull P player, boolean checkPermission);

    public abstract boolean isSneaking(@NotNull P player);

    public abstract boolean isFlying(@NotNull P player);

    public abstract boolean isGliding(@NotNull P player);

    public abstract void setGliding(@NotNull P player, boolean glide);

    public abstract void applyEffect(@NotNull P player, @NotNull String effect, int power);

    public abstract void removeEffect(@NotNull P player, @NotNull String effect);

    public abstract void dispatchCommand(@Nullable P executor, @NotNull String command);

    public abstract boolean hasPermission(@NotNull P player, @NotNull String permission);

    public abstract boolean hasPermission(@NotNull UUID player, @NotNull String permission);

    public abstract @NotNull String playerName(@NotNull P player);

    public abstract @NotNull UUID playerUUID(@NotNull P player);

    public abstract @NotNull WorldCoordinate playerLocation(@NotNull P player);

    public abstract @NotNull UUID playerWorld(@NotNull P player);

    /**
     * Finds the name of the proprietary of this UUID. {@code null} is used to find the name of console.
     *
     * @param uuid The UUID to find the display name of.
     * @return The name ready to be used in messages.
     */
    public abstract @NotNull String ownerName(@Nullable UUID uuid);

    protected abstract @NotNull R consoleRecipient();

    /**
     * Performs several checks to verify if a player is allowed to claim a terrain, and sends messages to the player in
     * case they're not.
     * <p>
     * This will check the player's limits and if the terrain is overlapping another terrain.
     *
     * @param player  The player claiming the terrain, <code>null</code> for console.
     * @param terrain The terrain to be claimed by this player.
     * @return Whether the terrain was claimed or not.
     */
    @Contract("null,_ -> true")
    public boolean performClaimChecks(@Nullable P player, @NotNull Terrain terrain) {
        if (player == null) return true;

        return performClaimChecks(player, terrain, false);
    }

    private boolean performClaimChecks(@NotNull P receiver, @NotNull Terrain terrain, boolean resize) {
        UUID ownerID = terrain.owner();
        if (resize && ownerID == null) return true;

        int maxClaims;
        UUID world = terrain.world();
        UUID terrainOwner = resize ? ownerID : playerUUID(receiver);

        if (!resize && !hasPermission(terrainOwner, "terrainer.bypass.limit.claims") && claimedTerrains(terrainOwner, world) >= (maxClaims = claimLimit(receiver))) {
            lang.send(receiver, lang.get("Create.Error.No Claim Limit").replace("<max>", Integer.toString(maxClaims)));
            return false;
        }

        if (!hasPermission(terrainOwner, "terrainer.bypass.overlap")) {
            Set<Terrain> overlapping = overlappingTerrains(terrain);

            if (!overlapping.isEmpty()) {
                if (!hasPermission(receiver, "terrainer.bypass.overlap.self")) {
                    // Send generic message if player not allowed to see the overlapping terrains names.
                    if (overlapping.stream().anyMatch(t -> !hasInfoPermission(receiver, t))) {
                        lang.send(receiver, lang.get("Create.Error.Overlap Several"));
                    } else {
                        lang.send(receiver, lang.get("Create.Error.Overlap").replace("<other>", TerrainerUtil.listToString(overlapping, Terrain::name)));
                    }
                    return false;
                } else {
                    for (Terrain t1 : overlapping) {
                        // Terrains can overlap if they are owned by the same person.
                        if (terrainOwner.equals(t1.owner())) continue;

                        if (!hasInfoPermission(receiver, t1)) { // Player not allowed to see terrain name.
                            lang.send(receiver, lang.get("Create.Error.Overlap Several"));
                        } else {
                            lang.send(receiver, lang.get("Create.Error.Overlap").replace("<other>", t1.name()));
                        }
                        return false;
                    }
                }
            }
        }

        if (!hasPermission(terrainOwner, "terrainer.bypass.limit.blocks")) {
            long maxBlocks;
            double area = terrain.area();
            double minArea = Configurations.CONFIG.getConfiguration().getNumber("Min Area").orElse(25.0).doubleValue();
            double minDimensions = Configurations.CONFIG.getConfiguration().getNumber("Min Dimensions").orElse(5.0).doubleValue();
            Coordinate max = terrain.maxDiagonal();
            Coordinate min = terrain.minDiagonal();

            if (area < minArea) {
                lang.send(receiver, lang.get("Create.Error.Too Small").replace("<min>", Double.toString(minArea)));
                return false;
            }
            if (max.x() - (min.x() - 1) < minDimensions || max.z() - (min.z() - 1) < minDimensions) {
                lang.send(receiver, lang.get("Create.Error.Dimensions").replace("<min>", Double.toString(minDimensions)));
                return false;
            }
            // Checking whether the terrain's area plus the player's used blocks will be greater than the player's limit.
            if (claimedBlocks(terrainOwner, world, terrain) > (maxBlocks = blockLimit(receiver))) {
                lang.send(receiver, lang.get("Create.Error.No Block Limit").replace("<area>", Double.toString(area)).replace("<free>", Long.toString(Math.max(maxBlocks - claimedBlocks(terrainOwner, world), 0))));
                return false;
            }
        }

        return true;
    }

    public boolean resizeTerrain(@Nullable P receiver, @NotNull Terrain terrain, @NotNull WorldCoordinate first, @NotNull WorldCoordinate second) {
        if (!first.world().equals(second.world()) || !first.world().equals(terrain.world())) {
            lang.send(receiver == null ? consoleRecipient() : receiver, lang.get("Create.Error.Different Worlds").replace("<label>", "tr"));
            return false;
        }

        Terrain tempTerrain = new Terrain(terrain); // Creating safe terrain clone.
        tempTerrain.setDiagonals(first.coordinate(), second.coordinate());

        // Checking resize.
        if (receiver != null && !performClaimChecks(receiver, tempTerrain, true)) return false;

        terrain.setDiagonals(tempTerrain.minDiagonal(), tempTerrain.maxDiagonal());
        return true;
    }

    private @NotNull Set<Terrain> overlappingTerrains(@NotNull Terrain terrain) {
        if (terrain.chunks().isEmpty()) return Collections.emptySet();

        HashSet<Terrain> overlapping = new HashSet<>();

        terrain.chunks().forEach(chunk -> TerrainManager.terrainsAtChunk(terrain.world(), chunk.x(), chunk.z()).forEach(t -> {
            if (!t.id().equals(terrain.id()) && t.isOverlapping(terrain)) overlapping.add(t);
        }));
        return overlapping;
    }

    /**
     * Gets the amount of blocks a player has claimed using terrains.
     * <p>
     * If there's two or more terrains covering the same block, the block will not be counted multiple times.
     *
     * @param player The player to get the used block limit, null for CONSOLE.
     * @param world  The world to get the limit. Irrelevant if Per World Block Limit is disabled in config, but it's recommended to always provide a world.
     * @return The amount of blocks this player has claimed.
     */
    public long claimedBlocks(@Nullable UUID player, @UnknownNullability UUID world) {
        return claimedBlocks(player, world, null);
    }

    /**
     * Gets the sum of the areas of all terrains owned by the specified player.
     * <p>
     * If there's an intersection of multiple terrains, it will count as a single area, in order to accurately return
     * the amount of claimed blocks.
     * <p>
     * This method allows the specification of a terrain that is not in the {@link TerrainManager#allTerrains()}
     * collection to take into account when checking intersections. Useful for checking limits when a terrain is being
     * claimed.
     *
     * @param player          The player to get the used block limit.
     * @param world           The world to get the limit. Irrelevant if Per World Block Limit is disabled in config, but it's recommended to always provide a world.
     * @param claimingTerrain The terrain to take into account when checking intersections.
     * @return The amount of blocks this player has claimed.
     */
    private long claimedBlocks(@Nullable UUID player, @UnknownNullability UUID world, @Nullable Terrain claimingTerrain) {
        long usedBlocks = 0;
        Iterable<Terrain> terrains = perWorldBlockLimit.get() ? TerrainManager.terrains(Objects.requireNonNull(world)) : TerrainManager.allTerrains();

        // Getting areas of all terrains owned by player.
        if (nestedTerrainsCountTowardsBlockLimit.get()) {
            for (Terrain terrain : terrains) {
                if (!Objects.equals(terrain.owner(), player)) continue;
                // Let specified instance take priority.
                if (claimingTerrain != null && terrain.id().equals(claimingTerrain.id())) continue;

                usedBlocks += (long) terrain.area();
            }

            if (claimingTerrain != null) usedBlocks += (long) claimingTerrain.area();

            return usedBlocks;
        }

        // Getting the total amount of claimed blocks without duplicates.
        var events = new ArrayList<int[]>();

        for (Terrain terrain : terrains) {
            if (!Objects.equals(terrain.owner(), player)) continue;
            if (claimingTerrain != null && terrain.id().equals(claimingTerrain.id())) {
                addEvents(events, claimingTerrain); // Use specified instance.
                claimingTerrain = null;
                continue;
            }

            addEvents(events, terrain);
        }

        if (claimingTerrain != null) addEvents(events, claimingTerrain);

        events.sort(Comparator.comparingInt(o -> o[0]));

        var activeIntervals = new TreeMap<Integer, Integer>();
        int prevX = events.get(0)[0];

        for (int[] event : events) {
            int currX = event[0];
            int width = currX - prevX;

            if (!activeIntervals.isEmpty()) usedBlocks += (long) (coveredHeight(activeIntervals) * width);

            updateActiveIntervals(activeIntervals, event[1], event[2], event[3]);
            prevX = currX;
        }

        return usedBlocks;
    }

    private void addEvents(@NotNull ArrayList<int[]> events, @NotNull Terrain terrain) {
        Coordinate max = terrain.maxDiagonal(), min = terrain.minDiagonal();

        // MaxZ and MaxX offset by 1 to account for minecraft coordinate system.
        events.add(new int[]{(int) min.x(), (int) min.z(), (int) max.z() + 1, 1});
        events.add(new int[]{(int) max.x() + 1, (int) min.z(), (int) max.z() + 1, -1});
    }

    private void updateActiveIntervals(@NotNull TreeMap<Integer, Integer> activeIntervals, int z1, int z2, int type) {
        activeIntervals.put(z1, activeIntervals.getOrDefault(z1, 0) + type);
        activeIntervals.put(z2, activeIntervals.getOrDefault(z2, 0) - type);

        if (activeIntervals.get(z1) == 0) activeIntervals.remove(z1);
        if (activeIntervals.get(z2) == 0) activeIntervals.remove(z2);
    }

    private double coveredHeight(@NotNull TreeMap<Integer, Integer> intervals) {
        int coveredHeight = 0;
        int activeCount = 0;
        int prevZ = -1;

        for (Map.Entry<Integer, Integer> entry : intervals.entrySet()) {
            int currZ = entry.getKey();
            if (activeCount > 0 && prevZ != -1) coveredHeight += currZ - prevZ;
            activeCount += entry.getValue();
            prevZ = currZ;
        }

        return coveredHeight;
    }

    /**
     * Gets the max amount of blocks a player can claim. This takes into account the block limit from permissions AND
     * from the player's additional block limit ({@link #boughtBlockLimit(Object)}).
     *
     * @param player The player to get the block limit.
     * @return The max amount of blocks this player can claim.
     */
    public long blockLimit(@NotNull P player) {
        long blockLimit = boughtBlockLimit(player);
        boolean onlyFirst = !sumIfTheresMultipleBlockLimitPermissions.get();

        for (Map.Entry<String, Long> defaultLimit : defaultBlockLimits) {
            if (hasPermission(player, "terrainer.limit.blocks." + defaultLimit.getKey())) {
                blockLimit += defaultLimit.getValue();
                if (onlyFirst) break;
            }
        }

        return blockLimit;
    }

    /**
     * Gets the max amount of blocks this player can claim. Players may claim even more blocks if they have the default
     * limit permissions set on config, which this method does not take into account.
     *
     * @param player The player to get the block limit.
     * @return The max amount of blocks this player can claim (without taking permissions into account).
     * @see #blockLimit(Object)
     */
    public abstract long boughtBlockLimit(@NotNull P player);

    /**
     * Sets this player's additional block limit.
     *
     * @param player     The player to add or remove block limit from.
     * @param blockLimit The new additional block limit.
     */
    public abstract void setBoughtBlockLimit(@NotNull P player, long blockLimit);

    /**
     * Gets the number of terrains the specified player owns.
     *
     * @param player The player to get the used claim limit.
     * @param world  The world to get the limit. Irrelevant if Per World Claim Limit is disabled in config, but it's recommended to always provide a world.
     * @return The amount of terrains this player has claimed.
     */
    public int claimedTerrains(@Nullable UUID player, @UnknownNullability UUID world) {
        int claimed = 0;
        Iterable<Terrain> terrains = perWorldClaimLimit.get() ? TerrainManager.terrains(Objects.requireNonNull(world)) : TerrainManager.allTerrains();

        for (Terrain terrain : terrains) {
            if (Objects.equals(terrain.owner(), player)) claimed++;
        }

        return claimed;
    }

    /**
     * Gets the max amount of terrains a player can claim. This takes into account the claim limit from permissions and
     * from the player's additional claim limit ({@link #boughtClaimLimit(Object)}).
     *
     * @param player The player to get the claim limit.
     * @return The max amount of terrains this player can claim.
     */
    public int claimLimit(@NotNull P player) {
        int claimLimit = boughtClaimLimit(player);
        boolean onlyFirst = !sumIfTheresMultipleClaimLimitPermissions.get();

        for (Map.Entry<String, Integer> defaultLimit : defaultClaimLimits) {
            if (hasPermission(player, "terrainer.limit.claims." + defaultLimit.getKey())) {
                claimLimit += defaultLimit.getValue();
                if (onlyFirst) break;
            }
        }

        return claimLimit;
    }

    /**
     * Gets the max amount of terrains this player can claim. Players may claim even more terrains if they have the
     * default limit permissions set on config, which this method does not take into account.
     *
     * @param player The player to get the claim limit.
     * @return The max amount of terrains this player can claim (without taking permissions into account).
     * @see #claimLimit(Object)
     */
    public abstract int boughtClaimLimit(@NotNull P player);

    /**
     * Sets this player's additional claim limit.
     *
     * @param player     The player to add or remove claim limit from.
     * @param claimLimit The new additional claim limit.
     */
    public abstract void setBoughtClaimLimit(@NotNull P player, int claimLimit);

    /**
     * Checks whether the specified player has permission to see information of the terrain.
     * <p>
     * The permission varies whether the terrain is global, is owned by console, or if the player has any relations to
     * it.
     *
     * @param player  The player to check permission.
     * @param terrain The terrain to check if the player can see information about it.
     * @return Whether the player can see the terrain's info.
     */
    public boolean hasInfoPermission(@NotNull P player, @NotNull Terrain terrain) {
        if (terrain instanceof WorldTerrain) {
            return hasPermission(player, "terrainer.info.world");
        } else if (terrain.owner() == null) {
            return hasPermission(player, "terrainer.info.console");
        } else if (!playerUUID(player).equals(terrain.owner()) && !hasAnyRelations(player, terrain)) {
            return hasPermission(player, "terrainer.info.others");
        } else return true;
    }

    /**
     * Whether a player has any relations with a terrain.
     * <p>
     * A player is related to a terrain if:
     * <ul>
     *     <li>they are the owner of the terrain;</li>
     *     <li>they are a member of the terrain;</li>
     *     <li>they are a moderator of the terrain.</li>
     * </ul>
     *
     * @param player  The player to check if they have a relation with the terrain.
     * @param terrain The terrain to check if the player is related.
     * @return True if the player is the owner, a member, or a moderator of the terrain.
     * @see #hasAnyRelations(UUID, Terrain)
     */
    public boolean hasAnyRelations(@NotNull P player, @NotNull Terrain terrain) {
        return hasAnyRelations(playerUUID(player), terrain);
    }

    /**
     * Whether a player has any relations with a terrain.
     * <p>
     * A player is related to a terrain if:
     * <ul>
     *     <li>they are the owner of the terrain;</li>
     *     <li>they are a member of the terrain;</li>
     *     <li>they are a moderator of the terrain.</li>
     * </ul>
     *
     * @param player  The player to check if they have a relation with the terrain.
     * @param terrain The terrain to check if the player is related.
     * @return True if the player is the owner, a member, or a moderator of the terrain.
     */
    public boolean hasAnyRelations(@NotNull UUID player, @NotNull Terrain terrain) {
        return player.equals(terrain.owner()) || terrain.members().view().contains(player) || terrain.moderators().view().contains(player);
    }

    private void spawnAtY(@NotNull Coordinate min, @NotNull Coordinate max, @NotNull P player, int y, @NotNull HashMap<SpawnedMarker, Boolean> ids, boolean fromSelection) throws Throwable {
        // Left Bottom Corner
        ids.put(spawnMarker(player, min.x(), y, min.z(), false, fromSelection), fromSelection);
        // Left Upper Corner
        ids.put(spawnMarker(player, min.x(), y, max.z(), false, fromSelection), fromSelection);
        // Right Bottom Corner
        ids.put(spawnMarker(player, max.x(), y, min.z(), false, fromSelection), fromSelection);
        // Right Upper Corner
        ids.put(spawnMarker(player, max.x(), y, max.z(), false, fromSelection), fromSelection);

        if (max.x() - min.x() > 6 && max.z() - min.z() > 6) {
            // Left Bottom Corner
            ids.put(spawnMarker(player, min.x(), y, min.z() + 1, true, fromSelection), fromSelection);
            ids.put(spawnMarker(player, min.x() + 1, y, min.z(), true, fromSelection), fromSelection);
            // Left Upper Corner
            ids.put(spawnMarker(player, min.x(), y, max.z() - 1, true, fromSelection), fromSelection);
            ids.put(spawnMarker(player, min.x() + 1, y, max.z(), true, fromSelection), fromSelection);
            // Right Bottom Corner
            ids.put(spawnMarker(player, max.x(), y, min.z() + 1, true, fromSelection), fromSelection);
            ids.put(spawnMarker(player, max.x() - 1, y, min.z(), true, fromSelection), fromSelection);
            // Right Upper Corner
            ids.put(spawnMarker(player, max.x(), y, max.z() - 1, true, fromSelection), fromSelection);
            ids.put(spawnMarker(player, max.x() - 1, y, max.z(), true, fromSelection), fromSelection);
        }
    }

    private void spawnMarkersAtBorders(@Nullable Coordinate min, @Nullable Coordinate max, @NotNull P player, int y, boolean fromSelection) {
        try {
            UUID uuid = playerUUID(player);

            if (min != null && max != null) {
                Coordinate tempMin = min, tempMax = max;
                min = new Coordinate(Math.min(tempMin.x(), tempMax.x()), Math.min(tempMin.y(), tempMax.y()), Math.min(tempMin.z(), tempMax.z()));
                max = new Coordinate(Math.max(tempMin.x(), tempMax.x()), Math.max(tempMin.y(), tempMax.y()), Math.max(tempMin.z(), tempMax.z()));
                HashMap<SpawnedMarker, Boolean> ids = markers.computeIfAbsent(uuid, k -> new HashMap<>());

                if (min.y() <= Integer.MIN_VALUE || max.y() >= Integer.MAX_VALUE) {
                    // 2D terrain.
                    spawnAtY(min, max, player, y, ids, fromSelection);
                    return;
                }

                // 3D terrain.
                spawnAtY(min, max, player, (int) min.y(), ids, fromSelection);
                spawnAtY(min, max, player, (int) max.y(), ids, fromSelection);

                if (max.x() - min.x() > 6 && max.z() - min.z() > 6 && max.y() - min.y() > 6) {
                    ids.put(spawnMarker(player, min.x(), min.y() + 1, min.z(), true, fromSelection), fromSelection);
                    ids.put(spawnMarker(player, min.x(), min.y() + 1, max.z(), true, fromSelection), fromSelection);
                    ids.put(spawnMarker(player, max.x(), min.y() + 1, min.z(), true, fromSelection), fromSelection);
                    ids.put(spawnMarker(player, max.x(), min.y() + 1, max.z(), true, fromSelection), fromSelection);
                    ids.put(spawnMarker(player, min.x(), max.y() - 1, min.z(), true, fromSelection), fromSelection);
                    ids.put(spawnMarker(player, min.x(), max.y() - 1, max.z(), true, fromSelection), fromSelection);
                    ids.put(spawnMarker(player, max.x(), max.y() - 1, min.z(), true, fromSelection), fromSelection);
                    ids.put(spawnMarker(player, max.x(), max.y() - 1, max.z(), true, fromSelection), fromSelection);
                }
                return;
            }

            if (max != null) min = max;
            if (min == null) return;
            markers.computeIfAbsent(uuid, k -> new HashMap<>()).put(spawnMarker(player, (int) min.x(), y, (int) min.z(), false, fromSelection), fromSelection);
        } catch (Throwable t) {
            Terrainer.logger().log("Failed to spawn marker entity for player " + ownerName(playerUUID(player)), ConsoleLogger.Level.WARN);
            t.printStackTrace();
        }
    }

    public final void showMarkers(@NotNull P player) {
        showMarkers(player, (int) playerLocation(player).coordinate().y() - 1, true, null);
    }

    public void showMarkers(@NotNull P player, int y, boolean terrainMarkers, @Nullable Coordinate location) {
        removeMarkers(player);

        if (terrainMarkers) {
            var terrains = terrainsToShowBorders(player, location == null ? playerLocation(player).coordinate() : location);
            terrains.removeIf(t -> t.chunks().isEmpty()); // Removing global terrains.

            for (Terrain terrain : terrains) { // Terrains to show borders.
                int terrainY = y;

                if (terrain.maxDiagonal().y() + 1 < y) terrainY = (int) terrain.maxDiagonal().y() + 1;
                else if (terrain.minDiagonal().y() > y) terrainY = (int) terrain.minDiagonal().y();

                spawnMarkersAtBorders(terrain.minDiagonal(), terrain.maxDiagonal(), player, terrainY, false);
            }
        }

        WorldCoordinate[] selections = selections(playerUUID(player)); // Showing borders to selections.
        spawnMarkersAtBorders(selections[0] == null ? null : selections[0].coordinate(), selections[1] == null ? null : selections[1].coordinate(), player, y, true);
    }

    public @NotNull Set<Terrain> terrainsToShowBorders(@NotNull P player, @Nullable Coordinate location) {
        Set<Terrain> terrains;

        if (location != null) {
            terrains = TerrainManager.terrainsAt(playerLocation(player).world(), (int) location.x(), (int) location.y(), (int) location.z());
        } else {
            terrains = TerrainManager.terrainsAt(playerLocation(player));
        }

        if (terrains.isEmpty()) return terrains;

        for (Terrain terrain : new ArrayList<>(terrains)) {
            if (!hasInfoPermission(player, terrain)) terrains.remove(terrain); // Show only allowed terrains.

            terrain.chunks().forEach(chunk -> TerrainManager.terrainsAtChunk(new WorldChunk(terrain.world(), chunk)).forEach(t1 -> {
                if (terrain != t1 && terrain.isOverlapping(t1) && hasInfoPermission(player, t1)) terrains.add(t1);
            }));
        }

        return terrains;
    }


    public void removeMarkers(@NotNull P player) {
        HashMap<SpawnedMarker, Boolean> ids = markers.remove(playerUUID(player));
        if (ids == null) return;
        ids.keySet().forEach(marker -> {
            try {
                killMarker(player, marker);
            } catch (Throwable t) {
                Terrainer.logger().log("Failed to kill marker entity " + marker + " for player " + ownerName(playerUUID(player)), ConsoleLogger.Level.WARN);
                t.printStackTrace();
            }
        });
    }

    public void updateSelectionMarkersToTerrainMarkers(@NotNull P player) {
        HashMap<SpawnedMarker, Boolean> ids = markers.get(playerUUID(player));
        if (ids == null) return;

        ids.forEach((marker, fromSelection) -> {
            if (!fromSelection) return;
            try {
                updateSelectionMarkerToTerrainMarker(marker, player);
            } catch (Throwable t) {
                Terrainer.logger().log("Failed to update marker entity " + marker + " for player " + ownerName(playerUUID(player)), ConsoleLogger.Level.WARN);
                t.printStackTrace();
            }
        });
    }

    /**
     * Send a packet to the player to kill the marker entity with provided ID.
     *
     * @param player The player to kill the marker entity.
     * @param marker The marker entity.
     */
    protected abstract void killMarker(@NotNull P player, @NotNull SpawnedMarker marker) throws Throwable;

    /**
     * Send a packet to the player to spawn an entity with glow effect in the location.
     *
     * @param player    The player to send the packet.
     * @param x         X coordinate.
     * @param y         Y coordinate.
     * @param z         Z coordinate.
     * @param edges     If this is an edge of the marker.
     * @param selection Whether this marker should be a selection marker or terrain marker.
     * @return The marker entity.
     */
    protected abstract @NotNull SpawnedMarker spawnMarker(@NotNull P player, double x, double y, double z, boolean edges, boolean selection) throws Throwable;

    /**
     * Changes the color of a marker's glow effect.
     *
     * @param marker The marker to colorize.
     * @param player The player to colorize the marker to.
     */
    protected abstract void updateSelectionMarkerToTerrainMarker(@NotNull SpawnedMarker marker, @NotNull P player) throws Throwable;

    public record SpawnedMarker(int entityID, @NotNull UUID entityUUID, @NotNull Object markerEntity) {
    }
}
