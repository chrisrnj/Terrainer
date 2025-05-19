/*
 * Terrainer - A minecraft terrain claiming protection plugin.
 * Copyright (C) 2025 Christiano Rangel
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

package com.epicnicity322.terrainer.bukkit.command.impl;

import com.epicnicity322.epicpluginlib.bukkit.command.CommandRunnable;
import com.epicnicity322.epicpluginlib.bukkit.command.TabCompleteRunnable;
import com.epicnicity322.epicpluginlib.bukkit.lang.MessageSender;
import com.epicnicity322.terrainer.bukkit.TerrainerPlugin;
import com.epicnicity322.terrainer.bukkit.command.TerrainerCommand;
import com.epicnicity322.terrainer.bukkit.util.CommandUtil;
import com.epicnicity322.terrainer.core.flag.Flags;
import com.epicnicity322.terrainer.core.terrain.Terrain;
import com.epicnicity322.terrainer.core.terrain.TerrainManager;
import com.epicnicity322.terrainer.core.terrain.WorldTerrain;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.UUID;

import static com.epicnicity322.terrainer.bukkit.util.CommandUtil.*;

public abstract class PermissionCommand extends TerrainerCommand {
    private static boolean canManageModerators(@NotNull CommandSender sender, @NotNull Terrain terrain) {
        if (sender.hasPermission(Flags.MANAGE_MODERATORS.bypassPermission()) || !(sender instanceof Player player) || player.getUniqueId().equals(terrain.owner())) {
            return true;
        }

        Boolean canManageOtherMods = terrain.memberFlags().getData(player.getUniqueId(), Flags.MANAGE_MODERATORS);
        if (canManageOtherMods == null) canManageOtherMods = terrain.flags().getData(Flags.MANAGE_MODERATORS);

        return canManageOtherMods != null && canManageOtherMods;
    }

    protected abstract boolean isGrant();

    protected abstract void managePermission(@NotNull CommandSender sender, boolean mod, @NotNull UUID toAdd, @NotNull Terrain terrain, @NotNull String who);

    @Override
    protected @NotNull CommandRunnable getNoPermissionRunnable() {
        return CommandUtil.noPermissionRunnable();
    }

    @Override
    public void run(@NotNull String label, @NotNull CommandSender sender0, @NotNull String[] args0) {
        MessageSender lang = TerrainerPlugin.getLanguage();
        findTerrain(getPermission() + ".others", getPermission() + ".world", true, label, sender0, args0, lang.getColored("Permission.Select"), commandArguments -> {
            String[] args = commandArguments.preceding();
            Terrain terrain = commandArguments.terrain();
            CommandSender sender = commandArguments.sender();

            if (args.length == 0) {
                //TODO: open permission management inventory.
                lang.send(sender, "Permission Management GUI is not done yet, please use the full command for managing terrain permissions.");
                return;
            }

            TargetResponse response = target(0, null, sender, args);
            if (response == null) return;

            UUID toAdd = response.id();

            if (response == TargetResponse.ALL) {
                lang.send(sender, lang.get("Permission.Error.Multiple"));
                return;
            }
            if (response == TargetResponse.CONSOLE) {
                lang.send(sender, lang.get("Permission.Error.Console"));
                return;
            }
            if (Objects.equals(toAdd, terrain.owner())) {
                lang.send(sender, lang.get("Permission.Error.Owner"));
                return;
            }

            boolean mod = false;

            if (args.length > 1) {
                String moderator = lang.get("Commands.Permission.Moderator");
                String member = lang.get("Commands.Permission.Member");

                if (args[1].equalsIgnoreCase("moderator") || args[1].equalsIgnoreCase(moderator)) {
                    if (!canManageModerators(sender, terrain)) {
                        lang.send(sender, lang.get("Permission.Error.Manage Other Moderators Denied"));
                        return;
                    }
                    mod = true;
                } else if (!args[1].equalsIgnoreCase("member") && !args[1].equalsIgnoreCase(member)) {
                    lang.send(sender, lang.get("Invalid Arguments.Error").replace("<label>", label).replace("<label2>", args0[0]).replace("<args>", lang.get("Invalid Arguments.Player") + " [" + moderator + "|" + member + "] " + lang.get("Invalid Arguments.Terrain Optional")));
                    return;
                }
            }

            managePermission(sender, mod, toAdd, terrain, response.who().get());
        });
    }

    @Override
    protected @NotNull TabCompleteRunnable getTabCompleteRunnable() {
        return (completions, label, sender, args) -> {
            if (args.length == 2) {
                CommandUtil.addTargetTabCompletion(completions, args);
                completions.remove("*");
                completions.remove("null");
                if (sender instanceof Player player) {
                    Location l = player.getLocation();
                    UUID playerID = player.getUniqueId();

                    for (var terrain : TerrainManager.terrainsAt(player.getWorld().getUID(), l.getBlockX(), l.getBlockY(), l.getBlockZ())) {
                        if (Objects.equals(playerID, terrain.owner()) || terrain.moderators().view().contains(playerID) || player.hasPermission(getPermission() + ".others") || (terrain instanceof WorldTerrain && player.hasPermission(getPermission() + ".world"))) {
                            terrain.members().view().forEach(uuid -> completions.add(TerrainerPlugin.getPlayerUtil().ownerName(uuid)));
                            terrain.moderators().view().forEach(uuid -> completions.add(TerrainerPlugin.getPlayerUtil().ownerName(uuid)));
                        }
                    }
                    completions.removeIf(completion -> !completion.startsWith(args[1]));
                }
                return;
            } else if (args.length == 3) {
                String member = TerrainerPlugin.getLanguage().get("Commands.Permission.Member");
                String moderator = TerrainerPlugin.getLanguage().get("Commands.Permission.Moderator");

                if (member.startsWith(args[2])) completions.add(member);
                if (moderator.startsWith(args[2])) completions.add(moderator);
                if (!completions.isEmpty()) return;
            }

            CommandUtil.addTerrainTabCompletion(completions, getPermission() + ".others", getPermission() + ".world", true, sender, args);
        };
    }

    public static final class GrantCommand extends PermissionCommand {
        @Override
        public @NotNull String getName() {
            return "grant";
        }

        @Override
        public @NotNull String getPermission() {
            return "terrainer.grant";
        }

        @Override
        public boolean isGrant() {
            return true;
        }

        @Override
        public void reloadCommand() {
            setAliases(TerrainerPlugin.getLanguage().get("Commands.Permission.Command Grant"));
        }

        @Override
        public void managePermission(@NotNull CommandSender sender, boolean mod, @NotNull UUID toAdd, @NotNull Terrain terrain, @NotNull String who) {
            MessageSender lang = TerrainerPlugin.getLanguage();
            if (mod) {
                if (terrain.moderators().view().contains(toAdd)) {
                    lang.send(sender, lang.get("Permission.Moderator.Error.Contains").replace("<who>", who).replace("<terrain>", terrain.name()));
                    return;
                }
                terrain.members().remove(toAdd);
                terrain.moderators().add(toAdd);
                lang.send(sender, lang.get("Permission.Moderator.Granted").replace("<who>", who).replace("<terrain>", terrain.name()));
                Player toAddPlayer = Bukkit.getPlayer(toAdd);
                if (toAddPlayer != null) {
                    lang.send(toAddPlayer, lang.get("Permission.Moderator.Notify").replace("<terrain>", terrain.name()));
                }
            } else {
                if (terrain.moderators().view().contains(toAdd) && !PermissionCommand.canManageModerators(sender, terrain)) {
                    lang.send(sender, lang.get("Permission.Error.Manage Other Moderators Denied"));
                    return;
                }
                if (terrain.members().view().contains(toAdd)) {
                    lang.send(sender, lang.get("Permission.Member.Error.Contains").replace("<who>", who).replace("<terrain>", terrain.name()));
                    return;
                }
                terrain.moderators().remove(toAdd);
                terrain.members().add(toAdd);
                lang.send(sender, lang.get("Permission.Member.Granted").replace("<who>", who).replace("<terrain>", terrain.name()));
                Player toAddPlayer = Bukkit.getPlayer(toAdd);
                if (toAddPlayer != null) {
                    lang.send(toAddPlayer, lang.get("Permission.Member.Notify").replace("<terrain>", terrain.name()));
                }
            }
        }
    }

    public static final class RevokeCommand extends PermissionCommand {
        @Override
        public @NotNull String getName() {
            return "revoke";
        }

        @Override
        public @NotNull String getPermission() {
            return "terrainer.revoke";
        }

        @Override
        public boolean isGrant() {
            return false;
        }

        @Override
        public void reloadCommand() {
            setAliases(TerrainerPlugin.getLanguage().get("Commands.Permission.Command Revoke"));
        }

        @Override
        public void managePermission(@NotNull CommandSender sender, boolean mod, @NotNull UUID toAdd, @NotNull Terrain terrain, @NotNull String who) {
            MessageSender lang = TerrainerPlugin.getLanguage();
            if (mod) {
                if (!terrain.moderators().view().contains(toAdd)) {
                    lang.send(sender, lang.get("Permission.Moderator.Error.Does Not Contain").replace("<who>", who).replace("<terrain>", terrain.name()));
                    return;
                }
                // "moderator" was said explicitly, so removing mod and giving member.
                terrain.moderators().remove(toAdd);
                terrain.members().add(toAdd);
                lang.send(sender, lang.get("Permission.Moderator.Revoked").replace("<who>", who).replace("<terrain>", terrain.name()));
            } else {
                boolean isMod = terrain.moderators().view().contains(toAdd);
                if (isMod && !PermissionCommand.canManageModerators(sender, terrain)) {
                    lang.send(sender, lang.get("Permission.Error.Manage Other Moderators Denied"));
                    return;
                }
                if (!terrain.members().view().contains(toAdd) && !isMod) {
                    lang.send(sender, lang.get("Permission.Member.Error.Does Not Contain").replace("<who>", who).replace("<terrain>", terrain.name()));
                    return;
                }
                terrain.moderators().remove(toAdd);
                terrain.members().remove(toAdd);
                lang.send(sender, lang.get("Permission.Member.Revoked").replace("<who>", who).replace("<terrain>", terrain.name()));
            }
        }
    }
}