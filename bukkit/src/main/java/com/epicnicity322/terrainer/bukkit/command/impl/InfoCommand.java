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
import com.epicnicity322.epicpluginlib.core.EpicPluginLib;
import com.epicnicity322.terrainer.bukkit.TerrainerPlugin;
import com.epicnicity322.terrainer.bukkit.command.TerrainerCommand;
import com.epicnicity322.terrainer.bukkit.util.BukkitPlayerUtil;
import com.epicnicity322.terrainer.bukkit.util.CommandUtil;
import com.epicnicity322.terrainer.core.location.Coordinate;
import com.epicnicity322.terrainer.core.terrain.Terrain;
import com.epicnicity322.terrainer.core.terrain.TerrainManager;
import com.epicnicity322.terrainer.core.terrain.WorldTerrain;
import com.epicnicity322.terrainer.core.util.TerrainerUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.time.format.DateTimeFormatter;
import java.util.*;

public final class InfoCommand extends TerrainerCommand {
    private final @NotNull BordersCommand bordersCommand;

    public InfoCommand(@NotNull BordersCommand bordersCommand) {
        this.bordersCommand = bordersCommand;
    }

    @Override
    public @NotNull String getName() {
        return "info";
    }

    @Override
    public @NotNull String getPermission() {
        return "terrainer.info";
    }

    @Override
    protected @NotNull CommandRunnable getNoPermissionRunnable() {
        return CommandUtil.noPermissionRunnable();
    }

    @Override
    public void reloadCommand() {
        setAliases(TerrainerPlugin.getLanguage().get("Commands.Info.Command"));
    }

    @SuppressWarnings("deprecation")
    @Override
    public void run(@NotNull String label, @NotNull CommandSender sender, @NotNull String[] args) {
        Collection<Terrain> terrains;

        if (args.length > 1) {
            UUID id = null;

            try {
                id = UUID.fromString(args[1]);
            } catch (IllegalArgumentException ignored) {
            }

            if (id != null) {
                Terrain terrain = TerrainManager.terrainByID(id);
                terrains = terrain != null ? new ArrayList<>(List.of(terrain)) : Collections.emptyList();
            } else {
                terrains = new ArrayList<>();
                String name = CommandUtil.join(args, 1);

                for (Terrain t : TerrainManager.allTerrains()) {
                    if (ChatColor.stripColor(t.name()).equals(name)) terrains.add(t);
                }
            }
        } else if (sender instanceof Player player) {
            Location loc = player.getLocation();
            terrains = TerrainManager.terrainsAt(player.getWorld().getUID(), loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
        } else {
            MessageSender lang = TerrainerPlugin.getLanguage();
            lang.send(sender, lang.get("Invalid Arguments.Error").replace("<label>", label).replace("<label2>", args[0]).replace("<args>", lang.get("Invalid Arguments.Terrain")));
            return;
        }

        sendInfo(sender, terrains, null);
    }

    /**
     * Sends a message containing all information of terrains to a user.
     * <p>
     * If the user has no permission to see a terrain's information, it is removed from the collection, and its
     * information is not shown.
     * <p>
     * If a global terrain ({@link WorldTerrain}) is on the collection, and it's not the only entry, it is removed from
     * the collection, and its information is not shown.
     *
     * @param sender   The sender to check for permissions and send the info message.
     * @param terrains The collection terrains with information to be sent.
     * @throws UnsupportedOperationException If the collection of terrains is not mutable.
     */
    public void sendInfo(@NotNull CommandSender sender, @NotNull Collection<Terrain> terrains) {
        MessageSender lang = TerrainerPlugin.getLanguage();

        if (terrains.isEmpty()) {
            lang.send(sender, lang.get("Info.Error.No Terrains"));
            return;
        }

        BukkitPlayerUtil util = TerrainerPlugin.getPlayerUtil();

        if (sender instanceof Player player) terrains.removeIf(terrain -> !util.hasInfoPermission(player, terrain));

        if (terrains.isEmpty()) {
            lang.send(sender, lang.get("Info.Error.No Relating Terrains"));
            return;
        }

        // WorldTerrain info is only shown when it's the only one present.
        if (terrains.size() > 1) {
            terrains.removeIf(t -> t instanceof WorldTerrain);
        } else if (terrains.iterator().next() instanceof WorldTerrain) {
            lang.send(sender, lang.get("Info.Global Terrain"));
        }

        for (Terrain t : terrains) {
            World w = Bukkit.getWorld(t.world());
            String worldName = w == null ? t.world().toString() : w.getName();
            Coordinate min = t.minDiagonal();
            Coordinate max = t.maxDiagonal();
            String text = lang.get("Info.Text")
                    .replace("<name>", t.name())
                    .replace("<owner>", util.ownerName(t.owner()))
                    .replace("<desc>", t.description())
                    .replace("<date>", t.creationDate().format(DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss")))
                    .replace("<area>", Double.toString(t.area()))
                    .replace("<world>", worldName)
                    .replace("<x1>", Double.toString(min.x()))
                    .replace("<y1>", Double.toString(min.y()))
                    .replace("<z1>", Double.toString(min.z()))
                    .replace("<x2>", Double.toString(max.x()))
                    .replace("<y2>", Double.toString(max.y()))
                    .replace("<z2>", Double.toString(max.z()))
                    .replace("<mods>", TerrainerUtil.listToString(t.moderators().view(), util::ownerName))
                    .replace("<members>", TerrainerUtil.listToString(t.members().view(), util::ownerName))
                    .replace("<flags>", TerrainerUtil.listToString(t.flags().view().keySet(), id -> id))
                    .replace("<priority>", Integer.toString(t.priority()));

            if (EpicPluginLib.Platform.isPaper()) {
                PaperCompatibility.sendInfoWithCopyableId(sender, text, t);
            } else {
                lang.send(sender, text.replace("<id>", t.id().toString()));
            }
        }
    }

    /**
     * Sends a message containing information of multiple terrains to a user, and, if the user is a {@link Player}, show
     * the borders of the terrains within the provided location.
     * <p>
     * If the user has no permission to see a terrain's information, it is removed from the collection, and its
     * information is not shown.
     * <p>
     * If a global terrain ({@link WorldTerrain}) is on the collection, and it's not the only entry, it is removed from
     * the collection, and its information is not shown.
     *
     * @param sender   The sender to check for permissions and send the info message.
     * @param terrains The collection terrains with information to be sent.
     * @param location The location to get the terrains to show the borders. {@code null} to use the player's location.
     * @throws UnsupportedOperationException If the collection of terrains is not mutable.
     */
    public void sendInfo(@NotNull CommandSender sender, @NotNull Collection<Terrain> terrains, @Nullable Location location) {
        sendInfo(sender, terrains);

        // Showing borders of terrains.
        if (sender instanceof Player player) {
            Coordinate coord = location == null ? null : new Coordinate(location.getBlockX(), location.getBlockY(), location.getBlockZ());
            terrains = TerrainerPlugin.getPlayerUtil().terrainsToShowBorders(player, coord);
            terrains.removeIf(t -> t.borders().isEmpty());
            bordersCommand.showBorders(player, terrains);
            TerrainerPlugin.getPlayerUtil().showMarkers(player, player.getLocation().getBlockY() - 1, true, coord);
        }
    }

    private static final class PaperCompatibility {
        private static void sendInfoWithCopyableId(@NotNull CommandSender sender, @NotNull String text, @NotNull Terrain terrain) {
            Component idHoverText = LegacyComponentSerializer.legacyAmpersand().deserialize(TerrainerPlugin.getLanguage().get("Info.ID Hover"));
            Component id = Component.text(terrain.id().toString()).clickEvent(ClickEvent.copyToClipboard(terrain.id().toString())).hoverEvent(HoverEvent.showText(idHoverText));
            Component textComponent = LegacyComponentSerializer.legacyAmpersand().deserialize(TerrainerPlugin.getLanguage().get("General.Prefix") + text).replaceText(b -> b.matchLiteral("<id>").replacement(id));
            sender.sendMessage(textComponent);
        }
    }
}
