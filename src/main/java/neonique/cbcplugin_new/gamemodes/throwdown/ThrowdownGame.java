package neonique.cbcplugin_new.gamemodes.throwdown;

import neonique.cbcplugin_new.CBCPlugin;
import neonique.cbcplugin_new.enums.CBCGamemode;
import neonique.cbcplugin_new.gamemodes._base.*;
import neonique.cbcplugin_new.gameobjects.DeathBorder;
import neonique.cbcplugin_new.gameobjects.FFASpawnpoint;
import neonique.cbcplugin_new.listeners.gamemodes.PlayerNoMove;
import neonique.cbcplugin_new.lobby.LobbyPlayer;
import neonique.cbcplugin_new.lobby.LobbyTeam;
import neonique.cbcplugin_new.managers.GameBossBarManager;
import neonique.cbcplugin_new.managers.GameManager;
import neonique.cbcplugin_new.managers.CombatManager;
import neonique.cbcplugin_new.playerclasses.CBCPlayer;
import neonique.cbcplugin_new.tasks.gamemodetasks.IncrementGameTimeTask;
import neonique.cbcplugin_new.tasks.gamemodetasks.throwdown.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import org.bukkit.Location;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;
import org.bukkit.scoreboard.Team;

import java.time.Duration;
import java.util.*;

public class ThrowdownGame extends FFAGame {

    // Game related variables
    private int roundsToWin = 3;

    // Map related variables
    private ThrowdownMap map;
    private List<FFASpawnpoint> spawns;

    // Teams - for display only
    private Team aliveTeam;
    private Team elimTeam;

    // Sudden death CONSTANTS
    private boolean suddenDeathEnabled;
    private int suddenDeathMaxTimer;
    private boolean suddenDeathBorderEnabled;

    // Set up round related variables
    private boolean roundInPlay = false;
    private int roundNumber = 0;
    private ThrowdownPlayer roundWinner = null;
    private boolean roundStartCountdown = false;

    // Sudden death related variables
    private boolean suddenDeath = false;
    private int suddenDeathTimer = 0;
    private DeathBorder suddenDeathBorder;

    // Statistical variables
    private int gameInRoundTime = 0;
    private int suddenDeathRounds = 0;

    // Event listeners and tasks
    private ThrowdownSDTimer sdTimerTask;

    private PlayerNoMove noMoveListener;

    public ThrowdownGame(GameManager gameManager, CombatManager combatManager) {
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
        setGamemode(CBCGamemode.THROWDOWN);
        createHeaderTitle();

        // Enable weapons
        combatManager.activateWeapons();
        gameManager.resetPlayerList();

        // Setup gamemode game variables
        this.roundsToWin = intVars.getOrDefault("pointsToWin", 3);
        this.suddenDeathEnabled = boolVars.getOrDefault("suddenDeathEnabled", true);

        // setGameCommands(new ShowdownGameCommands(gameManager, weaponManager, this));
        // Create players
        createPlayers(players);
        teleportSpectators();

        // Create teams
        ScoreboardManager scoreboardManager = CBCPlugin.getPlugin().getServer().getScoreboardManager();
        // Team scoreboard object
        Scoreboard scoreboard = scoreboardManager.getMainScoreboard();

        // Create safe team
        aliveTeam = scoreboard.registerNewTeam("01alive");
        aliveTeam.setCanSeeFriendlyInvisibles(false);
        aliveTeam.setAllowFriendlyFire(true); // Do not allow friendly fire
        aliveTeam.color(NamedTextColor.GREEN); // Set team color

        // Create danger team
        elimTeam = scoreboard.registerNewTeam("02eliminated");
        elimTeam.setCanSeeFriendlyInvisibles(false);
        elimTeam.setAllowFriendlyFire(true); // Do not allow friendly fire
        elimTeam.color(NamedTextColor.RED); // Set team color

        if (gameManager.getCbcScoreboardManager().isActive()) {
            gameManager.getCbcScoreboardManager().registerTeamForAllClients(aliveTeam);
            gameManager.getCbcScoreboardManager().registerTeamForAllClients(elimTeam);
        }

        // Setup tasks
        new ThrowdownPlayerCounts(gameManager, this).runTaskTimer(CBCPlugin.getPlugin(), 0, 4);

        // Setup listeners
        noMoveListener = new PlayerNoMove(gameManager);

        // Create sidebar manager and bossbar manager
        createUIManagers();

        // Start round 1
        setupRound();
    }

    public CBCPlayer createGamemodePlayer (Player playerEntity, int playerId) {
        return new ThrowdownPlayer(this, getGameManager(), getCombatManager(), playerEntity, playerId);
    }

    public GameSidebarManager createSidebarManager() {
        return new ThrowdownSidebarManager(getGameManager(), getCombatManager(), this);
    }

    @Override
    public GameBossBarManager createBossbarManager() {
        return new ThrowdownBossbarManager(this);
    }

    public void setupMap (CBCMap generalMap) {

        super.setupMap(generalMap);
        map = (ThrowdownMap) generalMap;

        // Variables that handle player movement at start of game
        if (map.getOverrideSpawns().isEmpty()) {
            spawns = map.getSpawns();
        } else {
            spawns = map.getOverrideSpawns();
        }

        // Setup sudden death
        if (map.isSuddenDeathEnabled()) {
            suddenDeathBorderEnabled = map.isSuddenDeathBorderEnabled();
            suddenDeathMaxTimer = map.getSuddenDeathTimer();
        }
    }

    public void setupRound () {

        roundInPlay = false;
        roundNumber++;

        // Reset players
        for (ThrowdownPlayer player : getThrowdownPlayers()) {
            player.playerSetupRound();
        }

        // Select each player's spawn randomly
        List<Location> spawnOrder = sortSpawns();
        List<ThrowdownPlayer> randomPlayerList = getThrowdownPlayers();
        Collections.shuffle(randomPlayerList);

        int spawnNum = 0;
        for (ThrowdownPlayer player : randomPlayerList) {

            if (!player.isOnline()) continue;
            player.teleportPlayerToSpawn(spawnOrder.get(spawnNum), map.getMapCentre());
            getGameManager().getCbcScoreboardManager().addTeamEntry(player.getName(), getAliveTeam());
            spawnNum++;

        }

        // Make sure players cannot move
        CBCPlugin.getPlugin().getServer().getPluginManager().registerEvents(noMoveListener, CBCPlugin.getPlugin());

        // Reset sudden death variables
        suddenDeath = false;
        suddenDeathBorder = null;
        suddenDeathTimer = suddenDeathMaxTimer;

        // Enable heal pads
        getCombatManager().enableAllHealPads();
        roundWinner = null;

        // Start countdown for next round
        if (roundNumber == 1) {
            new ThrowdownStartRoundTimer(getGameManager(), this, 11, true).runTaskTimer(CBCPlugin.getPlugin(), 0, 20);
        } else {
            new ThrowdownStartRoundTimer(getGameManager(), this, 6, false).runTaskTimer(CBCPlugin.getPlugin(), 0, 20);
        }
        roundStartCountdown = true;

        checkPlayerCounts();
        updateBossbarManager();
        updateServerSidebar();
    }

    public void startRound() {

        map.fillBlocksAtEnd();

        // Start round for players
        for (ThrowdownPlayer player : getThrowdownPlayers()) {
            player.playerStartRound();
        }

        roundInPlay = true;
        getCombatManager().setVoidKill(true);

        // Release players so they can move
        PlayerMoveEvent.getHandlerList().unregister(noMoveListener);
        roundStartCountdown = false;

        // Start sudden death mechanisms
        if (suddenDeathEnabled) {
            sdTimerTask = new ThrowdownSDTimer(getGameManager(), this);
            sdTimerTask.runTaskTimer(CBCPlugin.getPlugin(), 20, 20);
        }

        if (roundNumber == 1) {
            new IncrementGameTimeTask(this).runTaskTimer(CBCPlugin.getPlugin(), 20, 20);
        }

        checkPlayerCounts();
        updateServerSidebar();
        updateBossbarManager();
    }

    public void checkPlayerCounts() {

        if (isGameOver()) {
            return;
        }

        // Check if round is in play
        if (!roundInPlay) {
            return;
        }

        List<ThrowdownPlayer> playersAlive = new ArrayList<>();
        for (ThrowdownPlayer player : getThrowdownPlayers()) {
            if (!player.isEliminated()) playersAlive.add(player);
        }

        // Check if only one team is alive
        if (roundInPlay) {
            if (playersAlive.size() == 1) {
                // Team has won round
                playerWonRound(playersAlive.get(0));
            }
        }
    }

    public void playerWonRound(ThrowdownPlayer player) {

        // Increment player points
        player.playerWonRound();

        roundWinner = player;

        // Check if team has reached the required amount of points
        if (player.getRoundsWon() >= roundsToWin) {
            playerWonGame(player);
        } else {
            // Display title of round win
            Component titleToDisplay = Component.text("ROUND OVER")
                    .decorate(TextDecoration.BOLD).color(NamedTextColor.GREEN);

            Component subtitleToDisplay = Component.text(player.getName()).color(NamedTextColor.GREEN).append(
                    Component.text(" has won the the round!").color(NamedTextColor.WHITE)).decorate(TextDecoration.BOLD);

            getGameManager().sendGlobalTitle(Title.title(titleToDisplay, subtitleToDisplay,
                    Title.Times.times(Duration.ofMillis(0), Duration.ofMillis(3000), Duration.ofMillis(500))));

            // Send message of round win
            getGameManager().sendGlobalMessage(
                    Component.newline()
                            .append(Component.text("ROUND WIN > ").decorate(TextDecoration.BOLD).color(NamedTextColor.WHITE))
                            .append(Component.text(player.getName()).decorate(TextDecoration.BOLD).color(NamedTextColor.GREEN))
                            .append(Component.text(" has won the round!").color(NamedTextColor.WHITE))
                            .append(Component.newline())
            );

            // Play sound to all players
            getGameManager().playGlobalSound(Sound.ENTITY_PLAYER_LEVELUP, 200, 0);

            // End round and start the next round
            roundOver(true);
        }
    }

    public void roundOver(boolean startNextRound) {

        roundInPlay = false;

        // Set all alive players to immune
        for (CBCPlayer player : getGameManager().getAlivePlayers()) {
            player.setImmune(true);
        }

        // Deactivate border
        if (suddenDeathBorder != null) {
            if (suddenDeathBorder.isActive()) {
                suddenDeathBorder.deactivateBorder();
            }
            suddenDeathBorder = null;
        }

        // Cancel sudden death tasks if they are active
        cancelTask(sdTimerTask);

        // Make the void do nothing
        getCombatManager().setVoidKill(false);

        // Turn off heal pads
        getCombatManager().disableAllHealPads();

        // Start timer for next round
        if (startNextRound) {
            new ThrowdownNextRoundTimer(getGameManager(), this, 10)
                    .runTaskTimer(CBCPlugin.getPlugin(), 20, 20);
        }

        updateServerSidebar();
    }

    @Override
    public void playerWonGame (CBCPlayer winner) {

        super.playerWonGame(winner);

        // End round and do not start the next round
        roundOver(false);

    }

    @Override
    public void resetGame() {

        super.resetGame();

        if (suddenDeathBorder != null) {
            if (suddenDeathBorder.isActive()) {
                suddenDeathBorder.deactivateBorder();
            }
            suddenDeathBorder = null;
        }

        cancelTask(sdTimerTask);

        // Unregister teams
        CBCPlugin.getGameManager().getCbcScoreboardManager().unregisterTeamForAllClients(aliveTeam.getName());
        CBCPlugin.getGameManager().getCbcScoreboardManager().unregisterTeamForAllClients(elimTeam.getName());

        aliveTeam.unregister();
        elimTeam.unregister();

    }


    public List<Location> sortSpawns() {

        List<Location> roundSpawnList = new ArrayList<>(spawns);
        List<Location> spawnOrder = new ArrayList<>();

        // Select the first spawn
        Comparator<Location> byDistanceFromCenter =
                (Location loc1, Location loc2) -> Double.compare(loc1.distanceSquared(map.getMapCentre()), loc2.distanceSquared(map.getMapCentre()));
        roundSpawnList.sort(byDistanceFromCenter);
        Collections.reverse(roundSpawnList);

        spawnOrder.add(roundSpawnList.get(0));
        roundSpawnList.remove(0);

        while (spawnOrder.size() < getPlayers().size()) {
            double minDistanceFromSpawns = 0;
            Location spawnSelected = null;
            for (Location spawn : new ArrayList<>(roundSpawnList)) {
                double spawnMinDistanceFromSpawns = 300000;
                for (Location spawnAlreadySelected : spawnOrder) {
                    if (spawn.distanceSquared(spawnAlreadySelected) < spawnMinDistanceFromSpawns) {
                        spawnMinDistanceFromSpawns = spawn.distanceSquared(spawnAlreadySelected);
                    }
                }
                if (spawnMinDistanceFromSpawns > minDistanceFromSpawns) {
                    minDistanceFromSpawns = spawnMinDistanceFromSpawns;
                    spawnSelected = spawn;
                }
            }
            spawnOrder.add(spawnSelected);
            roundSpawnList.remove(spawnSelected);
        }

        return spawnOrder;
    }

    public List<ThrowdownPlayer> getThrowdownPlayers() {
        List<ThrowdownPlayer> playerList = new ArrayList<>();
        for (CBCPlayer player : getPlayers().values()) {
            playerList.add((ThrowdownPlayer) player);
        }
        return playerList;
    }

    @Override
    public PostGameStats getPostGameStats() {
        return new ThrowdownPostGameStats(this);
    }

    public int getRoundNumber() {
        return roundNumber;
    }

    public boolean isRoundNotInPlay() {
        return !roundInPlay;
    }

    public void decrementSDTimer() {

        if (suddenDeath) {
            sdTimerTask.cancel();
            return;
        }

        if (!roundInPlay) {
            sdTimerTask.cancel();
            return;
        }

        // Decrement timer
        suddenDeathTimer--;

        // Check if timer is 30 or 20 or equal or below 5
        if (suddenDeathTimer != 0) {
            if (suddenDeathTimer == 30 || suddenDeathTimer == 10 || suddenDeathTimer <= 3) {
                NamedTextColor timeColor = NamedTextColor.RED;
                if (suddenDeathTimer == 30) timeColor = NamedTextColor.YELLOW;
                else if (suddenDeathTimer == 10) timeColor = NamedTextColor.GOLD;
                getGameManager().sendGlobalMessage(
                        Component.text("Sudden Death").decorate(TextDecoration.BOLD).color(NamedTextColor.RED)
                                .append(Component.text(" begins in ").decoration(TextDecoration.BOLD, TextDecoration.State.FALSE).color(NamedTextColor.WHITE))
                                .append(Component.text(suddenDeathTimer + " seconds!").decorate(TextDecoration.BOLD).color(timeColor))
                );
                getGameManager().playGlobalSound(Sound.UI_BUTTON_CLICK, 200, 1);
            }
        }
        // If timer is zero then start sudden death
        else {
            startSuddenDeath();
            sdTimerTask.cancel();
        }

    }

    public void startSuddenDeath () {

        suddenDeath = true;
        suddenDeathRounds++;

        // Display title to everyone
        Title title = Title.title(
                Component.space(), Component.text("Sudden Death has started!").color(NamedTextColor.RED),
                Title.Times.times(Duration.ofMillis(0), Duration.ofMillis(1500), Duration.ofMillis(500))
        );
        getGameManager().sendGlobalTitle(title);

        // Play sound to everyone
        getGameManager().playGlobalSound(Sound.ENTITY_WITHER_DEATH, 200, 1);

        // Disable all health pads
        getCombatManager().disableAllHealPads();

        // Heal all players
        for (CBCPlayer player : getGameManager().getAlivePlayers()) {
            if (!player.isOnline()) continue;
            player.getPlayer().setHealth(player.getMaxHealth());
        }

        // If sudden death border is enabled
        if (suddenDeathBorderEnabled) {

            suddenDeathBorder = new DeathBorder(
                    getGameManager(), map.getMapCentre(), map.getSuddenDeathBorderShape(), map.getSuddenDeathBorderStartRadius(),
                    map.getSuddenDeathBorderRadiusLimit(), map.getSuddenDeathBorderUpwardsLimit(),
                    map.getSuddenDeathBorderDownwardsLimit(), map.getSuddenDeathBorderShrinkRate()
            );
            suddenDeathBorder.activateBorder();

        }
    }

    public boolean isSuddenDeath() {
        return suddenDeath;
    }

    public int getSuddenDeathTimer() {
        return suddenDeathTimer;
    }

    public int getGameInRoundTime() {
        return gameInRoundTime;
    }

    public int getSuddenDeathRounds() {
        return suddenDeathRounds;
    }

    @Override
    public void incrementGameTime() {
        super.incrementGameTime();
        if (roundInPlay) {
            gameInRoundTime++;
        }
    }

    public List<ThrowdownPlayer> getSortedPlayersIncludingEliminated() {
        List<ThrowdownPlayer> playerList = getThrowdownPlayers();
        playerList.sort(Comparator.comparing(ThrowdownPlayer::isEliminated)
                .thenComparing(Comparator.comparingInt(ThrowdownPlayer::getRoundsWon).reversed())
                .thenComparing(Comparator.comparingInt(ThrowdownPlayer::getKills).reversed())
                .thenComparing(Comparator.comparingInt(ThrowdownPlayer::getPlayerSecondsAlive).reversed())
                .thenComparing(ThrowdownPlayer::getName)
        );
        return playerList;
    }

    public int getRoundsToWin() {
        return roundsToWin;
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

    public Team getAliveTeam() {
        return aliveTeam;
    }

    public Team getElimTeam() {
        return elimTeam;
    }

    public ThrowdownPlayer getRoundWinner() {
        return roundWinner;
    }

    public boolean isRoundStartCountdown() {
        return roundStartCountdown;
    }
}
