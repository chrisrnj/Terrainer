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

package com.epicnicity322.terrainer.bukkit.command.impl;

import com.epicnicity322.epicpluginlib.bukkit.command.CommandRunnable;
import com.epicnicity322.epicpluginlib.bukkit.lang.MessageSender;
import com.epicnicity322.epicpluginlib.core.logger.ConsoleLogger;
import com.epicnicity322.terrainer.bukkit.TerrainerPlugin;
import com.epicnicity322.terrainer.bukkit.command.TerrainerCommand;
import com.epicnicity322.terrainer.bukkit.event.terrain.UserCreateTerrainEvent;
import com.epicnicity322.terrainer.bukkit.event.terrain.UserNameTerrainEvent;
import com.epicnicity322.terrainer.bukkit.util.CommandUtil;
import com.epicnicity322.terrainer.core.Terrainer;
import com.epicnicity322.terrainer.core.config.Configurations;
import com.epicnicity322.terrainer.core.event.terrain.IUserNameTerrainEvent;
import com.epicnicity322.terrainer.core.flag.Flag;
import com.epicnicity322.terrainer.core.flag.FlagTransformException;
import com.epicnicity322.terrainer.core.flag.Flags;
import com.epicnicity322.terrainer.core.location.Coordinate;
import com.epicnicity322.terrainer.core.location.WorldCoordinate;
import com.epicnicity322.terrainer.core.terrain.Terrain;
import com.epicnicity322.terrainer.core.terrain.TerrainManager;
import com.epicnicity322.terrainer.core.util.PlayerUtil;
import com.epicnicity322.yamlhandler.Configuration;
import com.epicnicity322.yamlhandler.ConfigurationSection;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Map;
import java.util.UUID;

/**
 * A command designed to create terrains that are protected against any threat.
 * This is useful for server buildings, such as the spawn.
 */
public final class DefineCommand extends TerrainerCommand {
    private static final @NotNull HashSet<FlagEntry<?>> defineFlags = new HashSet<>();

    @Override
    public @NotNull String getName() {
        return "define";
    }

    @Override
    public @NotNull String getPermission() {
        return "terrainer.define";
    }

    @Override
    protected @NotNull CommandRunnable getNoPermissionRunnable() {
        return CommandUtil.noPermissionRunnable();
    }

    @Override
    public void reloadCommand() {
        setAliases(TerrainerPlugin.getLanguage().get("Commands.Define.Command"));

        defineFlags.clear();
        Configuration flags = Configurations.FLAGS.getConfiguration();
        for (Map.Entry<String, Object> entry : flags.getNodes().entrySet()) {

            String flagId = entry.getKey();
            Flag<?> flag = Flags.matchFlag(flagId);
            if (flag == null || !(entry.getValue() instanceof ConfigurationSection flagSection)) continue;
            String defineValueSerialized = flagSection.getString("Define Value").orElse(null);
            if (defineValueSerialized == null) continue;
            addDefineFlagEntry(flag, defineValueSerialized);
        }
    }

    private <T> void addDefineFlagEntry(@NotNull Flag<T> flag, @NotNull String serializedValue) {
        try {
            defineFlags.add(new FlagEntry<>(flag, flag.transformer().apply(serializedValue)));
        } catch (FlagTransformException e) {
            Terrainer.logger().log("Unable to load 'Define Value' for flag '" + flag.id() + "' - " + e.getMessage(), ConsoleLogger.Level.WARN);
        } catch (Throwable ignored) {
        }
    }

    private <T> void putFlagInTerrain(@NotNull FlagEntry<T> flagEntry, @NotNull Terrain terrain) {
        terrain.flags().putFlag(flagEntry.flag(), flagEntry.value());
    }

    @SuppressWarnings("deprecation")
    @Override
    public void run(@NotNull String label, @NotNull CommandSender sender, @NotNull String[] args) {
        MessageSender lang = TerrainerPlugin.getLanguage();
        WorldCoordinate[] selection = PlayerUtil.selections(sender instanceof Player p ? p.getUniqueId() : null);

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
        UUID world = selection[0].world();
        String name;

        if (args.length > 1) {
            name = ChatColor.translateAlternateColorCodes('&', args[1]);
            String stripped = ChatColor.stripColor(name);
            if (stripped.isBlank()) name = null;
        } else name = null;

        var terrain = new Terrain(first, second, world);

        if (name != null) {
            String originalName = terrain.name();
            var event = new UserNameTerrainEvent(terrain, sender, originalName, name, IUserNameTerrainEvent.NameReason.CREATION);
            Bukkit.getPluginManager().callEvent(event);
            if (!event.isCancelled()) terrain.setName(event.newName());
        }

        // Setting strong protection flags.
        defineFlags.forEach(flagEntry -> putFlagInTerrain(flagEntry, terrain));

        if (TerrainManager.add(terrain)) {
            lang.send(sender, lang.get("Create.Define").replace("<terrain>", terrain.name()));
            var create = new UserCreateTerrainEvent(terrain, sender, true);
            Bukkit.getPluginManager().callEvent(create);

            if (sender instanceof Player player)
                TerrainerPlugin.getPlayerUtil().updateSelectionMarkersToTerrainMarkers(player);
        } else {
            if (sender instanceof Player player) TerrainerPlugin.getPlayerUtil().removeMarkers(player);
        }

        // Clearing selections.
        selection[0] = null;
        selection[1] = null;
    }

    private record FlagEntry<T>(@NotNull Flag<T> flag, @NotNull T value) {
    }
}
