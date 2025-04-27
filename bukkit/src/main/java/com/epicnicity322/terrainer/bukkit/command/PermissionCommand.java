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

package com.epicnicity322.terrainer.bukkit.command;

import com.epicnicity322.epicpluginlib.bukkit.command.Command;
import com.epicnicity322.epicpluginlib.bukkit.command.CommandRunnable;
import com.epicnicity322.epicpluginlib.bukkit.lang.MessageSender;
import com.epicnicity322.terrainer.bukkit.TerrainerPlugin;
import com.epicnicity322.terrainer.bukkit.util.CommandUtil;
import com.epicnicity322.terrainer.core.flag.Flags;
import com.epicnicity322.terrainer.core.terrain.Terrain;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.UUID;

import static com.epicnicity322.terrainer.bukkit.util.CommandUtil.*;

interface Permission {
    boolean isGrant();

    void managePermission(@NotNull CommandSender sender, boolean mod, @NotNull UUID toAdd, @NotNull Terrain terrain, @NotNull String who);
}

public abstract class PermissionCommand extends Command implements Permission {
    private @NotNull String @Nullable [] moderatorAliases = new String[]{"mod"};
    private @NotNull String @Nullable [] memberAliases = null;

    private static boolean contains(@NotNull String @Nullable [] array, @NotNull String value) {
        if (array == null) return false;
        for (String s : array) {
            if (value.equalsIgnoreCase(s)) return true;
        }
        return false;
    }

    private static boolean canManageModerators(@NotNull CommandSender sender, @NotNull Terrain terrain) {
        if (sender.hasPermission(Flags.MANAGE_MODERATORS.bypassPermission()) || !(sender instanceof Player player) || player.getUniqueId().equals(terrain.owner())) {
            return true;
        }

        Boolean canManageOtherMods = terrain.memberFlags().getData(player.getUniqueId(), Flags.MANAGE_MODERATORS);
        if (canManageOtherMods == null) canManageOtherMods = terrain.flags().getData(Flags.MANAGE_MODERATORS);

        return canManageOtherMods != null && canManageOtherMods;
    }

    public void setMemberAliases(@NotNull String @Nullable [] memberAliases) {
        this.memberAliases = memberAliases;
    }

    public void setModeratorAliases(@NotNull String @Nullable [] moderatorAliases) {
        this.moderatorAliases = moderatorAliases;
    }

    @Override
    protected @NotNull CommandRunnable getNoPermissionRunnable() {
        return CommandUtil.noPermissionRunnable();
    }

    @Override
    public void run(@NotNull String label, @NotNull CommandSender sender0, @NotNull String[] args0) {
        MessageSender lang = TerrainerPlugin.getLanguage();
        findTerrain(isGrant() ? "terrainer.grant.others" : "terrainer.revoke.others", isGrant() ? "terrainer.grant.world" : "terrainer.revoke.world", true, label, sender0, args0, lang.getColored("Permission.Select"), commandArguments -> {
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
                if (args[1].equalsIgnoreCase("moderator") || contains(moderatorAliases, args[1])) {
                    if (!canManageModerators(sender, terrain)) {
                        lang.send(sender, lang.get("Permission.Error.Manage Other Moderators Denied"));
                        return;
                    }
                    mod = true;
                } else if (!args[1].equalsIgnoreCase("member") && !contains(memberAliases, args[1])) {
                    lang.send(sender, lang.get("Invalid Arguments.Error").replace("<label>", label).replace("<label2>", args0[0]).replace("<args>", lang.get("Target.Player") + " [moderator|member] [--t " + lang.get("Target.Terrain") + "]"));
                    return;
                }
            }

            managePermission(sender, mod, toAdd, terrain, response.who().get());
        });
    }

    public static final class GrantCommand extends PermissionCommand {
        //TODO: "Add" alias
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
        //TODO: "Remove" alias
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