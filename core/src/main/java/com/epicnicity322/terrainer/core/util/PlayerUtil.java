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
import com.epicnicity322.terrainer.core.WorldCoordinate;
import com.epicnicity322.terrainer.core.config.Configurations;
import com.epicnicity322.terrainer.core.event.terrain.ITerrainAddEvent;
import com.epicnicity322.terrainer.core.terrain.Flags;
import com.epicnicity322.terrainer.core.terrain.Terrain;
import com.epicnicity322.terrainer.core.terrain.TerrainManager;
import com.epicnicity322.terrainer.core.terrain.WorldTerrain;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * All sorts of utility methods for claiming and testing terrains.
 *
 * @param <P> The player type of the platform.
 * @param <R> The message recipient type the platform, used for the {@link LanguageHolder}.
 */
public abstract class PlayerUtil<P extends R, R> {
    /**
     * The default permission based block limits set up on config.
     */
    private static final @NotNull HashMap<String, Long> defaultBlockLimits = new HashMap<>(8);
    /**
     * The default permission based claim limits set up on config.
     */
    private static final @NotNull HashMap<String, Integer> defaultClaimLimits = new HashMap<>(8);
    private static final @NotNull HashMap<UUID, ArrayList<Integer>> markers = new HashMap<>();
    private final @NotNull LanguageHolder<?, R> lang;

    protected PlayerUtil(@NotNull LanguageHolder<?, R> lang) {
        this.lang = lang;
    }

    public static void setDefaultBlockLimits(@NotNull Map<String, Long> blockLimits) {
        defaultBlockLimits.clear();
        defaultBlockLimits.putAll(blockLimits);
    }

    public static void setDefaultClaimLimits(@NotNull Map<String, Integer> claimLimits) {
        defaultClaimLimits.clear();
        defaultClaimLimits.putAll(claimLimits);
    }

    public abstract boolean canFly(@NotNull P player);

    public abstract void setResetFly(@NotNull P player, boolean checkPermission);

    public abstract boolean isSneaking(@NotNull P player);

    public abstract boolean hasPermission(@NotNull P player, @NotNull String permission);

    /**
     * Finds the name of the proprietary of this UUID. {@code null} is used to find the name of console.
     *
     * @param uuid The UUID to find the display name of.
     * @return The name ready to be used in messages.
     */
    public abstract @NotNull String getOwnerName(@Nullable UUID uuid);

    protected abstract @NotNull UUID getUniqueId(@NotNull P player);

    protected abstract @NotNull R getConsoleRecipient();

    // TODO: Check if terrain is intersecting another and remove intersection area from used blocks. Switch ON/OFF in config.

    /**
     * Attempts to claim a terrain with the player as owner.
     * <p>
     * This will do checks in the player's limits and update them, to make sure the player is allowed to claim terrains.
     * If the player is allowed to claim, the player is set as owner of the terrain and the terrain is registered using
     * {@link TerrainManager#add(Terrain)}.
     * <p>
     * This method also fires the implementation of {@link ITerrainAddEvent}
     * in your platform.
     *
     * @param player  The player claiming the terrain, <code>null</code> for console.
     * @param terrain The terrain to be claimed by this player.
     * @return Whether the terrain was claimed or not.
     */
    public boolean claimTerrain(@Nullable P player, @NotNull Terrain terrain) {
        R receiver;
        long usedBlocks = -1;
        long maxBlocks = -1;

        if (player != null) {
            UUID playerID = getUniqueId(player);
            int maxClaims;
            if (!hasPermission(player, "terrainer.bypass.limit.claims") && getUsedClaimLimit(playerID) >= (maxClaims = getMaxClaimLimit(player))) {
                lang.send(player, lang.get("Create.Error.No Claim Limit").replace("<max>", Integer.toString(maxClaims)));
                return false;
            }
            if (!hasPermission(player, "terrainer.bypass.limit.blocks")) {
                double area = terrain.area();
                double minArea = Configurations.CONFIG.getConfiguration().getNumber("Min Area").orElse(25.0).doubleValue();
                double minDimensions = Configurations.CONFIG.getConfiguration().getNumber("Min Dimensions").orElse(5.0).doubleValue();
                Coordinate max = terrain.maxDiagonal();
                Coordinate min = terrain.minDiagonal();

                if (area < minArea) {
                    lang.send(player, lang.get("Create.Error.Too Small").replace("<min>", Double.toString(minArea)));
                    return false;
                }
                if (max.x() - (min.x() - 1) < minDimensions || max.z() - (min.z() - 1) < minDimensions) {
                    lang.send(player, lang.get("Create.Error.Dimensions").replace("<min>", Double.toString(minDimensions)));
                    return false;
                }
                if ((area + (usedBlocks = getUsedBlockLimit(playerID))) > (maxBlocks = getMaxBlockLimit(player))) {
                    lang.send(player, lang.get("Create.Error.No Block Limit").replace("<area>", Double.toString(area)).replace("<used>", Long.toString(usedBlocks)));
                    return false;
                }
                usedBlocks += (long) area;
            }
            if (!hasPermission(player, "terrainer.bypass.overlap")) {
                for (Terrain t1 : TerrainManager.allTerrains()) {
                    // Terrains can overlap if they are owned by the same person.
                    if (playerID.equals(t1.owner())) continue;
                    if (t1.isOverlapping(terrain)) {
                        lang.send(player, lang.get("Create.Error.Overlap").replace("<other>", t1.name()));
                        return false;
                    }
                }
            }
            receiver = player;
        } else {
            receiver = getConsoleRecipient();
        }

        terrain.setOwner(player == null ? null : getUniqueId(player));
        if (TerrainManager.add(terrain)) {
            lang.send(receiver, lang.get("Create.Success").replace("<name>", terrain.name()).replace("<used>", Long.toString(usedBlocks)).replace("<max>", Long.toString(maxBlocks)));
            return true;
        }
        return false;
    }

    // TODO: Check if terrain is intersecting another and remove intersection area from used blocks. Switch ON/OFF in config.

    /**
     * Gets the sum of the areas of all terrains owned by the specified player.
     *
     * @param player The player to get the used block limit.
     * @return The amount of blocks this player has claimed.
     */
    public long getUsedBlockLimit(@Nullable UUID player) {
        long usedBlocks = 0;
        for (Terrain terrain : TerrainManager.allTerrains()) {
            if (!Objects.equals(terrain.owner(), player)) continue;
            usedBlocks += (long) terrain.area();
        }

        return usedBlocks;
    }

    /**
     * Gets the max amount of blocks a player can claim. This takes into account the block limit from permissions and
     * from the player's additional block limit ({@link #getAdditionalMaxBlockLimit(Object)}).
     *
     * @param player The player to get the block limit.
     * @return The max amount of blocks this player can claim.
     */
    public long getMaxBlockLimit(@NotNull P player) {
        long blockLimit = getAdditionalMaxBlockLimit(player);

        for (Map.Entry<String, Long> defaultLimit : defaultBlockLimits.entrySet()) {
            if (hasPermission(player, "terrainer.limit.blocks." + defaultLimit.getKey())) {
                blockLimit += defaultLimit.getValue();
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
     * @see #getMaxBlockLimit(Object)
     */
    public abstract long getAdditionalMaxBlockLimit(@NotNull P player);

    /**
     * Sets this player's additional block limit.
     *
     * @param player     The player to add or remove block limit from.
     * @param blockLimit The new additional block limit.
     */
    public abstract void setAdditionalMaxBlockLimit(@NotNull P player, long blockLimit);

    /**
     * Gets the number of terrains the specified player owns.
     *
     * @param player The player to get the used claim limit.
     * @return The amount of terrains this player has claimed.
     */
    public int getUsedClaimLimit(@Nullable UUID player) {
        int used = 0;
        for (Terrain terrain : TerrainManager.allTerrains()) {
            if (Objects.equals(terrain.owner(), player)) {
                used++;
            }
        }
        return used;
    }

    /**
     * Gets the max amount of terrains a player can claim. This takes into account the claim limit from permissions and
     * from the player's additional claim limit ({@link #getAdditionalMaxClaimLimit(Object)}).
     *
     * @param player The player to get the claim limit.
     * @return The max amount of terrains this player can claim.
     */
    public int getMaxClaimLimit(@NotNull P player) {
        int claimLimit = getAdditionalMaxClaimLimit(player);

        for (Map.Entry<String, Integer> defaultLimit : defaultClaimLimits.entrySet()) {
            if (hasPermission(player, "terrainer.limit.claims." + defaultLimit.getKey())) {
                claimLimit += defaultLimit.getValue();
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
     * @see #getMaxClaimLimit(Object)
     */
    public abstract int getAdditionalMaxClaimLimit(@NotNull P player);

    /**
     * Sets this player's additional claim limit.
     *
     * @param player     The player to add or remove claim limit from.
     * @param claimLimit The new additional claim limit.
     */
    public abstract void setAdditionalMaxClaimLimit(@NotNull P player, int claimLimit);

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
        return hasAnyRelations(getUniqueId(player), terrain);
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

    /**
     * Whether the player is allowed to select in the specified coordinate.
     * <p>
     * The player is only allowed if:
     * <ul>
     *     <li>there are no terrains in the coordinate;</li>
     *     <li>the world terrain has {@link Flags#BUILD} as undefined or allowed;</li>
     *     <li>there are terrains and the player owns them;</li>
     *     <li>the player has 'terrainer.bypass.overlap'.</li>
     * </ul>
     *
     * @param player     The player to check if is allowed to select in the coordinate.
     * @param coordinate The coordinate to check for terrains.
     * @return If the player is allowed to select in the coordinate.
     */
    public boolean isAllowedToSelect(@NotNull P player, @NotNull WorldCoordinate coordinate) {
        if (hasPermission(player, "terrainer.bypass.overlap")) return true;

        for (Terrain terrain : TerrainManager.allTerrains()) {
            Boolean state;
            if (terrain instanceof WorldTerrain && (state = terrain.flags().getData(Flags.BUILD)) != null && !state) {
                continue;
            }
            if (!Objects.equals(terrain.owner(), player) && terrain.isWithin(coordinate)) {
                return false;
            }
        }

        return true;
    }

    protected abstract @NotNull WorldCoordinate location(@NotNull P player);

    private void addOverlapping(@NotNull List<Terrain> terrains, @NotNull P player) {
        if (terrains.isEmpty()) return;

        boolean overlapPermission = hasPermission(player, "terrainer.bypass.overlap");
        UUID uuid = getUniqueId(player);

        for (Terrain terrain : new ArrayList<>(terrains)) {
            if (!Objects.equals(terrain.owner(), uuid) && !overlapPermission) continue;
            for (Terrain t1 : TerrainManager.terrains(terrain.world())) {
                if (t1 != terrain && terrain.isOverlapping(t1) && (Objects.equals(t1.owner(), uuid) || overlapPermission)) {
                    terrains.add(t1);
                }
            }
        }
    }

    private void spawnAtY(@NotNull Coordinate min, @NotNull Coordinate max, @NotNull P player, int y, @NotNull ArrayList<Integer> ids) throws Throwable {
        // Left Bottom Corner
        ids.add(spawnMarker(player, min.x(), y, min.z()));
        // Left Upper Corner
        ids.add(spawnMarker(player, min.x(), y, max.z()));
        // Right Bottom Corner
        ids.add(spawnMarker(player, max.x(), y, min.z()));
        // Right Upper Corner
        ids.add(spawnMarker(player, max.x(), y, max.z()));

        if (max.x() - min.x() > 6 && max.z() - min.z() > 6) {
            // Left Bottom Corner
            ids.add(spawnMarker(player, min.x(), y, min.z() + 1));
            ids.add(spawnMarker(player, min.x() + 1, y, min.z()));
            // Left Upper Corner
            ids.add(spawnMarker(player, min.x(), y, max.z() - 1));
            ids.add(spawnMarker(player, min.x() + 1, y, max.z()));
            // Right Bottom Corner
            ids.add(spawnMarker(player, max.x(), y, min.z() + 1));
            ids.add(spawnMarker(player, max.x() - 1, y, min.z()));
            // Right Upper Corner
            ids.add(spawnMarker(player, max.x(), y, max.z() - 1));
            ids.add(spawnMarker(player, max.x() - 1, y, max.z()));
        }
    }

    private void spawnMarkersAtBorders(@Nullable Coordinate min, @Nullable Coordinate max, @NotNull P player, int y, boolean fromATerrain) {
        try {
            UUID uuid = getUniqueId(player);

            if (min != null && max != null) {
                Coordinate tempMin = min, tempMax = max;
                min = new Coordinate(Math.min(tempMin.x(), tempMax.x()), Math.min(tempMin.y(), tempMax.y()), Math.min(tempMin.z(), tempMax.z()));
                max = new Coordinate(Math.max(tempMin.x(), tempMax.x()), Math.max(tempMin.y(), tempMax.y()), Math.max(tempMin.z(), tempMax.z()));
                ArrayList<Integer> ids = markers.computeIfAbsent(uuid, k -> new ArrayList<>());

                if (min.y() <= Integer.MIN_VALUE || max.y() >= Integer.MAX_VALUE) {
                    // 2D terrain.
                    spawnAtY(min, max, player, y, ids);
                    return;
                }

                // 3D terrain.
                spawnAtY(min, max, player, (int) min.y(), ids);
                spawnAtY(min, max, player, (int) max.y(), ids);

                if (max.x() - min.x() > 6 && max.z() - min.z() > 6 && max.y() - min.y() > 6) {
                    ids.add(spawnMarker(player, min.x(), min.y() + 1, min.z()));
                    ids.add(spawnMarker(player, min.x(), min.y() + 1, max.z()));
                    ids.add(spawnMarker(player, max.x(), min.y() + 1, min.z()));
                    ids.add(spawnMarker(player, max.x(), min.y() + 1, max.z()));
                    ids.add(spawnMarker(player, min.x(), max.y() - 1, min.z()));
                    ids.add(spawnMarker(player, min.x(), max.y() - 1, max.z()));
                    ids.add(spawnMarker(player, max.x(), max.y() - 1, min.z()));
                    ids.add(spawnMarker(player, max.x(), max.y() - 1, max.z()));
                }
                return;
            }

            if (max != null) min = max;
            if (min == null) return;
            markers.computeIfAbsent(uuid, k -> new ArrayList<>()).add(spawnMarker(player, (int) min.x(), y, (int) min.z()));
        } catch (Throwable t) {
            Terrainer.logger().log("Failed to spawn marker entity for player " + getOwnerName(getUniqueId(player)), ConsoleLogger.Level.WARN);
            t.printStackTrace();
        }
    }

    public final void showMarkers(@NotNull P player) {
        showMarkers(player, (int) location(player).coordinate().y() - 1);
    }

    public void showMarkers(@NotNull P player, int y) {
        removeMarkers(player);
        WorldCoordinate location = location(player);
        var terrains = TerrainManager.getTerrainsAt(location);
        terrains.removeIf(t -> t instanceof WorldTerrain);
        addOverlapping(terrains, player);

        for (Terrain terrain : terrains) {
            int terrainY = y;

            if (terrain.maxDiagonal().y() < y) terrainY = (int) terrain.maxDiagonal().y();
            else if (terrain.minDiagonal().y() > y) terrainY = (int) terrain.minDiagonal().y();

            spawnMarkersAtBorders(terrain.minDiagonal(), terrain.maxDiagonal(), player, terrainY, true);
        }

        WorldCoordinate[] selections = TerrainManager.getSelection(getUniqueId(player));
        spawnMarkersAtBorders(selections[0] == null ? null : selections[0].coordinate(), selections[1] == null ? null : selections[1].coordinate(), player, y, false);
    }

    public void removeMarkers(@NotNull P player) {
        ArrayList<Integer> ids = markers.get(getUniqueId(player));
        if (ids == null) return;
        ids.forEach(id -> {
            try {
                killMarker(player, id);
            } catch (Throwable t) {
                Terrainer.logger().log("Failed to kill marker entity with ID " + id + " for player " + getOwnerName(getUniqueId(player)), ConsoleLogger.Level.WARN);
                t.printStackTrace();
            }
        });
    }

    /**
     * Send a packet to the player to kill the marker entity with provided ID.
     *
     * @param player The player to kill the marker entity.
     * @param id     The ID of the marker entity.
     */
    protected abstract void killMarker(@NotNull P player, int id) throws Throwable;

    /**
     * Send a packet to the player to spawn an entity with glow effect in the location.
     *
     * @param player The player to send the packet.
     * @param x      X coordinate.
     * @param y      Y coordinate.
     * @param z      Z coordinate.
     * @return The marker's entity ID.
     */
    protected abstract int spawnMarker(@NotNull P player, double x, double y, double z) throws Throwable;
}
