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

package com.epicnicity322.terrainer.bukkit.command;

import com.epicnicity322.epicpluginlib.bukkit.command.Command;
import com.epicnicity322.epicpluginlib.bukkit.command.CommandRunnable;
import com.epicnicity322.epicpluginlib.bukkit.lang.MessageSender;
import com.epicnicity322.terrainer.bukkit.TerrainerPlugin;
import com.epicnicity322.terrainer.bukkit.util.BukkitPlayerUtil;
import com.epicnicity322.terrainer.bukkit.util.CommandUtil;
import com.epicnicity322.terrainer.core.terrain.Terrain;
import com.epicnicity322.terrainer.core.terrain.WorldTerrain;
import com.epicnicity322.terrainer.core.util.PlayerUtil;
import com.epicnicity322.terrainer.core.util.TerrainerUtil;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.lang.ref.WeakReference;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

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
    public void run(@NotNull String label, @NotNull CommandSender sender0, @NotNull String[] args0) {
        MessageSender lang = TerrainerPlugin.getLanguage();
        CommandUtil.findTerrain("terrainer.transfer.others", "terrainer.transfer.world", false, label, sender0, args0, lang.getColored("Transfer.Select"), arguments -> {
            Terrain terrain = arguments.terrain();
            CommandSender sender = arguments.sender();

            if (terrain instanceof WorldTerrain) {
                lang.send(sender, lang.get("Transfer.Error.World Terrain"));
                return;
            }

            String[] args = arguments.preceding();

            if (terrain.owner() == null && !sender.hasPermission("terrainer.transfer.console")) {
                lang.send(sender, lang.get("Transfer.Error.Not Allowed"));
                return;
            }

            if (args.length == 0) {
                lang.send(sender, lang.get("Invalid Arguments.Error").replace("<label>", label).replace("<label2>", args0[0]).replace("<args>", lang.get("Invalid Arguments.Player") + " " + lang.get("Invalid Arguments.Terrain Optional") + (sender.hasPermission("terrainer.transfer.force") ? " [" + lang.get("Commands.Transfer.Force") + "]" : "")));
                return;
            }

            CommandUtil.TargetResponse target = CommandUtil.target(0, null, sender, args);
            if (target == null) return;
            String who = target.who().get();
            Player newOwner;
            UUID newOwnerID;

            if (target == CommandUtil.TargetResponse.ALL) {
                lang.send(sender, lang.get("Invalid Arguments.Error").replace("<label>", label).replace("<label2>", args0[0]).replace("<args>", lang.get("Invalid Arguments.Player") + " " + lang.get("Invalid Arguments.Terrain Optional") + (sender.hasPermission("terrainer.transfer.force") ? " [" + lang.get("Commands.Transfer.Force") + "]" : "")));
                return;
            }

            if (target == CommandUtil.TargetResponse.CONSOLE) {
                if (!sender.hasPermission("terrainer.transfer.toconsole")) {
                    lang.send(sender, lang.get("General.No Permission"));
                    return;
                }
                if (terrain.owner() == null) {
                    lang.send(sender, lang.get("Transfer.Error.Nothing Changed").replace("<player>", lang.get("Target.Console")));
                    return;
                }
                newOwnerID = null;
                newOwner = null;
            } else {
                newOwnerID = target.id();
                BukkitPlayerUtil util = TerrainerPlugin.getPlayerUtil();

                if (Objects.equals(terrain.owner(), newOwnerID)) {
                    lang.send(sender, lang.get("Transfer.Error.Nothing Changed").replace("<player>", util.ownerName(newOwnerID)));
                    return;
                }

                if (args.length > 1 && args[1].equalsIgnoreCase(lang.get("Commands.Transfer.Force")) && sender.hasPermission("terrainer.transfer.force")) {
                    newOwner = null;
                } else {
                    newOwner = Bukkit.getPlayer(newOwnerID);

                    if (newOwner == null) {
                        lang.send(sender, lang.get("Transfer.Error.Not Online").replace("<player>", who));
                        return;
                    }
                    World world = Bukkit.getWorld(terrain.world());

                    if (world == null) {
                        lang.send(sender, lang.get("Transfer.Error.World No Longer Exists"));
                        return;
                    }
                    if (!newOwner.hasPermission("terrainer.world." + world.getName())) {
                        lang.send(sender, lang.get("Transfer.Error.World Prohibited").replace("<player>", who).replace("<terrain>", terrain.name()));
                        return;
                    }

                    Terrain tempTerrain = new Terrain(terrain); // Creating safe terrain clone.

                    tempTerrain.setOwner(newOwnerID); // Perform claim checks with the new owner as terrain owner.
                    PlayerUtil.ClaimResponse<?> claimResponse = util.performClaimChecks(tempTerrain);
                    PlayerUtil.ClaimResponseType<?> responseType = claimResponse.response();

                    if (responseType == PlayerUtil.ClaimResponseType.BLOCK_LIMIT_REACHED) {
                        lang.send(sender, lang.get("Transfer.Error.Low Block Limit").replace("<player>", who));
                        return;
                    } else if (responseType == PlayerUtil.ClaimResponseType.CLAIM_LIMIT_REACHED) {
                        lang.send(sender, lang.get("Transfer.Error.Low Claim Limit").replace("<player>", who));
                        return;
                    } else if (responseType == PlayerUtil.ClaimResponseType.OVERLAPPING_OTHERS) {
                        //noinspection unchecked
                        Set<Terrain> overlapping = (Set<Terrain>) claimResponse.variable();
                        if (sender instanceof Player p && overlapping.stream().anyMatch(t -> !util.hasInfoPermission(p, t))) {
                            // No permission to see terrain names.
                            lang.send(sender, lang.get("Transfer.Error.Overlap Several").replace("<player>", who).replace("<terrain>", terrain.name()));
                        } else {
                            lang.send(sender, lang.get("Transfer.Error.Overlap").replace("<player>", who).replace("<terrain>", terrain.name()).replace("<overlapping>", TerrainerUtil.listToString(overlapping, Terrain::name)));
                        }
                        return;
                    } else if (responseType != PlayerUtil.ClaimResponseType.SUCCESS) { // Probably Area or Dimension too small.
                        lang.send(sender, lang.get("Transfer.Error.Default").replace("<player>", who));
                        return;
                    }
                }
            }

            String name = terrain.name();

            if (newOwner == null || newOwner.equals(sender)) {
                terrain.setOwner(newOwnerID);
                lang.send(sender, lang.get("Transfer.Success").replace("<terrain>", name).replace("<who>", who));
            } else {
                WeakReference<Terrain> terrainRef = new WeakReference<>(terrain);
                int confirmationHash = Objects.hash("transfer", terrain.id());

                lang.send(sender, lang.get("Transfer.Requested").replace("<who>", who));
                lang.send(newOwner, lang.get("Transfer.Request").replace("<player>", sender.getName()).replace("<terrain>", name));

                WeakReference<CommandSender> senderRef = new WeakReference<>(sender);

                ConfirmCommand.requestConfirmation(newOwner, newOwner1 -> {
                    ConfirmCommand.cancelConfirmations(confirmationHash);

                    Terrain terrain1 = terrainRef.get();
                    if (terrain1 == null) return;
                    String name1 = terrain1.name();

                    terrain1.setOwner(newOwnerID);
                    CommandSender sender1 = senderRef.get();
                    if (sender1 != null)
                        lang.send(sender1, lang.get("Transfer.Success").replace("<terrain>", name1).replace("<who>", who));
                    lang.send(newOwner1, lang.get("Transfer.Success").replace("<terrain>", name1).replace("<who>", lang.get("Target.You").toLowerCase(Locale.ROOT)));
                }, () -> {
                    Terrain terrain1 = terrainRef.get();
                    return lang.get("Transfer.Confirmation Description").replace("<terrain>", terrain1 == null ? name : terrain1.name());
                }, confirmationHash);
            }
        });
    }
}
