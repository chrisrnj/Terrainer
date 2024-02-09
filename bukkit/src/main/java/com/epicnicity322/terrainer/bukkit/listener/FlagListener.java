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

package com.epicnicity322.terrainer.bukkit.listener;

import com.epicnicity322.terrainer.bukkit.TerrainerPlugin;
import com.epicnicity322.terrainer.bukkit.event.flag.FlagSetEvent;
import com.epicnicity322.terrainer.bukkit.event.flag.FlagUnsetEvent;
import com.epicnicity322.terrainer.bukkit.event.flag.UserFlagSetEvent;
import com.epicnicity322.terrainer.bukkit.event.flag.UserFlagUnsetEvent;
import com.epicnicity322.terrainer.core.terrain.Flag;
import com.epicnicity322.terrainer.core.terrain.Flags;
import com.epicnicity322.terrainer.core.terrain.Terrain;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

/**
 * A listener of flag set and unset events. Used for applying effects when the {@link Flags#EFFECTS} is set/unset, and
 * for enforcing the flags {@link Flags#MODS_CAN_MANAGE_MODS} and {@link Flags#MODS_CAN_EDIT_FLAGS}.
 * <p>
 * Players with permission {@link Flag#editPermission()} + ".others" are always allowed to edit the flags in terrains
 * they don't own.
 */
public final class FlagListener implements Listener {
    @EventHandler(priority = EventPriority.LOWEST)
    public void onUserFlagSet(UserFlagSetEvent event) {
        Flag<?> flag = event.flag();

        if (flag != Flags.MODS_CAN_MANAGE_MODS && flag != Flags.MODS_CAN_EDIT_FLAGS) return;
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

        if (flag != Flags.MODS_CAN_MANAGE_MODS && flag != Flags.MODS_CAN_EDIT_FLAGS) return;
        if (event.sender().hasPermission(flag.editPermission() + ".others")) return;
        if (!(event.sender() instanceof Player player)) return;

        if (!player.getUniqueId().equals(event.terrain().owner())) {
            TerrainerPlugin.getLanguage().send(player, TerrainerPlugin.getLanguage().get("Flags.Error.Not Owner"));
            event.setCancelled(true);
        }
    }

    @SuppressWarnings({"unchecked", "deprecation"})
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onFlagSet(FlagSetEvent<?> event) {
        Flag<?> flag = event.flag();
        if (flag != Flags.EFFECTS) return;
        Terrain terrain = event.terrain();
        World world = Bukkit.getWorld(terrain.world());
        if (world == null) return;

        // Removing previous effects from players within the terrain.
        removeEffects(terrain, world);

        // Applying new effects to players within the terrain.
        Map<String, Integer> newEffects = (Map<String, Integer>) event.data();

        for (Player player : world.getPlayers()) {
            Location loc = player.getLocation();
            if (!terrain.isWithin(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ())) continue;
            newEffects.forEach((effect, power) -> {
                PotionEffectType type = PotionEffectType.getByName(effect);
                if (type == null) return;
                player.addPotionEffect(new PotionEffect(type, Integer.MAX_VALUE, power, false, false));
            });
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onFlagUnset(FlagUnsetEvent<?> event) {
        Flag<?> flag = event.flag();
        if (flag != Flags.EFFECTS) return;
        Terrain terrain = event.terrain();
        World world = Bukkit.getWorld(terrain.world());
        if (world == null) return;

        // Removing effects from players within the terrain.
        removeEffects(terrain, world);
    }

    @SuppressWarnings("deprecation")
    private void removeEffects(@NotNull Terrain terrain, @NotNull World world) {
        Map<String, Integer> previousEffects = terrain.flags().getData(Flags.EFFECTS);

        if (previousEffects != null) {
            for (Player player : world.getPlayers()) {
                Location loc = player.getLocation();
                if (!terrain.isWithin(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ())) continue;
                previousEffects.forEach((effect, power) -> {
                    PotionEffectType type = PotionEffectType.getByName(effect);
                    if (type == null) return;
                    player.removePotionEffect(type);
                });
            }
        }
    }
}
