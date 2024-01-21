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

package com.epicnicity322.terrainer.bukkit.listener;

import com.epicnicity322.terrainer.bukkit.TerrainerPlugin;
import com.epicnicity322.terrainer.bukkit.event.flag.UserFlagSetEvent;
import com.epicnicity322.terrainer.bukkit.event.flag.UserFlagUnsetEvent;
import com.epicnicity322.terrainer.core.terrain.Flags;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public class FlagListener implements Listener {
    @EventHandler(priority = EventPriority.LOWEST)
    public void onFlagSet(UserFlagSetEvent event) {
        if (event.flag() == Flags.MODS_CAN_MANAGE_MODS) {
            if (event.sender().hasPermission("terrainer.bypass.modscanmanagemods")) return;
        } else if (event.flag() == Flags.MODS_CAN_EDIT_FLAGS) {
            if (event.sender().hasPermission("terrainer.bypass.modscaneditflags")) return;
        } else return;

        if (!(event.sender() instanceof Player player)) return;

        if (!player.getUniqueId().equals(event.terrain().owner())) {
            TerrainerPlugin.getLanguage().send(player, TerrainerPlugin.getLanguage().get("Flags.Error.Not Owner"));
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onFlagUnset(UserFlagUnsetEvent event) {
        if (event.flag() == Flags.MODS_CAN_MANAGE_MODS) {
            if (event.sender().hasPermission("terrainer.bypass.modscanmanagemods")) return;
        } else if (event.flag() == Flags.MODS_CAN_EDIT_FLAGS) {
            if (event.sender().hasPermission("terrainer.bypass.modscaneditflags")) return;
        } else return;

        if (!(event.sender() instanceof Player player)) return;

        if (!player.getUniqueId().equals(event.terrain().owner())) {
            TerrainerPlugin.getLanguage().send(player, TerrainerPlugin.getLanguage().get("Flags.Error.Not Owner"));
            event.setCancelled(true);
        }
    }
}
