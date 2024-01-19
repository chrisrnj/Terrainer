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

package com.epicnicity322.terrainer.core.util;

import com.epicnicity322.epicpluginlib.core.lang.LanguageHolder;
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

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

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
    private static final @NotNull HashMap<UUID, Integer[]> markers = new HashMap<>();
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
            lang.send(receiver, lang.get("Create.Success").replace("<name>", terrain.name())
                    .replace("<used>", Long.toString(usedBlocks)).replace("<max>", Long.toString(maxBlocks)));
            return true;
        }
        return false;
    }

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

    /**
     * Sends a visual marker
     *
     * @param player The player to remove the marker.
     * @param first  If the first or second marker should be removed.
     */
    public final void removeMarker(@NotNull P player, boolean first) {
        UUID playerId = getUniqueId(player);
        Integer[] markersIDs = markers.get(playerId);
        if (markersIDs == null) return;
        Integer markerId = markersIDs[first ? 0 : 1];
        markersIDs[first ? 0 : 1] = null;
        if (markerId == null) return;

        try {
            killMarker(player, markerId);
        } catch (Throwable t) {
            Terrainer.logger().log("Could not kill marker with entity ID: " + markerId + " for player: " + getOwnerName(playerId));
            t.printStackTrace();
        }
        if (markersIDs[0] == null && markersIDs[1] == null) markers.remove(playerId);
    }

    /**
     * Sends a visual marker to the player using a fake entity with glow effect.
     *
     * @param player The player to show the marker to.
     * @param first  If this is the first or second selection.
     * @param x      X coordinate.
     * @param y      Y coordinate.
     * @param z      Z coordinate.
     */
    public final void showMarker(@NotNull P player, boolean first, int x, int y, int z) {
        if (!Configurations.CONFIG.getConfiguration().getBoolean("Markers.Enabled").orElse(false)) return;
        removeMarker(player, first);
        try {
            int marker = spawnMarker(player, x, y, z);
            markers.computeIfAbsent(getUniqueId(player), k -> new Integer[2])[first ? 0 : 1] = marker;
        } catch (Throwable t) {
            Terrainer.logger().log("Could not spawn marker at X:" + x + " Y:" + y + " Z:" + z + " for player: " + getOwnerName(getUniqueId(player)));
            t.printStackTrace();
        }
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
    protected abstract int spawnMarker(@NotNull P player, int x, int y, int z) throws Throwable;
}
