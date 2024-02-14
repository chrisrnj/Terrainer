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

import com.epicnicity322.terrainer.bukkit.util.ToggleableListener;
import org.bukkit.Location;
import org.bukkit.entity.Mob;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.jetbrains.annotations.NotNull;

/**
 * A listener for exclusive for creature spawn event.
 */
public class CreatureSpawnListener extends ToggleableListener {
    private final @NotNull ProtectionsListener protectionsListener;

    public CreatureSpawnListener(@NotNull ProtectionsListener protectionsListener) {
        this.protectionsListener = protectionsListener;
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    public void onCreatureSpawn(CreatureSpawnEvent event) {
        if (!(event.getEntity() instanceof Mob)) return;
        Location loc = event.getLocation();

        switch (event.getSpawnReason()) {
            case BUILD_IRONGOLEM, BUILD_SNOWMAN, BUILD_WITHER, CUSTOM, DISPENSE_EGG, DUPLICATION, ENDER_PEARL, JOCKEY, MOUNT, NATURAL, NETHER_PORTAL, OCELOT_BABY, PATROL, RAID, REINFORCEMENTS, SILVERFISH_BLOCK, SLIME_SPLIT, SPAWNER_EGG, SPELL, TRAP, VILLAGE_DEFENSE, VILLAGE_INVASION -> {
                if (!protectionsListener.creatureSpawn(loc.getWorld().getUID(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ()))
                    event.setCancelled(true);
            }
            case SPAWNER -> {
                if (!protectionsListener.spawnerSpawn(loc.getWorld().getUID(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ()))
                    event.setCancelled(true);
            }
        }
    }
}
