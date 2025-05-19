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
import com.epicnicity322.epicpluginlib.bukkit.command.TabCompleteRunnable;
import com.epicnicity322.epicpluginlib.bukkit.lang.MessageSender;
import com.epicnicity322.epicpluginlib.core.util.ObjectUtils;
import com.epicnicity322.terrainer.bukkit.TerrainerPlugin;
import com.epicnicity322.terrainer.bukkit.command.TerrainerCommand;
import com.epicnicity322.terrainer.bukkit.gui.TerrainListGUI;
import com.epicnicity322.terrainer.bukkit.util.CommandUtil;
import com.epicnicity322.terrainer.core.config.Configurations;
import com.epicnicity322.terrainer.core.terrain.Terrain;
import com.epicnicity322.terrainer.core.terrain.TerrainManager;
import com.epicnicity322.yamlhandler.Configuration;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.HumanEntity;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.IntStream;

public final class ListCommand extends TerrainerCommand {
    private static final @NotNull Comparator<Terrain> terrainComparator = Comparator.comparing(Terrain::name).thenComparing(Terrain::id);

    private final @NotNull InfoCommand infoCommand;

    public ListCommand(@NotNull InfoCommand infoCommand) {
        this.infoCommand = infoCommand;
    }

    @Override
    public @NotNull String getName() {
        return "list";
    }

    @Override
    public @NotNull String getPermission() {
        return "terrainer.list";
    }

    @Override
    protected @NotNull CommandRunnable getNoPermissionRunnable() {
        return CommandUtil.noPermissionRunnable();
    }

    @Override
    public void reloadCommand() {
        setAliases(TerrainerPlugin.getLanguage().get("Commands.List.Command"));
    }

    @SuppressWarnings("deprecation")
    @Override
    public void run(@NotNull String label, @NotNull CommandSender sender, @NotNull String[] args) {
        MessageSender lang = TerrainerPlugin.getLanguage();
        CommandUtil.TargetResponse target = CommandUtil.target(1, "terrainer.list.others", sender, args);
        if (target == null) return;

        TreeSet<Terrain> terrains = new TreeSet<>(terrainComparator);

        if (target == CommandUtil.TargetResponse.ALL) {
            terrains.addAll(TerrainManager.allTerrains());
        } else {
            UUID id = target == CommandUtil.TargetResponse.CONSOLE ? null : target.id();
            for (Terrain t : TerrainManager.allTerrains()) {
                if (Objects.equals(id, t.owner())) terrains.add(t);
            }
        }

        String who = target.who().get();

        if (terrains.isEmpty()) {
            if (target == CommandUtil.TargetResponse.ALL) {
                lang.send(sender, lang.get("Terrain List.No Terrains.Everyone"));
            } else if (who.equals(lang.get("Target.You"))) {
                lang.send(sender, lang.get("Terrain List.No Terrains.Default"));
            } else {
                lang.send(sender, lang.get("Terrain List.No Terrains.Other").replace("<other>", who));
            }
            return;
        }

        int page = 1;

        if (args.length > 2) {
            try {
                page = Integer.parseInt(args[2]);
            } catch (NumberFormatException e) {
                lang.send(sender, lang.get("General.Not A Number").replace("<value>", args[2]));
                return;
            }
        }

        String chat = lang.get("Commands.List.Chat");

        // Display list in chat if explicitly asked for, or if sender is not a player.
        if (!(sender instanceof HumanEntity player) || (args.length > 3 && (args[3].equalsIgnoreCase("--chat") || args[3].equalsIgnoreCase(chat)))) {
            Configuration config = Configurations.CONFIG.getConfiguration();
            HashMap<Integer, ArrayList<Terrain>> pages = ObjectUtils.splitIntoPages(terrains, config.getNumber("List.Chat.Max Per Page").orElse(20).intValue());
            int total = pages.size();
            if (page > total) page = total;
            if (page < 1) page = 1;

            String header = (who.equals(lang.get("Target.You")) ? lang.get("Terrain List.Chat.Header.Default") : lang.get("Terrain List.Chat.Header.Other").replace("<other>", who)).replace("<page>", Integer.toString(page)).replace("<total>", Integer.toString(total));
            lang.send(sender, header);

            ArrayList<Terrain> currentPage = pages.get(page);
            int i = 0;
            TextComponent component = new TextComponent();

            for (Terrain t : currentPage) {
                var entry = new TextComponent(lang.getColored("Terrain List.Chat." + ((i++ & 1) == 0 ? "Entry" : "Alternate Entry")).replace("<name>", t.name()));
                entry.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(lang.getColored("Terrain List.Chat.Entry Hover").replace("<id>", t.id().toString()).replace("<desc>", ObjectUtils.getOrDefault(t.description(), "null")).replace("<area>", Double.toString(t.area())).replace("<owner>", TerrainerPlugin.getPlayerUtil().ownerName(t.owner())))));
                entry.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tr info " + t.id()));
                component.addExtra(entry);
                if (i != currentPage.size()) component.addExtra(lang.getColored("Terrain List.Chat.Separator"));
            }

            sender.spigot().sendMessage(component);

            if (page != total) {
                var footer = new TextComponent(lang.getColored("Terrain List.Chat.Footer").replace("<label>", label).replace("<arg>", args.length > 1 ? args[1] : "me").replace("<next>", Integer.toString(page + 1)));
                footer.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/" + label + " list " + (args.length > 1 ? args[1] : "me") + ' ' + (page + 1) + " --chat"));
                sender.spigot().sendMessage(footer);
            }
        } else {
            String title = who.equals(lang.get("Target.You")) ? lang.getColored("Terrain List.GUI.Title.Default") : lang.getColored("Terrain List.GUI.Title.Other").replace("<other>", who);
            new TerrainListGUI(terrains, title, (event, t) -> {
                // TODO: Open terrain editing GUI if the player has permission to edit it.
                HumanEntity p = event.getWhoClicked();
                p.closeInventory();
                infoCommand.sendInfo(p, new ArrayList<>(Collections.singletonList(t)));
            }, page).open(player);
        }
    }

    @Override
    protected @NotNull TabCompleteRunnable getTabCompleteRunnable() {
        return (completions, label, sender, args) -> {
            if (args.length == 2) {
                if (sender.hasPermission("terrainer.list.others"))
                    CommandUtil.addTargetTabCompletion(completions, args);
                else if ("me".startsWith(args[1])) completions.add("me");
            } else if (args.length == 3) {
                if (args[2].isEmpty()) IntStream.range(1, 5).forEach(i -> completions.add(Integer.toString(i)));
            } else if (args.length == 4) {
                String chat = TerrainerPlugin.getLanguage().get("Commands.List.Chat");
                if (chat.startsWith(args[3])) completions.add(chat);
            }
        };
    }
}
