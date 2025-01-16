package neonique.cbcplugin_new.gamemodes.tdm;

import neonique.cbcplugin_new.CBCPlugin;
import neonique.cbcplugin_new.enums.CBCGamemode;
import neonique.cbcplugin_new.gamemodes._base.CBCMap;
import neonique.cbcplugin_new.listeners.gamemodes.PlayerNoMove;
import neonique.cbcplugin_new.lobby.LobbyPlayer;
import neonique.cbcplugin_new.lobby.LobbyTeam;
import neonique.cbcplugin_new.managers.GameBossBarManager;
import neonique.cbcplugin_new.managers.GameManager;
import neonique.cbcplugin_new.managers.CombatManager;
import neonique.cbcplugin_new.playerclasses.CBCPlayer;
import neonique.cbcplugin_new.tasks.gamemodetasks.IncrementGameTimeTask;
import neonique.cbcplugin_new.tasks.gamemodetasks.UpdateBossbarsTask;
import neonique.cbcplugin_new.tasks.gamemodetasks.tdm.MapRushTDMGameTimerTask;
import neonique.cbcplugin_new.tasks.gamemodetasks.tdm.MapRushTDMStartMapTimer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.*;

import static net.kyori.adventure.text.Component.newline;

public class MapRushTDMGame extends TDMGame {


    // Map related variables
    private CBCMap currentMap;

    // Map changing related variables
    private final List<CBCMap> mapOrder = new ArrayList<>();
    private int mapNumber = 0;
    private int mapsMaximum = 10;

    // Map timer
    private MapRushTDMGameTimerTask mapTimerTask;
    private int mapTimer = 60;
    private int mapMaxTimer = 60;

    public MapRushTDMGame(GameManager gameManager, CombatManager combatManager) {
        super(gameManager, combatManager);
    }

    @Override
    public void setupGame(CBCMap mapChosen, LinkedHashMap<String, LobbyTeam> teams, Collection<LobbyPlayer> players,
                          HashMap<String, Boolean> boolVars, HashMap<String, Integer> intVars, HashMap<String, String> stringVars) {

        final GameManager gameManager = getGameManager();
        final CombatManager combatManager = getCombatManager();

        // Set gamemode information
        setGamemode(CBCGamemode.TDM);
        createHeaderTitle();

        // Setup default game variables
        setupDefaultGameVars(boolVars, intVars, stringVars);

        // Enable weapons
        combatManager.activateWeapons();
        gameManager.resetPlayerList();

        // Setup gamemode specific game variables
        this.gameByTimer = true;
        this.mapMaxTimer = intVars.getOrDefault("_maprush_gameMapTimer", 60);
        this.mapsMaximum = intVars.getOrDefault("_maprush_mapsAmount", 10);
        this.overtimeThreshold = intVars.getOrDefault("overtimeThreshold", 2);

        this.playersGlow = boolVars.getOrDefault("playersGlow", true);
        this.mapTimer = mapMaxTimer;

        // Setup game commands
        setGameCommands(new TDMGameCommands(gameManager, combatManager, this));

        randomSpawnsEnabled = true;

        // Create teams and players
        createTeams(teams);
        teleportSpectators();

        // Set map
        newMap();

        for (TDMTeam team : this.teams) {
            // Update player placements
            team.updateWithinTeamPlacements();
        }

        // Teleport players who are spectating
        teleportSpectators();

        // Update placements
        updatePlacements();

        // Create new sidebar manager
        createUIManagers();

    }

    @Override
    public GameBossBarManager createBossbarManager() {
        return new MapRushTDMBossbarManager(this);
    }

    @Override
    public void startGame () {

        startMap();

        // Start game length timer
        new IncrementGameTimeTask(this).runTaskTimer(CBCPlugin.getPlugin(), 20, 20);

        // Start game countdown timer if the game is by timer
        timerEnabled = true;

    }

    public void resetPlayers () {
        for (TDMTeam team : this.teams) {
            for (CBCPlayer player : team.getOnlinePlayers()) {
                player.setAlive(false); // Set player's alive state to false
                player.setRespawning(false);
                player.resetPlayer();
            }
        }
    }

    public void newMap () {

        if (currentMap != null) {
            // Teleport players back to spawns while waiting for reload
            teleportPlayersToNewMap();
        }

        resetPlayers();

        mapNumber++;

        // Select a new map
        CBCMap newMap = selectMap();

        mapOrder.add(newMap);

        // Set up the map
        setupMap(newMap);
        if (!newMap.isNightVisionAlwaysDisabled()) {
            getCombatManager().setNightVisionDisabled(isNightVisionDisabled());
        }
        else {
            getCombatManager().setNightVisionDisabled(true);
        }

        // Teleport all players to new map
        teleportPlayersToNewMap();

        // Teleport all spectators to new map
        for (Player player : getWorld().getPlayers()) {
            if (!getPlayers().containsKey(player.getUniqueId())) {
                // Player is spectating, put player into spectator mode
                player.setGameMode(GameMode.SPECTATOR);
                player.teleport(currentMap.getMapCentre());
            }
        }

        // Prevent movement from players - they should still be able to turn their heads
        playerNoMoveListener = new PlayerNoMove(getGameManager());
        CBCPlugin.getPlugin().getServer().getPluginManager().registerEvents(playerNoMoveListener, CBCPlugin.getPlugin());

        this.mapTimer = mapMaxTimer;

        // Start countdown for the map
        if (mapNumber == 1) {
            startGameTimer = new MapRushTDMStartMapTimer(getGameManager(), this, 11, isFinalMap());
        }
        else {
            startGameTimer = new MapRushTDMStartMapTimer(getGameManager(), this, 6, isFinalMap());
        }
        startGameTimer.runTaskTimer(CBCPlugin.getPlugin(), 0, 20);

    }

    public void startMap () {

        // Let all players move
        if (playerNoMoveListener != null) {
            PlayerMoveEvent.getHandlerList().unregister(playerNoMoveListener);
            playerNoMoveListener = null;
        }

        // Initialise all players
        for (TDMTeam team : teams) {
            for (CBCPlayer player : team.getOnlinePlayers()) {
                TDMPlayer tdmPlayer = (TDMPlayer) player;
                tdmPlayer.playerRefresh();
            }
        }

        currentMap.fillBlocksAtEnd();

        mapTimerTask = new MapRushTDMGameTimerTask(this);
        mapTimerTask.runTaskTimer(CBCPlugin.getPlugin(), 20, 20);

    }

    public void teleportPlayersToNewMap () {

        // Teleport all players
        for (TDMTeam team : teams) {

            team.setSpawns(teamSpawns.get(team.getTeamId()));

            List<Location> teamSpawnList = new ArrayList<>(team.getSpawns());
            Collections.shuffle(teamSpawnList);

            int playerinc = 0; // Increments every time we teleport a player
            for (CBCPlayer player : team.getPlayers()) {

                if (!player.isOnline()) continue;

                TDMPlayer tdmPlayer = (TDMPlayer) player;
                tdmPlayer.resetPlayer();
                // Spawns players in different spawnpoints - reason playerinc is used
                tdmPlayer.teleportPlayerToSpawn(teamSpawnList.get(playerinc % teamSpawnList.size()));

                playerinc++;

                createHeaderTitle();
                updateHeaderTitle();
            }
        }

    }

    public CBCMap selectMap () {

        List<CBCMap> mapList = new ArrayList<>(getGameManager().getPracticeMaps());
        Collections.shuffle(mapList);

        for (CBCMap map : mapList) {
            // Make sure map has not already been selected
            if (!mapOrder.contains(map)) {
                // Make sure map is a valid map with all 8 teams
                if (map.hasAllTeamsSpawns()) {
                    return map;
                }
            }
        }

        return mapList.get(0);

    }

    public void setupMap (CBCMap map) {

        randomSpawnsEnabled = true;
        currentMap = map;

        setGeneralMap(map);
        map.fillBlocksAtStart();
        getCombatManager().setupMap(map);

        getGameManager().sendGlobalMessage(Component.text("Loading chunks for next map...").color(NamedTextColor.YELLOW));
        map.loadMapChunks(false);
        getGameManager().sendGlobalMessage(Component.text("Finished loading chunks!").color(NamedTextColor.YELLOW));

        // Get team spawns
        teamSpawns = map.getDefaultTeamSpawns();
        spawns = map.getTDMSpawns();



    }

    @Override
    public void createHeaderTitle () {
        if (getMap() != null) {
            setHeaderTitle(smallText("          Crossbow Champions: ").color(NamedTextColor.YELLOW)
                    .append(
                            smallText( "Team Deathmatch          ").color(NamedTextColor.AQUA)
                    ).append(
                            newline()
                    ).append(
                            smallText(getMap().getMapName() + " (MAP RUSH) - ").color(NamedTextColor.GRAY)
                    ).append(
                            smallText(gameLengthToText()).color(NamedTextColor.GRAY)
                    ));
        }
    }

    public void decrementMapTimer (MapRushTDMGameTimerTask task) {

        if (!timerEnabled) {
            return;
        }
        mapTimer--;

        if (!isFinalMap()) {
            if (mapTimer == 30) {
                timerMessage(Component.text("30 seconds until the map switches!").color(NamedTextColor.YELLOW), 1);
            } else if (mapTimer == 10) {
                timerMessage(Component.text("10 seconds until the map switches!").color(NamedTextColor.GOLD), 2);
            } else if (mapTimer <= 5 && mapTimer > 1) {
                timerMessage(Component.text(mapTimer + " seconds until the map switches!").color(NamedTextColor.RED), 2);
            } else if (mapTimer == 1) {
                timerMessage(Component.text("1 second until the map switches!").color(NamedTextColor.RED), 2);
            }
        }
        else {
            if (mapTimer == 30) {
                timerMessage(Component.text("30 seconds left!").color(NamedTextColor.YELLOW), 1);
            } else if (mapTimer == 10) {
                timerMessage(Component.text("10 seconds left!").color(NamedTextColor.GOLD), 2);
            } else if (mapTimer <= 5 && mapTimer > 1) {
                timerMessage(Component.text(mapTimer + " seconds left!").color(NamedTextColor.RED), 2);
            } else if (mapTimer == 1) {
                timerMessage(Component.text("1 second left!").color(NamedTextColor.RED), 2);
            }
        }
        if (mapTimer == 0) {
            task.cancel();
            // Map timer is finished
            mapTimerFinished();
        }

        updateBossbarManager();
    }

    public void mapTimerFinished () {

        // Check if final map
        if (isFinalMap()) {
            // End game
            timerEnabled = false; // Disable timer
            // Check for winner of game
            checkWinner();
        }
        else {
            // Move onto next map
            newMap();
        }
    }

    public boolean isFinalMap () {
        return mapNumber == mapsMaximum;
    }

    public int getMapNumber() {
        return mapNumber;
    }

    public int getMapsMaximum() {
        return mapsMaximum;
    }

    public String mapTimerToText() {
        return String.format("%d:%02d", mapTimer / 60, mapTimer % 60);
    }

    public int getMapTimer() {
        return mapTimer;
    }
}
