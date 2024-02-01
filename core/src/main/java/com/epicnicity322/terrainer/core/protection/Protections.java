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

package com.epicnicity322.terrainer.core.protection;

import com.epicnicity322.epicpluginlib.core.lang.LanguageHolder;
import com.epicnicity322.terrainer.core.terrain.Flag;
import com.epicnicity322.terrainer.core.terrain.Flags;
import com.epicnicity322.terrainer.core.terrain.Terrain;
import com.epicnicity322.terrainer.core.terrain.TerrainManager;
import com.epicnicity322.terrainer.core.util.PlayerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;

public abstract class Protections<P extends R, R, M, B, E> {
    private final @NotNull PlayerUtil<P, R> playerUtil;
    private final @NotNull LanguageHolder<?, R> lang;

    protected Protections(@NotNull PlayerUtil<P, R> playerUtil, @NotNull LanguageHolder<?, R> lang) {
        this.playerUtil = playerUtil;
        this.lang = lang;
    }

    protected abstract boolean isContainer(@NotNull M material);

    /**
     * Blocks that have a reaction when right-clicked with no/any item.
     *
     * @param material The material to test.
     * @return Whether this material is a block that once you interact, something happens.
     */
    protected abstract boolean isInteractable(@NotNull M material);

    /**
     * Blocks that can be placed on the ground.
     *
     * @param material The material to test.
     * @return Whether this material is a block that can be placed.
     */
    protected abstract boolean isBuildingItem(@Nullable M material);

    /**
     * Items that can make a fire when used.
     *
     * @param material The material to test.
     * @return Whether this material can make fire.
     */
    protected abstract boolean isLighter(@Nullable M material);

    protected abstract boolean isFire(@NotNull M material);

    protected abstract boolean isBoat(@NotNull E entity);

    protected abstract int x(@NotNull B block);

    protected abstract int y(@NotNull B block);

    protected abstract int z(@NotNull B block);

    protected abstract @NotNull Flag<Boolean> flagEntityPlaced(@NotNull E entity);

    protected abstract @NotNull Flag<Boolean> flagPhysicalInteraction(@NotNull M material);

    protected abstract @NotNull Flag<Boolean> flagBlockRightClickInteraction(@NotNull M material);

    protected abstract @NotNull Flag<Boolean> flagItemUseInteraction(@NotNull M block, @NotNull M hand);

    protected abstract @NotNull Flag<Boolean> flagEntityInteraction(@NotNull E entity, @NotNull M mainHand, @NotNull M offHand, boolean sneaking);

    public boolean handleProtection(@NotNull P player, @NotNull UUID world, double x, double y, double z, @NotNull Flag<Boolean> flag, boolean message) {
        if (playerUtil.hasPermission(player, flag.bypassPermission())) return true;
        List<Terrain> terrains = TerrainManager.terrains(world);

        for (int i = 0; i < terrains.size(); i++) {
            Terrain terrain = terrains.get(i);

            if (!terrain.isWithin(x, y, z)) continue;
            if (playerUtil.hasAnyRelations(player, terrain)) return true;

            Boolean state = terrain.flags().getData(flag);

            if (state != null) {
                // Terrain with the highest priority that has the flag set has been found.
                // There may be more terrains in the location with same priority, but the terrain that was found first wins.

                if (!state) {
                    // Flag set to false, do additional checks to terrains with same priority and return.
                    // If the player has relations to any of the terrains in the location with same priority, then allow.
                    state = checkPlayerRelationToRestOfTerrainsWithSamePriority(player, terrains, i + 1, terrain.priority(), x, y, z);
                    if (!state && message) lang.send(player, lang.get("Protections." + flag.id()));
                    return state;
                }
                break;
            }
        }

        return true;
    }

    public boolean handleProtection(@NotNull UUID world, double x, double y, double z, @NotNull Flag<Boolean> flag) {
        List<Terrain> terrains = TerrainManager.terrains(world);

        for (Terrain terrain : terrains) {
            if (!terrain.isWithin(x, y, z)) continue;
            Boolean state = terrain.flags().getData(flag);
            if (state != null) return state;
        }

        return true;
    }

    public boolean handleProtection(@NotNull P player, @NotNull UUID world, double x, double y, double z, @NotNull Flag<Boolean> flag2) {
        if (playerUtil.hasPermission(player, Flags.BUILD.bypassPermission()) && playerUtil.hasPermission(player, flag2.bypassPermission()))
            return true;
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
                Boolean state1 = terrain.flags().getData(Flags.BUILD);

                if (state1 != null) {
                    if (!state1) {
                        state1 = checkPlayerRelationToRestOfTerrainsWithSamePriority(player, terrains, i + 1, terrain.priority(), x, y, z);
                        if (!state1) lang.send(player, lang.get("Protections." + Flags.BUILD.id()));
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
                        if (!state2) lang.send(player, lang.get("Protections." + flag2.id()));
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

    public boolean handleBlockFromTo(@NotNull UUID world, double x, double y, double z, double fromX, double fromY, double fromZ, @NotNull Flag<Boolean> flagInside, @NotNull Flag<Boolean> flagOutside) {
        List<Terrain> terrains = TerrainManager.terrains(world);

        // Highest priority wins.
        boolean flagInsideFound = false;
        boolean flagOutsideFound = false;

        for (Terrain terrain : terrains) {
            if (!terrain.isWithin(x, y, z)) continue;

            if (!flagInsideFound) {
                Boolean state = terrain.flags().getData(flagInside);
                if (state != null) {
                    flagInsideFound = true;
                    // Always return false if flagInside is false. If flagInside is true and flagOutside was already tested, then return true.
                    if (!state) return false;
                    else if (flagOutsideFound) return true;
                }
            }
            if (!flagOutsideFound) {
                Boolean state = terrain.flags().getData(flagOutside);
                if (state != null) {
                    flagOutsideFound = true;
                    if (state) {
                        if (flagInsideFound) return true;
                    } else {
                        // flagOutside is false, return false only if the from coordinate is not within, otherwise continue checking for flagInside.
                        if (!terrain.isWithin(fromX, fromY, fromZ)) {
                            return false;
                        } else if (flagInsideFound) return true;
                    }
                }
            }
        }

        return true;
    }

    public boolean handleOutsideAction(@NotNull UUID world, double x, double y, double z, double fromX, double fromY, double fromZ) {
        List<Terrain> terrains = TerrainManager.terrains(world);

        for (Terrain terrain : terrains) {
            if (!terrain.isWithin(x, y, z)) continue;
            Boolean state = terrain.flags().getData(Flags.BUILD);
            if (state == null) continue;
            if (!state && !terrain.isWithin(fromX, fromY, fromZ)) return false;
            else break;
        }

        return true;
    }

    public boolean handleOutsideBlockProtection(@NotNull UUID world, double x, double y, double z, @NotNull List<B> blocks, boolean removeFromList, @NotNull Flag<Boolean> flagInside, @NotNull Flag<Boolean> flagOutside) {
        List<Terrain> terrains = TerrainManager.terrains(world);
        ArrayList<B> blocksToTest = new ArrayList<>(blocks);
        boolean flagInsideFound = false;

        for (Terrain terrain : terrains) {
            Boolean isSourceWithin = null;
            Boolean insideState = null;
            if (!flagInsideFound && (isSourceWithin = terrain.isWithin(x, y, z))) {
                // Checking if flag is allowed in terrain.
                insideState = terrain.flags().getData(flagInside);
                if (insideState != null) {
                    if (!insideState) return false;
                    // flagInside found set as true, continue looping to check each block.
                    flagInsideFound = true;
                }
            }

            Boolean outsideState = terrain.flags().getData(flagOutside);
            if (outsideState == null) continue;

            // Checking if each block is within the terrain.
            for (int i = 0; i < blocksToTest.size(); i++) {
                B block = blocksToTest.get(i);
                if (!terrain.isWithin(x(block), y(block), z(block))) continue;

                // Highest priority flag for this block found. Removing from the list of blocks.
                blocksToTest.remove(block);
                i--;

                // If source is outside terrain and flagInside or flagOutside is false, then return false/remove from list.
                if (!(isSourceWithin == null ? isSourceWithin = terrain.isWithin(x, y, z) : isSourceWithin)) {
                    if (!outsideState || (insideState == null ? Boolean.FALSE.equals(insideState = terrain.flags().getData(flagInside)) : !insideState)) {
                        if (removeFromList) {
                            blocks.remove(block);
                        } else {
                            return false;
                        }
                    }
                }
            }

            // Breaking if PISTONS was found true and all blocks had terrains with OUTSIDE_PISTONS true.
            if (flagInsideFound && blocksToTest.isEmpty()) break;
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

    protected boolean bucketFill(@NotNull UUID world, double x, double y, double z, @NotNull P player) {
        return handleProtection(player, world, x, y, z, Flags.BUILD, true);
    }

    protected boolean bucketEmpty(@NotNull UUID world, double x, double y, double z, @NotNull P player) {
        return handleProtection(player, world, x, y, z, Flags.BUILD, true);
    }

    protected boolean physicalInteract(@NotNull UUID world, double x, double y, double z, @NotNull P player, @NotNull M material) {
        return handleProtection(player, world, x, y, z, flagPhysicalInteraction(material), true);
    }

    protected boolean placeEntity(@NotNull UUID world, double x, double y, double z, @NotNull P player, @NotNull E entity) {
        return handleProtection(player, world, x, y, z, flagEntityPlaced(entity), true);
    }

    protected boolean entityInteraction(@NotNull UUID world, double x, double y, double z, @NotNull P player, @NotNull E entity, @NotNull M mainHand, @NotNull M offHand) {
        return handleProtection(player, world, x, y, z, flagEntityInteraction(entity, mainHand, offHand, playerUtil.isSneaking(player)), true);
    }

    protected boolean itemPickup(@NotNull UUID world, double x, double y, double z, @NotNull P player) {
        return handleProtection(player, world, x, y, z, Flags.ITEM_PICKUP, true);
    }

    protected boolean pickupArrow(@NotNull UUID world, double x, double y, double z, @NotNull P player, boolean flyAtPlayer) {
        return handleProtection(player, world, x, y, z, Flags.ITEM_PICKUP, flyAtPlayer);
    }

    protected boolean itemDrop(@NotNull UUID world, double x, double y, double z, @NotNull P player) {
        return handleProtection(player, world, x, y, z, Flags.ITEM_DROP, true);
    }

    protected boolean leafDecay(@NotNull UUID world, double x, double y, double z) {
        return handleProtection(world, x, y, z, Flags.LEAF_DECAY);
    }

    protected boolean creatureSpawn(@NotNull UUID world, double x, double y, double z) {
        return handleProtection(world, x, y, z, Flags.MOB_SPAWN);
    }

    protected boolean spawnerSpawn(@NotNull UUID world, double x, double y, double z) {
        return handleProtection(world, x, y, z, Flags.SPAWNERS);
    }

    protected boolean liquidFlow(@NotNull UUID world, double x, double y, double z, double fromX, double fromY, double fromZ) {
        return handleBlockFromTo(world, x, y, z, fromX, fromY, fromZ, Flags.LIQUID_FLOW, Flags.BUILD);
    }

    protected boolean doubleChestOpen(@NotNull UUID world, double x, double y, double z, double otherX, double otherY, double otherZ, @NotNull P player) {
        return handleProtection(player, world, x, y, z, Flags.CONTAINERS, true) && handleProtection(player, world, otherX, otherY, otherZ, Flags.CONTAINERS, true);
    }

    protected boolean containerOpen(@NotNull UUID world, double x, double y, double z, @NotNull P player) {
        return handleProtection(player, world, x, y, z, Flags.CONTAINERS, true);
    }

    protected boolean fallingBlockFall(@NotNull UUID world, double x, double y, double z, double fromX, double fromY, double fromZ) {
        return handleOutsideAction(world, x, y, z, fromX, fromY, fromZ);
    }

    protected boolean structureGrow(@NotNull UUID world, double x, double y, double z, @NotNull List<B> blocks) {
        return handleOutsideBlockProtection(world, x, y, z, blocks, true, Flags.PLANT_GROW, Flags.BUILD) && !blocks.isEmpty();
    }

    protected boolean spongeAbsorb(@NotNull UUID world, double x, double y, double z, @NotNull List<B> blocks) {
        return handleOutsideBlockProtection(world, x, y, z, blocks, true, Flags.SPONGES, Flags.BUILD) && !blocks.isEmpty();
    }

    protected boolean startFlight(@NotNull UUID world, double x, double y, double z, @NotNull P player) {
        return handleProtection(player, world, x, y, z, Flags.FLY, true);
    }

    protected boolean signChange(@NotNull UUID world, double x, double y, double z, @NotNull P player) {
        return handleProtection(player, world, x, y, z, Flags.SIGN_EDIT, true);
    }

    protected boolean frostWalk(@NotNull UUID world, double x, double y, double z, @NotNull P player) {
        return handleProtection(player, world, x, y, z, Flags.FROST_WALK, false);
    }

    protected boolean projectileLaunch(@NotNull UUID world, double x, double y, double z, @Nullable P player) {
        if (player != null) {
            return handleProtection(player, world, x, y, z, Flags.PROJECTILES, false);
        } else {
            return handleProtection(world, x, y, z, Flags.PROJECTILES);
        }
    }

    protected boolean glide(@NotNull UUID world, double x, double y, double z, @NotNull P player) {
        return handleProtection(player, world, x, y, z, Flags.GLIDE, true);
    }

    protected boolean mount(@NotNull UUID world, double x, double y, double z, @NotNull P player) {
        return handleProtection(player, world, x, y, z, Flags.ENTER_VEHICLES, false);
    }

    protected boolean vehicleDestroy(@NotNull UUID world, double x, double y, double z, @NotNull P player, @NotNull E vehicle) {
        return handleProtection(player, world, x, y, z, isBoat(vehicle) ? Flags.BUILD_BOATS : Flags.BUILD_MINECARTS, true);
    }

    protected boolean blockFertilize(@NotNull UUID world, double x, double y, double z, @NotNull List<B> blocks) {
        return handleOutsideBlockProtection(world, x, y, z, blocks, true, Flags.PLANT_GROW, Flags.BUILD) && !blocks.isEmpty();
    }

    protected boolean blockGrow(@NotNull UUID world, double x, double y, double z, double fromX, double fromY, double fromZ) {
        return handleBlockFromTo(world, x, y, z, fromX, fromY, fromZ, Flags.PLANT_GROW, Flags.BUILD);
    }

    protected boolean blockSpread(@NotNull UUID world, double x, double y, double z, double fromX, double fromY, double fromZ) {
        //TODO: FIRE_SPREAD flag
        return handleBlockFromTo(world, x, y, z, fromX, fromY, fromZ, Flags.BLOCK_SPREAD, Flags.BUILD);
    }

    protected boolean blockBurn(@NotNull UUID world, double x, double y, double z) {
        return handleProtection(world, x, y, z, Flags.FIRE_DAMAGE);
    }

    protected boolean blockBurn(@NotNull UUID world, double x, double y, double z, double fromX, double fromY, double fromZ) {
        return handleBlockFromTo(world, x, y, z, fromX, fromY, fromZ, Flags.FIRE_DAMAGE, Flags.BUILD);
    }

    // Prevent block break if BUILD is false. If it's a container, prevent if BUILD or CONTAINERS is false.
    protected boolean blockBreak(@NotNull UUID world, double x, double y, double z, @NotNull P player, @NotNull M material) {
        if (isContainer(material)) {
            return handleProtection(player, world, x, y, z, Flags.CONTAINERS);
        } else {
            return handleProtection(player, world, x, y, z, Flags.BUILD, true);
        }
    }

    protected boolean blockPlace(@NotNull UUID world, double x, double y, double z, @NotNull P player, @NotNull M material) {
        if (isFire(material)) {
            return handleProtection(player, world, x, y, z, Flags.LIGHTERS);
        } else {
            return handleProtection(player, world, x, y, z, Flags.BUILD, true);
        }
    }

    protected boolean pistonRetract(@NotNull UUID world, double x, double y, double z, @NotNull List<B> movedBlocks) {
        return handleOutsideBlockProtection(world, x, y, z, movedBlocks, false, Flags.PISTONS, Flags.OUTSIDE_PISTONS);
    }

    protected boolean pistonExtend(@NotNull UUID world, @NotNull Collection<B> movedBlocks, @NotNull Function<B, B> relative) {
        for (B block : movedBlocks) {
            B to = relative.apply(block);
            if (!handleBlockFromTo(world, x(to), y(to), z(to), x(block), y(block), z(block), Flags.PISTONS, Flags.OUTSIDE_PISTONS)) {
                return false;
            }
        }

        return true;
    }

    protected boolean rightClickInteract(@NotNull UUID world, double x, double y, double z, @NotNull P player, @NotNull M material, @Nullable M hand) {
        Flag<Boolean> flag;

        if (isInteractable(material)) {
            if (playerUtil.isSneaking(player) && isBuildingItem(hand)) {
                // Let block place/bucket fill event handle it.
                return true;
            } else {
                flag = flagBlockRightClickInteraction(material);
            }
        } else if (isBuildingItem(hand)) {
            // Let block place/bucket fill event handle it.
            return true;
        } else {
            if (hand == null) return true;
            flag = flagItemUseInteraction(material, hand);
        }

        return handleProtection(player, world, x, y, z, flag, true);
    }
}
