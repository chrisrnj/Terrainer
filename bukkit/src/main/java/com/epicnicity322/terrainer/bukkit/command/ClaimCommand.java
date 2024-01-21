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

package com.epicnicity322.terrainer.bukkit.command;

import com.epicnicity322.epicpluginlib.bukkit.command.Command;
import com.epicnicity322.epicpluginlib.bukkit.command.CommandRunnable;
import com.epicnicity322.epicpluginlib.bukkit.lang.MessageSender;
import com.epicnicity322.terrainer.bukkit.TerrainerPlugin;
import com.epicnicity322.terrainer.bukkit.event.terrain.UserCreateTerrainEvent;
import com.epicnicity322.terrainer.bukkit.util.BukkitPlayerUtil;
import com.epicnicity322.terrainer.bukkit.util.CommandUtil;
import com.epicnicity322.terrainer.core.Coordinate;
import com.epicnicity322.terrainer.core.WorldCoordinate;
import com.epicnicity322.terrainer.core.config.Configurations;
import com.epicnicity322.terrainer.core.terrain.Terrain;
import com.epicnicity322.terrainer.core.terrain.TerrainManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;
import java.util.UUID;

public final class ClaimCommand extends Command {
    @Override
    public @NotNull String getName() {
        return "claim";
    }

    @Override
    public @NotNull String getPermission() {
        return "terrainer.claim";
    }

    @Override
    protected @NotNull CommandRunnable getNoPermissionRunnable() {
        return CommandUtil.noPermissionRunnable();
    }

    @Override
    public void run(@NotNull String label, @NotNull CommandSender sender, @NotNull String[] args) {
        MessageSender lang = TerrainerPlugin.getLanguage();
        Player player = sender instanceof Player player1 ? player1 : null;
        UUID owner = player == null ? null : player.getUniqueId();
        WorldCoordinate[] selection = TerrainManager.getSelection(owner);

        if (selection[0] == null || selection[1] == null) {
            lang.send(sender, lang.get("Create.Error.Not Selected").replace("<label>", label));
            return;
        }
        if (selection[0].world() != selection[1].world()) {
            lang.send(sender, lang.get("Create.Error.Different Worlds").replace("<label>", label));
            return;
        }

        Coordinate first = selection[0].coordinate();
        Coordinate second = selection[1].coordinate();
        World world = Bukkit.getWorld(selection[0].world());

        if (world == null) {
            lang.send(sender, lang.get("Create.Error.World No Longer Exists"));
            return;
        }
        if (!sender.hasPermission("terrainer.world." + world.getName().toLowerCase(Locale.ROOT))) {
            lang.send(sender, lang.get("Select.Error.World"));
            return;
        }

        String name;

        if (args.length > 1) {
            name = ChatColor.translateAlternateColorCodes('&', CommandUtil.join(args, 1)).trim();
            String stripped = ChatColor.stripColor(name);
            int maxNameLength = Configurations.CONFIG.getConfiguration().getNumber("Max Name Length").orElse(26).intValue();
            if (stripped.isBlank() || stripped.length() > maxNameLength) {
                lang.send(sender, lang.get("Rename.Error.Name Length").replace("<max>", Integer.toString(maxNameLength)));
                return;
            }
        } else name = null;

        BukkitPlayerUtil util = TerrainerPlugin.getPlayerUtil();

        // Clearing selections.
        selection[0] = null;
        selection[1] = null;
        if (player != null) {
            util.removeMarker(player, true);
            util.removeMarker(player, false);
        }

        var terrain = new Terrain(first, second, world.getUID());
        if (name != null) terrain.setName(name);
        if (util.claimTerrain(player, terrain)) {
            var create = new UserCreateTerrainEvent(terrain, sender, false);
            Bukkit.getPluginManager().callEvent(create);
        }
    }
}
