package com.epicnicity322.terrainer.bukkit.command;

import com.epicnicity322.epicpluginlib.bukkit.command.Command;
import com.epicnicity322.epicpluginlib.bukkit.command.CommandRunnable;
import com.epicnicity322.epicpluginlib.bukkit.lang.MessageSender;
import com.epicnicity322.terrainer.bukkit.TerrainerPlugin;
import com.epicnicity322.terrainer.bukkit.util.BukkitPlayerUtil;
import com.epicnicity322.terrainer.bukkit.util.CommandUtil;
import com.epicnicity322.terrainer.core.terrain.Terrain;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;
import java.util.Objects;

public final class TransferCommand extends Command {
    @Override
    public @NotNull String getName() {
        return "transfer";
    }

    @Override
    public @NotNull String getPermission() {
        return "terrainer.transfer";
    }

    @Override
    protected @NotNull CommandRunnable getNoPermissionRunnable() {
        return CommandUtil.noPermissionRunnable();
    }

    @Override
    public void run(@NotNull String label, @NotNull CommandSender sender, @NotNull String[] args) {
        MessageSender lang = TerrainerPlugin.getLanguage();
        CommandUtil.CommandArguments arguments = CommandUtil.findTerrain("terrainer.transfer.others", false, label, sender, args);
        if (arguments == null) return;
        Terrain terrain = arguments.terrain();
        String label2 = args[0];
        args = arguments.preceding();

        if (terrain.owner() == null && !sender.hasPermission("terrainer.transfer.console")) {
            lang.send(sender, lang.get("Transfer.Error.Not Allowed"));
            return;
        }

        if (args.length == 0) {
            lang.send(sender, lang.get("Invalid Arguments.Error").replace("<label>", label).replace("<label2>", label2).replace("<args>", lang.get("Invalid Arguments.Player") + " " + lang.get("Invalid Arguments.Terrain Optional")));
            return;
        }

        CommandUtil.TargetResponse target = CommandUtil.target(0, null, sender, args);
        if (target == null) return;
        String who = target.who().get();
        Player newOwner;

        if (target == CommandUtil.TargetResponse.ALL) {
            lang.send(sender, lang.get("Invalid Arguments.Error").replace("<label>", label).replace("<label2>", label2).replace("<args>", lang.get("Invalid Arguments.Player") + " " + lang.get("Invalid Arguments.Terrain Optional")));
            return;
        }

        if (target == CommandUtil.TargetResponse.CONSOLE) {
            if (!sender.hasPermission("terrainer.transfer.toconsole")) {
                lang.send(sender, lang.get("General.No Permission"));
                return;
            }
            if (terrain.owner() == null) {
                lang.send(sender, lang.get("Transfer.Nothing Changed"));
                return;
            }
            newOwner = null;
        } else {
            newOwner = Bukkit.getPlayer(target.id());

            if (newOwner == null) {
                lang.send(sender, lang.get("Transfer.Error.Not Online").replace("<player>", who));
                return;
            }
            if (Objects.equals(terrain.owner(), newOwner.getUniqueId())) {
                lang.send(sender, lang.get("Transfer.Nothing Changed"));
                return;
            }

            BukkitPlayerUtil util = TerrainerPlugin.getPlayerUtil();

            if (util.getMaxBlockLimit(newOwner) - util.getUsedBlockLimit(newOwner.getUniqueId()) < terrain.area()) {
                lang.send(sender, lang.get("Transfer.Error.Low Block Limit").replace("<player>", who));
                return;
            }
            if (util.getMaxClaimLimit(newOwner) - util.getUsedClaimLimit(newOwner.getUniqueId()) < 1) {
                lang.send(sender, lang.get("Transfer.Error.Low Claim Limit").replace("<player>", who));
                return;
            }
        }

        String name = terrain.name();

        if (newOwner == null) {
            terrain.setOwner(null);
            lang.send(sender, lang.get("Transfer.Success").replace("<terrain>", name).replace("<who>", who));
        } else {
            int confirmationHash = Objects.hash("transfer", terrain.id());

            lang.send(sender, lang.get("Transfer.Requested").replace("<who>", who));
            lang.send(newOwner, lang.get("Transfer.Request").replace("<player>", sender.getName()).replace("<terrain>", name));

            ConfirmCommand.requestConfirmation(newOwner, () -> {
                ConfirmCommand.cancelConfirmations(confirmationHash);
                terrain.setOwner(newOwner.getUniqueId());
                lang.send(sender, lang.get("Transfer.Success").replace("<terrain>", name).replace("<who>", who));
                lang.send(newOwner, lang.get("Transfer.Success").replace("<terrain>", name).replace("<who>", lang.get("Target.You").toLowerCase(Locale.ROOT)));
            }, () -> lang.get("Transfer.Confirmation").replace("<terrain>", name), confirmationHash);
        }
    }
}
