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
import com.epicnicity322.terrainer.core.Coordinate;
import com.epicnicity322.terrainer.core.config.Configurations;
import com.epicnicity322.terrainer.core.terrain.Flag;
import com.epicnicity322.terrainer.core.terrain.Flags;
import com.epicnicity322.terrainer.core.terrain.Terrain;
import com.epicnicity322.terrainer.core.terrain.TerrainManager;
import com.epicnicity322.terrainer.core.util.PlayerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Function;
import java.util.regex.Pattern;

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
     * Items that can be placed on the ground. Buckets of water, armor stands, item frames and paintings are some of the
     * items that are not considered blocks that should be considered building items.
     *
     * @param material The material to test.
     * @return Whether this material is a block that can be placed.
     */
    protected abstract boolean isPlaceable(@Nullable M material);

    protected abstract boolean isFire(@NotNull M material);

    protected abstract boolean isBoat(@NotNull E entity);

    /**
     * Entities that are a lingering or splash potion projectile.
     *
     * @param entity The entity to test.
     * @return Whether this entity is a potion.
     */
    protected abstract boolean isPotion(@NotNull E entity);

    protected abstract boolean isPlayer(@NotNull E entity);

    protected abstract @NotNull UUID world(@NotNull B block);

    protected abstract int x(@NotNull B block);

    protected abstract int y(@NotNull B block);

    protected abstract int z(@NotNull B block);

    protected abstract @NotNull Flag<Boolean> flagEntityPlaced(@NotNull E entity);

    protected abstract @NotNull Flag<Boolean> flagPhysicalInteraction(@NotNull M material);

    protected abstract @NotNull Flag<Boolean> flagInteractableBlock(@NotNull M material, @Nullable M hand);

    protected abstract @NotNull Flag<Boolean> flagItemUse(@NotNull M block, @Nullable M hand);

    protected abstract @NotNull Flag<Boolean> flagEntityInteraction(@NotNull E entity, @NotNull M mainHand, @NotNull M offHand, boolean sneaking);

    /**
     * Finds the appropriate flag for an entity being hit. This method will never be called when the victim is a player.
     *
     * @param entity The victim being hit.
     * @return The flag for entity being damaged.
     */
    protected abstract @NotNull Flag<Boolean> flagEntityHit(@NotNull E entity);

    /**
     * Converts an Entity type to a Player type. If entity is a projectile then return shooter of the projectile.
     *
     * @param entity The entity to convert.
     * @return The entity as a player, null if entity is not a player.
     */
    protected abstract @Nullable P entityOrShooterToPlayer(@NotNull E entity);

    /**
     * Gets the location where the entity originated.
     *
     * @param entity The entity to get the origin of.
     * @return The coordinates of where this entity was spawned.
     */
    protected abstract @NotNull Coordinate entityOrigin(@NotNull E entity);

    public boolean handleProtection(@NotNull P player, @NotNull UUID world, int x, int y, int z, @NotNull Flag<Boolean> flag, boolean message) {
        if (playerUtil.hasPermission(player, flag.bypassPermission())) return true;
        boolean allow = TerrainManager.isFlagAllowedAt(flag, playerUtil.getUniqueId(player), world, x, y, z);
        if (!allow && message) lang.send(player, lang.get("Protections." + flag.id()));
        return allow;
    }

    public boolean handleProtection(@NotNull P player, @NotNull UUID world, int x, int y, int z, @NotNull Flag<Boolean> flag1, @NotNull Flag<Boolean> flag2) {
        if (playerUtil.hasPermission(player, flag1.bypassPermission()) && playerUtil.hasPermission(player, flag2.bypassPermission()))
            return true;
        Set<Terrain> terrains = TerrainManager.terrainsAt(world, x, y, z);
        boolean state1Found = false;
        boolean state2Found = false;
        Iterator<Terrain> terrainIterator = terrains.iterator();

        while (terrainIterator.hasNext()) {
            Terrain terrain = terrainIterator.next();

            if (playerUtil.hasAnyRelations(player, terrain)) return true;

            // We're looking for the states with the highest priorities. If either one is false, finish checking if the
            // player has relations for the priority, then return immediately.

            if (!state1Found) {
                Boolean state1 = terrain.memberFlags().getData(playerUtil.getUniqueId(player), flag1);
                if (state1 == null) state1 = terrain.flags().getData(flag1);

                if (state1 != null) {
                    if (!state1) {
                        state1 = checkPlayerRelationToRestOfTerrainsWithSamePriority(player, terrainIterator, terrain.priority());
                        if (!state1) lang.send(player, lang.get("Protections." + flag1.id()));
                        return state1;
                    }
                    state1Found = true;
                }
            }
            if (!state2Found) {
                Boolean state2 = terrain.memberFlags().getData(playerUtil.getUniqueId(player), flag2);
                if (state2 == null) state2 = terrain.flags().getData(flag2);

                if (state2 != null) {
                    if (!state2) {
                        state2 = checkPlayerRelationToRestOfTerrainsWithSamePriority(player, terrainIterator, terrain.priority());
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

    public boolean handleProtection(@NotNull UUID world, int x, int y, int z, @NotNull Flag<Boolean> flag) {
        Set<Terrain> terrains = TerrainManager.terrainsAt(world, x, y, z);

        for (Terrain terrain : terrains) {
            Boolean state = terrain.flags().getData(flag);
            if (state != null) return state;
        }

        return true;
    }

    public boolean handleProtection(@NotNull UUID world, int x, int y, int z, @NotNull Flag<Boolean> flag1, @NotNull Flag<Boolean> flag2) {
        Set<Terrain> terrains = TerrainManager.terrainsAt(world, x, y, z);

        boolean state1Found = false;
        boolean state2Found = false;

        for (Terrain terrain : terrains) {
            // We're looking for the states with the highest priorities. If either one is false, return false.

            if (!state1Found) {
                Boolean state1 = terrain.flags().getData(flag1);

                if (state1 != null) {
                    if (!state1) return false;
                    state1Found = true;
                }
            }
            if (!state2Found) {
                Boolean state2 = terrain.flags().getData(flag2);

                if (state2 != null) {
                    if (!state2) return false;
                    state2Found = true;
                }
            }

            // Both states were found and both were set to true.
            if (state1Found && state2Found) break;
        }
        return true;
    }

    public boolean handleBlockFromTo(@NotNull UUID world, int x, int y, int z, int fromX, int fromY, int fromZ, @NotNull Flag<Boolean> flagInside, @NotNull Flag<Boolean> flagOutside) {
        Set<Terrain> terrains = TerrainManager.terrainsAt(world, x, y, z);

        // Highest priority wins.
        boolean flagInsideFound = false;
        boolean flagOutsideFound = false;

        for (Terrain terrain : terrains) {
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

    public boolean handleOutsideAction(@NotNull UUID world, int x, int y, int z, int fromX, int fromY, int fromZ, @NotNull Flag<Boolean> flag) {
        Collection<Terrain> terrains = TerrainManager.terrainsAt(world, x, y, z);

        for (Terrain terrain : terrains) {
            Boolean state = terrain.flags().getData(flag);
            if (state == null) continue;
            if (!state && !terrain.isWithin(fromX, fromY, fromZ)) return false;
            else break;
        }

        return true;
    }

    public boolean handleOutsideBlockProtection(@NotNull UUID world, int x, int y, int z, @NotNull List<B> blocks, boolean removeFromList, @NotNull Flag<Boolean> flagInside, @NotNull Flag<Boolean> flagOutside) {
        // Checking the flag for the source block.
        boolean sourceBlock = handleProtection(world, x, y, z, flagInside);
        if (!sourceBlock) return false;

        // Checking terrains in each block, and if the source is coming out of a terrain and any of the flags are false, remove block from list.
        if (removeFromList) {
            blocks.removeIf(block -> {
                Terrain terrain = TerrainManager.highestPriorityTerrainWithFlagAt(flagOutside, world, x(block), y(block), z(block));
                // If source is coming from outside the terrain and flagInside or flagOutside is false, then remove this block.
                if (terrain != null && !terrain.isWithin(x, y, z)) {
                    return Boolean.FALSE.equals(terrain.flags().getData(flagOutside)) || (Boolean.FALSE.equals(terrain.flags().getData(flagInside)));
                }
                return false;
            });
        } else {
            for (B block : blocks) {
                Terrain terrain = TerrainManager.highestPriorityTerrainWithFlagAt(flagOutside, world, x(block), y(block), z(block));
                // If source is coming from outside the terrain and flagInside or flagOutside is false, then break and disallow.
                if (terrain != null && !terrain.isWithin(x, y, z)) {
                    if (Boolean.FALSE.equals(terrain.flags().getData(flagOutside)) || (Boolean.FALSE.equals(terrain.flags().getData(flagInside)))) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private boolean checkPlayerRelationToRestOfTerrainsWithSamePriority(@NotNull P player, @NotNull Iterator<Terrain> terrains, int priority) {
        // Continue looping through terrains with same priority to see if player has relations to any of them.
        while (terrains.hasNext()) {
            Terrain terrain = terrains.next();

            if (terrain.priority() != priority) break;
            if (playerUtil.hasAnyRelations(player, terrain)) return true;
        }

        return false;
    }

    public boolean bucketFill(@NotNull UUID world, int x, int y, int z, @NotNull P player) {
        return handleProtection(player, world, x, y, z, Flags.BUILD, true);
    }

    public boolean bucketEmpty(@NotNull UUID world, int x, int y, int z, @NotNull P player) {
        return handleProtection(player, world, x, y, z, Flags.BUILD, true);
    }

    public boolean physicalInteract(@NotNull UUID world, int x, int y, int z, @NotNull P player, @NotNull M material) {
        return handleProtection(player, world, x, y, z, flagPhysicalInteraction(material), true);
    }

    public boolean placeEntity(@NotNull UUID world, int x, int y, int z, @NotNull P player, @NotNull E entity) {
        return handleProtection(player, world, x, y, z, flagEntityPlaced(entity), true);
    }

    public boolean entityInteraction(@NotNull UUID world, int x, int y, int z, @NotNull P player, @NotNull E entity, @NotNull M mainHand, @NotNull M offHand) {
        return handleProtection(player, world, x, y, z, flagEntityInteraction(entity, mainHand, offHand, playerUtil.isSneaking(player)), true);
    }

    public boolean itemPickup(@NotNull UUID world, int x, int y, int z, @NotNull P player) {
        return handleProtection(player, world, x, y, z, Flags.ITEM_PICKUP, true);
    }

    public boolean pickupArrow(@NotNull UUID world, int x, int y, int z, @NotNull P player, boolean flyAtPlayer) {
        return handleProtection(player, world, x, y, z, Flags.ITEM_PICKUP, flyAtPlayer);
    }

    public boolean itemDrop(@NotNull UUID world, int x, int y, int z, @NotNull P player) {
        return handleProtection(player, world, x, y, z, Flags.ITEM_DROP, true);
    }

    public boolean leafDecay(@NotNull UUID world, int x, int y, int z) {
        return handleProtection(world, x, y, z, Flags.LEAF_DECAY);
    }

    public boolean creatureSpawn(@NotNull UUID world, int x, int y, int z) {
        return handleProtection(world, x, y, z, Flags.MOB_SPAWN);
    }

    public boolean spawnerSpawn(@NotNull UUID world, int x, int y, int z) {
        return handleProtection(world, x, y, z, Flags.SPAWNERS);
    }

    public boolean liquidFlow(@NotNull UUID world, int x, int y, int z, int fromX, int fromY, int fromZ) {
        return handleBlockFromTo(world, x, y, z, fromX, fromY, fromZ, Flags.LIQUID_FLOW, Flags.BUILD);
    }

    public boolean doubleChestOpen(@NotNull UUID world, int x, int y, int z, int otherX, int otherY, int otherZ, @NotNull P player) {
        return handleProtection(player, world, x, y, z, Flags.CONTAINERS, true) && handleProtection(player, world, otherX, otherY, otherZ, Flags.CONTAINERS, true);
    }

    public boolean containerOpen(@NotNull UUID world, int x, int y, int z, @NotNull P player) {
        return handleProtection(player, world, x, y, z, Flags.CONTAINERS, true);
    }

    public boolean fallingBlockFall(@NotNull UUID world, int x, int y, int z, int fromX, int fromY, int fromZ) {
        return handleOutsideAction(world, x, y, z, fromX, fromY, fromZ, Flags.BUILD);
    }

    public boolean dispenserDispense(@NotNull UUID world, int x, int y, int z, int fromX, int fromY, int fromZ) {
        return handleBlockFromTo(world, x, y, z, fromX, fromY, fromZ, Flags.DISPENSERS, Flags.OUTSIDE_DISPENSERS);
    }

    public boolean structureGrow(@NotNull UUID world, int x, int y, int z, @NotNull List<B> blocks) {
        return handleOutsideBlockProtection(world, x, y, z, blocks, true, Flags.PLANT_GROW, Flags.BUILD) && !blocks.isEmpty();
    }

    public boolean spongeAbsorb(@NotNull UUID world, int x, int y, int z, @NotNull List<B> blocks) {
        return handleOutsideBlockProtection(world, x, y, z, blocks, true, Flags.SPONGES, Flags.BUILD) && !blocks.isEmpty();
    }

    public boolean portalCreate(@NotNull P player, @NotNull List<B> blocks) {
        for (B block : blocks) {
            if (!handleProtection(player, world(block), x(block), y(block), z(block), Flags.BUILD, true)) return false;
        }
        return true;
    }

    public boolean signChange(@NotNull UUID world, int x, int y, int z, @NotNull P player) {
        return handleProtection(player, world, x, y, z, Flags.SIGN_EDIT, true);
    }

    public boolean frostWalk(@NotNull UUID world, int x, int y, int z, @NotNull P player) {
        return handleProtection(player, world, x, y, z, Flags.FROST_WALK, false);
    }

    public boolean projectileLaunch(@NotNull UUID world, int x, int y, int z, @NotNull P shooter, @NotNull E projectile) {
        return handleProtection(shooter, world, x, y, z, isPotion(projectile) ? Flags.POTIONS : Flags.PROJECTILES, true);
    }

    public boolean glide(@NotNull UUID world, int x, int y, int z, @NotNull P player) {
        return handleProtection(player, world, x, y, z, Flags.GLIDE, true);
    }

    public boolean mount(@NotNull UUID world, int x, int y, int z, @NotNull P player) {
        return handleProtection(player, world, x, y, z, Flags.ENTER_VEHICLES, true);
    }

    public boolean vehicleDestroy(@NotNull UUID world, int x, int y, int z, @NotNull P player, @NotNull E vehicle) {
        return handleProtection(player, world, x, y, z, isBoat(vehicle) ? Flags.BUILD_BOATS : Flags.BUILD_MINECARTS, true);
    }

    public boolean itemConsume(@NotNull UUID world, int x, int y, int z, @NotNull P player, boolean potion) {
        return handleProtection(player, world, x, y, z, potion ? Flags.POTIONS : Flags.EAT, true);
    }

    public boolean projectileHit(@NotNull UUID world, int x, int y, int z, int fromX, int fromY, int fromZ, @NotNull E projectile, @Nullable P shooter) {
        if (shooter == null)
            return handleOutsideAction(world, x, y, z, fromX, fromY, fromZ, isPotion(projectile) ? Flags.POTIONS : Flags.OUTSIDE_PROJECTILES);

        return handleProtection(shooter, world, x, y, z, isPotion(projectile) ? Flags.POTIONS : Flags.OUTSIDE_PROJECTILES, true);
    }

    public boolean playerDamage(@NotNull UUID world, int x, int y, int z) {
        return handleProtection(world, x, y, z, Flags.VULNERABILITY);
    }

    public boolean playerFoodLeaveDecrease(@NotNull UUID world, int x, int y, int z) {
        return handleProtection(world, x, y, z, Flags.VULNERABILITY);
    }

    public boolean playerBlockIgnite(@NotNull UUID world, int x, int y, int z, @NotNull P player) {
        return handleProtection(player, world, x, y, z, Flags.LIGHTERS, true);
    }

    public boolean blockIgnite(@NotNull UUID world, int x, int y, int z, int fromX, int fromY, int fromZ) {
        return handleBlockFromTo(world, x, y, z, fromX, fromY, fromZ, Flags.FIRE_SPREAD, Flags.BUILD);
    }

    public boolean entityCombustByEntity(@NotNull UUID world, int x, int y, int z, int fromX, int fromY, int fromZ, @NotNull E entity, @NotNull E combuster) {
        P player = entityOrShooterToPlayer(combuster);
        if (player != null) {
            return handleProtection(player, world, x, y, z, flagEntityHit(entity), true);
        } else {
            return handleBlockFromTo(world, x, y, z, fromX, fromY, fromZ, Flags.FIRE_DAMAGE, flagEntityHit(entity));
        }
    }

    public boolean cauldronNaturallyChangeLevel(@NotNull UUID world, int x, int y, int z) {
        return handleProtection(world, x, y, z, Flags.CAULDRONS_CHANGE_LEVEL_NATURALLY);
    }

    public boolean cauldronExtinguishEntity(@NotNull UUID world, int x, int y, int z, int fromX, int fromY, int fromZ, @NotNull E entity) {
        P player = entityOrShooterToPlayer(entity);
        if (player != null) {
            return handleProtection(player, world, x, y, z, Flags.CAULDRONS, true);
        } else {
            return handleOutsideAction(world, x, y, z, fromX, fromY, fromZ, Flags.CAULDRONS_CHANGE_LEVEL_NATURALLY);
        }
    }

    public boolean entityDamageByExplodingBlock(@NotNull UUID world, int x, int y, int z, int fromX, int fromY, int fromZ, @NotNull E victim) {
        return handleBlockFromTo(world, x, y, z, fromX, fromY, fromZ, Flags.EXPLOSION_DAMAGE, flagEntityHit(victim));
    }

    public boolean entityDamageByEntity(@NotNull UUID world, int x, int y, int z, @NotNull E victim, @NotNull E damager, boolean explosion) {
        if (isPlayer(victim)) {
            P damagerPlayer = entityOrShooterToPlayer(damager);

            if (damagerPlayer != null) {
                Set<Terrain> terrains = TerrainManager.terrains(world);

                for (Terrain terrain : terrains) {
                    if (!terrain.isWithin(x, y, z)) continue;
                    Boolean state = terrain.flags().getData(Flags.PVP);
                    if (state != null) {
                        if (!state) lang.send(damagerPlayer, lang.get("Protections." + Flags.PVP.id()));
                        return state;
                    }
                }
            } else {
                return handleProtection(world, x, y, z, Flags.VULNERABILITY);
            }
        }

        P player;
        Flag<Boolean> entityHitFlag = flagEntityHit(victim);

        if (explosion) {
            Coordinate origin = entityOrigin(damager);
            return handleBlockFromTo(world, x, y, z, (int) origin.x(), (int) origin.y(), (int) origin.z(), Flags.EXPLOSION_DAMAGE, entityHitFlag);
        } else if ((player = entityOrShooterToPlayer(damager)) != null) { // When it's a player hitting an entity.
            return handleProtection(player, world, x, y, z, entityHitFlag, true);
        } else { // Usually reached when an arrow is shot by a dispenser or other type of entity hits an entity.
            Coordinate origin = entityOrigin(damager);
            return handleOutsideAction(world, x, y, z, (int) origin.x(), (int) origin.y(), (int) origin.z(), entityHitFlag);
        }
    }

    public boolean entityExplode(@NotNull UUID world, int x, int y, int z, @NotNull List<B> blocks) {
        return handleOutsideBlockProtection(world, x, y, z, blocks, true, Flags.EXPLOSION_DAMAGE, Flags.BUILD);
    }

    /**
     * Bukkit does not provide the TNT that exploded a hanging entity. Because of this, hangings on this platform will
     * not check if the TNT is coming from outside the terrain.
     */
    public boolean explodeHanging(@NotNull UUID world, int x, int y, int z) {
        return handleProtection(world, x, y, z, Flags.BUILD, Flags.EXPLOSION_DAMAGE);
    }

    public boolean blockExplode(@NotNull UUID world, int x, int y, int z, @NotNull List<B> blocks) {
        return handleOutsideBlockProtection(world, x, y, z, blocks, true, Flags.EXPLOSION_DAMAGE, Flags.BUILD);
    }

    public boolean blockFertilize(@NotNull UUID world, int x, int y, int z, @NotNull List<B> blocks) {
        return handleOutsideBlockProtection(world, x, y, z, blocks, true, Flags.PLANT_GROW, Flags.BUILD) && !blocks.isEmpty();
    }

    public boolean blockGrow(@NotNull UUID world, int x, int y, int z, int fromX, int fromY, int fromZ) {
        return handleBlockFromTo(world, x, y, z, fromX, fromY, fromZ, Flags.PLANT_GROW, Flags.BUILD);
    }

    public boolean blockForm(@NotNull UUID world, int x, int y, int z) {
        return handleProtection(world, x, y, z, Flags.BLOCK_FORM);
    }

    public boolean blockSpread(@NotNull UUID world, int x, int y, int z, int fromX, int fromY, int fromZ, @NotNull M material) {
        return handleBlockFromTo(world, x, y, z, fromX, fromY, fromZ, isFire(material) ? Flags.FIRE_SPREAD : Flags.BLOCK_SPREAD, Flags.BUILD);
    }

    public boolean blockBurn(@NotNull UUID world, int x, int y, int z) {
        return handleProtection(world, x, y, z, Flags.FIRE_DAMAGE);
    }

    public boolean blockBurn(@NotNull UUID world, int x, int y, int z, int fromX, int fromY, int fromZ) {
        return handleBlockFromTo(world, x, y, z, fromX, fromY, fromZ, Flags.FIRE_DAMAGE, Flags.BUILD);
    }

    // Prevent block break if BUILD is false. If it's a container, prevent if BUILD or CONTAINERS is false.
    public boolean blockBreak(@NotNull UUID world, int x, int y, int z, @NotNull P player, @NotNull M material) {
        if (isContainer(material)) {
            return handleProtection(player, world, x, y, z, Flags.BUILD, Flags.CONTAINERS);
        } else {
            return handleProtection(player, world, x, y, z, Flags.BUILD, true);
        }
    }

    public boolean blockPlace(@NotNull UUID world, int x, int y, int z, @NotNull P player, @NotNull M material) {
        if (isFire(material)) {
            return handleProtection(player, world, x, y, z, Flags.BUILD, Flags.LIGHTERS);
        } else {
            return handleProtection(player, world, x, y, z, Flags.BUILD, true);
        }
    }

    public boolean pistonRetract(@NotNull UUID world, int x, int y, int z, @NotNull List<B> movedBlocks) {
        return handleOutsideBlockProtection(world, x, y, z, movedBlocks, false, Flags.PISTONS, Flags.OUTSIDE_PISTONS);
    }

    public boolean pistonExtend(@NotNull UUID world, @NotNull Collection<B> movedBlocks, @NotNull Function<B, B> relative) {
        for (B block : movedBlocks) {
            B to = relative.apply(block);
            if (!handleBlockFromTo(world, x(to), y(to), z(to), x(block), y(block), z(block), Flags.PISTONS, Flags.OUTSIDE_PISTONS)) {
                return false;
            }
        }

        return true;
    }

    public boolean rightClickInteract(@NotNull UUID world, int x, int y, int z, @NotNull P player, @NotNull M material, @Nullable M hand) {
        Flag<Boolean> flag;
        boolean message = true;

        if (isInteractable(material)) {
            if (playerUtil.isSneaking(player) && isPlaceable(hand)) {
                // Let block place/bucket fill event handle it.
                return true;
            } else {
                flag = flagInteractableBlock(material, hand);
            }
        } else if (isPlaceable(hand)) {
            // Let block place/bucket fill event handle it.
            return true;
        } else {
            if (hand == null) message = false;
            flag = flagItemUse(material, hand);
        }

        return handleProtection(player, world, x, y, z, flag, message);
    }

    public boolean startFlight(@NotNull UUID world, int x, int y, int z, @NotNull P player) {
        if (!handleProtection(player, world, x, y, z, Flags.FLY, true)) {
            if (playerUtil.canFly(player)) {
                // Setting tag on player to return flight when leaving the terrain.
                String flyPermission = Configurations.CONFIG.getConfiguration().getString("Fly Permission").orElse("essentials.fly");
                playerUtil.setResetFly(player, playerUtil.hasPermission(player, flyPermission));
            }
            return false;
        }
        return true;
    }

    public boolean command(@NotNull UUID world, int x, int y, int z, @NotNull P player, @NotNull String command) {
        if (playerUtil.hasPermission(player, Flags.COMMAND_BLACKLIST.bypassPermission())) return true;
        Set<Terrain> terrains = TerrainManager.terrainsAt(world, x, y, z);

        Integer priorityFound = null;
        Set<String> blockedCommands = null;

        // Getting list of blacklisted commands in location in terrains with same priority.
        for (Terrain terrain : terrains) {
            if (priorityFound != null && priorityFound != terrain.priority()) break;
            if (playerUtil.hasAnyRelations(player, terrain)) return true;

            // Get member specific flag first.
            Set<String> commands = terrain.memberFlags().getData(playerUtil.getUniqueId(player), Flags.COMMAND_BLACKLIST);
            if (commands == null) commands = terrain.flags().getData(Flags.COMMAND_BLACKLIST);

            if (commands == null) continue;
            if (priorityFound == null) priorityFound = terrain.priority();
            if (commands.isEmpty()) continue;
            if (blockedCommands == null) blockedCommands = new HashSet<>(commands);
            else blockedCommands.addAll(commands);
        }

        if (blockedCommands == null) return true;
        command = command.toLowerCase(Locale.ROOT);
        boolean whitelist = blockedCommands.remove("*");

        if (whitelist) {
            for (String cmd : blockedCommands) {
                if (command.matches(Pattern.quote(cmd.toLowerCase(Locale.ROOT)) + "\\b.*")) return true;
            }
            lang.send(player, lang.get("Protections." + Flags.COMMAND_BLACKLIST.id()));
            return false;
        } else {
            for (String cmd : blockedCommands) {
                if (command.matches(Pattern.quote(cmd.toLowerCase(Locale.ROOT)) + "\\b.*")) {
                    lang.send(player, lang.get("Protections." + Flags.COMMAND_BLACKLIST.id()));
                    return false;
                }
            }
            return true;
        }
    }

    // TODO: protection against hoppers/hopper minecarts
}
