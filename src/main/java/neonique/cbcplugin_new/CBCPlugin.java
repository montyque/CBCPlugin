package neonique.cbcplugin_new;

import dev.jorel.commandapi.CommandAPICommand;
import neonique.cbcplugin_new.combat.display.DeathMessageLoader;
import neonique.cbcplugin_new.commands.GameCommand;
import neonique.cbcplugin_new.commands.LobbyCommand;
import neonique.cbcplugin_new.commands.PracticeCommand;
import neonique.cbcplugin_new.core.CBCGamemode;
import neonique.cbcplugin_new.lobby.Lobby;
import neonique.cbcplugin_new.mapconfig.MapLoader;
import neonique.cbcplugin_new.mapconfig.MapMechanicLoader;
import neonique.cbcplugin_new.mapconfig.MapRepository;
import neonique.cbcplugin_new.practice.PracticeManager;
import neonique.cbcplugin_new.scoreboard.CBCScoreboardManager;
import neonique.cbcplugin_new.services.ArmorTrimService;
import neonique.cbcplugin_new.resourcepack.ResourcePackManager;
import neonique.cbcplugin_new.session.Session;
import neonique.cbcplugin_new.session.SessionState;
import neonique.cbcplugin_new.util.ConfigUtil;
import neonique.cbcplugin_new.util.VectorUtil;
import net.kyori.adventure.text.Component;
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


public class CBCPlugin extends JavaPlugin implements Listener {

    private static final Set<CBCGamemode> ENABLED_GAMEMODES = Set.of(
            CBCGamemode.SHOWDOWN
    );

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
    private Lobby lobby;

    private Session session;
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

        // Create lobby
        loadLobby();

        // Create single session
        session = new Session(this, world, scoreboardManager, lobby, practiceManager);

        // Register command
        registerMainCommand();

        // Print to show finished initialisation
        getLogger().info("Finished initialising Crossbow Champions!");
        getLogger().info("=======================================");

    }

    private void registerMainCommand () {

        new CommandAPICommand("cbc")
                .withSubcommand(new PracticeCommand(practiceManager).get())
                .withSubcommand(new LobbyCommand(lobby, session).get())
                .withSubcommand(new GameCommand(session).get())
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

    public void loadLobby () {

        Configuration config = getConfig();
        ConfigurationSection lobbySection = ConfigUtil.requireConfigurationSection(config, "lobby");

        Location teleport = ConfigUtil.requireVector(lobbySection, "teleport").toLocation(world);

        lobby = new Lobby(this, world, scoreboardManager, mapRepository, teleport);

    }

    @Override
    public void onDisable() {

        // Plugin shutdown logic
        getLogger().info("Shutting down the Crossbow Champions Plugin...");

        // End practice if active
        if (practiceManager.instanceActive()) {
            practiceManager.endInstance();
        }

        // End session
        session.stateSwitch(SessionState.INACTIVE);

    }

    public static CBCPlugin getPlugin() {
        return plugin;
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

}
