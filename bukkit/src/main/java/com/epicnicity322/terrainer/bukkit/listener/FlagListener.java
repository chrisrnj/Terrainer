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
        if (event.flag() != Flags.MODS_CAN_MANAGE_MODS && event.flag() != Flags.MODS_CAN_EDIT_FLAGS) return;
        if (!(event.sender() instanceof Player player)) return;

        if (!player.getUniqueId().equals(event.terrain().owner())) {
            TerrainerPlugin.getLanguage().send(player, TerrainerPlugin.getLanguage().get("Flags.Error.Not Owner"));
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onFlagUnset(UserFlagUnsetEvent event) {
        if (event.flag() != Flags.MODS_CAN_MANAGE_MODS && event.flag() != Flags.MODS_CAN_EDIT_FLAGS) return;
        if (!(event.sender() instanceof Player player)) return;

        if (!player.getUniqueId().equals(event.terrain().owner())) {
            TerrainerPlugin.getLanguage().send(player, TerrainerPlugin.getLanguage().get("Flags.Error.Not Owner"));
            event.setCancelled(true);
        }
    }
}
