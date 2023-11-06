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

import com.epicnicity322.epicpluginlib.bukkit.command.CommandRunnable;
import com.epicnicity322.epicpluginlib.bukkit.lang.MessageSender;
import com.epicnicity322.epicpluginlib.bukkit.reflection.ReflectionUtil;
import com.epicnicity322.terrainer.bukkit.TerrainerPlugin;
import com.epicnicity322.terrainer.core.terrain.Terrain;
import com.epicnicity322.terrainer.core.terrain.TerrainManager;
import com.epicnicity322.terrainer.core.terrain.WorldTerrain;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Supplier;

public class CommandUtil {
    private static final @NotNull CommandRunnable noPermissionRunnable = (label, sender, args) -> TerrainerPlugin.getLanguage().send(sender, TerrainerPlugin.getLanguage().get("General.No Permission"));
    private static final boolean hasCachedOfflinePlayers = ReflectionUtil.getMethod(Bukkit.class, "getOfflinePlayerIfCached", String.class) != null;

    private CommandUtil() {
    }

    public static @NotNull CommandRunnable noPermissionRunnable() {
        return noPermissionRunnable;
    }

    /**
     * This method helps find a {@link Terrain} based on the command arguments passed to it. If the command includes "--t"
     * followed by the name of a terrain, that terrain with matching name will be returned, and any other preceding
     * arguments will also be returned. If the "--t" argument is not specified, the terrain at the sender's location is
     * inferred. If a terrain with the given name or {@link UUID} is not found, the method will suggest using a more
     * precise command to specify the terrain.
     *
     * @param permissionOthers The permission that allows the sender to find other people's terrains.
     * @param allowModerators  Allows moderators to find the terrain.
     * @param label            The command label.
     * @param sender           The sender of the command.
     * @param args             The arguments for the command
     * @return A {@link CommandArguments} object which includes both the arguments and the terrain found, or null if an
     * error occurs and a response has already been sent to the {@link CommandSender}.
     */
    public static @Nullable CommandArguments findTerrain(@NotNull String permissionOthers, boolean allowModerators, @NotNull String label, @NotNull CommandSender sender, @NotNull String @NotNull [] args) {
        MessageSender lang = TerrainerPlugin.getLanguage();
        StringBuilder terrainNameBuilder = new StringBuilder();
        boolean join = false;
        ArrayList<String> preceding = new ArrayList<>();
        StringBuilder exampleSyntax = new StringBuilder();

        for (int i = 1; i < args.length; ++i) {
            String s = args[i];
            if (join) {
                terrainNameBuilder.append(s);
                if (i != (args.length - 1)) terrainNameBuilder.append(" ");
                continue;
            }
            if (s.equals("--t")) {
                join = true;
            } else {
                preceding.add(s);
                exampleSyntax.append(s).append(" ");
            }
        }

        String terrainName = terrainNameBuilder.toString();
        Terrain terrain = null;

        if (terrainName.isEmpty()) {
            exampleSyntax.append("--t ").append(lang.get("Invalid Arguments.Terrain"));
            if (!(sender instanceof Player player)) {
                lang.send(sender, lang.get("Invalid Arguments.Error").replace("<label>", label).replace("<label2>", args[0]).replace("<args>", exampleSyntax));
                return null;
            }

            Location loc = player.getLocation();
            int x = loc.getBlockX();
            int y = loc.getBlockY();
            int z = loc.getBlockZ();

            for (Terrain t : TerrainManager.allTerrains()) {
                if (t instanceof WorldTerrain || !t.isWithin(x, y, z)) continue;
                if (terrain != null) {
                    lang.send(sender, lang.get("Matcher.Location.Multiple").replace("<label>", label).replace("<args>", args[0] + " " + exampleSyntax));
                    return null;
                } else {
                    terrain = t;
                }
            }

            if (terrain == null) {
                lang.send(sender, lang.get("Matcher.Location.Not Found").replace("<label>", label).replace("<args>", args[0] + " " + exampleSyntax));
                return null;
            }
        } else {
            UUID id = null;
            try {
                id = UUID.fromString(terrainName);
            } catch (IllegalArgumentException ignored) {
            }
            if (id != null) {
                terrain = TerrainManager.getTerrainByID(id);
            } else {
                for (Terrain t : TerrainManager.allTerrains()) {
                    if (!t.name().equals(terrainName)) continue;
                    if (terrain != null) {
                        lang.send(sender, lang.get("Matcher.Name.Multiple"));
                        return null;
                    } else {
                        terrain = t;
                    }
                }
            }

            if (terrain == null) {
                lang.send(sender, lang.get("Matcher.Name.Not Found"));
                return null;
            }
        }

        if (!sender.hasPermission(permissionOthers)) {
            UUID id = sender instanceof Player player ? player.getUniqueId() : null;
            if (!Objects.equals(terrain.owner(), id) && (!allowModerators || !terrain.moderators().view().contains(id))) {
                lang.send(sender, lang.get("Matcher.No Permission"));
                return null;
            }
        }

        return new CommandArguments(preceding.toArray(new String[0]), terrain);
    }

    public static @Nullable TargetResponse target(int targetIndex, @Nullable String permissionOthers, @NotNull CommandSender sender, @NotNull String[] args) {
        MessageSender lang = TerrainerPlugin.getLanguage();

        if (args.length > targetIndex) {
            if (permissionOthers != null && !sender.hasPermission(permissionOthers) &&
                    !args[targetIndex].equalsIgnoreCase(sender.getName()) && !args[targetIndex].equalsIgnoreCase("me")) {
                lang.send(sender, lang.get("General.No Permission Others"));
                return null;
            }
            switch (args[targetIndex].toLowerCase(Locale.ROOT)) {
                case "*" -> {
                    return TargetResponse.ALL;
                }
                case "me" -> {
                    if (sender instanceof Player player) {
                        return new TargetResponse(player.getUniqueId(), () -> lang.get("Target.You"));
                    } else {
                        return TargetResponse.CONSOLE;
                    }
                }
                case "null" -> {
                    return TargetResponse.CONSOLE;
                }
                default -> {
                    OfflinePlayer player = Bukkit.getPlayer(args[targetIndex]);
                    if (player == null) player = CommandUtil.tryOffline(args[targetIndex]);
                    if (player == null) {
                        try {
                            player = Bukkit.getOfflinePlayer(UUID.fromString(args[targetIndex]));
                        } catch (IllegalArgumentException ignored) {
                        }
                    }
                    if (player == null) {
                        lang.send(sender, lang.get("General.Player Not Found").replace("<value>", args[targetIndex]));
                        return null;
                    } else {
                        String name = player.getName() == null ? args[targetIndex] : player.getName();
                        return new TargetResponse(player.getUniqueId(), () -> name);
                    }
                }
            }
        } else {
            if (sender instanceof Player player) {
                return new TargetResponse(player.getUniqueId(), () -> lang.get("Target.You"));
            } else {
                return TargetResponse.CONSOLE;
            }
        }
    }

    /**
     * Tries to get the offline player. This uses the Paper method {@link Bukkit#getOfflinePlayerIfCached(String)}, so if
     * the server is not a variation of Paper, null is always returned.
     *
     * @param name The name of the player to try and get.
     * @return The player matching the given name.
     */
    public static @Nullable OfflinePlayer tryOffline(@NotNull String name) {
        if (hasCachedOfflinePlayers) {
            return Bukkit.getOfflinePlayerIfCached(name);
        } else {
            return null;
        }
    }

    public static @NotNull String join(@NotNull String[] args, int start) {
        StringBuilder name = new StringBuilder();
        for (int i = start; i < args.length; ++i) {
            name.append(args[i]);
            if (i != (args.length - 1)) name.append(" ");
        }
        return name.toString();
    }

    /**
     * Adds the "--t {@literal <terrain>}" completion to the list of completions.
     *
     * @param completions      The list of completions.
     * @param permissionOthers The permission that allows the sender to find other people's terrains.
     * @param allowModerators  Allows moderators to find the terrain.
     * @param sender           The person who is tab completing.
     * @param args             The arguments of the tab completion.
     */
    //TODO
    public static void addTerrainTabCompletion(@NotNull List<String> completions, @NotNull String permissionOthers, boolean allowModerators, @NotNull CommandSender sender, @NotNull String[] args) {
        String current = args[args.length - 1];

        if (args.length > 1 && args[args.length - 2].equals("--t")) {
            return;
        }
    }

    public record CommandArguments(@NotNull String[] preceding, @NotNull Terrain terrain) {
    }

    public record TargetResponse(@NotNull UUID id, @NotNull Supplier<String> who) {
        public static final @NotNull TargetResponse ALL = new TargetResponse(UUID.randomUUID(), () -> TerrainerPlugin.getLanguage().get("Target.Everyone"));
        public static final @NotNull TargetResponse CONSOLE = new TargetResponse(UUID.randomUUID(), () -> TerrainerPlugin.getLanguage().get("Target.Console"));
    }
}
