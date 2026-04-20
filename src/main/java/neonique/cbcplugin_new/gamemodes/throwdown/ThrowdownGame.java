package neonique.cbcplugin_new.gamemodes.throwdown;

import neonique.cbcplugin_new.CBCPlugin;
import neonique.cbcplugin_new.gamemodes.CBCGamemode;
import neonique.cbcplugin_new.gamemodes.GameContext;
import neonique.cbcplugin_new.gamemodes._base.*;
import neonique.cbcplugin_new.gameobjects.DeathBorder;
import neonique.cbcplugin_new.gameobjects.FFASpawnpoint;
import neonique.cbcplugin_new.listeners.gamemodes.PlayerNoMove;
import neonique.cbcplugin_new.managers.GameBossBarManager;
import neonique.cbcplugin_new.managers.GameManager;
import neonique.cbcplugin_new.managers.CombatManager;
import neonique.cbcplugin_new.playerclasses.CBCPlayer;
import neonique.cbcplugin_new.scoreboard.CBCScoreboardManager;
import neonique.cbcplugin_new.scoreboard.CBCScoreboardTeam;
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

public class ThrowdownGame extends FFAGame<ThrowdownPlayer, ThrowdownMap> {

    // Game related variables
    private int roundsToWin = 3;

    // Map related variables
    private List<FFASpawnpoint> spawns;

    // Teams - for display only
    private CBCScoreboardTeam aliveTeam;
    private CBCScoreboardTeam elimTeam;

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

    public ThrowdownGame (GameManager gameManager) {
        super(gameManager);
    }

    @Override
    public CBCGamemode getGamemode () {
        return CBCGamemode.THROWDOWN;
    }

    @Override
    public ThrowdownPlayer createPlayer(Player playerEntity) {
        return new ThrowdownPlayer(this, getGameManager(), getCombatManager(), playerEntity);
    }

    @Override
    public GameSidebarManager createSidebarManager () {
        return new ThrowdownSidebarManager(getGameManager(), getCombatManager(), this);
    }

    @Override
    public GameBossBarManager createBossbarManager () {
        return new ThrowdownBossbarManager(this);
    }

    @Override
    public void setupGame (GameContext ctx) {

        final GameManager gameManager = getGameManager();
        final CombatManager combatManager = getCombatManager();

        // Setup map
        setupMap((ThrowdownMap) ctx.getMap());
        // Setup default game variables
        setupDefaultGameVars(ctx.getBoolVars(), ctx.getIntVars(), ctx.getStringVars());

        createHeaderTitle();

        // Enable weapons
        combatManager.activateWeapons();

        // Setup gamemode game variables
        this.roundsToWin = ctx.getIntVars().getOrDefault("pointsToWin", 3);
        this.suddenDeathEnabled = ctx.getBoolVars().getOrDefault("suddenDeathEnabled", true);

        // setGameCommands(new ShowdownGameCommands(gameManager, weaponManager, this));
        // Create players
        createPlayers(ctx.getPlayers());
        teleportSpectators();

        // Create teams
        CBCScoreboardManager scoreboardManager = gameManager.getCbcScoreboardManager();

        // Create alive team
        aliveTeam = scoreboardManager.registerNewTeam("01alive");
        aliveTeam.setSeeFriendlyInvisiblesEnabled(false);
        aliveTeam.setFriendlyFireEnabled(true); // Do not allow friendly fire
        aliveTeam.setColor(NamedTextColor.GREEN); // Set team color

        // Create eliminated team
        elimTeam = scoreboardManager.registerNewTeam("02eliminated");
        elimTeam.setSeeFriendlyInvisiblesEnabled(false);
        elimTeam.setFriendlyFireEnabled(true); // Do not allow friendly fire
        elimTeam.setColor(NamedTextColor.RED); // Set team color

        // Setup tasks
        new ThrowdownPlayerCounts(gameManager, this).runTaskTimer(CBCPlugin.getPlugin(), 0, 4);

        // Setup listeners
        noMoveListener = new PlayerNoMove(gameManager);

        // Create sidebar manager and bossbar manager
        createUIManagers();

        // Start round 1
        setupRound();
    }

    public void setupMap (ThrowdownMap map) {

        super.setupMap(map);

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
        for (ThrowdownPlayer player : getPlayers()) {
            player.playerSetupRound();
        }

        // Select each player's spawn randomly
        List<Location> spawnOrder = sortSpawns();
        List<ThrowdownPlayer> randomPlayerList = new ArrayList<>(getPlayers());
        Collections.shuffle(randomPlayerList);

        int spawnNum = 0;
        for (ThrowdownPlayer player : randomPlayerList) {

            if (!player.isOnline()) continue;
            player.teleportPlayerToSpawn(spawnOrder.get(spawnNum), getMap().getMapCentre());
            aliveTeam.addEntityUUID(player.getUUID());
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
        getCombatManager().setAllPlayersImmune(false);

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
        CBCScoreboardManager.getInstance().unregisterTeam(aliveTeam);
        CBCScoreboardManager.getInstance().unregisterTeam(elimTeam);

    }

    @Override
    public void playerWonGame (ThrowdownPlayer winner) {
        super.playerWonGame(winner);
        // End round and do not start the next round
        roundOver(false);
    }

    @Override
    public PostGameStats getPostGameStats() {
        return new ThrowdownPostGameStats(this);
    }

    public void startRound() {

        getMap().fillBlocksAtEnd();

        // Start round for players
        for (ThrowdownPlayer player : getPlayers()) {
            player.playerStartRound();
        }

        roundInPlay = true;
        getCombatManager().setVoidKill(true);

        // Release players so they can move
        PlayerMoveEvent.getHandlerList().unregister(noMoveListener);
        roundStartCountdown = false;

        new ThrowdownTimeAliveTask(this.getGameManager(), this).runTaskTimer(CBCPlugin.getPlugin(), 20, 20);

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
        for (ThrowdownPlayer player : getPlayers()) {
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
        getCombatManager().setAllPlayersImmune(true);
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


    public List<Location> sortSpawns() {

        List<Location> roundSpawnList = new ArrayList<>(spawns);
        List<Location> spawnOrder = new ArrayList<>();

        // Select the first spawn
        Comparator<Location> byDistanceFromCenter =
                (Location loc1, Location loc2) -> Double.compare(loc1.distanceSquared(getMap().getMapCentre()), loc2.distanceSquared(getMap().getMapCentre()));
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
            player.healToFull();
        }

        // If sudden death border is enabled
        if (suddenDeathBorderEnabled) {
            suddenDeathBorder = getMap().getSuddenDeathBorder();
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
        List<ThrowdownPlayer> playerList = new ArrayList<>(getPlayers());
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

    public CBCScoreboardTeam getAliveTeam () {
        return aliveTeam;
    }

    public CBCScoreboardTeam getElimTeam () {
        return elimTeam;
    }

    public ThrowdownPlayer getRoundWinner() {
        return roundWinner;
    }

    public boolean isRoundStartCountdown() {
        return roundStartCountdown;
    }
}
