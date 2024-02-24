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
import com.epicnicity322.epicpluginlib.bukkit.reflection.ReflectionUtil;
import com.epicnicity322.epicpluginlib.core.EpicPluginLib;
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
import com.epicnicity322.terrainer.bukkit.util.*;
import com.epicnicity322.terrainer.core.Terrainer;
import com.epicnicity322.terrainer.core.config.Configurations;
import com.epicnicity322.terrainer.core.terrain.Flags;
import com.epicnicity322.terrainer.core.terrain.TerrainManager;
import com.epicnicity322.terrainer.core.util.PlayerUtil;
import com.epicnicity322.yamlhandler.Configuration;
import com.epicnicity322.yamlhandler.ConfigurationSection;
import org.bukkit.*;
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
import java.util.concurrent.atomic.AtomicBoolean;

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
        Flags.setEffectChecker(value -> {
            var key = NamespacedKey.fromString(value);
            if (key == null) return false;
            return Registry.EFFECT.get(key) != null;
        });
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
    private final @NotNull TaskFactory taskFactory = new TaskFactory(this);
    private final @NotNull AtomicBoolean enterLeaveEvents = new AtomicBoolean(true);
    private final @NotNull ProtectionsListener protectionsListener;
    private final @NotNull PistonListener pistonListener;
    private final @NotNull BlockFromToListener blockFromToListener;
    private final @NotNull CreatureSpawnListener creatureSpawnListener;
    private final @NotNull EnterLeaveListener enterLeaveListener = new EnterLeaveListener();
    private final @NotNull EntityMoveListener entityMoveListener = new EntityMoveListener();

    public TerrainerPlugin() {
        instance = this;
        Terrainer.setPlayerUtil(playerUtil);
        logger.setLogger(getLogger());
        protectionsListener = new ProtectionsListener(this, bordersCommand);
        pistonListener = new PistonListener(protectionsListener);
        blockFromToListener = new BlockFromToListener(protectionsListener);
        creatureSpawnListener = new CreatureSpawnListener(protectionsListener);
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

        // Loading listener performance settings
        instance.enterLeaveEvents.set(!config.getBoolean("Performance.Disable Enter Leave Events").orElse(false));
        instance.reloadListeners();

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
        logger.log("WARNING: This plugin is still in development and some protections may not work as intended! No guarantee is made that any terrains or flags made in this version will work in the final version.", ConsoleLogger.Level.WARN);
        if (!EpicPluginLib.Platform.isPaper()) {
            logger.log("ATTENTION: Although Terrainer runs on spigot/craftbukkit, some protections are only possible with Paper. If you keep using spigot, your terrains are at risk of not being fully protected against all threats!", ConsoleLogger.Level.ERROR);
        }
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

        if (ReflectionUtil.getClass("org.bukkit.event.entity.EntityMountEvent") != null) {
            pm.registerEvents(new MountListener(protectionsListener, enterLeaveEvents), this);
        } else if (ReflectionUtil.getClass("org.spigotmc.event.entity.EntityMountEvent") != null) {
            pm.registerEvents(new LegacyMountListener(protectionsListener, enterLeaveEvents), this);
        } else {
            logger.log("Could not find entity mount/dismount events, protections related to these events will not be enforced.", ConsoleLogger.Level.ERROR);
        }
        pm.registerEvents(protectionsListener, this);
        pm.registerEvents(new FlagListener(), this);
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

        // Terrains might hold data of other plugins, so they only load after the server is done loading.
        taskFactory.runGlobalAsyncTask(() -> {
            try {
                TerrainManager.load();

                // Loading worlds and world load listener.
                for (World world : getServer().getWorlds()) TerrainManager.loadWorld(world.getUID(), world.getName());
                pm.registerEvents(new WorldLoadListener(), this);

                logger.log(TerrainManager.terrainsAmount() + " terrains loaded.");
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
        boolean kickPlayers = Configurations.CONFIG.getConfiguration().getBoolean("Kick Players On Disable").orElse(false) && (ReflectionUtil.getClass("io.papermc.paper.threadedregions.RegionizedServer") == null);
        if (kickPlayers) {
            var players = Bukkit.getOnlinePlayers();
            if (!players.isEmpty()) {
                logger.log("Terrainer will kick all players to prevent damage to terrains.");
            }
            for (Player p : players) {
                p.kickPlayer(lang.getColored("Protections.Kick Message").replace("<default>", Objects.requireNonNullElse(getServer().getShutdownMessage(), "Server stopped")));
            }
        }
    }

    public @NotNull TaskFactory getTaskFactory() {
        return taskFactory;
    }

    private void loadCommands() {
        PluginCommand main = getCommand("terrainer");
        if (main == null) {
            logger.log("Unable to get 'terrainer' command. Commands will not be loaded.", ConsoleLogger.Level.ERROR);
            return;
        }
        CommandManager.registerCommand(main, commands);
    }

    private void reloadListeners() {
        Configuration config = Configurations.CONFIG.getConfiguration();
        boolean enterLeaveEvents = this.enterLeaveEvents.get();
        boolean entityMoveEvent = !config.getBoolean("Performance.Disable Entity Move Event").orElse(false);
        boolean pistonEvents = !config.getBoolean("Performance.Disable Piston Events").orElse(false);
        boolean blockFromToEvent = !config.getBoolean("Performance.Disable Block From To Event").orElse(false);
        boolean creatureSpawnEvent = !config.getBoolean("Performance.Disable Creature Spawn Event").orElse(false);

        loadListener(enterLeaveListener, enterLeaveEvents, "Enter/Leave", true);
        if (ReflectionUtil.getClass("io.papermc.paper.event.entity.EntityMoveEvent") != null) {
            loadListener(entityMoveListener, enterLeaveEvents && entityMoveEvent, "Entity Move", false);
        } else {
            logger.log("Entity Move listener could not be enabled because it is a PaperMC event! Protections and features related to this event will not work.", ConsoleLogger.Level.WARN);
        }
        loadListener(pistonListener, pistonEvents, "Piston", true);
        loadListener(blockFromToListener, blockFromToEvent, "Block From/To", false);
        loadListener(creatureSpawnListener, creatureSpawnEvent, "Creature Spawn", false);
    }

    private void loadListener(@NotNull ToggleableListener listener, boolean register, @NotNull String name, boolean plural) {
        if (register) {
            if (!listener.registered.get()) {
                getServer().getPluginManager().registerEvents(listener, this);
                listener.registered.set(true);
            }
        } else {
            logger.log(name + " event" + (plural ? "s are" : " is") + " disabled! Protections and features related to " + (plural ? "these events" : "this event") + " will not work.", ConsoleLogger.Level.WARN);
            if (listener.registered.get()) {
                HandlerList.unregisterAll(listener);
                listener.registered.set(false);
            }
        }
    }
}
