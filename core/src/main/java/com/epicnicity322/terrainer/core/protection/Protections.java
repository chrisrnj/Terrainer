package com.epicnicity322.terrainer.core.protection;

import com.epicnicity322.epicpluginlib.core.lang.LanguageHolder;
import com.epicnicity322.terrainer.core.terrain.Flag;
import com.epicnicity322.terrainer.core.terrain.Flags;
import com.epicnicity322.terrainer.core.terrain.Terrain;
import com.epicnicity322.terrainer.core.terrain.TerrainManager;
import com.epicnicity322.terrainer.core.util.PlayerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

public abstract class Protections<P extends R, R, M> {
    private final @NotNull PlayerUtil<P, R> playerUtil;
    private final @NotNull LanguageHolder<?, R> lang;

    protected Protections(@NotNull PlayerUtil<P, R> playerUtil, @NotNull LanguageHolder<?, R> lang) {
        this.playerUtil = playerUtil;
        this.lang = lang;
    }

    protected abstract boolean isContainer(@NotNull M material);

    private boolean handleProtection(@NotNull P player, @NotNull UUID world, double x, double y, double z, @NotNull Flag<Boolean> flag, @Nullable String message) {
        List<Terrain> terrains = TerrainManager.terrains(world);

        for (int i = 0; i < terrains.size(); i++) {
            Terrain terrain = terrains.get(i);

            if (!terrain.isWithin(x, y, z)) continue;
            if (playerUtil.hasAnyRelations(player, terrain)) return true;

            Boolean state = terrain.flags().getData(flag);

            if (state != null) {
                // Terrain with highest priority that has the flag set has been found.
                // There may be more terrains in the location with same priority, but the terrain that was found first wins.

                if (!state) {
                    // Flag set to false, do additional checks to terrains with same priority and return.
                    // If the player has relations to any of the terrains in the location with same priority, then allow.
                    state = checkPlayerRelationToRestOfTerrainsWithSamePriority(player, terrains, i + 1, terrain.priority(), x, y, z);
                    if (!state && message != null) lang.send(player, lang.get(message));
                    return state;
                }
                break;
            }
        }

        return true;
    }

    private boolean handleProtection(@NotNull P player, @NotNull UUID world, double x, double y, double z, @NotNull Flag<Boolean> flag, @NotNull Flag<Boolean> flag2, @Nullable String message) {
        List<Terrain> terrains = TerrainManager.terrains(world);
        boolean state1Found = false;
        boolean state2Found = false;

        for (int i = 0; i < terrains.size(); i++) {
            Terrain terrain = terrains.get(i);

            if (!terrain.isWithin(x, y, z)) continue;
            if (playerUtil.hasAnyRelations(player, terrain)) return true;

            // We're looking for the states with the highest priorities. If either one is false, finish checking if the
            // player has relations for the priority, then return immediately.

            if (!state1Found) {
                Boolean state1 = terrain.flags().getData(flag);

                if (state1 != null) {
                    if (!state1) {
                        state1 = checkPlayerRelationToRestOfTerrainsWithSamePriority(player, terrains, i + 1, terrain.priority(), x, y, z);
                        if (!state1 && message != null) lang.send(player, lang.get(message));
                        return state1;
                    }
                    state1Found = true;
                }
            }
            if (!state2Found) {
                Boolean state2 = terrain.flags().getData(flag2);

                if (state2 != null) {
                    if (!state2) {
                        state2 = checkPlayerRelationToRestOfTerrainsWithSamePriority(player, terrains, i + 1, terrain.priority(), x, y, z);
                        if (!state2 && message != null) lang.send(player, lang.get(message));
                        return state2;
                    }
                    state2Found = true;
                }
            }

            // Both states were found and both were set to true.
            if (state1Found && state2Found) break;
        }

        return true;
    }

    private boolean checkPlayerRelationToRestOfTerrainsWithSamePriority(@NotNull P player, @NotNull List<Terrain> terrains, int startingIndex, int priority, double x, double y, double z) {
        // Continue looping through terrains with same priority to see if player has relations to any of them.
        for (int i = startingIndex; i < terrains.size(); i++) {
            Terrain terrain = terrains.get(i);

            if (terrain.priority() != priority) break;
            if (!terrain.isWithin(x, y, z)) continue;
            if (playerUtil.hasAnyRelations(player, terrain)) return true;
        }

        return false;
    }

    // Prevent block break if BUILD is false. If it's a container, prevent if BUILD or CONTAINERS is false.
    public boolean blockBreak(@NotNull UUID world, double x, double y, double z, @NotNull P player, @NotNull M material) {
        if (isContainer(material)) {
            if (playerUtil.hasPermission(player, "terrainer.bypass.build") && playerUtil.hasPermission(player, "terrainer.bypass.containers"))
                return true;
            return handleProtection(player, world, x, y, z, Flags.BUILD, Flags.CONTAINERS, "Protections.Containers");
        } else {
            if (playerUtil.hasPermission(player, "terrainer.bypass.build")) return true;
            return handleProtection(player, world, x, y, z, Flags.BUILD, "Protections.Build");
        }
    }

    public boolean blockPlace(@NotNull UUID world, double x, double y, double z, @NotNull P player) {
        if (playerUtil.hasPermission(player, "terrainer.bypass.build")) return true;
        return handleProtection(player, world, x, y, z, Flags.BUILD, "Protections.Build");
    }

    public boolean bucketFill(@NotNull UUID world, double x, double y, double z, @NotNull P player) {
        if (playerUtil.hasPermission(player, "terrainer.bypass.build")) return true;
        return handleProtection(player, world, x, y, z, Flags.BUILD, "Protections.Build");
    }

    // Prevent interactions of:
    // Pressure Plates if PRESSURE_PLATES is false.
    // Farmland if FARMLAND_TRAMPLE is false.
    // Levers and buttons if BUTTONS is false.
    // Doors, gates, trapdoors if DOORS is false.
    // Signs if SIGN_CLICK is false.
    // Containers if CONTAINERS is false.
    // Double chests if any of the ends are within a terrain and CONTAINERS is false.
    // Any non interactable block if the item in hand is a building block and BUILD is false.
    // Any other interactable block if the item in hand is not a building block and INTERACTIONS is false.
    public boolean interact(@NotNull UUID world, double x, double y, double z, @NotNull P player, @Nullable M material) {
        return true;
    }
}
