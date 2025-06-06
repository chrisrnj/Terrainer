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

package com.epicnicity322.terrainer.bukkit;

import com.epicnicity322.epicpluginlib.bukkit.command.CommandManager;
import com.epicnicity322.epicpluginlib.bukkit.lang.MessageSender;
import com.epicnicity322.epicpluginlib.bukkit.logger.Logger;
import com.epicnicity322.epicpluginlib.bukkit.reflection.ReflectionUtil;
import com.epicnicity322.epicpluginlib.core.EpicPluginLib;
import com.epicnicity322.epicpluginlib.core.config.ConfigurationHolder;
import com.epicnicity322.epicpluginlib.core.logger.ConsoleLogger;
import com.epicnicity322.epicpluginlib.core.tools.GitHubUpdateChecker;
import com.epicnicity322.terrainer.bukkit.command.TerrainerCommand;
import com.epicnicity322.terrainer.bukkit.command.impl.*;
import com.epicnicity322.terrainer.bukkit.event.flag.FlagSetEvent;
import com.epicnicity322.terrainer.bukkit.event.flag.FlagUnsetEvent;
import com.epicnicity322.terrainer.bukkit.event.terrain.TerrainAddEvent;
import com.epicnicity322.terrainer.bukkit.event.terrain.TerrainRemoveEvent;
import com.epicnicity322.terrainer.bukkit.hook.EconomyHandler;
import com.epicnicity322.terrainer.bukkit.hook.NMSHandler;
import com.epicnicity322.terrainer.bukkit.hook.economy.VaultHook;
import com.epicnicity322.terrainer.bukkit.hook.nms.ReflectionHook;
import com.epicnicity322.terrainer.bukkit.hook.placeholderapi.TerrainerPlaceholderExpansion;
import com.epicnicity322.terrainer.bukkit.listener.*;
import com.epicnicity322.terrainer.bukkit.util.BukkitPlayerUtil;
import com.epicnicity322.terrainer.bukkit.util.TaskFactory;
import com.epicnicity322.terrainer.bukkit.util.ToggleableListener;
import com.epicnicity322.terrainer.core.Terrainer;
import com.epicnicity322.terrainer.core.TerrainerVersion;
import com.epicnicity322.terrainer.core.config.Configurations;
import com.epicnicity322.terrainer.core.flag.Flags;
import com.epicnicity322.terrainer.core.terrain.TerrainManager;
import com.epicnicity322.terrainer.core.util.PlayerUtil;
import com.epicnicity322.yamlhandler.Configuration;
import com.epicnicity322.yamlhandler.ConfigurationSection;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.ToIntFunction;
import java.util.function.ToLongFunction;

public final class TerrainerPlugin extends JavaPlugin {
    private static final @NotNull MessageSender lang = new MessageSender(() -> Configurations.CONFIG.getConfiguration().getString("Language").orElse("EN_US"), Configurations.LANG_EN_US.getDefaultConfiguration());
    private static final @NotNull Logger logger = new Logger(Terrainer.logger().getPrefix());
    private static final @NotNull TreeSet<Map.Entry<String, Long>> defaultBlockLimits = new TreeSet<>(Comparator.comparingLong((ToLongFunction<Map.Entry<String, Long>>) Map.Entry::getValue).reversed().thenComparing(Map.Entry::getKey));
    private static final @NotNull TreeSet<Map.Entry<String, Integer>> defaultClaimLimits = new TreeSet<>(Comparator.comparingInt((ToIntFunction<Map.Entry<String, Integer>>) Map.Entry::getValue).reversed().thenComparing(Map.Entry::getKey));
    private static final @NotNull AtomicBoolean nestedTerrainsCountTowardsBlockLimit = new AtomicBoolean(false), perWorldBlockLimit = new AtomicBoolean(false), perWorldClaimLimit = new AtomicBoolean(false), sumIfTheresMultipleBlockLimitPermissions = new AtomicBoolean(true), sumIfTheresMultipleClaimLimitPermissions = new AtomicBoolean(true);
    private static final @NotNull AtomicBoolean updateFound = new AtomicBoolean(false);
    private static final @NotNull AtomicReference<TaskFactory.CancellableTask> updateChecker = new AtomicReference<>();
    private static @Nullable TerrainerPlugin instance;
    private static @Nullable EconomyHandler economyHandler;

    static {
        Terrainer.setLang(lang);
        Terrainer.setLogger(logger);
        //noinspection deprecation - backwards compatibility
        Flags.setEffectChecker(value -> PotionEffectType.getByName(value) != null);
        //TODO: Translate to languages.
        lang.addLanguage("EN_US", Configurations.LANG_EN_US);
        lang.addLanguage("PT_BR", Configurations.LANG_EN_US);
        lang.addLanguage("ES_LA", Configurations.LANG_EN_US);
    }

    private final @NotNull BukkitPlayerUtil playerUtil = new BukkitPlayerUtil(this, getNMSHandler(), defaultBlockLimits, defaultClaimLimits, nestedTerrainsCountTowardsBlockLimit, perWorldBlockLimit, perWorldClaimLimit, sumIfTheresMultipleBlockLimitPermissions, sumIfTheresMultipleClaimLimitPermissions);
    private final @NotNull TaskFactory taskFactory = new TaskFactory(this);
    private final @NotNull BordersCommand bordersCommand = new BordersCommand(this);
    private final @NotNull InfoCommand infoCommand = new InfoCommand(bordersCommand);
    private final @NotNull SelectionListener selectionListener = new SelectionListener(new NamespacedKey(this, "selector-wand"), new NamespacedKey(this, "info-wand"), infoCommand);
    private final @NotNull Set<TerrainerCommand> commands = Set.of(bordersCommand, new ClaimCommand(), new ConfirmCommand(), new DefineCommand(), new DeleteCommand(), new DescriptionCommand(), new FlagCommand(), new PermissionCommand.GrantCommand(), new PermissionCommand.RevokeCommand(), infoCommand, new LimitCommand(), new ListCommand(infoCommand), new PosCommand.Pos1Command(), new PosCommand.Pos2Command(), new Pos3DCommand.Pos13DCommand(), new Pos3DCommand.Pos23DCommand(), new PriorityCommand(), new ReloadCommand(), new RenameCommand(), new ResizeCommand(), new ShopCommand(), new TeleportCommand(this), new TransferCommand(), new WandCommand());
    private final @NotNull AtomicBoolean enterLeaveEvents = new AtomicBoolean(true);
    private final @NotNull PreLoginListener preLoginListener = new PreLoginListener();
    private final @NotNull ProtectionsListener protectionsListener;
    private final @NotNull PistonListener pistonListener;
    private final @NotNull BlockFromToListener blockFromToListener;
    private final @NotNull CreatureSpawnListener creatureSpawnListener;
    private final @NotNull EnterLeaveListener enterLeaveListener = new EnterLeaveListener();
    private final @NotNull EntityMoveListener entityMoveListener = new EntityMoveListener();
    private final boolean reloadDetected;

    public TerrainerPlugin() {
        instance = this;
        reloadDetected = !getServer().getWorlds().isEmpty() || !getServer().getOnlinePlayers().isEmpty();
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

    private static @NotNull NMSHandler getNMSHandler() {
        try {
            return new ReflectionHook();
        } catch (Throwable t) {
            logger.log("Unknown issue happened while using reflection. Markers will not work!", ConsoleLogger.Level.WARN);
            t.printStackTrace();
            // Dummy NMSHandler.
            return new NMSHandler() {
                @Override
                public @NotNull PlayerUtil.SpawnedMarker spawnMarkerEntity(@NotNull Player player, int x, int y, int z, boolean edge, boolean selection) {
                    return new PlayerUtil.SpawnedMarker(0, new UUID(0, 0), new Object());
                }

                @Override
                public void killEntity(@NotNull Player player, @NotNull PlayerUtil.SpawnedMarker marker) {
                }

                @Override
                public void updateSelectionMarkerToTerrainMarker(@NotNull PlayerUtil.SpawnedMarker marker, @NotNull Player player) {
                }
            };
        }
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
        defaultBlockLimits.clear();
        ConfigurationSection blockLimits = config.getConfigurationSection("Block Limits");
        if (blockLimits != null) {
            Map<String, Object> nodes = blockLimits.getNodes();
            for (Map.Entry<String, Object> node : nodes.entrySet()) {
                try {
                    defaultBlockLimits.add(Map.entry(node.getKey(), Long.parseLong(node.getValue().toString())));
                } catch (NumberFormatException ignored) {
                }
            }
        }

        // Claim Limits
        defaultClaimLimits.clear();
        ConfigurationSection claimLimits = config.getConfigurationSection("Claim Limits");
        if (claimLimits != null) {
            Map<String, Object> nodes = claimLimits.getNodes();
            for (Map.Entry<String, Object> node : nodes.entrySet()) {
                try {
                    defaultClaimLimits.add(Map.entry(node.getKey(), Integer.parseInt(node.getValue().toString())));
                } catch (NumberFormatException ignored) {
                }
            }
        }

        // Limit Options
        nestedTerrainsCountTowardsBlockLimit.set(config.getBoolean("Limits.Nested Terrains Count Towards Block Limit").orElse(false));
        perWorldBlockLimit.set(config.getBoolean("Limits.Per World Block Limit").orElse(false));
        perWorldClaimLimit.set(config.getBoolean("Limits.Per World Claim Limit").orElse(false));
        sumIfTheresMultipleBlockLimitPermissions.set(config.getBoolean("Limits.Sum If Theres Multiple Block Limit Permissions").orElse(true));
        sumIfTheresMultipleClaimLimitPermissions.set(config.getBoolean("Limits.Sum If Theres Multiple Claim Limit Permissions").orElse(true));

        // Marker options
        try {
            ReflectionHook.setMarkerColors(Integer.parseInt(config.getString("Markers.Selection Color").orElse("FFFF55"), 16), Integer.parseInt(config.getString("Markers.Terrain Color").orElse("FFFFFF"), 16), Integer.parseInt(config.getString("Markers.Created Color").orElse("55FF55"), 16));
        } catch (NumberFormatException e) {
            logger.log("Marker colors could not be updated because of invalid hex codes. " + e.getMessage(), ConsoleLogger.Level.WARN);
        }
        ReflectionHook.setMarkerBlocks(Material.getMaterial(config.getString("Markers.Selection Block").orElse("GLOWSTONE")), Material.getMaterial(config.getString("Markers.Selection Edge Block").orElse("GOLD_BLOCK")), Material.getMaterial(config.getString("Markers.Terrain Block").orElse("DIAMOND_BLOCK")), Material.getMaterial(config.getString("Markers.Terrain Edge Block").orElse("GLASS")));

        // Instance required from now on
        if (instance == null) return false;

        // Loading listener performance settings
        instance.enterLeaveEvents.set(!config.getBoolean("Protections And Performance.Disable Enter Leave Events").orElse(false));
        instance.reloadListeners();

        // Selection and Info Items
        instance.selectionListener.reloadItems();

        // Command aliases and settings
        instance.commands.forEach(TerrainerCommand::reloadCommand);

        // Update checker
        instance.reloadUpdater();

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

        // Flag defaults are only loaded once to avoid potential issues, like players keeping the potion effects of
        //EFFECTS flag from a previous default.
        Flags.loadFlagDefaults();

        PluginManager pm = getServer().getPluginManager();

        // Hooks
        if (pm.getPlugin("Vault") != null) {
            try {
                economyHandler = new VaultHook();
                logger.log("Vault was found and hooked!");
            } catch (Throwable t) {
                logger.log("Vault was found, but there is no economy handling plugin.", ConsoleLogger.Level.WARN);
            }
        }
        if (pm.getPlugin("PlaceholderAPI") != null) {
            try {
                new TerrainerPlaceholderExpansion().register();
                logger.log("PlaceholderAPI was found, 'terrainer' placeholder registered!");
            } catch (Throwable t) {
                logger.log("PlaceholderAPI was found, but an unknown issue happened while registering the placeholder.", ConsoleLogger.Level.WARN);
            }
        }

        // Listeners
        if (ReflectionUtil.getClass("org.bukkit.event.entity.EntityMountEvent") != null) {
            pm.registerEvents(new MountListener(protectionsListener, enterLeaveEvents), this);
        } else if (ReflectionUtil.getClass("org.spigotmc.event.entity.EntityMountEvent") != null) {
            pm.registerEvents(new LegacyMountListener(protectionsListener, enterLeaveEvents), this);
        } else {
            logger.log("Could not find entity mount/dismount events, protections related to those events will not be enforced.", ConsoleLogger.Level.ERROR);
        }
        pm.registerEvents(protectionsListener, this);
        pm.registerEvents(new FlagListener(), this);
        pm.registerEvents(selectionListener, this);

        TerrainManager.setOnTerrainAddListener(event -> {
            var add = new TerrainAddEvent(event.terrain());
            getServer().getPluginManager().callEvent(add);
            return add.isCancelled();
        });
        TerrainManager.setOnTerrainRemoveListener(event -> {
            var remove = new TerrainRemoveEvent(event.terrain());
            getServer().getPluginManager().callEvent(remove);
            return remove.isCancelled();
        });
        TerrainManager.setOnFlagSetListener(event -> {
            var set = new FlagSetEvent<>(event);
            getServer().getPluginManager().callEvent(set);
            return set.isCancelled();
        });
        TerrainManager.setOnFlagUnsetListener(event -> {
            var unset = new FlagUnsetEvent<>(event);
            getServer().getPluginManager().callEvent(unset);
            return unset.isCancelled();
        });

        // Commands
        loadCommands();

        // Terrains
        try {
            TerrainManager.load();

            // Loading worlds and world load listener.
            for (World world : getServer().getWorlds()) TerrainManager.loadWorld(world.getUID(), world.getName());
            pm.registerEvents(new WorldLoadListener(), this);

            logger.log(TerrainManager.allTerrains().size() + " terrains loaded.");
        } catch (IOException e) {
            logger.log("Unable to load terrains due to exception:", ConsoleLogger.Level.ERROR);
            e.printStackTrace();
        }

        taskFactory.runGlobalAsyncTask(() -> {
            // Daily tasks once all worlds are loaded. (Terrain pruner, taxes, etc...)
            Terrainer.loadDailyTimer();
            HandlerList.unregisterAll(preLoginListener);
            if (reloadDetected) {
                logger.log("You should never reload Terrainer, otherwise bad things could happen, such as: Protections failing, players keeping infinite potion effects, or some terrains ceasing to exist!", ConsoleLogger.Level.ERROR);
            }
        });
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onDisable() {
        Scoreboard mainScoreboard = getServer().getScoreboardManager().getMainScoreboard();

        Team createdTeam = mainScoreboard.getTeam("TRcreatedTeam");
        if (createdTeam != null) createdTeam.unregister();
        Team selectionTeam = mainScoreboard.getTeam("TRselectionTeam");
        if (selectionTeam != null) selectionTeam.unregister();
        Team terrainTeam = mainScoreboard.getTeam("TRterrainTeam");
        if (terrainTeam != null) terrainTeam.unregister();

        boolean kickPlayers = Configurations.CONFIG.getConfiguration().getBoolean("Kick Players On Disable").orElse(false) && (ReflectionUtil.getClass("io.papermc.paper.threadedregions.RegionizedServer") == null);
        if (kickPlayers) {
            var players = Bukkit.getOnlinePlayers();
            if (!players.isEmpty()) logger.log("Terrainer will kick all players to prevent damage to terrains.");
            for (Player p : players) {
                p.kickPlayer(lang.getColored("Protections.Shutdown Kick Message").replace("<default>", Objects.requireNonNullElse(getServer().getShutdownMessage(), "Server stopped")));
            }
        }

        Terrainer.stopDailyTimer();
        TerrainManager.save();
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

    private void reloadListeners() {
        Configuration config = Configurations.CONFIG.getConfiguration();
        boolean enterLeaveEvents = this.enterLeaveEvents.get();
        boolean entityMoveEvent = !config.getBoolean("Protections And Performance.Disable Entity Move Event").orElse(false);
        boolean pistonEvents = !config.getBoolean("Protections And Performance.Disable Piston Events").orElse(false);
        boolean blockFromToEvent = !config.getBoolean("Protections And Performance.Disable Block From To Event").orElse(false);
        boolean creatureSpawnEvent = !config.getBoolean("Protections And Performance.Disable Creature Spawn Event").orElse(false);

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

    private void reloadUpdater() {
        if (updateFound.get()) return;

        if (!Configurations.CONFIG.getConfiguration().getBoolean("Update Checker.Enabled").orElse(true)) {
            TaskFactory.CancellableTask updaterTask = updateChecker.getAndSet(null);
            if (updaterTask != null) updaterTask.cancel();
            return;
        }

        if (updateChecker.get() == null) {
            GitHubUpdateChecker updater = new GitHubUpdateChecker("chrisrnj/Terrainer", TerrainerVersion.VERSION);
            AtomicBoolean sentFirstMessage = new AtomicBoolean(false);

            TaskFactory.CancellableTask updaterTask = taskFactory.runGlobalTaskAtFixedRate(checkerTask -> {
                boolean log = !sentFirstMessage.getAndSet(true) || Configurations.CONFIG.getConfiguration().getBoolean("Update Checker.Log Messages").orElse(false);

                if (log) logger.log("&7Checking for updates...");

                updater.check((available, version) -> {
                    if (!available) {
                        if (log) logger.log("&7No updates found.");
                    } else {
                        checkerTask.cancel();
                        updateChecker.set(null);

                        if (!updateFound.getAndSet(true)) {
                            // Alert task.
                            taskFactory.runGlobalTaskAtFixedRate(task -> logger.log("Update v" + version + " available! Download at https://github.com/chrisrnj/Terrainer/releases/latest"), 72000);
                            // Alert message.
                            getServer().getPluginManager().registerEvents(new Listener() {
                                @EventHandler
                                public void onJoin(PlayerJoinEvent event) {
                                    Player player = event.getPlayer();
                                    if (player.hasPermission("terrainer.updateavailablealert")) {
                                        lang.send(player, false, lang.get("Update Available").replace("<version>", version.getVersion()));
                                    }
                                }
                            }, this);
                        }
                    }
                }, (result, exception) -> {
                    if (log) logger.log("&cCould not check for updates.");
                });
            }, 72000);

            updateChecker.set(updaterTask);
        }
    }
}
