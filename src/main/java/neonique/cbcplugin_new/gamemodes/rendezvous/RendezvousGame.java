package neonique.cbcplugin_new.gamemodes.rendezvous;

import neonique.cbcplugin_new.CBCPlugin;
import neonique.cbcplugin_new.core.TeamGame;
import neonique.cbcplugin_new.core.TeamLike;
import neonique.cbcplugin_new.core.CBCGamemode;
import neonique.cbcplugin_new.core.FFAGameContext;
import neonique.cbcplugin_new.gamemodes._base.*;
import neonique.cbcplugin_new.listeners.gamemodes.PlayerNoMove;
import neonique.cbcplugin_new.managers.GameBossBarManager;
import neonique.cbcplugin_new.managers.GameManager;
import neonique.cbcplugin_new.combat.CombatManager;
import neonique.cbcplugin_new.tasks.gamemodetasks.IncrementGameTimeTask;
import neonique.cbcplugin_new.gamemodes.rendezvous.tasks.RendezvousCheckpointTask;
import neonique.cbcplugin_new.gamemodes.rendezvous.tasks.RendezvousStartTimer;
import neonique.cbcplugin_new.util.StringUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

public class RendezvousGame extends TeamGame<RendezvousPlayer, RendezvousMap, RendezvousTeam> {

    // Game related variables
    private RendezvousSwapSystem swapType = RendezvousSwapSystem.TIMER;
    private int swapTimer;
    private int swapPeriod;
    private int scoreStart = 1;

    private int teamsToWin = 1;
    private int teamsWon = 0;
    private RendezvousTeam originalWinningTeam = null;

    // Map related variables
    private List<RendezvousCheckpoint> checkpoints;
    private List<RendezvousSpawn> spawns;
    private Map<String, List<Location>> teamStartSpawns;
    private boolean finalCheckpointEnabled;
    private RendezvousCheckpoint finalCheckpoint;

    // Checkpoint related variables
    private double checkpointMinDistance;
    private double checkpointMaxDistance;
    private final List<Double> checkpointTargetDistances = new ArrayList<>();
    private boolean canHalfStick = false;

    // Tasks and event listeners
    private RendezvousStartTimer startGameTimer;
    private RendezvousCheckpointTask checkpointTracking;
    private PlayerNoMove playerNoMoveListener;

    public RendezvousGame(GameManager gameManager) {
        super(gameManager);
    }

    @Override
    public CBCGamemode getGamemode () {
        return CBCGamemode.RENDEZVOUS;
    }

    @Override
    public RendezvousTeam createGamemodeTeam (TeamLike team, int teamNum) {
        return new RendezvousTeam(this, team, Integer.toString(teamNum));
    }

    @Override
    public RendezvousPlayer createPlayer(Player playerEntity) {
        return new RendezvousPlayer(this, getGameManager(), getCombatManager(), playerEntity);
    }

    @Override
    public GameSidebarManager createSidebarManager() {
        return new RendezvousSidebarManager(getGameManager(), getCombatManager(), this);
    }

    @Override
    public GameBossBarManager createBossbarManager() {
        return new RendezvousBossbarManager(this);
    }

    @Override
    public void setupMap (RendezvousMap map) {

        super.setupMap(map);

        teamStartSpawns = map.getTeamStartSpawns();
        spawns = map.getRandomSpawns(this);
        checkpoints = map.getCheckpoints();

        // Get min and max checkpoint distance
        checkpointMinDistance = map.getCheckpointDistanceMin();
        checkpointMaxDistance = map.getCheckpointDistanceMax();

        // Get final checkpoint
        finalCheckpointEnabled = map.isFinalCheckpointEnabled();
        if (finalCheckpointEnabled) {
            finalCheckpoint = map.getFinalCheckpoint();
        }
    }

    @Override
    public void setupGame (FFAGameContext ctx) {

        final GameManager gameManager = getGameManager();
        final CombatManager combatManager = getCombatManager();

        // Setup map
        setupMap((RendezvousMap) ctx.getMap());

        // Setup default game variables
        setupDefaultGameVars(ctx.getBoolVars(), ctx.getIntVars(), ctx.getStringVars());

        // Set gamemode information
        createHeaderTitle();

        // Enable weapons
        combatManager.activate(this);

        // Set game commands
        setGameCommands(new RendezvousGameCommands(this));

        // Setup gamemode game variables
        this.scoreStart = ctx.getIntVars().getOrDefault("checkpointsToWin", 8);

        String swapSystemType = ctx.getStringVars().getOrDefault("swapSystemType", "timer").toLowerCase();
        if (swapSystemType.equals("totalcheckpoints")) {
            swapType = RendezvousSwapSystem.TOTAL_CHECKPOINTS;
        }
        else if (swapSystemType.equals("teamcheckpoints")) {
            swapType = RendezvousSwapSystem.TEAM_CHECKPOINTS;
        }
        else {
            swapType = RendezvousSwapSystem.TIMER;
            swapPeriod = ctx.getIntVars().getOrDefault("swapTimer", 120);
            swapTimer = swapPeriod;
        }

        canHalfStick = ctx.getBoolVars().getOrDefault("canHalfStick", false);
        int timeToCaptureCheckpoint = ctx.getIntVars().getOrDefault("captureTime", 30);

        // Change final checkpoint
        if (finalCheckpointEnabled) {
            finalCheckpointEnabled = ctx.getBoolVars().getOrDefault("finalCheckpointEnabled", false);
        }

        this.teamsToWin = ctx.getIntVars().getOrDefault("teamsToWin", 1);

        // Create teams and players
        createTeams(ctx.getTeams());
        teleportSpectators();

        // Teleport all players
        for (RendezvousTeam team : getTeams()) {

            team.setSpawns(teamStartSpawns.get(team.id()));

            List<Location> teamSpawnList = new ArrayList<>(team.getSpawns());
            Collections.shuffle(teamSpawnList);

            int playerinc = 0; // Increments every time we teleport a player
            for (RendezvousPlayer player : team.players()) {

                player.resetPlayer();

                // Spawns players in different spawnpoints - reason playerinc is used
                player.teleportPlayerToSpawn(teamSpawnList.get(playerinc % teamSpawnList.size()), this.getMap().getMapCentre());
                playerinc++;

            }

            // Select a player's runner
            String teamId = team.id();
            String runnerOrderString = ctx.getStringVars().getOrDefault("_" + teamId + "RunnerOrder", "0").toLowerCase();

            team.setRunnerQueue(runnerOrderString);
            team.setRunnerNextPlayerInQueue();

            // Set team capture time
            team.setProgressMax(timeToCaptureCheckpoint);
        }

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

    @Override
    public void gameWon (RendezvousTeam team) {

        super.gameWon(team);
        for (RendezvousPlayer player : team.players()) {
            player.addGamePoints(40);
        }

        cancelTask(checkpointTracking);

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

    public void startGame() {

        this.getMap().fillBlocksAtEnd();

        // Allow players to move
        PlayerMoveEvent.getHandlerList().unregister(playerNoMoveListener);
        playerNoMoveListener = null;

        // Initialise all players
        for (RendezvousTeam team : getTeams()) {
            for (RendezvousPlayer player : team.onlinePlayers()) {
                player.playerSetup(2);
                player.setTempImmune(60);
                if (player.isPlayerRunner()) {
                    player.getPlayer().addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, -1,
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
        List<RendezvousTeam> sortedTeamList = getTeams();
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
                            .append(Component.text(team.name()).decorate(TextDecoration.BOLD).color(team.textColor()))
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
        for (RendezvousTeam team : getTeams()) {
            if (team.isOutOfGame()) continue;
            // Select a player's runner
            team.setRunnerNextPlayerInQueue();
        }

        // Send message and play sound
        getGameManager().playGlobalSound(Sound.BLOCK_BEACON_POWER_SELECT, 200, 2);

        updateServerSidebar();
        updateBossbarManager();

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

}
