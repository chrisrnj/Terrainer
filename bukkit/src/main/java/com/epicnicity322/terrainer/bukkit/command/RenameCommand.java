package com.epicnicity322.terrainer.bukkit.command;

import com.epicnicity322.epicpluginlib.bukkit.command.Command;
import com.epicnicity322.epicpluginlib.bukkit.command.CommandRunnable;
import com.epicnicity322.epicpluginlib.bukkit.lang.MessageSender;
import com.epicnicity322.terrainer.bukkit.TerrainerPlugin;
import com.epicnicity322.terrainer.bukkit.util.CommandUtil;
import com.epicnicity322.terrainer.core.config.Configurations;
import com.epicnicity322.terrainer.core.terrain.Terrain;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

//TODO: Rename command
public final class RenameCommand extends Command {
    @Override
    public @NotNull String getName() {
        return "rename";
    }

    @Override
    public @NotNull String getPermission() {
        return "terrainer.rename";
    }

    @Override
    protected @NotNull CommandRunnable getNoPermissionRunnable() {
        return CommandUtil.noPermissionRunnable();
    }

    @Override
    public void run(@NotNull String label, @NotNull CommandSender sender, @NotNull String[] args) {
        MessageSender lang = TerrainerPlugin.getLanguage();
        CommandUtil.CommandArguments arguments = CommandUtil.findTerrain("terrainer.rename.others", false, label, sender, args);
        if (arguments == null) return;
        Terrain terrain = arguments.terrain();
        String newName = ChatColor.translateAlternateColorCodes('&', CommandUtil.join(arguments.preceding(), 0)).trim();

        if (newName.isBlank()) {
            newName = terrain.id().toString().substring(0, terrain.id().toString().indexOf('-'));
            lang.send(sender, lang.get("Rename.Reset").replace("<new>", newName).replace("<old>", terrain.name()));
        } else {
            String stripped = ChatColor.stripColor(newName);
            int maxNameLength = Configurations.CONFIG.getConfiguration().getNumber("Max Name Length").orElse(26).intValue();
            if (stripped.isBlank() || stripped.length() > maxNameLength) {
                lang.send(sender, lang.get("Rename.Error.Name Length").replace("<max>", Integer.toString(maxNameLength)));
                return;
            }
            lang.send(sender, lang.get("Rename.Renamed").replace("<new>", newName).replace("<old>", terrain.name()));
        }

        terrain.setName(newName);
    }
}
