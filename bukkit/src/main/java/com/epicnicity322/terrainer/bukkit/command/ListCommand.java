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

package com.epicnicity322.terrainer.bukkit.command;

import com.epicnicity322.epicpluginlib.bukkit.command.Command;
import com.epicnicity322.epicpluginlib.bukkit.command.CommandRunnable;
import com.epicnicity322.epicpluginlib.bukkit.lang.MessageSender;
import com.epicnicity322.epicpluginlib.core.util.ObjectUtils;
import com.epicnicity322.terrainer.bukkit.TerrainerPlugin;
import com.epicnicity322.terrainer.bukkit.gui.ListGUI;
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

public final class ListCommand extends Command {
    private static final @NotNull Comparator<Terrain> comparator = Comparator.comparing(Terrain::name);

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

    @SuppressWarnings("deprecation")
    @Override
    public void run(@NotNull String label, @NotNull CommandSender sender, @NotNull String[] args) {
        MessageSender lang = TerrainerPlugin.getLanguage();
        CommandUtil.TargetResponse target = CommandUtil.target(1, "terrainer.list.others", sender, args);

        if (target == null) return;

        TreeSet<Terrain> terrains = new TreeSet<>(comparator);

        if (target == CommandUtil.TargetResponse.ALL) {
            terrains.addAll(TerrainManager.terrains());
        } else {
            UUID id = target == CommandUtil.TargetResponse.CONSOLE ? null : target.id();
            for (Terrain t : TerrainManager.terrains()) {
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

        if (!(sender instanceof HumanEntity player) || (args.length > 2 && args[2].equalsIgnoreCase("--chat"))) {
            Configuration config = Configurations.CONFIG.getConfiguration();
            int page = 1;
            HashMap<Integer, ArrayList<Terrain>> pages = ObjectUtils.splitIntoPages(terrains, config.getNumber("List.Chat.Max Per Page").orElse(20).intValue());
            int total = pages.size();

            if (args.length > 3) {
                try {
                    page = Integer.parseInt(args[3]);
                    if (page > total) page = total;
                    if (page < 1) page = 1;
                } catch (NumberFormatException e) {
                    lang.send(sender, lang.get("General.Not A Number").replace("<value>", args[3]));
                    return;
                }
            }

            String header = (who.equals(lang.get("Target.You")) ? lang.get("Terrain List.Chat.Header.Default") : lang.get("Terrain List.Chat.Header.Other").replace("<other>", who))
                    .replace("<page>", Integer.toString(page)).replace("<total>", Integer.toString(total));
            lang.send(sender, header);

            ArrayList<Terrain> currentPage = pages.get(page);
            int i = 0;
            TextComponent component = new TextComponent();

            for (Terrain t : currentPage) {
                var entry = new TextComponent(lang.getColored("Terrain List.Chat." + ((i++ & 1) == 0 ? "Entry" : "Alternate Entry")).replace("<name>", t.name()));
                entry.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(lang.getColored("Terrain List.Chat.Entry Hover")
                        .replace("<id>", t.id().toString())
                        .replace("<desc>", ObjectUtils.getOrDefault(t.description(), "null"))
                        .replace("<area>", Double.toString(t.area()))
                        .replace("<owner>", TerrainerPlugin.getPlayerUtil().getOwnerName(t.owner())))));
                entry.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/tr info " + t.id()));
                component.addExtra(entry);
                if (i != currentPage.size()) component.addExtra(lang.getColored("Terrain List.Chat.Separator"));
            }

            sender.spigot().sendMessage(component);

            if (page != total) {
                var footer = new TextComponent(lang.getColored("Terrain List.Chat.Footer").replace("<label>", label)
                        .replace("<arg>", args.length > 1 ? args[1] : "me")
                        .replace("<next>", Integer.toString(page + 1)));
                footer.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/" + label + " list " + (args.length > 1 ? args[1] : "me") + " --chat " + (page + 1)));
                sender.spigot().sendMessage(footer);
            }
        } else {
            String title = who.equals(lang.get("Target.You")) ? lang.getColored("Terrain List.GUI.Title.Default") : lang.getColored("Terrain List.GUI.Title.Other").replace("<other>", who);
            new ListGUI.TerrainListGUI(terrains, title).open(player);
        }
    }
}
