package neonique.cbcplugin_new;

import neonique.cbcplugin_new.commands.*;
import neonique.cbcplugin_new.managers.GameState;
import neonique.cbcplugin_new.services.ArmorTrimService;
import neonique.cbcplugin_new.managers.GameManager;
import neonique.cbcplugin_new.resourcepack.ResourcePackManager;
import neonique.cbcplugin_new.services.WeaponPresetService;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabExecutor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;
import java.util.stream.Collectors;


public final class CBCPlugin extends JavaPlugin implements Listener {

    private static CBCPlugin plugin;

    private static GameManager gameManager;
    private static ResourcePackManager resourcePackManager;

    private ArmorTrimService trimService;
    private WeaponPresetService weaponPresetService;

    private static Set<UUID> gameOperators;
    private static Set<UUID> gameAdmins;

    @Override
    public void onEnable() {

        // Plugin startup logic
        plugin = this;

        // Create config yml file if not existent
        plugin.saveDefaultConfig();

        System.out.println("=======================================");
        System.out.println("Starting the Crossbow Champions plugin (1.21.1) developed by Neonique");

        // Load permissions from config.yml
        loadPermissions();

        // Print out operator and administrator names
        List<String> operatorNames = new ArrayList<>(gameOperators).stream().map(uuid -> Bukkit.getOfflinePlayer(uuid).getName()).collect(Collectors.toList());
        System.out.println("CBC Operators (perms no. 2): " + String.join(", ", operatorNames));
        List<String> adminNames = new ArrayList<>(gameAdmins).stream().map(uuid -> Bukkit.getOfflinePlayer(uuid).getName()).collect(Collectors.toList());
        System.out.println("CBC Administrators (perms no. 1): " + String.join(", ", adminNames));

        // Create all required services
        trimService = new ArmorTrimService();

        weaponPresetService = new WeaponPresetService();
        weaponPresetService.loadWeaponPresets();

        resourcePackManager = new ResourcePackManager();

        // Register join server and leave server listeners
        getServer().getPluginManager().registerEvents(this, this);

        // Create game manager
        gameManager = new GameManager(this);

        // Register commands
        registerCommand("lobby", new LobbyCommand(gameManager));
        registerCommand("practice", new PracticeCommand(gameManager, gameManager.practiceManager));
        registerCommand("game", new GameCommand(gameManager));
        registerCommand("chat", new ChatCommand(gameManager, gameManager.getChatManager()));
        registerCommand("cbcevent", new CBCEventCommand(gameManager));
        registerCommand("cbcpack", new CBCPackCommand(resourcePackManager));
        registerCommand("alphaorder", new AlphaOrderCommand());

        Objects.requireNonNull(getCommand("getblockcoords")).setExecutor(new GetBlockLocationsCommand());
        Objects.requireNonNull(getCommand("cbcreload")).setExecutor(new CBCReloadCommand(gameManager));
        Objects.requireNonNull(getCommand("sidebar")).setExecutor(new SidebarCommand());

        // Print to show finished initialisation
        System.out.println("Crossbow Champions plugin initialisation finished");
    }

    public void registerCommand(String command, TabExecutor commandObject) {

        // Check if command is registered in config.yml
        PluginCommand pluginCommand = getCommand(command);
        if (pluginCommand == null) return;

        // Register command
        pluginCommand.setExecutor(commandObject);

    }

    @Override
    public void onDisable() {

        // Plugin shutdown logic
        System.out.println("Shutting down CBC Plugin...");

        gameManager.onServerClose();

        // Check if game is active
        if (gameManager.getCurrentGame() != null) {
            System.out.println("Shutting down current game...");
            gameManager.getCurrentGame().resetGame();
        }

        // Check if lobby is active
        if (gameManager.getGameState() == GameState.LOBBY) {
            System.out.println("Shutting down current lobby...");
            gameManager.stopLobby();
        }

        // Check if practice is active
        if (gameManager.isPracticeActive()) {
            System.out.println("Shutting down practice arena...");
            gameManager.practiceManager.disable();
        }

    }

    public static CBCPlugin getPlugin() {
        return plugin;
    }

    public static ResourcePackManager getResourcePackManager() {
        return resourcePackManager;
    }

    public static void loadPermissions () {

        // Open configuration file
        FileConfiguration configFile = getPlugin().getConfig();

        // Get UUIDs (in string form) of operators and administrators
        List<String> operatorUUIDs = configFile.getStringList("operators");
        List<String> adminUUIDs = configFile.getStringList("admins");

        // Convert operator string UUIDs to UUID objects
        gameOperators = new HashSet<>();
        for (String uuidString : operatorUUIDs) {
            // Attempt to convert string to UUID
            try {
                UUID uuid = UUID.fromString(uuidString);
                gameOperators.add(uuid);
            } catch (IllegalArgumentException ignored) {}
        }

        // Convert admin string UUIDs to UUID objects
        gameAdmins = new HashSet<>();
        for (String uuidString : adminUUIDs) {
            // Attempt to convert string to UUID
            try {
                UUID uuid = UUID.fromString(uuidString);
                gameAdmins.add(uuid);
            } catch (IllegalArgumentException ignored) {}
        }

    }

    public static void savePermissions () {}

    public static boolean isPlayerOperator (UUID uuid) {
        return gameOperators.contains(uuid);
    }

    public static boolean isPlayerAdmin (UUID uuid) {
        return gameAdmins.contains(uuid);
    }

    @EventHandler
    public void playerJoin(PlayerJoinEvent e) {

        final Player playerJoined = e.getPlayer();
        final CBCPlugin plugin = this;

        // Add player to resource pack
        new BukkitRunnable() {

            @Override
            public void run() {
                resourcePackManager.addPlayerHead(playerJoined.getUniqueId(), playerJoined.getName(), plugin);
            }
        }.runTaskAsynchronously(CBCPlugin.getPlugin());

        // Load player's armor trim
        new BukkitRunnable() {

            @Override
            public void run() {
                trimService.loadPlayerTrimFromFile(playerJoined);
                gameManager.getGlobalKillsManager().loadPlayerGlobalKills(playerJoined);
            }
        }.runTaskAsynchronously(CBCPlugin.getPlugin());
    }

    public static GameManager getGameManager() {
        return gameManager;
    }

    public ArmorTrimService getTrimService () {
        return trimService;
    }

    public WeaponPresetService getWeaponPresetService () {
        return weaponPresetService;
    }
}
