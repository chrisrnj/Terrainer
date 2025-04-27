/*
 * Terrainer - A minecraft terrain claiming protection plugin.
 * Copyright (C) 2025 Christiano Rangel
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

package com.epicnicity322.terrainer.bukkit.listener;

import com.epicnicity322.terrainer.bukkit.TerrainerPlugin;
import com.epicnicity322.terrainer.bukkit.event.flag.FlagSetEvent;
import com.epicnicity322.terrainer.bukkit.event.flag.FlagUnsetEvent;
import com.epicnicity322.terrainer.bukkit.event.flag.UserFlagSetEvent;
import com.epicnicity322.terrainer.bukkit.event.flag.UserFlagUnsetEvent;
import com.epicnicity322.terrainer.core.flag.Flag;
import com.epicnicity322.terrainer.core.flag.Flags;
import com.epicnicity322.terrainer.core.terrain.Terrain;
import com.epicnicity322.terrainer.core.terrain.TerrainManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;

/**
 * A listener of flag set and unset events. Used for applying effects when the {@link Flags#EFFECTS} is set/unset, and
 * for enforcing the flags {@link Flags#MANAGE_MODERATORS} and {@link Flags#EDIT_FLAGS}.
 * <p>
 * Players with permission {@link Flag#editPermission()} + ".others" are always allowed to edit the flags in terrains
 * they don't own.
 */
public final class FlagListener implements Listener {
    private static void doEffectsFlagUpdate(@NotNull Terrain terrain, @Nullable UUID affectedMember, @Nullable Map<String, Integer> data) {
        UUID wUID = terrain.world();
        World world = Bukkit.getWorld(wUID);
        if (world == null) return;

        if (affectedMember == null) {
            for (Player player : world.getPlayers()) {
                playerEffectsUpdate(player, terrain, true, data);
            }
        } else {
            Player player = Bukkit.getPlayer(affectedMember);
            if (player == null || player.getWorld() != world) return;
            playerEffectsUpdate(player, terrain, false, data);
        }
    }

    private static void playerEffectsUpdate(@NotNull Player player, @NotNull Terrain changedTerrain, boolean generalFlag, @Nullable Map<String, Integer> newEffects) {
        Location loc = player.getLocation();
        int x = loc.getBlockX(), y = loc.getBlockY(), z = loc.getBlockZ();
        if (!changedTerrain.isWithin(x, y, z)) return;

        // Removing previous effects from player.
        TerrainManager.getMapFlagDataAt(Flags.EFFECTS, player.getUniqueId(), changedTerrain.world(), x, y, z, false, Collections.emptySet()).forEach((effect, level) -> TerrainerPlugin.getPlayerUtil().removeEffect(player, effect));

        // Getting the effects at the player's location and applying with the new flag value. This will ensure the player has the correct effects according to terrain priority.
        applyNewEffects(player, changedTerrain.world(), x, y, z, changedTerrain, generalFlag, newEffects);
    }

    private static void applyNewEffects(@NotNull Player player, @NotNull UUID world, int x, int y, int z, @NotNull Terrain changedTerrain, boolean generalFlag, @Nullable Map<String, Integer> newEffects) {
        Integer priorityFound = null;

        for (Terrain terrain : TerrainManager.terrainsAt(world, x, y, z)) {
            if (priorityFound != null && priorityFound != terrain.priority()) break;

            Map<String, Integer> map;

            if (generalFlag) {
                map = terrain.memberFlags().getData(player.getUniqueId(), Flags.EFFECTS);
                if (map == null) map = terrain == changedTerrain ? newEffects : terrain.flags().getData(Flags.EFFECTS);
            } else {
                map = terrain == changedTerrain ? newEffects : terrain.memberFlags().getData(player.getUniqueId(), Flags.EFFECTS);
                if (map == null) map = terrain.flags().getData(Flags.EFFECTS);
            }

            if (map == null) continue;
            if (priorityFound == null) priorityFound = terrain.priority();
            map.forEach((effect, level) -> TerrainerPlugin.getPlayerUtil().applyEffect(player, effect, level));
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onUserFlagSet(UserFlagSetEvent event) {
        Flag<?> flag = event.flag();

        if (flag != Flags.MANAGE_MODERATORS && flag != Flags.EDIT_FLAGS) return;
        if (event.sender().hasPermission(flag.editPermission() + ".others")) return;
        if (!(event.sender() instanceof Player player)) return;

        if (!player.getUniqueId().equals(event.terrain().owner())) {
            TerrainerPlugin.getLanguage().send(player, TerrainerPlugin.getLanguage().get("Flags.Error.Not Owner"));
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onUserFlagUnset(UserFlagUnsetEvent event) {
        Flag<?> flag = event.flag();

        if (flag != Flags.MANAGE_MODERATORS && flag != Flags.EDIT_FLAGS) return;
        if (event.sender().hasPermission(flag.editPermission() + ".others")) return;
        if (!(event.sender() instanceof Player player)) return;

        if (!player.getUniqueId().equals(event.terrain().owner())) {
            TerrainerPlugin.getLanguage().send(player, TerrainerPlugin.getLanguage().get("Flags.Error.Not Owner"));
            event.setCancelled(true);
        }
    }

    @SuppressWarnings("unchecked")
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onFlagSet(FlagSetEvent<?> event) {
        if (event.flag() == Flags.EFFECTS)
            doEffectsFlagUpdate(event.terrain(), event.affectedMember(), (Map<String, Integer>) event.data());
    }

    @SuppressWarnings("unchecked")
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onFlagUnset(FlagUnsetEvent<?> event) {
        if (event.flag() == Flags.EFFECTS)
            doEffectsFlagUpdate(event.terrain(), event.affectedMember(), (Map<String, Integer>) event.data());
    }
}
