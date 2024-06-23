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
import com.epicnicity322.epicpluginlib.core.logger.ConsoleLogger;
import com.epicnicity322.epicpluginlib.core.util.ObjectUtils;
import com.epicnicity322.terrainer.bukkit.TerrainerPlugin;
import com.epicnicity322.terrainer.bukkit.util.CommandUtil;
import com.epicnicity322.terrainer.core.Terrainer;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Supplier;

public final class ConfirmCommand extends Command {
    private static final @NotNull UUID console = UUID.randomUUID();
    private static final @NotNull HashMap<UUID, ArrayList<PendingConfirmation>> requests = new HashMap<>(4);

    /**
     * Request the player to confirm something, and run the runnable only after they use the confirm command.
     *
     * @param player      The command sender to ask the confirmation.
     * @param onConfirm   The runnable to run when they confirm.
     * @param description The description to show the player when listing their confirmations.
     * @param hash        The hash of the confirmation, to avoid multiple confirmations that do the same thing.
     * @return Whether if there was no other confirmation for this player with this hash.
     */
    public static boolean requestConfirmation(@NotNull CommandSender player, @NotNull Runnable onConfirm, @NotNull Supplier<String> description, int hash) {
        return requestConfirmation(player instanceof Player p ? p.getUniqueId() : null, onConfirm, description, hash);
    }

    /**
     * Request the player to confirm something, and run the runnable only after they use the confirm command.
     *
     * @param player      The ID of the player to ask the confirmation. Null for console.
     * @param onConfirm   The runnable to run when they confirm.
     * @param description The description to show the player when listing their confirmations.
     * @param hash        The hash of the confirmation, to avoid multiple confirmations that do the same thing.
     * @return Whether if there was no other confirmation for this player with this hash.
     */
    public static boolean requestConfirmation(@Nullable UUID player, @NotNull Runnable onConfirm, @NotNull Supplier<String> description, int hash) {
        if (player == null) player = console;
        ArrayList<PendingConfirmation> onConfirmList = requests.get(player);

        if (onConfirmList == null) {
            onConfirmList = new ArrayList<>(3);
            requests.put(player, onConfirmList);
        } else {
            for (PendingConfirmation confirmation : onConfirmList) {
                if (confirmation.hash == hash) return false;
            }
        }

        onConfirmList.add(new PendingConfirmation(onConfirm, description, hash));
        return true;
    }

    /**
     * Cancels all confirmations that have this hash from all players.
     *
     * @param hash The hash to cancel confirmations.
     */
    public static void cancelConfirmations(int hash) {
        cancelConfirmations(hash, null);
    }

    /**
     * Cancels all confirmations that have this hash from all players.
     *
     * @param hash           The hash to cancel confirmations.
     * @param playerConsumer A consumer to run for each player found that had the confirmation.
     */
    public static void cancelConfirmations(int hash, @Nullable Consumer<UUID> playerConsumer) {
        requests.entrySet().removeIf(entry -> {
            if (entry.getValue().removeIf(confirmation -> confirmation.hash == hash)) {
                if (playerConsumer != null) playerConsumer.accept(entry.getKey() == console ? null : entry.getKey());
            }
            return entry.getValue().isEmpty();
        });
    }

    /**
     * Cancel a confirmation that has this hash from a player.
     *
     * @param player The ID of the player to cancel the confirmations. Null for console.
     * @param hash   The hash of the confirmation.
     * @return Whether any confirmation was cancelled.
     */
    public static boolean cancelConfirmation(@Nullable UUID player, int hash) {
        if (player == null) player = console;
        ArrayList<PendingConfirmation> confirmations = requests.get(player);
        if (confirmations == null) return false;
        boolean anyRemoved = confirmations.removeIf(confirmation -> confirmation.hash == hash);
        if (confirmations.isEmpty()) requests.remove(player);
        return anyRemoved;
    }

    /**
     * Cancels all requests of confirmations of a player.
     *
     * @param player The ID of the player to cancel the confirmations. Null for console.
     * @return Whether the player had any confirmations.
     */
    public static boolean cancelConfirmations(@Nullable UUID player) {
        if (player == null) player = console;
        return requests.remove(player) != null;
    }

    @Override
    public @NotNull String getName() {
        return "confirm";
    }

    @Override
    protected @NotNull CommandRunnable getNoPermissionRunnable() {
        return CommandUtil.noPermissionRunnable();
    }

    @SuppressWarnings("deprecation")
    @Override
    public void run(@NotNull String label, @NotNull CommandSender sender, @NotNull String[] args) {
        MessageSender lang = TerrainerPlugin.getLanguage();
        ArrayList<PendingConfirmation> confirmations = requests.get(sender instanceof Player player ? player.getUniqueId() : console);

        if (confirmations == null) {
            lang.send(sender, lang.get("Confirm.Error.Nothing Pending"));
            return;
        }

        Integer id = null;

        if (args.length > 1) {
            if (args[1].equalsIgnoreCase("list") || args[1].equalsIgnoreCase(lang.get("Commands.Confirm.List"))) {
                HashMap<Integer, ArrayList<PendingConfirmation>> pages = ObjectUtils.splitIntoPages(confirmations, 5);
                int page = 1;

                if (args.length > 2) {
                    try {
                        page = Integer.parseInt(args[2]);
                    } catch (NumberFormatException e) {
                        lang.send(sender, lang.get("General.Not A Number").replace("<value>", args[2]));
                        return;
                    }
                    if (page > pages.size()) page = pages.size();
                    if (page < 1) page = 1;
                }

                lang.send(sender, lang.get("Confirm.Header").replace("<page>", Integer.toString(page)).replace("<total>", Integer.toString(pages.size())));
                String entryFormat = lang.getColored("Confirm.Entry");
                int entryId = (page * 5) - 5;

                for (PendingConfirmation confirmation : pages.get(page)) {
                    String description;

                    try {
                        description = confirmation.description.get();
                        if (description == null) description = "null";
                    } catch (Throwable t) {
                        Terrainer.logger().log("Unable to get description of confirmation " + entryId + " for player " + sender.getName() + ":", ConsoleLogger.Level.WARN);
                        t.printStackTrace();
                        description = "null";
                    }

                    TextComponent text = new TextComponent(entryFormat.replace("<id>", Integer.toString(entryId)).replace("<description>", description));
                    text.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/" + label + " " + args[0] + " " + entryId));
                    sender.spigot().sendMessage(text);
                    entryId++;
                }

                if (page != pages.size()) {
                    String command = "/" + label + " " + args[0] + " " + args[1] + " " + (page + 1);
                    TextComponent text = new TextComponent(lang.getColored("Confirm.Footer").replace("<command>", command));
                    text.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command));
                    sender.spigot().sendMessage(text);
                }

                return;
            } else {
                try {
                    id = Integer.parseInt(args[1]);
                } catch (NumberFormatException e) {
                    lang.send(sender, lang.get("Invalid Arguments.Error").replace("<label>", label)
                            .replace("<label2>", args[0]).replace("<args>", lang.get("Confirm.Arguments")));
                    return;
                }
                if (id >= confirmations.size() || id < 0) {
                    lang.send(sender, lang.get("Confirm.Error.Not Found"));
                    return;
                }
            }
        }

        if (id == null) {
            if (confirmations.size() > 1) {
                lang.send(sender, lang.get("Confirm.Error.Multiple").replace("<command>", "/" + label + " " + args[0] + " " + lang.get("Commands.Confirm.List")));
                return;
            } else {
                id = 0;
            }
        }

        try {
            Runnable runnable = confirmations.get(id).onConfirm;
            confirmations.remove(id.intValue());
            if (confirmations.isEmpty()) {
                requests.remove(sender instanceof Player player ? player.getUniqueId() : console);
            }
            runnable.run();
        } catch (Throwable e) {
            lang.send(sender, lang.get("Confirm.Error.Run"));
            Terrainer.logger().log("Something went wrong while running the confirmation " + id + " for player " + sender.getName() + ":", ConsoleLogger.Level.WARN);
            e.printStackTrace();
        }
    }

    private record PendingConfirmation(@NotNull Runnable onConfirm, @NotNull Supplier<String> description, int hash) {
    }
}
