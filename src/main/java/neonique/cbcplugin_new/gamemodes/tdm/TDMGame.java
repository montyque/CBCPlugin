package neonique.cbcplugin_new.gamemodes.tdm;

import neonique.cbcplugin_new.CBCPlugin;
import neonique.cbcplugin_new.core.TeamGame;
import neonique.cbcplugin_new.gamemodes.CBCGamemode;
import neonique.cbcplugin_new.gamemodes.GameContext;
import neonique.cbcplugin_new.gamemodes._base.*;
import neonique.cbcplugin_new.listeners.gamemodes.PlayerNoMove;
import neonique.cbcplugin_new.lobby.LobbyTeam;
import neonique.cbcplugin_new.managers.GameBossBarManager;
import neonique.cbcplugin_new.managers.GameManager;
import neonique.cbcplugin_new.combat.CombatManager;
import neonique.cbcplugin_new.tasks.gamemodetasks.IncrementGameTimeTask;
import neonique.cbcplugin_new.gamemodes.tdm.tasks.TDMGameTimerTask;
import neonique.cbcplugin_new.gamemodes.tdm.tasks.TDMStartGameTimer;
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

public class TDMGame extends TeamGame<TDMPlayer, TDMMap, TDMTeam> {

    // Game related variables
    private boolean gameByTimer; // If this is set to false, this means the game is by kills
    private boolean timerEnabled = false;
    private int overtimeThreshold = 2;
    private boolean overtime = false;
    private int overtimeKillsToWin;
    private int maxTimer;
    private int timer;
    private boolean playersGlow = true;

    // Map related variables
    protected boolean randomSpawnsEnabled;
    protected List<TDMSpawn> spawns;
    protected HashMap<String, Set<Location>> teamSpawns;

    // Event listeners and tasks
    protected TDMStartGameTimer startGameTimer;
    protected TDMGameTimerTask tdmGameTimerTask;
    protected PlayerNoMove playerNoMoveListener;

    public TDMGame(GameManager gameManager) {
        super(gameManager);
    }

    @Override
    public CBCGamemode getGamemode () {
        return CBCGamemode.TDM;
    }

    @Override
    public TDMTeam createGamemodeTeam (LobbyTeam team, int teamNum) {
        return new TDMTeam(this, team.id(),
                Integer.toString(teamNum), team.name(), team.getColor(),
                team.prefix(), team.getIconItem(), team.getGlassHead()
        );
    }

    @Override
    public TDMPlayer createPlayer(Player playerEntity) {
        return new TDMPlayer(this, getGameManager(), getCombatManager(), playerEntity);
    }

    @Override
    public GameSidebarManager createSidebarManager() {
        return new TDMSidebarManager(getGameManager(), getCombatManager(), this);
    }

    @Override
    public GameBossBarManager createBossbarManager() {
        return new TDMBossbarManager(this);
    }

    @Override
    public void setupGame (GameContext ctx) {

        final GameManager gameManager = getGameManager();
        final CombatManager combatManager = getCombatManager();

        // Setup map
        setupMap((TDMMap) ctx.getMap());

        // Setup default game variables
        setupDefaultGameVars(ctx.getBoolVars(), ctx.getIntVars(), ctx.getStringVars());

        // Set gamemode information
        createHeaderTitle();

        // Enable weapons
        combatManager.activateWeapons();

        // Setup gamemode specific game variables
        gameByTimer = ctx.getBoolVars().getOrDefault("gameByTimer", true);
        if (gameByTimer) {
            maxTimer = ctx.getIntVars().getOrDefault("gameTimer", 600);
            overtimeThreshold = ctx.getIntVars().getOrDefault("overtimeThreshold", 2);
            timer = maxTimer;
        }

        playersGlow = ctx.getBoolVars().getOrDefault("playersGlow", true);

        // Setup game commands
        setGameCommands(new TDMGameCommands(this));

        createTeams(ctx.getTeams());
        teleportSpectators();

        // Teleport all players
        for (TDMTeam team : getTeams()) {

            team.setSpawns(teamSpawns.get(team.id()));
            List<Location> teamSpawnList = new ArrayList<>(team.getSpawns());
            Collections.shuffle(teamSpawnList);

            // Spawn each player in the team in different spawnpoints
            int playerinc = 0;
            for (TDMPlayer player : team.getPlayers()) {
                player.resetPlayer();
                player.teleportPlayerToSpawn(teamSpawnList.get(playerinc % teamSpawnList.size()));
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

    public void setupMap (TDMMap map) {

        super.setupMap(map);

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

        getMap().fillBlocksAtEnd();

        PlayerMoveEvent.getHandlerList().unregister(playerNoMoveListener);
        playerNoMoveListener = null;

        // Initialise all players
        for (TDMTeam team : getTeams()) {
            for (TDMPlayer player : team.getPlayers()) {
                if (player.isOnline()) {
                    player.playerRefresh();
                }
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

        updateBossbarManager();

    }

    public List<TDMTeam> getLeadingTeams() {
        Optional<Integer> maxKills = getTeams().stream().map(TDMTeam::getKills).max(Comparator.naturalOrder());
        return maxKills.map(max -> getTeams().stream().filter(t -> t.getKills() == max).toList()).orElseGet(ArrayList::new);
    }

    public void checkWinner() {
        // Check for the winning team
        List<TDMTeam> leadingTeams = getLeadingTeams();

        // If single team leads, end game, otherwise go to overtime
        if (leadingTeams.size() == 1) {
            TDMTeam winningTeam = leadingTeams.get(0);
            gameWon(winningTeam);
        } else {
            startOvertime();
        }

    }

    public void startOvertime() {

        overtime = true;

        // Find the highest kill amount, set winning threshold to that plus
        int highestKills = 0;
        for (TDMTeam team : getTeams()) {
            if (team.getKills() > highestKills) {
                highestKills = team.getKills();
            }
        }

        overtimeKillsToWin = highestKills + overtimeThreshold;

        // Reset all players
        for (TDMTeam team : getTeams()) {

            // Teleport all players back to team spawns
            List<Location> teamSpawnList = new ArrayList<>(team.getSpawns());
            Collections.shuffle(teamSpawnList);

            int playerInc = 0;
            for (TDMPlayer player : team.getOnlinePlayers()) {
                if (player.isRespawning()) {
                    getCombatManager().playerRespawn(player);
                } else {
                    player.playerRefresh();
                }
                player.teleportPlayerToSpawn(teamSpawnList.get(playerInc % teamSpawnList.size()));
                playerInc++;
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

    public void gameWon (TDMTeam team) {

        super.gameWon(team);

        // Add bonus points for winning
        for (TDMPlayer player : team.getPlayers()) {
            player.addGamePoints(40);
        }

    }

    public void resetGame() {

        super.resetGame();

        cancelTask(startGameTimer);
        cancelTask(tdmGameTimerTask);

    }

    public List<TDMTeam> getTeamsByKills() {

        List<TDMTeam> sortedTeamList = new ArrayList<>(getTeams());
        sortedTeamList.sort(Comparator.comparingInt(TDMTeam::getKills));
        Collections.reverse(sortedTeamList);

        return sortedTeamList;

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
