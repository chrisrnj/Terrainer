package com.epicnicity322.terrainer.bukkit.command;

import com.epicnicity322.epicpluginlib.bukkit.command.Command;
import com.epicnicity322.epicpluginlib.bukkit.command.CommandRunnable;
import com.epicnicity322.epicpluginlib.bukkit.lang.MessageSender;
import com.epicnicity322.terrainer.bukkit.TerrainerPlugin;
import com.epicnicity322.terrainer.bukkit.listener.SelectionListener;
import com.epicnicity322.terrainer.bukkit.util.CommandUtil;
import com.epicnicity322.terrainer.core.config.Configurations;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public final class WandCommand extends Command {
    @Override
    public @NotNull String getName() {
        return "wand";
    }

    @Override
    public @NotNull String getPermission() {
        return "terrainer.wand";
    }

    @Override
    protected @NotNull CommandRunnable getNoPermissionRunnable() {
        return CommandUtil.noPermissionRunnable();
    }

    @Override
    public void run(@NotNull String label, @NotNull CommandSender sender, @NotNull String[] args) {
        MessageSender lang = TerrainerPlugin.getLanguage();
        boolean info = false;

        if (args.length > 1) {
            String infoType = lang.get("Commands.Wand.Info");
            String selectorType = lang.get("Commands.Wand.Selector");

            if (args[1].equalsIgnoreCase(infoType)) {
                info = true;
            } else if (!args[1].equalsIgnoreCase(selectorType)) {
                lang.send(sender, lang.get("Invalid Arguments.Error").replace("<label>", label).replace("<label2>", args[0]).replace("<args>", "<" + infoType + "|" + selectorType + "> " + lang.get("Invalid Arguments.Player Optional")));
                return;
            }
        }

        if (!sender.hasPermission("terrainer.wand." + (info ? "info" : "selector"))) {
            lang.send(sender, lang.get("General.No Permission"));
            return;
        }

        ItemStack item = info ? SelectionListener.getInfoWand() : SelectionListener.getSelectorWand();
        double price = Configurations.CONFIG.getConfiguration().getNumber((info ? "Info " : "Selector ") + "Wand.Price").orElse(50).doubleValue();

        if (!sender.hasPermission("terrainer.wand." + (info ? "info" : "selector") + ".free") && sender instanceof Player player) {
            if (price != 0) {
                if (TerrainerPlugin.getEconomyHandler() == null) {
                    lang.send(sender, lang.get("General.No Economy"));
                    return;
                }
                if (!TerrainerPlugin.getEconomyHandler().withdrawPlayer(player, price)) {
                    lang.send(sender, lang.get("General.Not Enough Money"));
                    return;
                }
            }
        } else {
            price = 0;
        }

        if (args.length > 2 && sender.hasPermission("terrainer.wand.others")) {
            Player player = Bukkit.getPlayer(args[2]);
            if (player == null) {
                lang.send(sender, lang.get("General.Player Not Found").replace("<value>", args[2]));
                return;
            }
            if (!player.getInventory().addItem(item).isEmpty()) {
                player.getWorld().dropItem(player.getLocation(), item);
            }
            lang.send(sender, lang.get("Wand.Given").replace("<type>", item.getItemMeta().getDisplayName())
                    .replace("<player>", player.getName())
                    .replace("<price>", Double.toString(price)));
            lang.send(player, lang.get("Wand.Received").replace("<player>", sender.getName()).replace("<type>", item.getItemMeta().getDisplayName()));
            return;
        }

        if (!(sender instanceof Player player)) {
            lang.send(sender, lang.get("General.Not A Player"));
            return;
        }

        if (!player.getInventory().addItem(item).isEmpty()) {
            player.getWorld().dropItem(player.getLocation(), item);
        }

        lang.send(sender, lang.get("Wand.Bought").replace("<type>", item.getItemMeta().getDisplayName())
                .replace("<price>", Double.toString(price)));
    }
}
