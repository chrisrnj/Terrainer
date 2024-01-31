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

package com.epicnicity322.terrainer.bukkit;

import com.epicnicity322.epicpluginlib.bukkit.command.Command;
import com.epicnicity322.epicpluginlib.bukkit.command.CommandManager;
import com.epicnicity322.epicpluginlib.bukkit.lang.MessageSender;
import com.epicnicity322.epicpluginlib.bukkit.logger.Logger;
import com.epicnicity322.epicpluginlib.core.config.ConfigurationHolder;
import com.epicnicity322.epicpluginlib.core.logger.ConsoleLogger;
import com.epicnicity322.terrainer.bukkit.command.*;
import com.epicnicity322.terrainer.bukkit.event.flag.FlagSetEvent;
import com.epicnicity322.terrainer.bukkit.event.flag.FlagUnsetEvent;
import com.epicnicity322.terrainer.bukkit.event.terrain.TerrainAddEvent;
import com.epicnicity322.terrainer.bukkit.event.terrain.TerrainRemoveEvent;
import com.epicnicity322.terrainer.bukkit.hook.economy.VaultHook;
import com.epicnicity322.terrainer.bukkit.hook.nms.ReflectionHook;
import com.epicnicity322.terrainer.bukkit.listener.*;
import com.epicnicity322.terrainer.bukkit.util.BukkitPlayerUtil;
import com.epicnicity322.terrainer.bukkit.util.EconomyHandler;
import com.epicnicity322.terrainer.bukkit.util.NMSHandler;
import com.epicnicity322.terrainer.core.Terrainer;
import com.epicnicity322.terrainer.core.config.Configurations;
import com.epicnicity322.terrainer.core.terrain.TerrainManager;
import com.epicnicity322.terrainer.core.util.PlayerUtil;
import com.epicnicity322.yamlhandler.Configuration;
import com.epicnicity322.yamlhandler.ConfigurationSection;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.*;

public final class TerrainerPlugin extends JavaPlugin {
    private static final @NotNull MessageSender lang = new MessageSender(() -> Configurations.CONFIG.getConfiguration().getString("Language").orElse("EN_US"), Configurations.LANG_EN_US.getDefaultConfiguration());
    private static final @NotNull Logger logger = new Logger(Terrainer.logger().getPrefix());
    private static @Nullable TerrainerPlugin instance;
    private static @Nullable EconomyHandler economyHandler;
    private static @NotNull NMSHandler nmsHandler = new NMSHandler() {
        @Override
        public int spawnMarkerEntity(@NotNull Player player, int x, int y, int z) {
            return 0;
        }

        @Override
        public void killEntity(@NotNull Player player, int entityID) {
        }
    };

    static {
        Terrainer.setLang(lang);
        Terrainer.setLogger(logger);
        //TODO: Translate to languages.
        lang.addLanguage("EN_US", Configurations.LANG_EN_US);
        lang.addLanguage("PT_BR", Configurations.LANG_EN_US);
        lang.addLanguage("ES_LA", Configurations.LANG_EN_US);
    }

    private final @NotNull BordersCommand bordersCommand = new BordersCommand(this);
    private final @NotNull NamespacedKey selectorWandKey = new NamespacedKey(this, "selector-wand");
    private final @NotNull NamespacedKey infoWandKey = new NamespacedKey(this, "info-wand");
    private final @NotNull Set<Command> commands = Set.of(bordersCommand, new ClaimCommand(), new ConfirmCommand(), new DefineCommand(), new DeleteCommand(), new DescriptionCommand(), new FlagCommand(), new PermissionCommand.GrantCommand(), new PermissionCommand.RevokeCommand(), new InfoCommand(bordersCommand), new LimitCommand(), new ListCommand(), new PosCommand.Pos1Command(), new PosCommand.Pos2Command(), new Pos3DCommand.Pos13DCommand(), new Pos3DCommand.Pos23DCommand(), new PriorityCommand(), new ReloadCommand(), new RenameCommand(), new ShopCommand(), new TransferCommand(), new WandCommand(selectorWandKey, infoWandKey));
    private final @NotNull BukkitPlayerUtil playerUtil = new BukkitPlayerUtil(this);
    private final @NotNull PreLoginListener preLoginListener = new PreLoginListener();

    public TerrainerPlugin() {
        instance = this;
        logger.setLogger(getLogger());
    }

    public static @NotNull MessageSender getLanguage() {
        return lang;
    }

    public static @NotNull BukkitPlayerUtil getPlayerUtil() {
        if (instance == null) {
            throw new UnsupportedOperationException("TerrainerPlugin was not instantiated yet.");
        }
        return instance.playerUtil;
    }

    public static @Nullable EconomyHandler getEconomyHandler() {
        return economyHandler;
    }

    public static @NotNull NMSHandler getNMSHandler() {
        return nmsHandler;
    }

    /**
     * Reloads Terrainer's configurations and listeners.
     *
     * @return If every config loaded successfully.
     */
    public static boolean reload() {
        ConsoleLogger<?> logger = Terrainer.logger();
        HashMap<ConfigurationHolder, Exception> exceptions = Configurations.loader().loadConfigurations();

        exceptions.forEach((config, exception) -> {
            logger.log("Unable to load " + config.getPath().getFileName() + " configuration:", ConsoleLogger.Level.ERROR);
            exception.printStackTrace();
            logger.log("Default values will be used!", ConsoleLogger.Level.ERROR);
        });

        Configuration config = Configurations.CONFIG.getConfiguration();

        // Entry Cancelled Commands
        EnterLeaveListener.setCommandsOnEntryCancelled(config.getCollection("Commands When TerrainEnterEvent Cancelled on Join or Create", Object::toString));

        // Block Limits
        Map<String, Long> defaultBlockLimits = Collections.emptyMap();
        ConfigurationSection blockLimits = config.getConfigurationSection("Block Limits");
        if (blockLimits != null) {
            Map<String, Object> nodes = blockLimits.getNodes();
            defaultBlockLimits = new HashMap<>((int) (nodes.size() / 0.75) + 1);
            for (Map.Entry<String, Object> node : nodes.entrySet()) {
                try {
                    defaultBlockLimits.put(node.getKey(), Long.parseLong(node.getValue().toString()));
                } catch (NumberFormatException ignored) {
                }
            }
        }
        PlayerUtil.setDefaultBlockLimits(defaultBlockLimits);

        // Claim Limits
        Map<String, Integer> defaultClaimLimits = Collections.emptyMap();
        ConfigurationSection claimLimits = config.getConfigurationSection("Claim Limits");
        if (claimLimits != null) {
            Map<String, Object> nodes = claimLimits.getNodes();
            defaultClaimLimits = new HashMap<>((int) (nodes.size() / 0.75) + 1);
            for (Map.Entry<String, Object> node : nodes.entrySet()) {
                try {
                    defaultClaimLimits.put(node.getKey(), Integer.parseInt(node.getValue().toString()));
                } catch (NumberFormatException ignored) {
                }
            }
        }
        PlayerUtil.setDefaultClaimLimits(defaultClaimLimits);

        // Instance required from now on
        if (instance == null) return false;

        // Borders Particle
        String particleName = config.getString("Borders.Particle").orElse("CLOUD").toUpperCase(Locale.ROOT);
        try {
            instance.bordersCommand.setParticle(Particle.valueOf(particleName));
        } catch (IllegalArgumentException e) {
            logger.log("A particle with name '" + particleName + "' was not found. Using CLOUD as particle for borders.");
            instance.bordersCommand.setParticle(Particle.CLOUD);
        }

        // Selection and Info Items
        SelectionListener.reloadItems(instance.selectorWandKey, instance.infoWandKey);
        return exceptions.isEmpty();
    }

    @Override
    public void onLoad() {
        // Registering PreLoginListener without checking if the plugin is enabled.

        boolean enabled = isEnabled();
        try {
            Field isEnabled = JavaPlugin.class.getDeclaredField("isEnabled");
            isEnabled.setAccessible(true);
            isEnabled.set(this, true);
            getServer().getPluginManager().registerEvents(preLoginListener, this);
            isEnabled.set(this, enabled);
        } catch (NoSuchFieldException | IllegalAccessException ignored) {
        }
    }

    @Override
    public void onEnable() {
        if (reload()) logger.log("Configurations loaded successfully.");

        PluginManager pm = getServer().getPluginManager();

        if (pm.getPlugin("Vault") != null) {
            try {
                economyHandler = new VaultHook();
                logger.log("Vault was found and hooked!");
            } catch (Throwable t) {
                logger.log("Vault was found, but there is no economy handling plugin.", ConsoleLogger.Level.WARN);
            }
        }

        try {
            nmsHandler = new ReflectionHook();
        } catch (Throwable t) {
            logger.log("Unknown issue happened while using reflection. Markers will not work!", ConsoleLogger.Level.WARN);
            t.printStackTrace();
        }

        pm.registerEvents(new EnterLeaveListener(), this);
        pm.registerEvents(new PaperListener(), this);
        pm.registerEvents(new FlagListener(), this);
        pm.registerEvents(new ProtectionsListener(this, bordersCommand), this);
        pm.registerEvents(new SelectionListener(selectorWandKey, infoWandKey, bordersCommand), this);

        TerrainManager.setOnTerrainAddListener(event -> {
            var add = new TerrainAddEvent(event.terrain());
            pm.callEvent(add);
            return add.isCancelled();
        });
        TerrainManager.setOnTerrainRemoveListener(event -> {
            var remove = new TerrainRemoveEvent(event.terrain());
            pm.callEvent(remove);
            return remove.isCancelled();
        });
        TerrainManager.setOnFlagSetListener(event -> {
            var set = new FlagSetEvent<>(event);
            pm.callEvent(set);
            return set.isCancelled();
        });
        TerrainManager.setOnFlagUnsetListener(event -> {
            var unset = new FlagUnsetEvent<>(event.terrain(), event.flag());
            pm.callEvent(unset);
            return unset.isCancelled();
        });

        loadCommands();

        // Terrains might have data of other plugins, so they load when the server is done loading.
        getServer().getScheduler().runTask(this, () -> {
            try {
                TerrainManager.load();
                logger.log("Terrains loaded.");
            } catch (IOException e) {
                logger.log("Unable to load terrains due to exception:", ConsoleLogger.Level.ERROR);
                e.printStackTrace();
            }
            HandlerList.unregisterAll(preLoginListener);
        });
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onDisable() {
        TerrainManager.save();
        var players = Bukkit.getOnlinePlayers();
        if (!players.isEmpty()) {
            logger.log("Terrainer will kick all players to prevent damage to terrains.");
        }
        for (Player p : players) {
            p.kickPlayer(lang.getColored("Protections.Kick Message").replace("<default>", Objects.requireNonNullElse(getServer().getShutdownMessage(), "Server stopped")));
        }
    }

    private void loadCommands() {
        PluginCommand main = getCommand("terrainer");
        if (main == null) {
            logger.log("Unable to get 'terrainer' command. Commands will not be loaded.", ConsoleLogger.Level.ERROR);
            return;
        }
        CommandManager.registerCommand(main, commands);
    }
}
