package neonique.cbcplugin_new.managers;

import neonique.cbcplugin_new.CBCPlugin;
import neonique.cbcplugin_new.cbcevents.CBCEventManager;
import neonique.cbcplugin_new.combat.CombatManager;
import neonique.cbcplugin_new.gamemodes.CBCGamemode;
import neonique.cbcplugin_new.gamemodes.FFAGameContext;
import neonique.cbcplugin_new.core.BaseGameCommands;
import neonique.cbcplugin_new.core.Game;
import neonique.cbcplugin_new.core.TeamGame;
import neonique.cbcplugin_new.gamemodes.GameContext;
import neonique.cbcplugin_new.gamemodes.TeamGameContext;
import neonique.cbcplugin_new.gamemodes._base.PostGameStats;
import neonique.cbcplugin_new.mapconfig.CBCMap;
import neonique.cbcplugin_new.mapconfig.GamemodeMapData;
import neonique.cbcplugin_new.mapconfig.MapLoader;
import neonique.cbcplugin_new.mapconfig.MapMechanicLoader;
import neonique.cbcplugin_new.scoreboard.CBCScoreboardManager;
import neonique.cbcplugin_new.weapons.WeaponFactory;
import neonique.cbcplugin_new.weapons.WeaponType;
import neonique.cbcplugin_new.weapons.presets.CreeperPreset;
import neonique.cbcplugin_new.weapons.presets.FlamePreset;
import neonique.cbcplugin_new.mechanics.GamemodeOptions;
import neonique.cbcplugin_new.weapons.presets.XbowPreset;
import neonique.cbcplugin_new.listeners.GameJoinListener;
import neonique.cbcplugin_new.listeners.GameLeaveListener;
import neonique.cbcplugin_new.lobby.Lobby;
import neonique.cbcplugin_new.core.CBCPlayer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.util.*;
import java.util.logging.Level;

public class GameManager {

    public CBCPlugin plugin;

    // Setting up other managers
    public CombatManager combatManager;
    public PracticeManager practiceManager;
    private final GlobalKillsManager globalKillsManager;
    private final ChatManager chatManager;

    private final MapLoader mapLoader;
    private final MapMechanicLoader mechanicLoader;

    private CBCEventManager eventManager = null;

    // Game state
    private GameState gameState;
    private final Lobby lobby;

    private HashMap<UUID, CBCPlayer> playerList;
    private PlayerRegistry playerRegistry;

    private final UUID worldUUID;

    // Loaded maps
    private Map<String, CBCMap> loadedMaps;
    private Map<CBCGamemode, List<GamemodeMapData>> loadedGamemodeMaps;

    // Gamemodes and classes
    private LinkedHashMap<CBCGamemode, GamemodeOptions> gamemodes;

    // Loaded maps
    private HashMap<Integer, CBCGamemode> gamemodeToIntList;
    private HashMap<CBCGamemode, List<CBCMap>> gamemodeAndMapList;
    private HashMap<String, CBCMap> practiceMaps;

    // Image file paths
    private HashMap<CBCGamemode, HashMap<String, String>> gamemodeMapImageFiles = new HashMap<>();

    // Other statistics
    private PostGameStats lastGameStats = null;

    private Game<?> currentGame = null;
    private Game<?> lastGame = null;
    private BaseGameCommands gameCommands = null;

    // Join and leave listeners
    private GameJoinListener gameJoinListener;
    private GameLeaveListener gameLeaveListener;

    // Audience to send all messages to
    private Set<Player> audience = null;

    // Player list header and footer
    private Component playerListHeader = Component.text("");
    private Component playerListFooter = Component.text("");

    // Scoreboard manager
    private final CBCScoreboardManager cbcScoreboardManager;

    public void createDirectoryAttempt(File file) {
        // Folder does not exist, therefore one will be created
        if (file.mkdir()) {
            System.out.println("No folder found, created folder " + file.getName());
        } else {
            System.out.println("No folder found, error creating folder for gamemode " + file.getName());
        }
    }

    public GameManager(CBCPlugin plugin) {

        this.plugin = plugin;

        worldUUID = Objects.requireNonNull(Bukkit.getWorld("world")).getUID();

        // Create managers
        mechanicLoader = new MapMechanicLoader();
        mapLoader = new MapLoader(this, mechanicLoader, CBCPlugin.getPlugin().getLogger());

        combatManager = new CombatManager(this);
        practiceManager = new PracticeManager(this, combatManager);
        chatManager = new ChatManager(this);
        globalKillsManager = new GlobalKillsManager();
        cbcScoreboardManager = new CBCScoreboardManager(Bukkit.getScoreboardManager());
        playerRegistry = new PlayerRegistry();

        gameState = GameState.DISABLED;
        lobby = new Lobby(this);

        // Create required listeners and tasks
        gameJoinListener = new GameJoinListener(this);
        gameLeaveListener = new GameLeaveListener(this);
        plugin.getServer().getPluginManager().registerEvents(gameJoinListener, plugin);
        plugin.getServer().getPluginManager().registerEvents(gameLeaveListener, plugin);

        loadMaps();

    }

    public void loadMaps () {

        File pluginFolder = CBCPlugin.getPlugin().getDataFolder();
        File mapFolder = new File(pluginFolder, "maps");
        if (!mapFolder.exists()) {
            mapFolder.mkdir();
            loadedMaps = Map.of();
            loadedGamemodeMaps = Map.of();
            return;
        }

        loadedMaps = mapLoader.loadMapsFromDirectory(mapFolder);

        try {
            loadedMaps = mapLoader.loadMapsFromDirectory(mapFolder);
            CBCPlugin.getPlugin().getLogger().info(
                    "Successfully loaded " + loadedMaps.size() + " maps!"
            );
        } catch (IllegalArgumentException e) {
            CBCPlugin.getPlugin().getLogger().log(Level.WARNING,
                    "Failure occurred while attempting to load base CBC maps", e);
        }

        File gamemodeFolderFile = new File(pluginFolder, "gamemodes");
        if (!mapFolder.exists()) {
            mapFolder.mkdir();
            loadedMaps = Map.of();
            loadedGamemodeMaps = Map.of();
            return;
        }

        // Load gamemodes
        loadGamemode(CBCGamemode.CTF, gamemodeFolderFile);

    }

    public void loadGamemode (CBCGamemode gamemode, File parentGamemodeFolderFile) {

        File gamemodeFolder = new File(parentGamemodeFolderFile, gamemode.name().toLowerCase());
        if (!gamemodeFolder.exists()) return;

        try {

            List<GamemodeMapData> gamemodeMaps = mapLoader.loadGamemodeMapsFromDirectory(gamemode,
                    loadedMaps, gamemodeFolder);
            loadedGamemodeMaps.put(gamemode, gamemodeMaps);
            CBCPlugin.getPlugin().getLogger().info(
                    "Successfully loaded " + loadedMaps.size() + " maps for gamemode " + gamemode.name() + "!"
            );

        } catch (IllegalArgumentException e) {
            CBCPlugin.getPlugin().getLogger().log(Level.WARNING,
                    "Failure occurred while attempting to load " + gamemode.name() + " CBC maps", e);
        }

    }

    public void openPractice (CBCMap map) {
        if (this.gameState != GameState.ACTIVE) {
            practiceManager.enable(map);
        }
    }

    public CBCMap getPracticeMap (String mapId) {
        return practiceMaps.getOrDefault(mapId, null);
    }

    public CBCMap getCurrentPracticeMapId () {
        return practiceManager.getMap();
    }

    public void startGame (CBCGamemode gamemode, GameContext context) {

        // If the practice arena is active close it
        if (practiceManager.isEnabled()) {
            practiceManager.disable();
        }

        // Set game state to ACTIVE
        this.gameState = GameState.ACTIVE;

        // Start the game depending on the gamemode
        try {

            cbcScoreboardManager.activate();

            currentGame = gamemode.newGameInstance(this);
            currentGame.setupGame(context);
            gameCommands = currentGame.getGameCommands();

            // Set weapon presets
            WeaponFactory weaponFactory = combatManager.getWeaponFactory();
            weaponFactory.setCreeperVar((CreeperPreset) lobby.getWeaponPreset(WeaponType.CREEPER));
            weaponFactory.setFlameVar((FlamePreset) lobby.getWeaponPreset(WeaponType.FLAME));
            weaponFactory.setXbowVar((XbowPreset) lobby.getWeaponPreset(WeaponType.XBOW));
            lobby.putTeamOverridesPresetsIntoWeaponManager();

            playerRegistry.bind(currentGame);

        } catch (Exception e) {

            e.printStackTrace();
            getWorld().sendMessage(Component.text("Error occured while attempting to start game!").color(NamedTextColor.RED));

            // Attempt to reset games
            try {
                currentGame.resetGame();
            } catch (Exception ignore) {}

            currentGame = null;
            gameCommands = null;
            this.gameState = GameState.LOBBY;
            lobby.deactivate();

            // Turn weapons off
            if (combatManager.isActive()) {
                combatManager.disableWeapons();
            }

            new BukkitRunnable() {
                @Override
                public void run () {
                    lobby.activate();
                }
            }.runTaskLater(CBCPlugin.getPlugin(), 5);

            return;
        }

        // Turn off lobby
        lobby.deactivate();
    }

    public void endGame () {

        if (currentGame == null || gameState != GameState.ACTIVE) {
            return;
        }


        lastGameStats = currentGame.getPostGameStats();
        for (CBCPlayer player : playerList.values()) {
            player.resetPlayerListName();
        }

        try {
            lastGameStats.sendPostGameSummary(getWorld());
        }
        catch (NullPointerException e) {
            sendGlobalMessage(Component.text("Post game stats are unavailable for this game.").color(NamedTextColor.YELLOW));
        }

        // If event is active and game is counted as an event game, run end game function
        if (isCBCEventActive()) {
            if (eventManager.isLastGameEventGame()) {
                if (currentGame instanceof TeamGame<?, ?> eventGame) {
                    eventManager.eventGameEnded(eventGame);
                }
            }
        }

        // Clear chat settings
        chatManager.clearChatManager();

        // Reset the game
        currentGame.resetGame();
        playerRegistry.clear();

        // Setup lobby
        startLobby();

        Game<?> game = currentGame;
        lastGame = currentGame;

        // No more game
        currentGame = null;
        gameCommands = null;

        // Restore teams if the game is a team game
        if (game instanceof TeamGame<?, ?> teamGame) {
            // Check if teams should be restored
            if (teamGame.isRestoreTeamsAfterGame()) {
                lobby.restoreTeams(teamGame.getPlayersTeamIds());
            }
        }

        // Update global kills
        if (game.isGlobalKillsEnabled()) {
            globalKillsManager.addPlayersKills(game.getPlayers());
        }
    }

    public void resetPlayerList () {
        playerList = new HashMap<>();
    }

    public void addPlayer (CBCPlayer player) {
        playerList.put(player.getOfflinePlayer().getUniqueId(), player);
    }

    public void removePlayer (CBCPlayer player) {
        playerList.remove(player.getOfflinePlayer().getUniqueId());
    }

    public GameState getGameState () {
        return gameState;
    }

    public void startLobby () {
        gameState = GameState.LOBBY;
        lobby.activate();
    }

    public void stopLobby() {
        gameState = GameState.DISABLED;
        lobby.deactivate();
    }

    public Lobby getLobby() {
        return lobby;
    }

    public World getWorld() {
        return Bukkit.getWorld(worldUUID);
    }

    public LinkedHashMap<CBCGamemode, GamemodeOptions> getGamemodes() {
        return gamemodes;
    }

    public HashMap<CBCGamemode, List<CBCMap>> getGamemodeAndMapList() {
        return gamemodeAndMapList;
    }

    public HashMap<Integer, CBCGamemode> getGamemodeToIntegerList() {
        return gamemodeToIntList;
    }

    public boolean hasPlayer (Player player) {
        return this.playerList.containsKey(player.getUniqueId());
    }

    public Collection<CBCPlayer> getPlayers () {
        return playerList.values();
    }

    public Set<CBCPlayer> getAlivePlayers () {

        Set<CBCPlayer> alivePlayers = new HashSet<>();
        for (CBCPlayer player : playerList.values()) {
            if (player.isAlive()) {alivePlayers.add(player);}
        }

        return alivePlayers;
    }

    public CBCPlayer getPlayer(Player player) {
        return this.playerList.getOrDefault(player.getUniqueId(), null);
    }

    public Game<?> getCurrentGame() {
        return currentGame;
    }

    public Set<Player> getPlayerEntities() {
        Set<Player> playerEntityList = new HashSet<>();
        for (CBCPlayer player : playerList.values()) {
            if (player.isOnline()) {
                playerEntityList.add(player.getPlayer());
            }
        }
        return playerEntityList;
    }

    public PostGameStats getPostGameStats() {
        return lastGameStats;
    }

    public void playerJoinServer(Player player) {

        if (cbcScoreboardManager.isActive()) {
            cbcScoreboardManager.addPlayer(player);
        }

        if (gameState == GameState.LOBBY) {
            lobby.playerJoinServer(player);
        }
        else if (gameState == GameState.ACTIVE) {
            if (currentGame != null) {
                currentGame.playerJoinServer(player);
                // Reset chat manager for player
                chatManager.setChatType(player.getUniqueId(), ChatType.ALL);
            }
        }

        player.sendPlayerListFooter(playerListFooter);
        player.sendPlayerListHeader(playerListHeader);
    }

    public void playerLeaveServer(Player player) {

        if (cbcScoreboardManager.isActive()) {
            cbcScoreboardManager.removePlayer(player);
        }

        if (gameState == GameState.LOBBY) {
            lobby.playerLeaveServer(player);
        }
        else if (gameState == GameState.ACTIVE) {
            if (currentGame != null) {
                currentGame.playerLeaveServer(player);
            }
        }

        if (practiceManager.isEnabled()) {
            practiceManager.playerLeaveServer(player);
        }
    }

    public void setAudience(Set<Player> audience) {
        this.audience = audience;
    }

    public void sendGlobalMessage(Component component) {
        if (audience == null) {
            getWorld().sendMessage(component);
        } else {
            for (Player player : audience) {
                player.sendMessage(component);
            }
        }
    }

    public void sendGlobalTitle(Title title) {
        if (audience == null) {
            getWorld().showTitle(title);
        } else {
            for (Player player : audience) {
                player.showTitle(title);
            }
        }
    }

    public void playGlobalSound(Sound sound, float volume, float pitch) {
        if (audience == null) {
            for (Player player : getWorld().getPlayers()) {
                player.playSound(player.getLocation(), sound, volume, pitch);
            }
        } else {
            for (Player player : audience) {
                player.playSound(player.getLocation(), sound, volume, pitch);
            }
        }
    }

    public void playSound(Location loc, Sound sound, float volume, float pitch) {
        if (audience == null) {
            for (Player player : getWorld().getPlayers()) {
                player.playSound(loc, sound, volume, pitch);
            }
        } else {
            for (Player player : audience) {
                player.playSound(loc, sound, volume, pitch);
            }
        }
    }

    public void showGlobalBossbarManager (GameBossBarManager bossBarManager) {
        if (audience == null) {
            for (Player player : getWorld().getPlayers()) {
                bossBarManager.addPlayer(player);
            }
        } else {
            for (Player player : audience) {
                bossBarManager.addPlayer(player);
            }
        }
    }

    public void showGlobalBossbarManager (Player player, GameBossBarManager bossBarManager) {
        bossBarManager.addPlayer(player);
    }

    public void hideGlobalBossbarManager (GameBossBarManager bossBarManager) {
        for (Player player : getWorld().getPlayers()) {
            bossBarManager.hidePlayerBossbars(player);
        }
    }

    public boolean isPracticeActive() {
        return practiceManager.isEnabled();
    }

    public Collection<CBCMap> getPracticeMaps() {
        return practiceMaps.values();
    }

    public BaseGameCommands getGameCommands() {
        return gameCommands;
    }

    public void setPlayerListHeader(Component header) {
        playerListHeader = header;
        if (audience != null) {
            for (Player player : audience) {
                player.sendPlayerListHeader(playerListHeader);
            }
        } else {
            getWorld().sendPlayerListHeader(playerListHeader);
        }
    }

    public void setPlayerListFooter(Component footer) {
        playerListFooter = footer;
        if (audience != null) {
            for (Player player : audience) {
                player.sendPlayerListFooter(playerListFooter);
            }
        } else {
            getWorld().sendPlayerListFooter(playerListFooter);
        }
    }

    public String getImageFile(CBCGamemode gamemode, String mapId) {

        if (!gamemodeMapImageFiles.containsKey(gamemode)) return null;

        HashMap<String, String> gamemodeImageFiles = gamemodeMapImageFiles.get(gamemode);

        if (!gamemodeImageFiles.containsKey(mapId)) return null;

        return gamemodeImageFiles.get(mapId);

    }

    public CBCScoreboardManager getCbcScoreboardManager() {
        return cbcScoreboardManager;
    }

    public void onServerClose () {

    }

    public ChatManager getChatManager() {
        return chatManager;
    }

    public CBCEventManager getEventManager() {
        return eventManager;
    }

    public void createCBCEvent() {
        eventManager = new CBCEventManager(this);
        eventManager.activate();
    }

    public boolean isCBCEventActive () {
        return (eventManager != null);
    }

    public boolean isEventGame() {
        if (eventManager == null) return false;
        else return (eventManager.getNextGameNum() <= CBCEventManager.getGameAmount() + 1);
    }

    public Game<?> getLastGame() {
        return lastGame;
    }

    public GlobalKillsManager getGlobalKillsManager() {
        return globalKillsManager;
    }

    public CombatManager getCombatManager() {
        return combatManager;
    }

    public PlayerRegistry getPlayerRegistry() {
        return playerRegistry;
    }

    public MapMechanicLoader mechanicLoader() {
        return mechanicLoader;
    }
}
