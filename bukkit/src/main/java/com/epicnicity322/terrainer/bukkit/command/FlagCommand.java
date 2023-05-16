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
import com.epicnicity322.epicpluginlib.bukkit.lang.MessageSender;
import com.epicnicity322.terrainer.bukkit.TerrainerPlugin;
import com.epicnicity322.terrainer.bukkit.gui.ListGUI;
import com.epicnicity322.terrainer.bukkit.util.CommandUtil;
import com.epicnicity322.terrainer.core.terrain.Flag;
import com.epicnicity322.terrainer.core.terrain.Flags;
import com.epicnicity322.terrainer.core.terrain.Terrain;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.HumanEntity;
import org.jetbrains.annotations.NotNull;

public final class FlagCommand extends Command {
    @Override
    public @NotNull String getName() {
        return "flag";
    }

    @Override
    public @NotNull String getPermission() {
        return "terrainer.flag";
    }

    @Override
    public void run(@NotNull String label, @NotNull CommandSender sender, @NotNull String[] args) {
        CommandUtil.CommandArguments arguments = CommandUtil.findTerrain("terrainer.flag.others", true, label, sender, args);
        if (arguments == null) return;
        String[] flagArgs = arguments.preceding();
        Terrain terrain = arguments.terrain();

        MessageSender lang = TerrainerPlugin.getLanguage();

        if (flagArgs.length == 0) {
            if (sender instanceof HumanEntity player) {
                new ListGUI.FlagListGUI(player, terrain).open(player);
            } else {
                lang.send(sender, lang.get("Invalid Arguments.Error").replace("<label>", label).replace("<label2>", args[0])
                        .replace("<args>", lang.get("Invalid Arguments.Flag") + " " + lang.get("Invalid Arguments.Flag Values") + " --t " + lang.get("Invalid Arguments.Terrain")));
                return;
            }
            return;
        }

        Flag<?> flag = Flags.matchFlag(flagArgs[0]);

        if (flag == null) {
            lang.send(sender, lang.get("Flags.Error.Not Found").replace("<value>", flagArgs[0]));
            return;
        }
        if (flagArgs.length == 1) {
            // remove
            if (terrain.flags().view().containsKey(flag)) {
                terrain.flags().removeFlag(flag);
                lang.send(sender, lang.get("Flags.Remove").replace("<flag>", lang.get("Flags.Values." + flag.id() + ".Display Name")));
                return;
            }
        }
        //TODO
    }
}
