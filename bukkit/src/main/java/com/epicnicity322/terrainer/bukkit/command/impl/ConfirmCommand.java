/*
 * Terrainer - A minecraft terrain claiming protection plugin.
 * Copyright (C) 2025-2026 Christiano Rangel
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
import com.epicnicity322.epicpluginlib.core.logger.ConsoleLogger;
import com.epicnicity322.epicpluginlib.core.util.ObjectUtils;
import com.epicnicity322.terrainer.bukkit.TerrainerPlugin;
import com.epicnicity322.terrainer.bukkit.command.TerrainerCommand;
import com.epicnicity322.terrainer.bukkit.util.CommandUtil;
import com.epicnicity322.terrainer.core.Terrainer;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;

public final class ConfirmCommand extends TerrainerCommand {
    private static final @NotNull UUID console = UUID.randomUUID();
    private static final @NotNull HashMap<UUID, TreeMap<Integer, PendingConfirmation>> requests = new HashMap<>(4);

    /**
     * Request the player to confirm something, and run the runnable only after they use the confirm command.
     *
     * @param player      The command sender to ask the confirmation.
     * @param onConfirm   The consumer to apply when they confirm.
     * @param description The description to show the player when listing their confirmations.
     * @param hash        The hash of the confirmation, to avoid multiple confirmations that do the same thing.
     * @return Whether if there was no other confirmation for this player with this hash.
     */
    public static boolean requestConfirmation(@NotNull CommandSender player, @NotNull Consumer<CommandSender> onConfirm, @NotNull Supplier<String> description, int hash) {
        return requestConfirmation(player instanceof Player p ? p.getUniqueId() : null, onConfirm, description, hash);
    }

    /**
     * Request the player to confirm something, and run the runnable only after they use the confirm command.
     *
     * @param player      The ID of the player to ask the confirmation. Null for console.
     * @param onConfirm   The consumer to apply when they confirm.
     * @param description The description to show the player when listing their confirmations.
     * @param hash        The hash of the confirmation, to avoid multiple confirmations that do the same thing.
     * @return Whether if there was no other confirmation for this player with this hash.
     */
    public static synchronized boolean requestConfirmation(@Nullable UUID player, @NotNull Consumer<CommandSender> onConfirm, @NotNull Supplier<String> description, int hash) {
        if (player == null) player = console;
        PendingConfirmation request = new PendingConfirmation(onConfirm, description, hash);
        TreeMap<Integer, PendingConfirmation> playerConfirmationRequests = requests.get(player);

        if (playerConfirmationRequests == null) {
            playerConfirmationRequests = new TreeMap<>();
            requests.put(player, playerConfirmationRequests);
        } else {
            if (playerConfirmationRequests.containsValue(request)) return false;
        }

        playerConfirmationRequests.put(playerConfirmationRequests.isEmpty() ? 0 : (playerConfirmationRequests.lastKey() + 1), request);
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
     * @param hash        The hash to cancel confirmations.
     * @param foundPlayer A consumer to run for each player found that had the confirmation.
     */
    public static synchronized void cancelConfirmations(int hash, @Nullable Consumer<UUID> foundPlayer) {
        requests.entrySet().removeIf(entry -> {
            Map<Integer, PendingConfirmation> playerConfirmationRequests = entry.getValue();

            if (playerConfirmationRequests.values().removeIf(confirmationRequest -> confirmationRequest.hash == hash)) {
                if (foundPlayer != null) foundPlayer.accept(entry.getKey() == console ? null : entry.getKey());
            }
            return playerConfirmationRequests.isEmpty();
        });
    }

    /**
     * Cancel a confirmation that has this hash from a player.
     *
     * @param player The ID of the player to cancel the confirmations. Null for console.
     * @param hash   The hash of the confirmation.
     * @return Whether any confirmation was cancelled.
     */
    public static synchronized boolean cancelConfirmation(@Nullable UUID player, int hash) {
        if (player == null) player = console;
        Map<Integer, PendingConfirmation> confirmations = requests.get(player);
        if (confirmations == null) return false;
        boolean anyRemoved = confirmations.values().removeIf(confirmation -> confirmation.hash == hash);
        if (confirmations.isEmpty()) requests.remove(player);
        return anyRemoved;
    }

    /**
     * Cancels all requests of confirmations of a player.
     *
     * @param player The ID of the player to cancel the confirmations. Null for console.
     * @return Whether the player had any confirmations.
     */
    public static synchronized boolean cancelConfirmations(@Nullable UUID player) {
        if (player == null) player = console;
        return requests.remove(player) != null;
    }

    public static synchronized @Nullable TreeMap<Integer, PendingConfirmation> getPendingConfirmations(@Nullable UUID player) {
        return requests.get(player == null ? console : player);
    }

    private static synchronized void confirm(@NotNull Map<Integer, PendingConfirmation> confirmations, int id, @NotNull CommandSender sender) {
        Consumer<CommandSender> consumer = confirmations.get(id).onConfirm;
        confirmations.remove(id);
        if (confirmations.isEmpty()) {
            requests.remove(sender instanceof Player player ? player.getUniqueId() : console);
        }
        consumer.accept(sender);
    }

    @Override
    public @NotNull String getName() {
        return "confirm";
    }

    @Override
    protected @NotNull CommandRunnable getNoPermissionRunnable() {
        return CommandUtil.noPermissionRunnable();
    }

    @Override
    public void reloadCommand() {
        setAliases(TerrainerPlugin.getLanguage().get("Commands.Confirm.Command"));
    }

    @SuppressWarnings("deprecation")
    @Override
    public void run(@NotNull String label, @NotNull CommandSender sender, @NotNull String[] args) {
        MessageSender lang = TerrainerPlugin.getLanguage();
        TreeMap<Integer, PendingConfirmation> confirmations = getPendingConfirmations(sender instanceof Player player ? player.getUniqueId() : null);

        if (confirmations == null) {
            lang.send(sender, lang.get("Confirm.Error.Nothing Pending"));
            return;
        }

        Integer id = null;

        if (args.length > 1) {
            // Displaying list of pending confirmations.
            if (args[1].equalsIgnoreCase("list") || args[1].equalsIgnoreCase(lang.get("Commands.Confirm.List"))) {
                HashMap<Integer, ArrayList<Map.Entry<Integer, PendingConfirmation>>> pages = ObjectUtils.splitIntoPages(confirmations.entrySet(), 5);
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

                for (Map.Entry<Integer, PendingConfirmation> confirmation : pages.get(page)) {
                    int entryId = confirmation.getKey();
                    String description;

                    try {
                        description = confirmation.getValue().description.get();
                        if (description == null) description = "null";
                    } catch (Throwable t) {
                        Terrainer.logger().log("Unable to get description of confirmation " + entryId + " for player " + sender.getName() + ":", ConsoleLogger.Level.WARN);
                        t.printStackTrace();
                        description = "null";
                    }

                    TextComponent text = new TextComponent(entryFormat.replace("<id>", Integer.toString(entryId)).replace("<description>", description));
                    text.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/" + label + " " + args[0] + " " + entryId));
                    sender.spigot().sendMessage(text);
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

                if (!confirmations.containsKey(id)) {
                    lang.send(sender, lang.get("Confirm.Error.Not Found"));
                    return;
                }
            }
        }

        if (id == null) {
            if (confirmations.size() > 1) {
                lang.send(sender, lang.get("Confirm.Error.Multiple").replace("<command>", "/" + label + " " + args[0] + " " + lang.get("Commands.Confirm.List")));
                return;
            } else id = confirmations.firstKey();
        }

        try {
            confirm(confirmations, id, sender);
        } catch (Throwable e) {
            lang.send(sender, lang.get("Confirm.Error.Run"));
            Terrainer.logger().log("Something went wrong while running the confirmation " + id + " for player " + sender.getName() + ":", ConsoleLogger.Level.WARN);
            e.printStackTrace();
        }
    }

    @Override
    protected @NotNull TabCompleteRunnable getTabCompleteRunnable() {
        return (completions, label, sender, args) -> {
            if (args.length != 2) return;

            if (args[1].isEmpty()) {
                Map<Integer, PendingConfirmation> pendingRequests = getPendingConfirmations(sender instanceof Player player ? player.getUniqueId() : null);
                if (pendingRequests != null && !pendingRequests.isEmpty()) {
                    pendingRequests.keySet().forEach(i -> completions.add(Integer.toString(i)));
                }
            }

            String list = TerrainerPlugin.getLanguage().get("Commands.Confirm.List");
            if (list.startsWith(args[1])) completions.add(list);
        };
    }

    public record PendingConfirmation(@NotNull Consumer<CommandSender> onConfirm,
                                      @NotNull Supplier<String> description, int hash) {
        @Override
        public boolean equals(Object o) {
            if (o == null || getClass() != o.getClass()) return false;

            PendingConfirmation that = (PendingConfirmation) o;
            return hash == that.hash;
        }

        @Override
        public int hashCode() {
            return hash;
        }
    }
}
