package neonique.cbcplugin_new.managers;

import neonique.cbcplugin_new.CBCPlugin;
import neonique.cbcplugin_new.cbcevents.CBCEventManager;
import neonique.cbcplugin_new.enums.*;
import neonique.cbcplugin_new.gamemodes._base.BaseGameCommands;
import neonique.cbcplugin_new.gamemodes._base.Game;
import neonique.cbcplugin_new.gamemodes._base.TeamGame;
import neonique.cbcplugin_new.gamemodes.assassin.AssassinGame;
import neonique.cbcplugin_new.gamemodes.crossbowtag.TagGame;
import neonique.cbcplugin_new.gamemodes.ctf.CTFGame;
import neonique.cbcplugin_new.gamemodes.flagrush.FlagRushGame;
import neonique.cbcplugin_new.gamemodes.holdthegold.HTGGame;
import neonique.cbcplugin_new.gamemodes.kmation.KMationGame;
import neonique.cbcplugin_new.gamemodes._base.PostGameStats;
import neonique.cbcplugin_new.gamemodes.koth.KOTHGame;
import neonique.cbcplugin_new.gamemodes.rendezvous.RendezvousGame;
import neonique.cbcplugin_new.gamemodes.showdown.ShowdownGame;
import neonique.cbcplugin_new.gamemodes._base.CBCMap;
import neonique.cbcplugin_new.gamemodes.tdm.MapRushTDMGame;
import neonique.cbcplugin_new.gamemodes.tdm.TDMGame;
import neonique.cbcplugin_new.gamemodes.throwdown.ThrowdownGame;
import neonique.cbcplugin_new.weapons.presets.CreeperPreset;
import neonique.cbcplugin_new.weapons.presets.FlamePreset;
import neonique.cbcplugin_new.gameobjects.GamemodeOptions;
import neonique.cbcplugin_new.weapons.presets.XbowPreset;
import neonique.cbcplugin_new.listeners.GameJoinListener;
import neonique.cbcplugin_new.listeners.GameLeaveListener;
import neonique.cbcplugin_new.lobby.Lobby;
import neonique.cbcplugin_new.playerclasses.CBCPlayer;
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

public class GameManager {

    public CBCPlugin plugin;

    // Setting up other managers
    public CombatManager combatManager;
    public PracticeManager practiceManager;
    private final GlobalKillsManager globalKillsManager;
    private final ChatManager chatManager;
    private CBCEventManager eventManager = null;

    // Game state
    private GameState gameState;
    private final Lobby lobby;

    private HashMap<UUID, CBCPlayer> playerList;
    private HashMap<Integer, CBCPlayer> playerIdList;

    private final UUID worldUUID;

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

    private Game currentGame = null;
    private Game lastGame = null;
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
        combatManager = new CombatManager(this);
        practiceManager = new PracticeManager(this, combatManager);
        chatManager = new ChatManager(this);
        globalKillsManager = new GlobalKillsManager();

        gameState = GameState.DISABLED;
        lobby = new Lobby(this);

        gameJoinListener = new GameJoinListener(this);
        gameLeaveListener = new GameLeaveListener(this);
        plugin.getServer().getPluginManager().registerEvents(gameJoinListener, plugin);
        plugin.getServer().getPluginManager().registerEvents(gameLeaveListener, plugin);

        cbcScoreboardManager = new CBCScoreboardManager(this, Bukkit.getScoreboardManager());

        loadMaps();
    }

    public void loadMaps () {

        CBCPlugin.getPlugin().getLogger().info("\nLoading maps...");

        // Setup hashmaps
        gamemodeAndMapList = new HashMap<>();
        gamemodeToIntList = new HashMap<>();
        gamemodeMapImageFiles = new HashMap<>();

        // Add gamemodes and their ids
        gamemodes = new LinkedHashMap<>();

        // CAPTURE THE FLAG GAMEMODE
        gamemodes.put(CBCGamemode.CTF, new GamemodeOptions(CBCGamemode.CTF, "ctf",
                "Capture The Flag", true, 2, 4));

        // SHOWDOWN GAMEMODE
        gamemodes.put(CBCGamemode.SHOWDOWN, new GamemodeOptions(CBCGamemode.SHOWDOWN, "showdown",
                "Showdown", true, 2, 8));

        // HOLD THE GOLD GAMEMODE
        gamemodes.put(CBCGamemode.HOLDTHEGOLD, new GamemodeOptions(CBCGamemode.HOLDTHEGOLD, "holdthegold",
                "Hold The Gold", true, 2, 8));

        // TEAM DEATHMATCH GAMEMODE
        gamemodes.put(CBCGamemode.TDM, new GamemodeOptions(CBCGamemode.TDM, "tdm",
                "Team Deathmatch", true, 2, 8));

        // RENDEZVOUS GAMEMODE
        gamemodes.put(CBCGamemode.RENDEZVOUS, new GamemodeOptions(CBCGamemode.RENDEZVOUS, "rendezvous",
                "Rendezvous", true, 2, 8));

        // CROSSBOW TAG GAMEMODE
        gamemodes.put(CBCGamemode.CBCTAG, new GamemodeOptions(CBCGamemode.CBCTAG, "cbctag",
                "Crossbow Tag", true, 2, 8));

        // CROSSBOW TAG GAMEMODE
        gamemodes.put(CBCGamemode.KOTH, new GamemodeOptions(CBCGamemode.KOTH, "koth",
                "King Of The Hill", true, 1, 8));

        // FLAG RUSH GAMEMODE
        gamemodes.put(CBCGamemode.FLAGRUSH, new GamemodeOptions(CBCGamemode.FLAGRUSH, "flagrush",
                "Flag Rush", true, 2, 4));

        // THROWDOWN GAMEMODE
        gamemodes.put(CBCGamemode.THROWDOWN, new GamemodeOptions(CBCGamemode.THROWDOWN, "throwdown",
                "Throwdown", false, 1));

        // KILLIMINATION GAMEMODE
        gamemodes.put(CBCGamemode.KMATION, new GamemodeOptions(CBCGamemode.KMATION, "kmation",
                "Killimination", false, 1));

        // ASSASSIN GAMEMODE
        gamemodes.put(CBCGamemode.ASSASSIN, new GamemodeOptions(CBCGamemode.ASSASSIN, "assassin",
                "Assassin", false, 3));

        for (CBCGamemode gamemode : gamemodes.keySet()) {
            gamemodeToIntList.put(gamemode.gamemodeNum(), gamemode);
        }

        // ********************************************
        // Import gamemodes and maps from files
        // Attempt to find generic maps file
        File baseMapFolderFile = new File(plugin.getDataFolder(), "maps");
        // Attempt to make this a directory
        if (!baseMapFolderFile.exists()) {
            boolean folderMade = baseMapFolderFile.mkdir();
            return;
        }

        File gamemodeMasterFolderFile = new File(plugin.getDataFolder(), "gamemodes");
        // Attempt to make this a directory
        if (!gamemodeMasterFolderFile.exists()) {
            boolean folderMade = gamemodeMasterFolderFile.mkdir();
            return;
        }

        // Go through each gamemode
        for (CBCGamemode gamemode : CBCGamemode.values()) {
            String gamemodeId = gamemodes.get(gamemode).getGamemodeId();
            if (gamemodeId == null) continue;

            // Search for folder in the "gamemodes" folder named after the gamemode
            File gamemodeFolderFile = new File(gamemodeMasterFolderFile, gamemodeId);
            if (gamemode == CBCGamemode.FLAGRUSH) {
                gamemodeFolderFile = new File(gamemodeMasterFolderFile, "ctf");
            }

            if (!gamemodeFolderFile.exists()) {
                // Folder does not exist, therefore one will be created
                createDirectoryAttempt(gamemodeFolderFile);
                continue;
            }

            // Folder does exist, therefore search in the gamemode folder for the "maps" folder
            File gamemodeMapsFolderFile = new File(gamemodeFolderFile, "maps");
            if (!gamemodeMapsFolderFile.exists()) {
                // Folder does not exist, therefore one will be created
                createDirectoryAttempt(gamemodeMapsFolderFile);
                continue;
            }

            // Go through all files in the maps folder
            // Find all .yml files in maps directory
            File[] mapFiles = gamemodeMapsFolderFile.listFiles((dir, name) -> name.endsWith(".yml"));
            if (mapFiles == null) {
                CBCPlugin.getPlugin().getLogger().warning("Could not find files in " + gamemodeMapsFolderFile.getPath());
                continue;
            }
            CBCPlugin.getPlugin().getLogger().info(mapFiles.length + " files found in " + gamemodeMapsFolderFile.getPath());
            // Iterate through each yml file
            List<CBCMap> mapList = new ArrayList<>();
            for (File mapFile : mapFiles) {
                YamlConfiguration ymlMapConfig = YamlConfiguration.loadConfiguration(mapFile); // Get YAML config object
                CBCMap mapObject = null;
                String mapId;

                // Check if ymlMapConfig has the correct keys for the selected gamemode
                if (!CBCMap.isYmlInvalidGamemodeMap(ymlMapConfig, gamemode)) {
                    CBCPlugin.getPlugin().getLogger().warning(mapFile.getName() + " is not a valid " + gamemode.toString() + " map");
                    continue;
                }

                mapId = ymlMapConfig.getString("MapId");


                if (mapId == null) {
                    CBCPlugin.getPlugin().getLogger().warning("Could not find map id for file " + mapFile.getName());
                    continue;
                }

                // Find base map yml config in the maps folder
                File baseMapYml = new File(baseMapFolderFile, mapId + ".yml");
                if (!baseMapYml.exists()) {
                    CBCPlugin.getPlugin().getLogger().warning("Could not find base map file for map " + mapId);
                    continue;
                }
                if (!baseMapYml.getName().endsWith(".yml")) {
                    CBCPlugin.getPlugin().getLogger().warning("Could not find base map file with filetype yml for map" + mapId);
                    continue;
                }
                YamlConfiguration baseYmlMapConfig = YamlConfiguration.loadConfiguration(baseMapYml);
                if (CBCMap.isYmlInvalidMap(baseYmlMapConfig)) {
                    CBCPlugin.getPlugin().getLogger().warning("Base map file for map " + mapId + " is not valid");
                    continue;
                }

                // Create map object
                try {
                    mapObject = CBCMap.createMap(gamemode, baseYmlMapConfig, ymlMapConfig, this, this.combatManager);
                } catch (Exception e) {
                    CBCPlugin.getPlugin().getLogger().warning("Exception raised when loading map " + mapId + " for gamemode " + gamemodeId + ": ");
                }

                if (mapObject == null) continue;

                mapList.add(mapObject);
            }
            gamemodeAndMapList.put(gamemode, mapList);
            CBCPlugin.getPlugin().getLogger().info("Loaded " + mapList.size() + " maps for '" + gamemodeId + "'");
        }

        // Loading practice maps
        CBCPlugin.getPlugin().getLogger().info("\nLoading practice maps...");
        practiceMaps = new HashMap<>();
        for (File mapFile : Objects.requireNonNull(baseMapFolderFile.listFiles((dir, name) -> name.endsWith(".yml")))) {
            CBCMap mapObject;

            // Find base map yml config in the maps folder
            YamlConfiguration baseYmlMapConfig = YamlConfiguration.loadConfiguration(mapFile);
            if (CBCMap.isYmlInvalidMap(baseYmlMapConfig)) {
                CBCPlugin.getPlugin().getLogger().warning("Base map file is not valid for file " + mapFile.getName());
                continue;
            }

            try {
                mapObject = new CBCMap(baseYmlMapConfig, this, combatManager);
            } catch (Exception e) {
                CBCPlugin.getPlugin().getLogger().warning(
                        "Could not load practice map for file " + mapFile.getName()
                );
                continue;
            }

            if (!mapObject.isPracticeMap()) {
                continue;
            }

            practiceMaps.put(mapObject.getMapId(), mapObject);
        }
        CBCPlugin.getPlugin().getLogger().info("Loaded " + practiceMaps.size() + " practice maps.");

        // Load image file names from yml file
        File gamemodeMapImageFilesYaml = new File(plugin.getDataFolder(), "gamemodemapimagefiles.yml");

        if (gamemodeMapImageFilesYaml.exists()) {
            YamlConfiguration gamemodeMapImageFilesConfig = YamlConfiguration.loadConfiguration(gamemodeMapImageFilesYaml);
            // Go through all gamemodes
            for (CBCGamemode gamemode : gamemodes.keySet()) {
                GamemodeOptions gamemodeOptions = gamemodes.get(gamemode);
                String gamemodeId = gamemodeOptions.getGamemodeId();

                ConfigurationSection gamemodeConfigSection = gamemodeMapImageFilesConfig.getConfigurationSection(gamemodeId);
                if (gamemodeConfigSection == null) continue;
                if (!gamemodeAndMapList.containsKey(gamemode)) continue;

                // Key is the map Id, value is the file name
                HashMap<String, String> gamemodeMapFileNames = new HashMap<>();

                for (CBCMap map : gamemodeAndMapList.get(gamemode)) {
                    String imageFileName = gamemodeConfigSection.getString(map.getMapId());
                    if (imageFileName == null) continue;
                    gamemodeMapFileNames.put(map.getMapId(), imageFileName);
                }

                // Add file names from this gamemode into the main hashmap
                gamemodeMapImageFiles.put(gamemode, gamemodeMapFileNames);
            }
        }

        CBCPlugin.getPlugin().getLogger().info("Finished loading gamemodes and maps.");
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

    public void startGame (CBCGamemode gamemode, CBCMap map) {

        // If the practice arena is active close it
        if (practiceManager.isEnabled()) {
            practiceManager.disable();
        }

        HashMap<String, Boolean> boolVars = lobby.getBoolVars();
        HashMap<String, Integer> intVars = lobby.getIntVars();
        HashMap<String, String> stringVars = lobby.getStringVars();

        // Set game state to ACTIVE
        this.gameState = GameState.ACTIVE;

        // Load the chunks of the map
        map.loadMapChunks(true);

        // Start the game depending on the gamemode
        try {
            cbcScoreboardManager.activate();
            if (gamemode == CBCGamemode.SHOWDOWN) {
                currentGame = new ShowdownGame(this, combatManager);
            } else if (gamemode == CBCGamemode.CTF) {
                currentGame = new CTFGame(this, combatManager);
            } else if (gamemode == CBCGamemode.HOLDTHEGOLD) {
                currentGame = new HTGGame(this, combatManager);
            } else if (gamemode == CBCGamemode.TDM) {
                if (map.isMapRush()) {
                    currentGame = new MapRushTDMGame(this, combatManager);
                }
                else {
                    currentGame = new TDMGame(this, combatManager);
                }
            } else if (gamemode == CBCGamemode.RENDEZVOUS) {
                currentGame = new RendezvousGame(this, combatManager);
            } else if (gamemode == CBCGamemode.CBCTAG) {
                currentGame = new TagGame(this, combatManager);
            } else if (gamemode == CBCGamemode.KOTH) {
                currentGame = new KOTHGame(this, combatManager);
            } else if (gamemode == CBCGamemode.FLAGRUSH) {
                currentGame = new FlagRushGame(this, combatManager);
            } else if (gamemode == CBCGamemode.THROWDOWN) {
                currentGame = new ThrowdownGame(this, combatManager);
            } else if (gamemode == CBCGamemode.KMATION) {
                currentGame = new KMationGame(this, combatManager);
            } else if (gamemode == CBCGamemode.ASSASSIN) {
                currentGame = new AssassinGame(this, combatManager);
            }

            currentGame.setupGame(map, lobby.getTeams(), lobby.getLobbyPlayersPlayingAndOnline(), boolVars, intVars, stringVars);

            gameCommands = currentGame.getGameCommands();

            // Set weapon presets
            combatManager.setCreeperWeaponVariables((CreeperPreset) lobby.getWeaponPreset(WeaponType.CREEPER));
            combatManager.setFlameWeaponVariables((FlamePreset) lobby.getWeaponPreset(WeaponType.FLAME));
            combatManager.setXbowWeaponVariables((XbowPreset) lobby.getWeaponPreset(WeaponType.XBOW));

            lobby.putTeamOverridesPresetsIntoWeaponManager();

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
                if (currentGame instanceof TeamGame eventGame) {
                    eventManager.eventGameEnded(eventGame);
                }
            }
        }

        // Clear player list
        resetPlayerList();

        // Clear chat settings
        chatManager.clearChatManager();

        // Reset the game
        currentGame.resetGame();

        // Setup lobby
        startLobby();

        Game game = currentGame;
        lastGame = currentGame;

        // No more game
        currentGame = null;
        gameCommands = null;

        // Restore teams if the game is a team game
        if (game instanceof TeamGame teamGame) {
            // Check if teams should be restored
            if (teamGame.isRestoreTeamsAfterGame()) {

                lobby.restoreTeams(teamGame.getPlayersTeamIds());

            }
        }

        // Update global kills
        if (game.isGlobalKillsEnabled()) {
            globalKillsManager.addPlayersKills(game.getPlayers().values());
        }
    }

    public void resetPlayerList () {
        playerList = new HashMap<>();
        playerIdList = new HashMap<>();
    }

    public void addPlayer (CBCPlayer player, int playerId) {
        playerList.put(player.getOfflinePlayer().getUniqueId(), player);
        playerIdList.put(playerId, player);
    }

    public void removePlayer (CBCPlayer player) {
        playerList.remove(player.getOfflinePlayer().getUniqueId());
        playerIdList.remove(player.getPlayerId());
    }

    public void replacePlayerEntityKey(Player origin, Player newPlayer) {
        if (playerList.containsKey(origin.getUniqueId())) {
            CBCPlayer cbcPlayer = playerList.get(origin.getUniqueId());
            playerList.remove(origin.getUniqueId());
            playerList.put(newPlayer.getUniqueId(), cbcPlayer);
            cbcPlayer.setNewPlayer(newPlayer);
        }
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

    public boolean hasPlayerId (Integer id) {
        return this.playerIdList.containsKey(id);
    }

    public CBCPlayer getPlayerById (Integer id) {
        return this.playerIdList.get(id);
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

    public Game getCurrentGame() {
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
        else return (eventManager.getNextGameNum() < 5);
    }

    public Game getLastGame() {
        return lastGame;
    }

    public GlobalKillsManager getGlobalKillsManager() {
        return globalKillsManager;
    }

    public CombatManager getCombatManager() {
        return combatManager;
    }
}
