package neonique.cbcplugin_new.gamemodes.flagrush;

import neonique.cbcplugin_new.CBCPlugin;
import neonique.cbcplugin_new.enums.CBCGamemode;
import neonique.cbcplugin_new.gamemodes._base.CBCMap;
import neonique.cbcplugin_new.gamemodes._base.CBCTeam;
import neonique.cbcplugin_new.gamemodes._base.GameSidebarManager;
import neonique.cbcplugin_new.gamemodes.ctf.*;
import neonique.cbcplugin_new.gamemodes._base.PostGameStats;
import neonique.cbcplugin_new.listeners.gamemodes.PlayerNoMove;
import neonique.cbcplugin_new.lobby.LobbyPlayer;
import neonique.cbcplugin_new.lobby.LobbyTeam;
import neonique.cbcplugin_new.managers.GameBossBarManager;
import neonique.cbcplugin_new.managers.GameManager;
import neonique.cbcplugin_new.managers.CombatManager;
import neonique.cbcplugin_new.playerclasses.CBCPlayer;
import neonique.cbcplugin_new.tasks.gamemodetasks.ctf.CTFGlowManagerTask;
import neonique.cbcplugin_new.tasks.gamemodetasks.ctf.CTFPlayerTrackingTask;
import neonique.cbcplugin_new.tasks.gamemodetasks.ctf.CTFStartGameTimer;
import neonique.cbcplugin_new.tasks.gamemodetasks.flagrush.FlagRushGameTimerTask;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.util.*;

public class FlagRushGame extends CTFGame {

    // Game related variables
    private int timer;
    private int maxTimer;
    private boolean overtime = false;
    private int overtimeTarget = 0;
    private boolean timerEnabled = true;

    // Game variables set by player
    private int scoreStart = 0;
    private int flagCapturePoints = 100;
    private int flagLostPoints = 50;

    // DISPLAY MANAGERS

    // Extra listeners / tasks
    FlagRushGameTimerTask gameTimerTask;

    public FlagRushGame(GameManager gameManager, CombatManager combatManager) {
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
        setGamemode(CBCGamemode.FLAGRUSH);
        createHeaderTitle();

        // Enable weapons
        combatManager.activateWeapons();
        gameManager.resetPlayerList();

        // Setup gamemode game variables
        this.scoreStart = intVars.getOrDefault("scoreStart", 0);
        this.flagCapturePoints = intVars.getOrDefault("flagCapturePoints", 100);
        this.flagLostPoints = intVars.getOrDefault("flagLostPoints", 50);
        this.maxTimer = intVars.getOrDefault("gameTimer", 1200);
        this.timer = maxTimer;

        // Setup game commands
        setGameCommands(new FlagRushGameCommands(gameManager, combatManager, this));

        // Setup teams/players
        createTeams(teams);
        teleportSpectators();

        // Create random list of integers for randomizing team bases
        List<Integer> randomIntegerList = new ArrayList<>();
        if (randomBases) {
            for (int i = 0; i < randomFlagLocations.size(); i++) randomIntegerList.add(i);
            Collections.shuffle(randomIntegerList);
        }

        int teamNum = 0;
        map.fillBlocksAtStart();
        for (CTFTeam team : this.teams) {

            // Set team's base variables
            if (randomBases) {
                int index = randomIntegerList.get(teamNum);
                team.setBaseVariables(randomFlagLocations.get(index), randomBaseSpawns.get(index));
            }
            else {
                team.setBaseVariables(nonRandomFlagLocations.get(team.getTeamId()), nonRandomBaseSpawns.get(team.getTeamId()));
            }

            // Teleport all players to their spawn
            for (CBCPlayer player : team.getPlayers()) {
                FlagRushPlayer ctfPlayer = (FlagRushPlayer) player;
                ctfPlayer.resetPlayer();
                ctfPlayer.teleportPlayerToSpawn();
            }

            teamNum++;
        }

        // Teleport players who are spectating
        teleportSpectators();

        // Setup sidebar
        createUIManagers();

        glowManager = new CTFGlowManager(world, this);
        glowManager.activate();

        updateGlowTask = new CTFGlowManagerTask(this);
        updateGlowTask.runTaskTimer(CBCPlugin.getPlugin(), 0, 15);

        playerTrackingTask = new CTFPlayerTrackingTask(this);
        playerTrackingTask.runTaskTimer(CBCPlugin.getPlugin(), 0, 6);

        // Start countdown for the game
        startGameTimer = new CTFStartGameTimer(gameManager, this, 11);
        startGameTimer.runTaskTimer(CBCPlugin.getPlugin(), 0, 20);

        if (!map.isCanMoveAtGameStart()) {
            playerNoMoveListener = new PlayerNoMove(gameManager);
            CBCPlugin.getPlugin().getServer().getPluginManager().registerEvents(playerNoMoveListener, CBCPlugin.getPlugin());
        }
    }

    public CBCTeam createGamemodeTeam (LobbyTeam team, int teamNum) {
        FlagRushTeam createdTeam = new FlagRushTeam(this, team.getTeamId(),
                Integer.toString(teamNum), team.getTeamName(), team.getColor(),
                team.getPrefix(), team.getItem(), team.getGlassHead()
        );
        teams.add(createdTeam);
        return createdTeam;
    }

    public CBCPlayer createGamemodePlayer (Player playerEntity, int playerId) {
        return new FlagRushPlayer(this, getGameManager(), getCombatManager(), playerEntity, playerId);
    }

    public GameSidebarManager createSidebarManager() {
        return new FlagRushSidebarManager(getGameManager(), getCombatManager(), this);
    }

    @Override
    public GameBossBarManager createBossbarManager() {
        return new FlagRushBossbarManager(this);
    }

    @Override
    public void startGame () {
        super.startGame();

        // Start game timer
        gameTimerTask = new FlagRushGameTimerTask(this);
        gameTimerTask.runTaskTimer(CBCPlugin.getPlugin(), 20, 20);
    }

    public List<FlagRushTeam> getTeamsByScore() {

        List<FlagRushTeam> sortedTeamList = new ArrayList<>(getFlagRushTeams());
        sortedTeamList.sort(Comparator.comparingInt(FlagRushTeam::getScore).reversed());

        return sortedTeamList;

    }

    public void updatePlacements () {

        List<FlagRushTeam> teamsByScore = getTeamsByScore();

        int placement = 0;
        int currentScore = 100000;
        int i = 0;

        for (FlagRushTeam team : teamsByScore) {

            boolean tied = false;
            if (team.getScore() < currentScore) {
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

        // Update board of server
        updateServerSidebar();
    }

    public List<FlagRushTeam> getLeadingTeams() {

        // Figure out which team is currently leading
        List<FlagRushTeam> teamsLeading = getTeamsByScore();
        int highestKills = 0;
        for (CTFTeam team : teams) {
            FlagRushTeam frTeam = (FlagRushTeam) team;
            if (frTeam.getScore() > highestKills) {
                teamsLeading.clear();
                teamsLeading.add(frTeam);
                highestKills = frTeam.getScore();
            } else if (frTeam.getScore() == highestKills) {
                teamsLeading.add(frTeam);
            }
        }

        return teamsLeading;

    }

    public void timerMessage(Component message, float pitch) {

        getGameManager().sendGlobalMessage(message);
        getGameManager().playGlobalSound(Sound.BLOCK_NOTE_BLOCK_BIT, 100, pitch);

    }

    public void decrementTimer() {

        if (!timerEnabled) {
            return;
        }

        timer--;

        if (timer == 300) {
            timerMessage(Component.text("5 minutes remain!").color(NamedTextColor.YELLOW).decorate(TextDecoration.BOLD), 1);
        } else if (timer == 180) {
            timerMessage(Component.text("3 minutes remain!").color(NamedTextColor.YELLOW).decorate(TextDecoration.BOLD), 1);
        } else if (timer == 60) {
            timerMessage(Component.text("1 minute remains!").color(NamedTextColor.RED).decorate(TextDecoration.BOLD), 1);
        } else if (timer == 30) {
            timerMessage(Component.text("30 seconds remain!").color(NamedTextColor.RED).decorate(TextDecoration.BOLD), 1);
        } else if (timer == 10) {
            timerMessage(Component.text("10 SECONDS REMAIN!").color(NamedTextColor.RED).decorate(TextDecoration.BOLD), 2);
        } else if (timer <= 5 && timer > 1) {
            timerMessage(Component.text(timer + " SECONDS REMAIN!").color(NamedTextColor.RED).decorate(TextDecoration.BOLD), 2);
        } else if (timer == 1) {
            timerMessage(Component.text("1 SECOND REMAINS!").color(NamedTextColor.RED).decorate(TextDecoration.BOLD), 2);
        } else if (timer == 0) {
            // End game
            timerEnabled = false; // Disable timer
            // Check for winner of game
            checkWinner();
        }
    }

    public void checkWinner() {
        // Check for the winning team
        List<FlagRushTeam> leadingTeams = getLeadingTeams();

        // If the amount of teams in the lead is 1, then end the game
        if (leadingTeams.size() == 1) {
            FlagRushTeam winningTeam = leadingTeams.get(0);
            gameWon(winningTeam);
        } else {
            // Go to overtime -
            startOvertime();
        }
    }

    public void checkWinnerOvertime(FlagRushTeam team) {
        if (team.getScore() >= overtimeTarget) {
            gameWon(team);
        }
    }

    public void startOvertime() {

        overtime = true;

        // Find the highest kill amount, set overtime threshold to that plus two
        int highestScore = 0;
        for (FlagRushTeam team : getTeamsByScore()) {
            if (team.getScore() > highestScore) {
                highestScore = team.getScore();
            }
        }

        overtimeTarget = highestScore + flagCapturePoints;

        // Play sound
        getGameManager().playGlobalSound(Sound.ENTITY_WITHER_DEATH, 100, 1);

        // Play title
        Title overtimeTitle = Title.title(
                Component.text("OVERTIME").color(NamedTextColor.RED).decorate(TextDecoration.BOLD),
                Component.text("First team to reach ").color(NamedTextColor.WHITE).append(
                        Component.text(overtimeTarget).color(NamedTextColor.RED).decorate(TextDecoration.BOLD)
                ).append(
                        Component.text(" wins!").color(NamedTextColor.WHITE)
                ),
                Title.Times.times(Duration.ofMillis(0), Duration.ofMillis(1250), Duration.ofMillis(500))
        );

        getGameManager().sendGlobalTitle(overtimeTitle);

        // Send message of game win
        getGameManager().sendGlobalMessage(
                Component.newline()
                        .append(Component.text("OVERTIME > ").decorate(TextDecoration.BOLD).color(NamedTextColor.WHITE))
                        .append(Component.text("You no longer lose points when your flag is captured. First team to reach ").color(NamedTextColor.WHITE))
                        .append(Component.text(overtimeTarget).color(NamedTextColor.RED).decorate(TextDecoration.BOLD))
                        .append(Component.text(" wins the game!").color(NamedTextColor.WHITE))
                        .append(Component.newline())
        );
    }

    @Override
    public void resetGame() {
        super.resetGame();
        cancelTask(gameTimerTask);
    }

    @Override
    public PostGameStats getPostGameStats() {
        return new FlagRushPostGameStats(this);
    }

    public int getScoreStart() {
        return scoreStart;
    }

    public int getTimer() {
        return timer;
    }

    public int getMaxTimer() {
        return maxTimer;
    }

    public String timerToText() {
        return String.format("%d:%02d", timer / 60, timer % 60);
    }

    public Set<FlagRushTeam> getFlagRushTeams() {
        Set<FlagRushTeam> teams = new HashSet<>();
        for (CTFTeam team : this.teams) {
            teams.add((FlagRushTeam) team);
        }
        return teams;
    }

    public boolean isOvertime() {
        return overtime;
    }

    public int getOvertimeTarget() {
        return overtimeTarget;
    }

    public void setTimer(int newTime) {
        timer = newTime;
    }

    public boolean isTimerEnabled() {
        return timerEnabled;
    }

    public int getFlagCapturePoints() {
        return flagCapturePoints;
    }

    public int getFlagLostPoints() {
        return flagLostPoints;
    }
}
