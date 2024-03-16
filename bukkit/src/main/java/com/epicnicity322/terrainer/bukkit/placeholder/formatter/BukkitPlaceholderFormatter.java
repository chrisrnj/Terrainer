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

package com.epicnicity322.terrainer.bukkit.placeholder.formatter;

import com.epicnicity322.terrainer.bukkit.TerrainerPlugin;
import com.epicnicity322.terrainer.core.Coordinate;
import com.epicnicity322.terrainer.core.WorldCoordinate;
import com.epicnicity322.terrainer.core.placeholder.formatter.PlaceholderFormatter;
import com.epicnicity322.terrainer.core.util.PlayerUtil;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public abstract class BukkitPlaceholderFormatter implements PlaceholderFormatter<OfflinePlayer, Player> {
    @Override
    public boolean isOnline(@NotNull OfflinePlayer offlinePlayer) {
        return offlinePlayer.isOnline();
    }

    @Override
    public @NotNull UUID uuid(@NotNull OfflinePlayer offlinePlayer) {
        return offlinePlayer.getUniqueId();
    }

    @Override
    public @Nullable WorldCoordinate location(@NotNull OfflinePlayer player) {
        Location loc = player.getLocation();
        if (loc == null) return null;
        World world = loc.getWorld();
        if (world == null) return null;
        return new WorldCoordinate(world.getUID(), new Coordinate(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ()));
    }

    @Override
    public @NotNull PlayerUtil<Player, CommandSender> playerUtil() {
        return TerrainerPlugin.getPlayerUtil();
    }
}
