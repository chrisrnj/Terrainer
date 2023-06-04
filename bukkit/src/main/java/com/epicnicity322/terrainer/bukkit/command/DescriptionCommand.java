package com.epicnicity322.terrainer.bukkit.command;

import com.epicnicity322.epicpluginlib.bukkit.command.Command;
import com.epicnicity322.epicpluginlib.bukkit.command.CommandRunnable;
import com.epicnicity322.epicpluginlib.bukkit.lang.MessageSender;
import com.epicnicity322.terrainer.bukkit.TerrainerPlugin;
import com.epicnicity322.terrainer.bukkit.util.CommandUtil;
import com.epicnicity322.terrainer.core.terrain.Terrain;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public class DescriptionCommand extends Command {
    private int maxDescriptionLength = 100;

    @Override
    public @NotNull String getName() {
        return "description";
    }

    @Override
    public @NotNull String getPermission() {
        return "terrainer.description";
    }

    @Override
    protected @NotNull CommandRunnable getNoPermissionRunnable() {
        return CommandUtil.noPermissionRunnable();
    }

    public void setMaxDescriptionLength(int maxDescriptionLength) {
        this.maxDescriptionLength = maxDescriptionLength;
    }

    @Override
    public void run(@NotNull String label, @NotNull CommandSender sender, @NotNull String[] args) {
        CommandUtil.CommandArguments arguments = CommandUtil.findTerrain("terrainer.description.others", true, label, sender, args);
        if (arguments == null) return;
        Terrain terrain = arguments.terrain();
        args = arguments.preceding();
        MessageSender lang = TerrainerPlugin.getLanguage();

        if (args.length == 0) {
            terrain.setDescription(null);
            lang.send(sender, lang.get("Description.Reset").replace("<terrain>", terrain.name()));
            return;
        }

        String description = CommandUtil.join(args, 0);

        if (description.isBlank()) {
            terrain.setDescription(null);
            lang.send(sender, lang.get("Description.Reset").replace("<terrain>", terrain.name()));
            return;
        }

        if (description.length() > maxDescriptionLength) {
            lang.send(sender, lang.get("Description.Error.Length").replace("<max>", Integer.toString(maxDescriptionLength)));
            return;
        }

        terrain.setDescription(ChatColor.translateAlternateColorCodes('&', description));
        lang.send(sender, lang.get("Description.Set").replace("<description>", description).replace("<terrain>", terrain.name()));
    }
}
