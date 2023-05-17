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
     * @return The identifier of this request, used for cancelling on {@link #cancelConfirmation(CommandSender, UUID)}.
     */
    public static @NotNull UUID requestConfirmation(@NotNull CommandSender player, @NotNull Runnable onConfirm, @NotNull Supplier<String> description) {
        return requestConfirmation(player instanceof Player p ? p.getUniqueId() : null, onConfirm, description);
    }

    /**
     * Request the player to confirm something, and run the runnable only after they use the confirm command.
     *
     * @param player      The ID of the player to ask the confirmation. Null for console.
     * @param onConfirm   The runnable to run when they confirm.
     * @param description The description to show the player when listing their confirmations.
     * @return The identifier of this request, used for cancelling on {@link #cancelConfirmation(CommandSender, UUID)}.
     */
    public static @NotNull UUID requestConfirmation(@Nullable UUID player, @NotNull Runnable onConfirm, @NotNull Supplier<String> description) {
        if (player == null) player = console;
        ArrayList<PendingConfirmation> onConfirmList = requests.computeIfAbsent(player, k -> new ArrayList<>(3));
        UUID requestID = UUID.randomUUID();
        onConfirmList.add(new PendingConfirmation(requestID, onConfirm, description));
        return requestID;
    }

    /**
     * Cancels the request for a confirmation of a player.
     *
     * @param player       The player to remove the request.
     * @param confirmation The ID of the request to cancel.
     * @return Whether a confirmation with the ID was present.
     */
    public static boolean cancelConfirmation(@NotNull CommandSender player, @NotNull UUID confirmation) {
        return cancelConfirmation(player instanceof Player p ? p.getUniqueId() : null, confirmation);
    }

    /**
     * Cancels the request for a confirmation of a player.
     *
     * @param player       The ID of the player to remove the request. Null for console.
     * @param confirmation The ID of the request to cancel.
     * @return Whether a confirmation with the ID was present.
     */
    public static boolean cancelConfirmation(@Nullable UUID player, @NotNull UUID confirmation) {
        if (player == null) player = console;
        ArrayList<PendingConfirmation> confirmations = requests.get(player);
        try {
            return confirmations.removeIf(pendingConfirmation -> pendingConfirmation.requestID().equals(confirmation));
        } finally {
            if (confirmations.isEmpty()) requests.remove(player);
        }
    }

    /**
     * Cancels all requests of confirmations of a player.
     *
     * @param player The player to cancel the confirmations.
     * @return Whether the player had any confirmations.
     */
    public static boolean cancelConfirmations(@NotNull CommandSender player) {
        return cancelConfirmations(player instanceof Player p ? p.getUniqueId() : null);
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

    private record PendingConfirmation(@NotNull UUID requestID, @NotNull Runnable onConfirm,
                                       @NotNull Supplier<String> description) {
    }
}
