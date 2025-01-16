package neonique.cbcplugin_new.gamemodes.koth;

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
import neonique.cbcplugin_new.tasks.gamemodetasks.koth.KOTHHillParticlesTask;
import neonique.cbcplugin_new.tasks.gamemodetasks.koth.KOTHHillTask;
import neonique.cbcplugin_new.tasks.gamemodetasks.koth.KOTHScoreTask;
import neonique.cbcplugin_new.tasks.gamemodetasks.koth.KOTHStartTimer;
import neonique.cbcplugin_new.util.StringUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerMoveEvent;

import java.time.Duration;
import java.util.*;

public class KOTHGame extends TeamGame {

    // Gamemode and map
    private KOTHMap map;

    // List of teams and other team related variables
    private final List<KOTHTeam> teams = new ArrayList<>();

    // Game related variables
    private boolean hillEnabled = false;
    private KOTHTeam pointControlTeam = null; // The team that currently controls the point
    private KOTHTeam pointCaptureTeam = null;
    private int pointCaptureProgress = 0;
    private int pointCaptureMax = 240;
    private int pointCaptureRate = 80; // Amount of progress to put into capturing per second
    private int pointLoseRate = 120; // Amount of progress to put into capturing per second

    private float capturingPlayerPercentage = 0.50f;

    private int pointsStart = 101;
    private int ticksToScore = 20;

    // Map variables
    private KOTHHill hill; // Hill objective

    private boolean randomSpawnsEnabled; // If players are to spawn randomly around the map
    private Set<Location> randomSpawns; // Where players will spawn randomly

    // Tasks and listeners
    private PlayerNoMove playerNoMoveListener; // Activated at start of game so players cannot move
    private KOTHStartTimer startGameTimer;
    private KOTHHillParticlesTask hillParticlesTask;
    private KOTHHillTask hillControlTask;
    private KOTHScoreTask scoreTask;

    private int teamsToWin = 1;
    private int teamsWon = 0;
    private KOTHTeam originalWinningTeam = null;

    public KOTHGame(GameManager gameManager, CombatManager combatManager) {
        super(gameManager, combatManager);
    }

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
        setGamemode(CBCGamemode.KOTH);
        createHeaderTitle();

        // Enable weapons
        combatManager.activateWeapons();
        gameManager.resetPlayerList();

        // Setup gamemode game variables
        this.pointsStart = intVars.getOrDefault("pointsStart", 40);
        this.ticksToScore = intVars.getOrDefault("ticksToScore", 40);
        this.capturingPlayerPercentage = (float) intVars.getOrDefault("captureMajorityPercentage", 50) / 100;
        this.teamsToWin = intVars.getOrDefault("teamsToWin", 1);

        // Make sure not to go out of bounds
        if (this.capturingPlayerPercentage > 1 || this.capturingPlayerPercentage <= 0) {
            this.capturingPlayerPercentage = 0.5f;
        }

        // Setup game commands - TO DO LATER
        setGameCommands(new BaseTeamGameCommands(gameManager, combatManager, this));

        // Create teams and players
        HashMap<String, Set<Location>> teamSpawns = map.getTeamSpawns();
        createTeams(teams);
        teleportSpectators();

        // Teleport all players to their spawns
        for (KOTHTeam team : this.teams) {

            team.setScore(pointsStart);
            team.setTeamSpawns(teamSpawns.get(team.getTeamId()));

            List<Location> teamSpawnList = new ArrayList<>(team.getTeamSpawns());
            Collections.shuffle(teamSpawnList);

            int playerinc = 0; // Increments every time we teleport a player
            for (KOTHPlayer player : team.getKothPlayers()) {
                // Reset player to get ready for game and teleport to spawn
                player.resetPlayer();
                // Teleports player to a spawn not already used
                player.teleportPlayerToSpawn(teamSpawnList.get(playerinc % teamSpawnList.size()));
                playerinc++;
            }
        }

        // Prevent movement from players - they should still be able to turn their heads
        playerNoMoveListener = new PlayerNoMove(gameManager);
        CBCPlugin.getPlugin().getServer().getPluginManager().registerEvents(playerNoMoveListener, CBCPlugin.getPlugin());

        // Update placements -- TO DO
        updatePlacements();

        // Update stat lists for server board -- TO DO
        //
        //

        // Setup sidebar and bossbar
        createUIManagers();

        // Start hill detection task
        float detectionFrequency = 5; // Amount of times per second to run this task
        int detectionPeriodInTicks = Math.round(20f / detectionFrequency); // Period of running task in ticks
        detectionFrequency = 20f / (float) detectionPeriodInTicks;

        hillControlTask = new KOTHHillTask(this, hill, detectionFrequency);
        hillControlTask.runTaskTimer(CBCPlugin.getPlugin(), 0, detectionPeriodInTicks);

        // Start hill particles task
        hillParticlesTask = new KOTHHillParticlesTask(this, hill);
        hillParticlesTask.runTaskTimer(CBCPlugin.getPlugin(), 0, 5);

        // Start countdown for the game
        startGameTimer = new KOTHStartTimer(gameManager, this, 11);
        startGameTimer.runTaskTimer(CBCPlugin.getPlugin(), 0, 20);

    }

    public CBCTeam createGamemodeTeam (LobbyTeam team, int teamNum) {
        KOTHTeam createdTeam = new KOTHTeam(this, team.getTeamId(),
                Integer.toString(teamNum), team.getTeamName(), team.getColor(),
                team.getPrefix(), team.getItem(), team.getGlassHead()
        );
        teams.add(createdTeam);
        return createdTeam;
    }

    public CBCPlayer createGamemodePlayer (Player playerEntity, int playerId) {
        return new KOTHPlayer(this, getGameManager(), getCombatManager(), playerEntity, playerId);
    }

    public GameSidebarManager createSidebarManager() {
        return new KOTHSidebarManager(getGameManager(), getCombatManager(), this);
    }

    @Override
    public GameBossBarManager createBossbarManager() {
        return new KOTHBossbarManager(this);
    }

    public void setupMap (CBCMap generalMap) {

        super.setupMap(generalMap);
        this.map = (KOTHMap) generalMap;

        // Get hill
        hill = map.getHill(getGameManager());

    }

    public void startGame () {

        map.fillBlocksAtEnd();

        // Let players move again
        PlayerMoveEvent.getHandlerList().unregister(playerNoMoveListener);
        playerNoMoveListener = null;

        // Initialise all players
        for (KOTHTeam team : teams) {
            for (KOTHPlayer player : team.getKothPlayers()) {
                if (!player.isOnline()) continue;
                player.playerRefresh();
            }
        }

        // Enable hill
        hillEnabled = true;

        // Start game length timer
        new IncrementGameTimeTask(this).runTaskTimer(CBCPlugin.getPlugin(), 20, 20);

    }

    public void capturingByTeam (KOTHTeam team) {

        // Increase capture progress
        pointCaptureProgress += pointCaptureRate / 5;

        if (pointCaptureTeam == null) {
            pointCaptureTeam = team;
        }

        if (pointCaptureProgress >= pointCaptureMax) {
            // Give control of point to team
            pointCaptureProgress = pointCaptureMax;
        }

        // Check if capture progress has reached max
        if (pointCaptureProgress == pointCaptureMax && pointControlTeam == null) {
            // Give control of point to team
            capturedPoint(team);
        }

    }

    public void capturedPoint (KOTHTeam team) {

        pointControlTeam = team;

        // Play sound to all players
        getGameManager().playSound(hill.getCenter().clone().add(0, 1.5, 0), Sound.BLOCK_BEACON_ACTIVATE, 20, 2);

        // Handle statistics
        team.hillCaptured();

        // Play title and sound to all players on team
        Title title =  Title.title(
                Component.text("Point captured!").color(team.getColor()).decorate(TextDecoration.BOLD),
                Component.space(),
                Title.Times.times(Duration.ofMillis(0), Duration.ofMillis(1000), Duration.ofMillis(500))
        );

        for (CBCPlayer player : team.getPlayers()) {
            if (!player.isOnline()) continue;
            Player playerEntity = player.getPlayer();
            playerEntity.playSound(playerEntity.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 300, 1);
            playerEntity.showTitle(title);
        }

        // Send global message
        getGameManager().sendGlobalMessage(
                Component.text("POINT CONTROL > ").decorate(TextDecoration.BOLD).color(NamedTextColor.WHITE)
                        .append(team.getTeamComponent(false).decoration(TextDecoration.BOLD, TextDecoration.State.FALSE))
                        .append(Component.text(" has taken control of the point!").color(NamedTextColor.WHITE)
                                .decoration(TextDecoration.BOLD, TextDecoration.State.FALSE))
        );

        // Play hill particles
        hill.particlesOnCapture(team.getColor());

        // Start scoring
        scoreTask = new KOTHScoreTask(this);
        scoreTask.runTaskTimer(CBCPlugin.getPlugin(), ticksToScore, ticksToScore);

        // Update sidebar manager
        updateServerSidebar();

    }

    public void uncapturingPoint () {

        if (pointCaptureProgress == 0) return;

        // Decrease capture progress
        pointCaptureProgress -= pointLoseRate / 5;

        if (pointCaptureProgress <= 0) {
            pointCaptureProgress = 0;
            pointCaptureTeam = null;
            // Check if team has control of the point, and if so relinquish control
            if (pointControlTeam != null) {
                uncapturedPoint();
            }
        }

    }

    public void uncapturedPoint () {

        // Play sound to all players
        getGameManager().playSound(hill.getCenter().clone().add(0, 1.5, 0), Sound.BLOCK_BEACON_DEACTIVATE, 10, 2);

        // Send global message
        getGameManager().sendGlobalMessage(
                Component.text("POINT CONTROL > ").decorate(TextDecoration.BOLD).color(NamedTextColor.WHITE)
                        .append(pointControlTeam.getTeamComponent(false).decoration(TextDecoration.BOLD, TextDecoration.State.FALSE))
                        .append(Component.text(" has lost control of the point!").color(NamedTextColor.WHITE)
                                .decoration(TextDecoration.BOLD, TextDecoration.State.FALSE))
        );

        pointControlTeam = null;
        pointCaptureProgress = 0;

        // Stop scoring
        cancelTask(scoreTask);
        scoreTask = null;

        // Update sidebar manager
        updateServerSidebar();

    }

    public void teamScore () {

        if (pointControlTeam == null) return; // Check if the gold is currently held
        pointControlTeam.score();

        if (pointControlTeam.getScore() == 0) {
            // Win game
            // Don't end game yet if there are more teams to win
            if (teamsToWin > 1) {

                // Don't end game yet if the team amount to win has not been met
                teamsWon++;
                if (teamsWon == 1) {
                    originalWinningTeam = pointControlTeam;
                }

                // Say a team's placement
                getGameManager().sendGlobalMessage(
                        Component.newline()
                                .append(Component.text("GAME PLACEMENT > ").decorate(TextDecoration.BOLD).color(NamedTextColor.WHITE))
                                .append(Component.text(pointControlTeam.getTeamName()).decorate(TextDecoration.BOLD).color(pointControlTeam.getColor()))
                                .append(Component.text(" has placed " + StringUtil.getPlacementString(teamsWon) + "!").color(NamedTextColor.WHITE))
                                .append(Component.newline())
                );

                // Play sound
                getGameManager().playGlobalSound(Sound.ENTITY_PLAYER_LEVELUP, 100, 0);

                // Check if team amount to win has been met
                if (teamsWon == teamsToWin) {
                    gameWon(originalWinningTeam);
                    scoreTask.cancel();
                }
                else {
                    // Has not been met yet, so do not end the game
                    pointControlTeam.teamOutOfGame(teamsWon);
                }

                uncapturedPoint();

            }
            else {
                gameWon(pointControlTeam);
                scoreTask.cancel();
            }

        } else if (pointControlTeam.getScore() <= 10) {
            // Play sound
            getGameManager().playGlobalSound(Sound.BLOCK_NOTE_BLOCK_BIT, 100, 2);
        }

        // Update sidebar manager
        updatePlacements();
        updateServerSidebar();
        updateBossbarManager();
    }

    @Override
    public void gameWon (CBCTeam team) {

        super.gameWon(team);

        // Disable hill
        hillEnabled = false;

    }

    public void resetGame() {

        super.resetGame();

        // Cancel tasks
        cancelTask(startGameTimer);
        cancelTask(hillParticlesTask);
        cancelTask(hillControlTask);
        cancelTask(scoreTask);
        scoreTask = null;

    }

    public void updatePlacements () {

        List<KOTHTeam> teamsByScore = getTeamsByScore();

        int placement = 0;
        int currentScore = -1;
        int i = 0;

        for (KOTHTeam team : teamsByScore) {

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

    public List<KOTHTeam> getTeamsByScore() {

        List<KOTHTeam> sortedTeamList = new ArrayList<>(getTeams());
        sortedTeamList.sort(Comparator.comparingInt(KOTHTeam::getScore));

        return sortedTeamList;

    }

    @Override
    public PostGameStats getPostGameStats() {
        return new KOTHPostGameStats(this);
    }

    public List<KOTHTeam> getTeams() {
        return teams;
    }

    public KOTHTeam getPointControlTeam() {
        return pointControlTeam;
    }

    public KOTHTeam getPointCaptureTeam() {
        return pointCaptureTeam;
    }

    public int getPointCaptureProgress() {
        return pointCaptureProgress;
    }

    public int getPointCaptureMax() {
        return pointCaptureMax;
    }

    public boolean isHillEnabled() {
        return hillEnabled;
    }

    public float getCapturingPlayerPercentage() {
        return capturingPlayerPercentage;
    }

}
