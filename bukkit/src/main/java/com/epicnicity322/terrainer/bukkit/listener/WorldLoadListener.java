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

import com.epicnicity322.terrainer.core.terrain.TerrainManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldLoadEvent;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.UUID;

public final class WorldLoadListener implements Listener {
    private final @NotNull ArrayList<UUID> alreadyLoadedWorlds;

    public WorldLoadListener(@NotNull ArrayList<UUID> alreadyLoadedWorlds) {
        this.alreadyLoadedWorlds = alreadyLoadedWorlds;
    }

    @EventHandler
    public void onWorldLoad(WorldLoadEvent event) {
        UUID world = event.getWorld().getUID();
        if (alreadyLoadedWorlds.contains(world)) return;
        TerrainManager.loadWorld(world, event.getWorld().getName());
    }
}
