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

package com.epicnicity322.terrainer.bukkit.hook.placeholderapi;

import com.epicnicity322.terrainer.bukkit.placeholder.*;
import com.epicnicity322.terrainer.bukkit.util.CommandUtil;
import com.epicnicity322.terrainer.core.TerrainerVersion;
import com.epicnicity322.terrainer.core.placeholder.formatter.PlaceholderFormatter;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class TerrainerPlaceholderExpansion extends PlaceholderExpansion {
    private static final @NotNull Map<String, PlaceholderFormatter<OfflinePlayer, Player>> placeholders = Stream.of(new AssociatedTerrainsPlaceholder(), new FlagDataPlaceholder(), new FreeBlockLimitPlaceholder(), new FreeClaimLimitPlaceholder(), new MaxBlockLimitPlaceholder(), new MaxClaimLimitPlaceholder(), new OwningTerrainsPlaceholder(), new RoleInTerrainPlaceholder(), new TerrainAreaPlaceholder(), new TerrainCreationDatePlaceholder(), new TerrainDescriptionPlaceholder(), new TerrainFlagsPlaceholder(), new TerrainMembersPlaceholder(), new TerrainModeratorsPlaceholder(), new TerrainNamePlaceholder(), new TerrainOwnerPlaceholder(), new TerrainPriorityPlaceholder(), new TerrainUUIDPlaceholder(), /*TODO: Add TopAssociated TopUsedBlocks TopUsedClaims placeholders*/ new UsedBlocksPlaceholder(), new UsedClaimsPlaceholder()).collect(Collectors.toMap(PlaceholderFormatter::name, value -> value));

    private static @Nullable OfflinePlayer player(@Nullable OfflinePlayer requester, @NotNull String params) {
        String namespace = "_player_";
        int namespaceIndex = params.lastIndexOf(namespace);
        if (namespaceIndex < 0) return requester;
        String playerID = params.substring(namespaceIndex + namespace.length());
        int endingUnderline = playerID.indexOf('_');
        if (endingUnderline > 0) playerID = playerID.substring(0, endingUnderline);

        try {
            // Trying UUID first.
            return Bukkit.getOfflinePlayer(UUID.fromString(playerID));
        } catch (IllegalArgumentException e) {
            playerID = playerID.replace('-', '_'); // Player names in the placeholder require underlines to be replaces with hyphens.
            OfflinePlayer player = CommandUtil.tryOffline(playerID);
            if (player == null) player = Bukkit.getPlayer(playerID);
            return player;
        }
    }

    @Override
    public final @NotNull String getIdentifier() {
        return "terrainer";
    }

    @Override
    public final @NotNull String getAuthor() {
        return "Epicnicity322";
    }

    @Override
    public final @NotNull String getVersion() {
        return TerrainerVersion.VERSION_STRING;
    }

    @Override
    public boolean persist() {
        return true;
    }

    @SuppressWarnings("deprecation")
    @Override
    public @Nullable String onRequest(@Nullable OfflinePlayer player, @NotNull String params) {
        String placeholderName = params;
        int underlineIndex = placeholderName.indexOf('_');
        if (underlineIndex > 0) placeholderName = placeholderName.substring(0, underlineIndex);

        PlaceholderFormatter<OfflinePlayer, Player> formatter = placeholders.get(placeholderName.toLowerCase(Locale.ROOT));
        if (formatter == null) return null;
        String value = formatter.formatPlaceholder(player(player, params), params);
        if (value == null) return null;
        return ChatColor.translateAlternateColorCodes('&', value);
    }
}
