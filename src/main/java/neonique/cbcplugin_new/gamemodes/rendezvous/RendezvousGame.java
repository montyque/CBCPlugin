package neonique.cbcplugin_new.gamemodes.rendezvous;

import neonique.cbcplugin_new.CBCPlugin;
import neonique.cbcplugin_new.enums.CBCGamemode;
import neonique.cbcplugin_new.gamemodes._base.*;
import neonique.cbcplugin_new.listeners.gamemodes.PlayerNoMove;
import neonique.cbcplugin_new.lobby.LobbyPlayer;
import neonique.cbcplugin_new.lobby.LobbyTeam;
import neonique.cbcplugin_new.managers.GameBossBarManager;
import neonique.cbcplugin_new.managers.GameManager;
import neonique.cbcplugin_new.managers.CombatManager;
import neonique.cbcplugin_new.playerclasses.CBCPlayer;
import neonique.cbcplugin_new.tasks.gamemodetasks.IncrementGameTimeTask;
import neonique.cbcplugin_new.tasks.gamemodetasks.rendezvous.RendezvousCheckpointTask;
import neonique.cbcplugin_new.tasks.gamemodetasks.rendezvous.RendezvousStartTimer;
import neonique.cbcplugin_new.util.StringUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

import static neonique.cbcplugin_new.util.StatsUtil.sortPlayerStatList;

public class RendezvousGame extends TeamGame {

    // Team list
    private final List<RendezvousTeam> teams = new ArrayList<>();

    // Game related variables
    private RendezvousSwapSystem swapType = RendezvousSwapSystem.TIMER;
    private int swapTimer;
    private int swapPeriod;
    private int checkpointsToSwap;
    private int scoreStart = 1;

    private int teamsToWin = 1;
    private int teamsWon = 0;
    private RendezvousTeam originalWinningTeam = null;

    // Map related variables
    private RendezvousMap map;
    private List<RendezvousCheckpoint> checkpoints;
    private List<RendezvousSpawn> spawns;
    private HashMap<String, Set<Location>> teamStartSpawns;
    private boolean finalCheckpointEnabled;
    private RendezvousCheckpoint finalCheckpoint;

    // Checkpoint related variables
    private double checkpointMinDistance;
    private double checkpointMaxDistance;
    private final List<Double> checkpointTargetDistances = new ArrayList<>();
    private boolean canHalfStick = false;
    private int timeToCaptureCheckpoint = 30;

    // Tasks and event listeners
    private RendezvousStartTimer startGameTimer;
    private RendezvousCheckpointTask checkpointTracking;
    private PlayerNoMove playerNoMoveListener;

    // Current leaderboards
    private List<PlayerStatObject> topKills;
    private List<PlayerStatObject> topGameScore;
    private List<PlayerStatObject> topCheckpoints;
    private List<PlayerStatObject> topRunnerKills;

    public RendezvousGame(GameManager gameManager, CombatManager combatManager) {
        super(gameManager, combatManager);
    }

    @Override
    public void setupGame(CBCMap mapChosen, LinkedHashMap<String, LobbyTeam> teams, Collection<LobbyPlayer> players,
                          HashMap<String, Boolean> boolVars, HashMap<String, Integer> intVars, HashMap<String, String> stringVars) {

        final GameManager gameManager = getGameManager();
        final CombatManager combatManager = getCombatManager();
        final World world = getWorld();

        // Setup map
        setupMap(mapChosen);
        // Setup default game variables
        setupDefaultGameVars(boolVars, intVars, stringVars);

        // Set gamemode information
        setGamemode(CBCGamemode.RENDEZVOUS);
        createHeaderTitle();

        // Enable weapons
        combatManager.activateWeapons();
        gameManager.resetPlayerList();

        // Set game commands
        setGameCommands(new RendezvousGameCommands(gameManager, combatManager, this));

        // Setup gamemode game variables
        this.scoreStart = intVars.getOrDefault("checkpointsToWin", 8);

        String swapSystemType = stringVars.getOrDefault("swapSystemType", "timer").toLowerCase();
        if (swapSystemType.equals("totalcheckpoints")) {
            swapType = RendezvousSwapSystem.TOTAL_CHECKPOINTS;
            checkpointsToSwap = intVars.getOrDefault("swapCheckpoints", 8);
        }
        else if (swapSystemType.equals("teamcheckpoints")) {
            swapType = RendezvousSwapSystem.TEAM_CHECKPOINTS;
            checkpointsToSwap = intVars.getOrDefault("swapCheckpoints", 4);
        }
        else {
            swapType = RendezvousSwapSystem.TIMER;
            swapPeriod = intVars.getOrDefault("swapTimer", 120);
            swapTimer = swapPeriod;
        }

        canHalfStick = boolVars.getOrDefault("canHalfStick", false);
        timeToCaptureCheckpoint = intVars.getOrDefault("captureTime", 30);

        // Change final checkpoint
        if (finalCheckpointEnabled) {
            finalCheckpointEnabled = boolVars.getOrDefault("finalCheckpointEnabled", false);
        }

        this.teamsToWin = intVars.getOrDefault("teamsToWin", 1);

        // Create teams and players
        createTeams(teams);
        teleportSpectators();

        // Teleport all players
        for (RendezvousTeam team : this.teams) {

            team.setSpawns(teamStartSpawns.get(team.getTeamId()));

            List<Location> teamSpawnList = new ArrayList<>(team.getSpawns());
            Collections.shuffle(teamSpawnList);

            int playerinc = 0; // Increments every time we teleport a player
            for (CBCPlayer player : team.getPlayers()) {
                RendezvousPlayer rdvPlayer = (RendezvousPlayer) player;
                rdvPlayer.resetPlayer();
                // Spawns players in different spawnpoints - reason playerinc is used
                rdvPlayer.teleportPlayerToSpawn(teamSpawnList.get(playerinc % teamSpawnList.size()));

                playerinc++;
            }

            // Select a player's runner
            String teamId = team.getTeamId();
            String runnerOrderString = stringVars.getOrDefault("_" + teamId + "RunnerOrder", "0").toLowerCase();

            team.setRunnerQueue(runnerOrderString);
            team.setRunnerNextPlayerInQueue();

            // Set team capture time
            team.setProgressMax(timeToCaptureCheckpoint);
        }

        // Update leaderboards
        updateTopKillsList();
        updateTopRunnerKillsList();
        updateTopCheckpointsList();
        updateTopGameScoreList();

        // Setup sidebar and bossbar
        createUIManagers();

        // Checkpoint tracking event
        checkpointTracking = new RendezvousCheckpointTask(this);
        checkpointTracking.runTaskTimer(CBCPlugin.getPlugin(), 2, 2);

        // Prevent movement from players - they should still be able to turn their heads
        playerNoMoveListener = new PlayerNoMove(gameManager);
        CBCPlugin.getPlugin().getServer().getPluginManager().registerEvents(playerNoMoveListener, CBCPlugin.getPlugin());

        // Start countdown for the game
        startGameTimer = new RendezvousStartTimer(gameManager, this, 11);
        startGameTimer.runTaskTimer(CBCPlugin.getPlugin(), 0, 20);
    }

    public CBCTeam createGamemodeTeam (LobbyTeam team, int teamNum) {
        RendezvousTeam createdTeam = new RendezvousTeam(this, team.getTeamId(),
                Integer.toString(teamNum), team.getTeamName(), team.getColor(),
                team.getPrefix(), team.getItem(), team.getGlassHead()
        );
        teams.add(createdTeam);
        return createdTeam;
    }

    public CBCPlayer createGamemodePlayer (Player playerEntity, int playerId) {
        return new RendezvousPlayer(this, getGameManager(), getCombatManager(), playerEntity, playerId);
    }

    public GameSidebarManager createSidebarManager() {
        return new RendezvousSidebarManager(getGameManager(), getCombatManager(), this);
    }

    public GameBossBarManager createBossbarManager() {
        return new RendezvousBossbarManager(this);
    }

    public void setupMap (CBCMap generalMap) {

        super.setupMap(generalMap);
        this.map = (RendezvousMap) generalMap;

        // Get team start spawns
        teamStartSpawns = this.map.getTeamStartSpawns();

        // Get spawns
        spawns = this.map.getRandomSpawns();

        // Get checkpoints
        checkpoints = this.map.getCheckpoints();

        // Get min and max checkpoint distance
        checkpointMinDistance = this.map.getCheckpointDistanceMin();
        checkpointMaxDistance = this.map.getCheckpointDistanceMax();

        // Get final checkpoint
        finalCheckpointEnabled = this.map.isFinalCheckpointEnabled();
        if (finalCheckpointEnabled) {
            finalCheckpoint = this.map.getFinalCheckpoint();
        }
    }

    public void startGame() {

        map.fillBlocksAtEnd();

        PlayerMoveEvent.getHandlerList().unregister(playerNoMoveListener);
        playerNoMoveListener = null;

        // Initialise all players
        for (RendezvousTeam team : teams) {
            for (CBCPlayer player : team.getOnlinePlayers()) {
                RendezvousPlayer rdvPlayer = (RendezvousPlayer) player;

                rdvPlayer.playerSetup();
                rdvPlayer.setReloadsBySecond(1);
                rdvPlayer.setTempImmune(60);

                if (rdvPlayer.isPlayerRunner()) {
                    rdvPlayer.getPlayer().addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, 800000,
                            0, false, false, false));
                }
            }
        }

        // Start game length timer
        new IncrementGameTimeTask(this).runTaskTimer(CBCPlugin.getPlugin(), 20, 20);

    }

    public void updatePlacements () {

        List<RendezvousTeam> teamsByScore = getTeamsByScore();

        int placement = 0;
        int currentScore = -1;
        int i = 0;

        for (RendezvousTeam team : teamsByScore) {

            boolean tied = false;
            if (team.getScore() > currentScore) {
                placement = i + 1;
                currentScore = team.getScore();
                if (teamsByScore.size() - 1 != i) {
                    if (teamsByScore.get(i + 1).getScore() == currentScore) {
                        tied = true;
                    }
                }
            }
            else if (currentScore == team.getScore()) {
                tied = true;
            }

            team.setPlacement(placement, tied);
            i++;
        }
    }

    public Set<RendezvousCheckpoint> getInPlayCheckpoints () {

        Set<RendezvousCheckpoint> inPlayCheckpoints = new HashSet<>();
        for (RendezvousTeam team : getTeams()) {
            if (team.getTargetCheckpoint() != null) {
                inPlayCheckpoints.add(team.getTargetCheckpoint());
            }
        }

        return inPlayCheckpoints;

    }

    public List<RendezvousTeam> getTeamsByScore() {

        List<RendezvousTeam> sortedTeamList = new ArrayList<>(teams);
        sortedTeamList.sort(Comparator.comparingInt(RendezvousTeam::getScore));

        return sortedTeamList;

    }

    public void checkTeamWon (RendezvousTeam team) {

        // Don't end game yet
        if (teamsToWin > 1) {

            // Don't end game yet if the team amount to win has not been met
            teamsWon++;
            if (teamsWon == 1) {
                originalWinningTeam = team;
            }

            // Say a team's placement
            getGameManager().sendGlobalMessage(
                    Component.newline()
                            .append(Component.text("GAME PLACEMENT > ").decorate(TextDecoration.BOLD).color(NamedTextColor.WHITE))
                            .append(Component.text(team.getTeamName()).decorate(TextDecoration.BOLD).color(team.getColor()))
                            .append(Component.text(" has placed " + StringUtil.getPlacementString(teamsWon) + "!").color(NamedTextColor.WHITE))
                            .append(Component.newline())
            );

            // Play sound
            getGameManager().playGlobalSound(Sound.ENTITY_PLAYER_LEVELUP, 100, 0);

            // Check if team amount to win has been met
            if (teamsWon == teamsToWin) {
                gameWon(originalWinningTeam);
            }
            else {
                // Has not been met yet, so do not end the game
                team.teamOutOfGame(teamsWon);
            }

        }
        else {
            gameWon(team);
        }
    }

    @Override
    public void gameWon (CBCTeam team) {

        super.gameWon(team);

        // Add bonus points for winning
        for (CBCPlayer player : team.getPlayers()) {
            player.addGamePoints(40);
        }

        cancelTask(checkpointTracking);

    }

    public void decrementSwapTimer() {

        final GameManager gameManager = getGameManager();
        swapTimer--;

        if (swapTimer == 0) {
            // Swap runners
            swapAllRunners();
            // Set timer back up to normal
            swapTimer = swapPeriod;
        }
        else if (swapTimer == 60) {
            gameManager.sendGlobalMessage(Component.text("1 minute until teams swap runners!")
                    .color(NamedTextColor.YELLOW));
            gameManager.playGlobalSound(Sound.BLOCK_NOTE_BLOCK_BIT, 200, 1);
        }
        else if (swapTimer == 30) {
            gameManager.sendGlobalMessage(Component.text("30 seconds until teams swap runners!")
                    .color(NamedTextColor.GOLD));
            gameManager.playGlobalSound(Sound.BLOCK_NOTE_BLOCK_BIT, 200, 1);
        }
        else if (swapTimer == 10) {
            gameManager.sendGlobalMessage(Component.text("10 seconds until teams swap runners!")
                    .color(NamedTextColor.RED));
            gameManager.playGlobalSound(Sound.BLOCK_NOTE_BLOCK_BIT, 200, 2);
        }

        if (swapTimer <= 10) {
            for (RendezvousTeam team : getTeams()) {
                team.nextRunnerWarning();
            }
        }

        updateBossbarManager();
    }

    public void swapAllRunners() {

        // Teleport all players
        for (RendezvousTeam team : teams) {
            if (team.isOutOfGame()) continue;
            // Select a player's runner
            team.setRunnerNextPlayerInQueue();
        }

        // Send message and play sound
        getGameManager().playGlobalSound(Sound.BLOCK_BEACON_POWER_SELECT, 200, 2);

    }

    @Override
    public void resetGame() {

        super.resetGame();

        // Cancel tasks
        cancelTask(startGameTimer);
        cancelTask(checkpointTracking);

    }

    @Override
    public void incrementGameTime() {
        super.incrementGameTime();

        // If swap by timer, decrement swap timer
        if (swapType == RendezvousSwapSystem.TIMER) {
            if (getWinner() == null) {
                decrementSwapTimer();
            }
        }
    }

    @Override
    public PostGameStats getPostGameStats() {
        return new RendezvousPostGameStats(this);
    }

    public int getScoreStart() {
        return scoreStart;
    }

    public List<RendezvousCheckpoint> getCheckpoints() {
        return checkpoints;
    }

    public List<RendezvousSpawn> getSpawns() {
        return spawns;
    }

    public List<RendezvousTeam> getTeams() {
        return teams;
    }

    public boolean isFinalCheckpointEnabled() {
        return finalCheckpointEnabled;
    }

    public RendezvousCheckpoint getFinalCheckpoint() {
        return finalCheckpoint;
    }

    public int getTargetDistancesSize () {
        return checkpointTargetDistances.size();
    }

    public double getRandomCheckpointDistance () {
        return ThreadLocalRandom.current().nextDouble(checkpointMinDistance, checkpointMaxDistance);
    }

    public List<Double> getCheckpointTargetDistances() {
        return checkpointTargetDistances;
    }

    public void addCheckpointTargetDistance (double d) {
        checkpointTargetDistances.add(d);
    }

    public RendezvousSwapSystem getSwapType() {
        return swapType;
    }

    public int getSwapTimer() {
        return swapTimer;
    }

    public boolean canHalfStick() {
        return canHalfStick;
    }

    public Set<RendezvousPlayer> getCurrentAliveRunners () {
        Set<RendezvousPlayer> aliveRunners = new HashSet<>();
        for (RendezvousTeam team : getTeams()) {
            if (team.getRunner() != null) {
                if (team.getRunner().isAlive()) {
                    aliveRunners.add(team.getRunner());
                }
            }
        }

        return aliveRunners;
    }

    public void updateTopKillsList () {
        // Create new top kills list
        topKills = new ArrayList<>();
        for (RendezvousPlayer player : getRendezvousPlayers()) {
            // Add player's kills to the list
            topKills.add(new PlayerStatObject(player, player.getKills()));
        }
        // Sort list
        sortPlayerStatList(topKills, true);
    }

    public List<PlayerStatObject> getTopKillsList () {
        return topKills;
    }

    public void updateTopGameScoreList () {
        // Create new top game score list
        topGameScore = new ArrayList<>();
        for (RendezvousPlayer player : getRendezvousPlayers()) {
            // Add player's game score to the list
            topGameScore.add(new PlayerStatObject(player, player.getGamePoints()));
        }
        // Sort list
        sortPlayerStatList(topGameScore, true);
    }

    public List<PlayerStatObject> getTopGameScoreList () {
        return topGameScore;
    }

    public void updateTopCheckpointsList () {
        // Create new top checkpoints list
        topCheckpoints = new ArrayList<>();
        for (RendezvousPlayer player : getRendezvousPlayers()) {
            // Add player's checkpoints to the list
            topCheckpoints.add(new PlayerStatObject(player, player.getCheckpointsCleared()));
        }
        // Sort list
        sortPlayerStatList(topCheckpoints, true);
    }

    public List<PlayerStatObject> getTopCheckpointsList () {
        return topCheckpoints;
    }

    public void updateTopRunnerKillsList () {
        // Create new top runner kills list
        topRunnerKills = new ArrayList<>();
        for (RendezvousPlayer player : getRendezvousPlayers()) {
            // Add player's runner kills to the list
            topRunnerKills.add(new PlayerStatObject(player, player.getEnemyRunnersKilled()));
        }
        // Sort list
        sortPlayerStatList(topRunnerKills, true);
    }

    public List<PlayerStatObject> getTopRunnerKills () {
        return topRunnerKills;
    }

    public List<RendezvousPlayer> getRendezvousPlayers () {
        // Get all players as RendezvousPlayer objects
        List<RendezvousPlayer> playersList = new ArrayList<>();
        for (CBCPlayer player : getPlayers().values()) {
            if (player instanceof RendezvousPlayer) {
                playersList.add((RendezvousPlayer) player);
            }
        }
        return playersList;
    }
}
