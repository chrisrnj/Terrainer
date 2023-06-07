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

package com.epicnicity322.terrainer.bukkit.util;

import com.epicnicity322.terrainer.bukkit.TerrainerPlugin;
import com.epicnicity322.terrainer.core.util.PlayerUtil;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public final class BukkitPlayerUtil extends PlayerUtil<Player, CommandSender> {
    private final @NotNull NamespacedKey blockLimitKey;
    private final @NotNull NamespacedKey claimLimitKey;

    public BukkitPlayerUtil(@NotNull TerrainerPlugin plugin) {
        super(TerrainerPlugin.getLanguage());
        blockLimitKey = new NamespacedKey(plugin, "block-limit");
        claimLimitKey = new NamespacedKey(plugin, "claim-limit");
    }

    @Override
    protected boolean hasPermission(@NotNull Player player, @NotNull String permission) {
        return player.hasPermission(permission);
    }

    @Override
    public @NotNull String getOwnerName(@Nullable UUID uuid) {
        if (uuid == null) {
            return TerrainerPlugin.getLanguage().get("Target.Console");
        } else {
            String name = Bukkit.getOfflinePlayer(uuid).getName();
            return name == null ? uuid.toString() : name;
        }
    }

    @Override
    protected @NotNull UUID getUniqueId(@NotNull Player player) {
        return player.getUniqueId();
    }

    @Override
    protected @NotNull CommandSender getConsoleRecipient() {
        return Bukkit.getConsoleSender();
    }

    @Override
    public long getAdditionalMaxBlockLimit(@NotNull Player player) {
        return player.getPersistentDataContainer().getOrDefault(blockLimitKey, PersistentDataType.LONG, 0L);
    }

    @Override
    public void setAdditionalMaxBlockLimit(@NotNull Player player, long blockLimit) {
        if (blockLimit <= 0) {
            player.getPersistentDataContainer().remove(blockLimitKey);
        } else {
            player.getPersistentDataContainer().set(blockLimitKey, PersistentDataType.LONG, blockLimit);
        }
    }

    @Override
    public int getAdditionalMaxClaimLimit(@NotNull Player player) {
        return player.getPersistentDataContainer().getOrDefault(claimLimitKey, PersistentDataType.INTEGER, 0);
    }

    @Override
    public void setAdditionalMaxClaimLimit(@NotNull Player player, int claimLimit) {
        if (claimLimit <= 0) {
            player.getPersistentDataContainer().remove(claimLimitKey);
        } else {
            player.getPersistentDataContainer().set(claimLimitKey, PersistentDataType.INTEGER, claimLimit);
        }
    }

    //TODO:
    @Override
    protected void killMarker(@NotNull Player player, int id) {

    }

    @Override
    protected int spawnMarker(@NotNull Player player, int x, int y, int z) {
        return 0;
    }
}
