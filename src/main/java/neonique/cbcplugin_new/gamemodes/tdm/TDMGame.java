package neonique.cbcplugin_new.gamemodes.tdm;

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
import neonique.cbcplugin_new.tasks.gamemodetasks.tdm.TDMGameTimerTask;
import neonique.cbcplugin_new.tasks.gamemodetasks.tdm.TDMStartGameTimer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerMoveEvent;

import java.time.Duration;
import java.util.*;

public class TDMGame extends TeamGame {

    // Create list of teams
    protected final List<TDMTeam> teams = new ArrayList<>();

    // Game related variables
    protected boolean gameByTimer; // If this is set to false, this means the game is by kills
    protected boolean timerEnabled = false;
    protected int overtimeThreshold = 2;
    protected boolean overtime = false;
    protected int overtimeKillsToWin;
    protected int maxTimer;
    protected int timer;
    protected int killsToWin;
    protected boolean playersGlow = true;

    // Map related variables
    private TDMMap map;
    protected boolean randomSpawnsEnabled;
    protected List<TDMSpawn> spawns;
    protected HashMap<String, Set<Location>> teamSpawns;

    // Event listeners and tasks
    protected TDMStartGameTimer startGameTimer;
    protected TDMGameTimerTask tdmGameTimerTask;
    protected PlayerNoMove playerNoMoveListener;

    public TDMGame(GameManager gameManager, CombatManager combatManager) {
        super(gameManager, combatManager);
    }

    @Override
    public void setupGame(CBCMap mapChosen, LinkedHashMap<String, LobbyTeam> teams, Collection<LobbyPlayer> players,
                          HashMap<String, Boolean> boolVars, HashMap<String, Integer> intVars, HashMap<String, String> stringVars) {

        final GameManager gameManager = getGameManager();
        final CombatManager combatManager = getCombatManager();

        // Setup map
        setupMap(mapChosen);

        // Setup default game variables
        setupDefaultGameVars(boolVars, intVars, stringVars);

        // Set gamemode information
        setGamemode(CBCGamemode.TDM);
        createHeaderTitle();

        // Enable weapons
        combatManager.activateWeapons();
        gameManager.resetPlayerList();

        // Setup gamemode specific game variables
        this.gameByTimer = boolVars.getOrDefault("gameByTimer", true);
        if (this.gameByTimer) {
            this.maxTimer = intVars.getOrDefault("gameTimer", 600);
            this.overtimeThreshold = intVars.getOrDefault("overtimeThreshold", 2);
            this.timer = maxTimer;
        } else {
            this.killsToWin = intVars.getOrDefault("killsToWin", 50);
        }

        this.playersGlow = boolVars.getOrDefault("playersGlow", true);

        // Setup game commands
        setGameCommands(new TDMGameCommands(gameManager, combatManager, this));

        createTeams(teams);
        teleportSpectators();

        // Teleport all players
        for (TDMTeam team : this.teams) {

            team.setSpawns(teamSpawns.get(team.getTeamId()));
            List<Location> teamSpawnList = new ArrayList<>(team.getSpawns());
            Collections.shuffle(teamSpawnList);

            // Spawn each player in the team in different spawnpoints
            int playerinc = 0;
            for (CBCPlayer player : team.getPlayers()) {
                TDMPlayer tdmPlayer = (TDMPlayer) player;
                tdmPlayer.resetPlayer();
                tdmPlayer.teleportPlayerToSpawn(teamSpawnList.get(playerinc % teamSpawnList.size()));
                playerinc++;
            }

            // Update player placements
            team.updateWithinTeamPlacements();

        }

        // Prevent movement from players - they should still be able to turn their heads
        playerNoMoveListener = new PlayerNoMove(gameManager);
        CBCPlugin.getPlugin().getServer().getPluginManager().registerEvents(playerNoMoveListener, CBCPlugin.getPlugin());

        // Update placements
        updatePlacements();

        // Create sidebar and bossbar managers
        createUIManagers();

        // Start countdown for the game
        startGameTimer = new TDMStartGameTimer(gameManager, this, 11);
        startGameTimer.runTaskTimer(CBCPlugin.getPlugin(), 0, 20);

    }

    public CBCTeam createGamemodeTeam (LobbyTeam team, int teamNum) {
        TDMTeam createdTeam = new TDMTeam(this, team.getTeamId(),
                Integer.toString(teamNum), team.getTeamName(), team.getColor(),
                team.getPrefix(), team.getItem(), team.getGlassHead()
        );
        teams.add(createdTeam);
        return createdTeam;
    }

    public CBCPlayer createGamemodePlayer (Player playerEntity, int playerId) {
        return new TDMPlayer(this, getGameManager(), getCombatManager(), playerEntity, playerId);
    }

    public GameSidebarManager createSidebarManager() {
        return new TDMSidebarManager(getGameManager(), getCombatManager(), this);
    }

    @Override
    public GameBossBarManager createBossbarManager() {
        return new TDMBossbarManager(this);
    }

    public void setupMap (CBCMap generalMap) {

        super.setupMap(generalMap);
        map = (TDMMap) generalMap;

        // Get team spawns
        teamSpawns = map.getTeamSpawns();

        // Get random spawns
        if (map.isRandomSpawnsEnabled()) {
            randomSpawnsEnabled = true;
            spawns = map.getRandomSpawns();
        } else {
            randomSpawnsEnabled = false;
        }
    }

    public void startGame () {

        map.fillBlocksAtEnd();

        PlayerMoveEvent.getHandlerList().unregister(playerNoMoveListener);
        playerNoMoveListener = null;

        // Initialise all players
        for (TDMTeam team : teams) {
            for (CBCPlayer player : team.getOnlinePlayers()) {
                TDMPlayer tdmPlayer = (TDMPlayer) player;
                tdmPlayer.playerRefresh();
            }
        }

        // Start game length timer
        new IncrementGameTimeTask(this).runTaskTimer(CBCPlugin.getPlugin(), 20, 20);

        // Start game countdown timer if the game is by timer
        if (gameByTimer) {
            timerEnabled = true;

            tdmGameTimerTask = new TDMGameTimerTask(this);
            tdmGameTimerTask.runTaskTimer(CBCPlugin.getPlugin(), 20, 20);
        }
    }

    public void updatePlacements () {

        List<TDMTeam> teamsByKills = getTeamsByKills();

        int placement = 0;
        int currentkills = 100000;
        int i = 0;

        for (TDMTeam team : teamsByKills) {

            boolean tied = false;
            if (team.getKills() < currentkills) {
                placement = i + 1;
                currentkills = team.getKills();
                if (teamsByKills.size() - 1 != i) {
                    if (teamsByKills.get(i + 1).getKills() == currentkills) {
                        tied = true;
                    }
                }
            }
            else if (currentkills == team.getKills()) {
                tied = true;
            }

            team.setPlacement(placement, tied);

            i++;

        }
    }

    @Override
    public PostGameStats getPostGameStats() {
        return new TDMPostGameStats(this);
    }

    @Override
    public void playerJoinServer(Player player) {
        super.playerJoinServer(player);
        // Handle sidebar
        getSidebarManager().addPlayerSidebar(player);
    }

    @Override
    public void playerLeaveServer(Player player) {
        super.playerLeaveServer(player);
        // Handle sidebar
        getSidebarManager().removePlayerSidebar(player);
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

        if (timer == 180) {
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

    public List<TDMTeam> getLeadingTeams() {

        // Figure out which team is currently leading
        List<TDMTeam> teamsLeading = new ArrayList<>();
        int highestKills = 0;
        for (TDMTeam team : teams) {

            if (team.getKills() > highestKills) {
                teamsLeading.clear();
                teamsLeading.add(team);
                highestKills = team.getKills();
            } else if (team.getKills() == highestKills) {
                teamsLeading.add(team);
            }
        }

        return teamsLeading;

    }

    public void checkWinner() {
        // Check for the winning team
        List<TDMTeam> leadingTeams = getLeadingTeams();

        // If the amount of teams in the lead is 1, then end the game
        if (leadingTeams.size() == 1) {
            TDMTeam winningTeam = leadingTeams.get(0);
            gameWon(winningTeam);
        } else {
            // Go to overtime - last team to get a kill wins
            startOvertime();
        }
    }

    public void startOvertime() {

        overtime = true;

        // Find the highest kill amount, set overtime threshold to that plus two
        int highestKills = 0;
        for (TDMTeam team : teams) {
            if (team.getKills() > highestKills) {
                highestKills = team.getKills();
            }
        }

        overtimeKillsToWin = highestKills + overtimeThreshold;

        // Reset all players
        for (TDMTeam team : teams) {
            for (CBCPlayer player : team.getOnlinePlayers()) {
                TDMPlayer tdmPlayer = (TDMPlayer) player;
                tdmPlayer.resetPlayer();
                if (tdmPlayer.isRespawning()) {
                    getCombatManager().playerRespawn(player);
                }
                else {
                    tdmPlayer.playerRefresh();
                }
            }

            // Teleport all players back to team spawns
            List<Location> teamSpawnList = new ArrayList<>(team.getSpawns());
            Collections.shuffle(teamSpawnList);

            int playerinc = 0; // Increments every time we teleport a player
            for (CBCPlayer player : team.getPlayers()) {
                TDMPlayer tdmPlayer = (TDMPlayer) player;
                // Spawns players in different spawnpoints - reason playerinc is used
                tdmPlayer.teleportPlayerToSpawn(teamSpawnList.get(playerinc % teamSpawnList.size()));
                playerinc++;
            }
        }

        // Play sound
        getGameManager().playGlobalSound(Sound.ENTITY_WITHER_DEATH, 100, 1);

        // Play title
        Title overtimeTitle = Title.title(
                Component.text("OVERTIME").color(NamedTextColor.RED).decorate(TextDecoration.BOLD),
                Component.text("First team to reach ").color(NamedTextColor.WHITE).append(
                        Component.text(overtimeKillsToWin).color(NamedTextColor.RED).decorate(TextDecoration.BOLD)
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
                        .append(Component.text("First team to reach ").color(NamedTextColor.WHITE))
                        .append(Component.text(overtimeKillsToWin).color(NamedTextColor.RED).decorate(TextDecoration.BOLD))
                        .append(Component.text(" wins the game!").color(NamedTextColor.WHITE))
                        .append(Component.newline())
        );

        // Update bossbar
        updateBossbarManager();
    }

    public void killDuringOvertime (TDMTeam team) {
        if (getWinner() != null) return;
        if (team.getKills() >= overtimeKillsToWin) {
            gameWon(team);
        }
    }

    public void gameWon (CBCTeam team) {

        super.gameWon(team);

        // Add bonus points for winning
        for (CBCPlayer player : team.getPlayers()) {
            player.addGamePoints(40);
        }

        // Update bossbar
        updateBossbarManager();
        updateServerSidebar();
    }

    public void resetGame() {

        super.resetGame();

        cancelTask(startGameTimer);
        cancelTask(tdmGameTimerTask);

    }

    public List<TDMTeam> getTeamsByKills() {

        List<TDMTeam> sortedTeamList = new ArrayList<>(teams);
        sortedTeamList.sort(Comparator.comparingInt(TDMTeam::getKills));
        Collections.reverse(sortedTeamList);

        return sortedTeamList;

    }

    public List<TDMPlayer> getTDMPlayers () {
        // Get all players as TDMPlayer objects
        List<TDMPlayer> playersList = new ArrayList<>();
        for (CBCPlayer player : getPlayers().values()) {
            if (player instanceof TDMPlayer) {
                playersList.add((TDMPlayer) player);
            }
        }
        return playersList;
    }

    public String timerToText() {
        return String.format("%d:%02d", timer / 60, timer % 60);
    }

    public int getTimer() {
        return timer;
    }

    public int getMaxTimer() {
        return maxTimer;
    }

    public boolean isGameByTimer() {
        return gameByTimer;
    }

    public List<TDMTeam> getTeams() {
        return teams;
    }

    public List<TDMSpawn> getRandomSpawns() {
        return spawns;
    }

    public boolean isRandomSpawns() {
        return randomSpawnsEnabled;
    }

    public boolean isOvertime() {
        return overtime;
    }

    public int getOvertimeKillsToWin() {
        return overtimeKillsToWin;
    }

    public boolean isTimerEnabled() {
        return timerEnabled;
    }

    public void setTimer(int newTime) {
        timer = newTime;
    }

    public boolean isPlayerGlowingEnabled () {
        return playersGlow;
    }

}
