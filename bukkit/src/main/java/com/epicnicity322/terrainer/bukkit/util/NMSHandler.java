package com.epicnicity322.terrainer.bukkit.util;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public interface NMSHandler {
    /**
     * Spawns a marker entity at the location.
     *
     * @param player The player to send the fake entity spawn packet to.
     * @param x      X coordinate.
     * @param y      Y coordinate.
     * @param z      Z coordinate.
     * @return The entity's ID.
     */
    int spawnMarkerEntity(@NotNull Player player, int x, int y, int z) throws Throwable;

    /**
     * Kills an entity with the ID.
     *
     * @param player   The player to send the entity remove packet to.
     * @param entityID The ID of the entity to remove.
     */
    void killEntity(@NotNull Player player, int entityID) throws Throwable;
}
