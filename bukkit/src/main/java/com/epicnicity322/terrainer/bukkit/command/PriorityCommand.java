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
import com.epicnicity322.terrainer.bukkit.util.CommandUtil;
import com.epicnicity322.terrainer.core.terrain.Terrain;
import com.epicnicity322.terrainer.core.terrain.TerrainManager;
import com.epicnicity322.terrainer.core.util.TerrainerUtil;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

public final class PriorityCommand extends Command {
    @Override
    public @NotNull String getName() {
        return "priority";
    }

    @Override
    public @NotNull String getPermission() {
        return "terrainer.priority";
    }

    @Override
    protected @NotNull CommandRunnable getNoPermissionRunnable() {
        return CommandUtil.noPermissionRunnable();
    }

    @Override
    public void run(@NotNull String label, @NotNull CommandSender sender0, @NotNull String[] args0) {
        MessageSender lang = TerrainerPlugin.getLanguage();

        if (sender0 instanceof Player player && args0.length == 2 && (args0[1].equalsIgnoreCase("-h") || args0[1].equalsIgnoreCase(lang.get("Commands.Priority.Here")))) {
            Location loc = player.getLocation();
            UUID world = player.getWorld().getUID();
            Collection<Terrain> terrains = TerrainManager.terrainsAt(world, loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
            boolean removed = false;

            // World Terrains are not shown with -here.
            terrains.removeIf(t -> t.id().equals(world));

            if (!player.hasPermission("terrainer.priority.others")) {
                removed = terrains.removeIf(t -> !player.getUniqueId().equals(t.owner()));
            }
            if (terrains.isEmpty()) {
                lang.send(player, lang.get("Priority.Error.No Terrains"));
                return;
            }
            if (terrains.size() == 1) {
                Terrain single = terrains.iterator().next();
                lang.send(sender0, lang.get("Priority.Single").replace("<priority>", Integer.toString(single.priority())).replace("<terrain>", single.name()));
                return;
            }
            if (checkIfTerrainsHaveSamePriority(terrains)) {
                Terrain first = terrains.iterator().next();
                lang.send(sender0, lang.get("Priority.Same.Here").replace("<priority>", Integer.toString(first.priority())).replace("<terrains>", TerrainerUtil.listToString(terrains, Terrain::name)));
                if (removed) lang.send(sender0, lang.get("Priority.Removed"));
                return;
            }

            lang.send(sender0, lang.get("Priority.Here"));

            for (Terrain t : terrains) {
                lang.send(sender0, false, lang.get("Priority.Priority").replace("<priority>", Integer.toString(t.priority())).replace("<terrain>", t.name()));
            }

            if (removed) lang.send(sender0, lang.get("Priority.Removed"));

            return;
        }

        CommandUtil.findTerrain("terrainer.priority.others", "terrainer.priority.world", false, label, sender0, args0, lang.getColored("Priority.Select"), arguments -> {
            if (arguments == null) return;
            String[] args = arguments.preceding();
            Terrain terrain = arguments.terrain();
            CommandSender sender = arguments.sender();
            UUID senderID = sender instanceof Player player ? player.getUniqueId() : null;
            List<Terrain> overlappingTerrains = getOverlappingTerrains(terrain);

            if (args.length == 0) {
                if (overlappingTerrains.size() <= 1 || !sender.hasPermission("terrainer.priority.overlappinginfo")) {
                    lang.send(sender, lang.get("Priority.Single").replace("<priority>", Integer.toString(terrain.priority())).replace("<terrain>", terrain.name()));
                    return;
                }

                boolean showOtherPriorities = sender.hasPermission("terrainer.priority.others");

                if (showOtherPriorities && checkIfTerrainsHaveSamePriority(overlappingTerrains)) {
                    return;
                }

                lang.send(sender, lang.get("Priority.Overlapping").replace("<priority>", Integer.toString(terrain.priority())).replace("<terrain>", terrain.name()));

                for (Terrain t : overlappingTerrains) {
                    lang.send(sender, false, lang.get("Priority.Priority").replace("<priority>", (showOtherPriorities || Objects.equals(senderID, t.owner())) ? Integer.toString(t.priority()) : lang.get("Priority.Unknown")).replace("<terrain>", t.name()));
                }

                return;
            }

            if (args.length > 1) {
                lang.send(sender, lang.get("Invalid Arguments.Error").replace("<label>", label).replace("<label2>", args0[0]).replace("<args>", lang.get("Invalid Arguments.Priority")));
                return;
            }

            int newPriority;

            try {
                newPriority = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                lang.send(sender, lang.get("Invalid Arguments.Error").replace("<label>", label).replace("<label2>", args0[0]).replace("<args>", lang.get("Invalid Arguments.Priority")));
                return;
            }

            if (!sender.hasPermission("terrainer.bypass.overlap")) {
                for (Terrain t : overlappingTerrains) {
                    if (!Objects.equals(senderID, t.owner())) {
                        lang.send(sender, lang.get("Priority.Error.Overlap"));
                        return;
                    }
                }
            }

            terrain.setPriority(newPriority);
            lang.send(sender, lang.get("Priority.Set").replace("<new>", Integer.toString(newPriority)).replace("<terrain>", terrain.name()));
        });
    }

    private List<Terrain> getOverlappingTerrains(Terrain terrain) {
        return TerrainManager.terrains(terrain.world()).filter(terrain::isOverlapping).collect(Collectors.toList());
    }

    private boolean checkIfTerrainsHaveSamePriority(Collection<Terrain> terrains) {
        Integer priority = null;

        for (Terrain terrain : terrains) {
            if (priority == null) {
                priority = terrain.priority();
                continue;
            }

            if (priority != terrain.priority()) return false;
        }

        return true;
    }
}
