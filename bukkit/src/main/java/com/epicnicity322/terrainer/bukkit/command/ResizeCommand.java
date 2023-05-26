package com.epicnicity322.terrainer.bukkit.command;

import com.epicnicity322.epicpluginlib.bukkit.command.Command;
import com.epicnicity322.epicpluginlib.bukkit.command.CommandRunnable;
import com.epicnicity322.terrainer.bukkit.util.CommandUtil;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

//TODO:
public final class ResizeCommand extends Command {
    @Override
    public @NotNull String getName() {
        return "shop";
    }

    @Override
    public @NotNull String getPermission() {
        return "terrainer.shop";
    }

    @Override
    protected @NotNull CommandRunnable getNoPermissionRunnable() {
        return CommandUtil.noPermissionRunnable();
    }

    @Override
    public void run(@NotNull String label, @NotNull CommandSender sender, @NotNull String[] args) {

    }
}
