package neonique.cbcplugin_new;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandTree;
import dev.jorel.commandapi.arguments.LiteralArgument;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import neonique.cbcplugin_new.combat.display.DeathMessageLoader;
import neonique.cbcplugin_new.commands.PracticeCommand;
import neonique.cbcplugin_new.core.CBCGamemode;
import neonique.cbcplugin_new.mapconfig.MapLoader;
import neonique.cbcplugin_new.mapconfig.MapMechanicLoader;
import neonique.cbcplugin_new.mapconfig.MapRepository;
import neonique.cbcplugin_new.practice.PracticeManager;
import neonique.cbcplugin_new.scoreboard.CBCScoreboardManager;
import neonique.cbcplugin_new.services.ArmorTrimService;
import neonique.cbcplugin_new.resourcepack.ResourcePackManager;
import neonique.cbcplugin_new.util.ConfigUtil;
import neonique.cbcplugin_new.util.VectorUtil;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import java.io.FileNotFoundException;
import java.util.*;


public final class CBCPlugin extends JavaPlugin implements Listener {

    private Set<CBCGamemode> ENABLED_GAMEMODES = Set.of();

    private static CBCPlugin plugin;

    private World world;

    private MapMechanicLoader mechanicLoader;
    private MapLoader mapLoader;
    private MapRepository mapRepository;

    private DeathMessageLoader deathMessageLoader;

    private ResourcePackManager resourcePackManager;
    private ArmorTrimService trimService;
    // private WeaponPresetService weaponPresetService;

    private CBCScoreboardManager scoreboardManager;
    private PracticeManager practiceManager;
    // private GameManager gameManager;

    @Override
    public void onEnable() {

        // Plugin startup logic
        plugin = this;

        // Create config yml file if not existent
        plugin.saveDefaultConfig();

        getLogger().info("=======================================");
        getLogger().info("Crossbow Champions - A combat Minecraft minigame");
        getLogger().info("Written by Neonique");
        getLogger().info("Initialising...");

        getLogger().info("");

        // Load the world
        String worldName = ConfigUtil.requireString(getConfig(), "world_name");
        world = getServer().getWorld(worldName);
        if (world == null) throw new IllegalStateException("World '" + "' was not found on this server");
        getLogger().info("Primary world: " + worldName + " has been found.");

        /*
        // Load permissions from config.yml
        loadPermissions();

        // Print out operator and administrator names
        List<String> operatorNames = new ArrayList<>(gameOperators).stream().map(uuid -> Bukkit.getOfflinePlayer(uuid).getName()).collect(Collectors.toList());
        System.out.println("CBC Operators (perms no. 2): " + String.join(", ", operatorNames));
        List<String> adminNames = new ArrayList<>(gameAdmins).stream().map(uuid -> Bukkit.getOfflinePlayer(uuid).getName()).collect(Collectors.toList());
        System.out.println("CBC Administrators (perms no. 1): " + String.join(", ", adminNames));*/

        // Register join server and leave server listeners
        getServer().getPluginManager().registerEvents(this, this);

        scoreboardManager = new CBCScoreboardManager(getServer().getScoreboardManager());
        scoreboardManager.activate();

        // Create all required services
        trimService = new ArmorTrimService();
        resourcePackManager = new ResourcePackManager();
        // weaponPresetService = new WeaponPresetService();

        deathMessageLoader = new DeathMessageLoader();
        try {
            deathMessageLoader.loadDefaults(getDataFolder());
        } catch (FileNotFoundException e) {
            throw new IllegalStateException("deathmessages.yml was not found!");
        }

        // Load maps
        mechanicLoader = new MapMechanicLoader();
        mapLoader = new MapLoader(mechanicLoader, deathMessageLoader);
        mapRepository = new MapRepository();
        loadMaps();

        // Create practice manager
        loadPracticeManager();

        // Create game manager
        // gameManager = new GameManager(this);


        registerMainCommand();

        // Print to show finished initialisation
        getLogger().info("Finished initialising Crossbow Champions!");
        getLogger().info("=======================================");

    }

    private void registerMainCommand () {

        new CommandAPICommand("cbc")
                .withSubcommand(new PracticeCommand(practiceManager).get())
                .register(this);

    }

    public void loadMaps () {
        try {
            mapLoader.loadAllIntoRepository(getDataFolder(), getLogger(), mapRepository, ENABLED_GAMEMODES);
        } catch (FileNotFoundException e) {
            getLogger().warning("The plugin's data folder does not exist");
        }
    }

    public void loadPracticeManager () {

        Configuration config = getConfig();
        ConfigurationSection practiceSection = ConfigUtil.requireConfigurationSection(config, "practice");

        Location portal = VectorUtil.vecToLocation(ConfigUtil.requireVector(practiceSection, "portal"), world);
        Location hologram = VectorUtil.vecToLocation(ConfigUtil.requireVector(practiceSection, "hologram"), world);
        Location teleport = VectorUtil.vecToLocation(ConfigUtil.requireVector(practiceSection, "teleport"), world);

        practiceManager = new PracticeManager(this, world, scoreboardManager, mapRepository, portal, hologram, teleport);

    }

    @Override
    public void onDisable() {

        // Plugin shutdown logic
        getLogger().info("Shutting down the Crossbow Champions Plugin...");

        //
        if (practiceManager.instanceActive()) {
            practiceManager.endInstance();
        }

        /*gameManager.onServerClose();

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
        }*/

    }

    public static CBCPlugin getPlugin() {
        return plugin;
    }

    /*
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

    }*/

    /*
    public static void savePermissions () {}

    public static boolean isPlayerOperator (UUID uuid) {
        return gameOperators.contains(uuid);
    }

    public static boolean isPlayerAdmin (UUID uuid) {
        return gameAdmins.contains(uuid);
    }*/

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
        }.runTaskAsynchronously(this);

        // Load player's armor trim
        new BukkitRunnable() {

            @Override
            public void run() {
                trimService.loadPlayerTrimFromFile(playerJoined);
                // gameManager.getGlobalKillsManager().loadPlayerGlobalKills(playerJoined);
            }
        }.runTaskAsynchronously(this);
    }

    /*
    public static GameManager getGameManager() {
        return gameManager;
    }

    public ArmorTrimService getTrimService () {
        return trimService;
    }

    public WeaponPresetService getWeaponPresetService () {
        return weaponPresetService;
    }

    public void registerListener (Listener listener) {
        getServer().getPluginManager().registerEvents(listener, this);
    }

    public void unregisterListener (Listener listener) {
        HandlerList.unregisterAll(listener);
    }*/

}
