package neonique.cbcplugin_new.gamemodes.showdown;

import neonique.cbcplugin_new.CBCPlugin;
import neonique.cbcplugin_new.gamemodes.CBCGamemode;
import neonique.cbcplugin_new.gamemodes.GameContext;
import neonique.cbcplugin_new.gamemodes._base.*;
import neonique.cbcplugin_new.gamemodes.showdown.tasks.*;
import neonique.cbcplugin_new.mechanics.DeathBorder;
import neonique.cbcplugin_new.lobby.LobbyTeam;
import neonique.cbcplugin_new.managers.GameBossBarManager;
import neonique.cbcplugin_new.managers.GameManager;
import neonique.cbcplugin_new.managers.CombatManager;
import neonique.cbcplugin_new.playerclasses.CBCPlayer;
import neonique.cbcplugin_new.tasks.gamemodetasks.IncrementGameTimeTask;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.*;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.util.*;

public class ShowdownGame extends TeamGame<ShowdownPlayer, ShowdownMap, ShowdownTeam> {

    // Game related variables
    private int roundsToWin = 4;

    // Map related variables
    private boolean randomSpawns;
    private List<ShowdownSpawn> randomTeamSpawns;
    private HashMap<String, ShowdownSpawn> nonRandomTeamSpawns;

    // Sudden death variables
    private boolean suddenDeathEnabled;
    private boolean suddenDeathBorderEnabled;
    private int suddenDeathMaxTimer;

    // Set up round related variables
    private boolean roundInPlay = false;
    private boolean roundStartCountdown = false;
    private int roundNumber = 0;
    private final List<ShowdownTeam> roundWinOrder = new ArrayList<>();
    private ShowdownTeam roundWinner = null;

    // Sudden death related variables
    private int suddenDeathTimer;
    private boolean suddenDeath = false;
    private DeathBorder suddenDeathBorder = null;

    // Event listeners and tasks
    private ShowdownPlayerCounts showdownPlayerCountsTask;
    private ShowdownSDTimer sdTimerTask;

    // Statistical variables
    private int gameInRoundTime = 0;
    private int suddenDeathRounds = 0;

    // Other game variables
    private boolean playersGlow = true;

    public ShowdownGame (GameManager gameManager) {
        super(gameManager);
    }

    @Override
    public CBCGamemode getGamemode () {
        return CBCGamemode.SHOWDOWN;
    }

    @Override
    public ShowdownTeam createGamemodeTeam (LobbyTeam team, int teamNum) {
        return new ShowdownTeam(this, team.getTeamId(),
                Integer.toString(teamNum), team.getTeamName(), team.getColor(),
                team.getPrefix(), team.getItem(), team.getGlassHead()
        );
    }

    @Override
    public ShowdownPlayer createPlayer(Player playerEntity) {
        return new ShowdownPlayer(this, getGameManager(), getCombatManager(), playerEntity);
    }

    @Override
    public GameSidebarManager createSidebarManager () {
        return new ShowdownSidebarManager(getGameManager(), getCombatManager(), this);
    }

    @Override
    public GameBossBarManager createBossbarManager () {
        return new ShowdownBossbarManager(this);
    }

    @Override
    public void setupMap (ShowdownMap map) {

        super.setupMap(map);

        // Get spawns
        randomSpawns = map.isRandomTeamSpawns();
        if (randomSpawns) {
            randomTeamSpawns = map.getTeamSpawns();
            randomTeamSpawns = sortSpawns();
        } else {
            nonRandomTeamSpawns = map.getTeamSpawnsWithKeys();
        }

        // Setup sudden death
        if (map.isSuddenDeathEnabled()) {
            suddenDeathBorderEnabled = map.isSuddenDeathBorderEnabled();
            suddenDeathMaxTimer = map.getSuddenDeathTimer();
        }
    }

    @Override
    public void setupGame (GameContext ctx) {

        final GameManager gameManager = getGameManager();
        final CombatManager combatManager = getCombatManager();

        // Setup map
        setupMap((ShowdownMap) ctx.getMap());

        // Setup default game variables
        setupDefaultGameVars(ctx.getBoolVars(), ctx.getIntVars(), ctx.getStringVars());

        // Set gamemode information
        createHeaderTitle();

        // Enable weapons
        combatManager.activateWeapons();

        // Setup game listeners and tasks
        CBCPlugin plugin = CBCPlugin.getPlugin();

        showdownPlayerCountsTask = new ShowdownPlayerCounts(gameManager, this);
        showdownPlayerCountsTask.runTaskTimer(plugin, 0, 4);

        // Setup gamemode game variables
        this.roundsToWin = ctx.getIntVars().getOrDefault("pointsToWin", 4);
        this.suddenDeathEnabled = ctx.getBoolVars().getOrDefault("suddenDeathEnabled", true);
        this.playersGlow = ctx.getBoolVars().getOrDefault("playersGlow", true);

        setGameCommands(new ShowdownGameCommands(this));

        // Create teams and players
        createTeams(ctx.getTeams());
        teleportSpectators();

        // Create bossbar and sidebar displays
        createUIManagers();

        // Start round 1
        setupRound();

    }

    public void setupRound () {

        roundInPlay = false;
        roundWinner = null;
        roundStartCountdown = true;
        roundNumber++;

        // Update footer
        createFooter();

        // Setup spawns
        if (randomSpawns) {
            for (ShowdownSpawn spawn : randomTeamSpawns) {
                spawn.setupSpawn();
            }
        } else {
            for (ShowdownSpawn spawn : nonRandomTeamSpawns.values()) {
                spawn.setupSpawn();
            }
        }

        getMap().fillBlocksAtStart();

        // Teleport players to spawns
        if (randomSpawns) {
            List<ShowdownTeam> shuffledTeams = new ArrayList<>(getTeams());
            Collections.shuffle(shuffledTeams);
            int spawnNum = 0;
            for (ShowdownTeam team : shuffledTeams) {
                ShowdownSpawn spawnToTeleport = randomTeamSpawns.get(spawnNum);
                team.setRoundSpawn(spawnToTeleport);
                team.teleportPlayers(spawnToTeleport);
                spawnNum++;
            }
        } else {
            for (ShowdownTeam team : getTeams()) {
                ShowdownSpawn spawn = nonRandomTeamSpawns.get(team.getTeamId());
                team.setRoundSpawn(spawn);
                team.teleportPlayers(spawn);
            }
        }

        // Reset sudden death variables
        suddenDeath = false;
        suddenDeathBorder = null;
        suddenDeathTimer = suddenDeathMaxTimer;

        getCombatManager().setAllPlayersImmune(false);
        getCombatManager().enableAllHealPads();

        // Reset team variables
        for (ShowdownTeam team : getTeams()) {
            team.setupRound();
        }

        // Start countdown for next round
        if (roundNumber == 1) {
            new ShowdownStartRoundTimer(getGameManager(), this, 11, true).runTaskTimer(CBCPlugin.getPlugin(), 0, 20);
        } else {
            new ShowdownStartRoundTimer(getGameManager(), this, 6, false).runTaskTimer(CBCPlugin.getPlugin(), 0, 20);
        }

        // Update UI elements
        updateBossbarManager();
        updateServerSidebar();

    }

    public void startRound () {

        getMap().fillBlocksAtEnd();

        roundInPlay = true;
        getCombatManager().setVoidKill(true);

        for (ShowdownPlayer player : getPlayers()) {
            player.playerStartRound();
        }

        // Release players
        if (randomSpawns) {
            for (ShowdownSpawn spawn : randomTeamSpawns) {
                spawn.roundStart();
            }
        } else {
            for (ShowdownSpawn spawn : nonRandomTeamSpawns.values()) {
                spawn.roundStart();
            }
        }

        roundStartCountdown = false;

        // Start counting player alive time
        new ShowdownTimeAliveTask(this).runTaskTimer(CBCPlugin.getPlugin(), 20, 20);

        // Start sudden death mechanisms
        if (suddenDeathEnabled) {
            sdTimerTask = new ShowdownSDTimer(getGameManager(), this);
            sdTimerTask.runTaskTimer(CBCPlugin.getPlugin(), 20, 20);
        }

        if (roundNumber == 1) {
            new IncrementGameTimeTask(this).runTaskTimer(CBCPlugin.getPlugin(), 20, 20);
        }

        // Update UI elements
        updateServerSidebar();
    }

    public void checkPlayerCounts () {

        if (isGameOver()) {
            return;
        }

        // Check if round is in play
        if (!roundInPlay) {
            return;
        }

        List<ShowdownTeam> teamsAlive = new ArrayList<>();
        for (ShowdownTeam team : getTeams()) {
            if (!team.isTeamAlive()) continue; // Make sure team is alive

            int teamPlayerCount = team.updatePlayersLeftAlive(true);
            if (teamPlayerCount > 0) {
                teamsAlive.add(team);
            } else {
                // Eliminate team if they are no longer alive
                if (team.isTeamAlive()) {
                    team.eliminateTeam();
                }
            }
        }

        // Check if only one team is alive
        if (teamsAlive.size() == 1) {
            // Team has won round
            teamWonRound(teamsAlive.get(0));
        }
    }

    public void teamWonRound (ShowdownTeam team) {

        // Increment team count
        team.teamWonRound();

        // Add team to list
        roundWinner = team;
        roundWinOrder.add(team);

        // Update footer
        createFooter();

        // Check if team has reached the required amount of points
        if (team.getRoundsWon() >= roundsToWin) {
            gameWon(team);
        } else {
            // Display title of round win
            Component titleToDisplay = Component.text(team.getTeamName().toUpperCase() + " WINS ROUND " + roundNumber + "!")
                            .decorate(TextDecoration.BOLD).color(team.getColor());

            getGameManager().sendGlobalTitle(Title.title(titleToDisplay, Component.space(),
                    Title.Times.times(Duration.ofMillis(0), Duration.ofMillis(3000), Duration.ofMillis(500))));

            // Send message of round win
            getGameManager().sendGlobalMessage(
                    Component.newline()
                            .append(Component.text("ROUND WIN > ").decorate(TextDecoration.BOLD).color(NamedTextColor.WHITE))
                            .append(Component.text(team.getTeamName()).decorate(TextDecoration.BOLD).color(team.getColor()))
                            .append(Component.text(" has won the round!").color(NamedTextColor.WHITE))
                            .append(Component.newline())
            );

            // Play sound to all players
            getGameManager().playGlobalSound(Sound.ENTITY_PLAYER_LEVELUP, 200, 0);

            // End round and start the next round
            roundOver(true);
        }

        // Update bossbar
        updateBossbarManager();
    }

    public void roundOver (boolean startNextRound) {

        roundInPlay = false;

        // Set all alive players to immune
        for (ShowdownPlayer player : getPlayers()) {
            if (player.isAlive()) {
                player.setImmune(true);
            }
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
            new ShowdownNextRoundTimer(getGameManager(), this, 10).runTaskTimer(CBCPlugin.getPlugin(), 20, 20);
        }

        // Update sidebar manager
        updateServerSidebar();
    }

    @Override
    public void gameWon (ShowdownTeam team) {

        super.gameWon(team);

        // Add bonus points for winning
        for (CBCPlayer player : team.getPlayers()) {
            player.addGamePoints(40);
        }

        // End round and do not start the next round
        roundOver(false);

    }

    public PostGameStats getPostGameStats() {
        return new ShowdownPostGameStats(this);
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
        cancelTask(showdownPlayerCountsTask);

    }

    public int getRoundNumber() {
        return roundNumber;
    }

    public boolean isRoundNotInPlay() {
        return !roundInPlay;
    }

    public int getRoundsToWin() {
        return roundsToWin;
    }

    // Sudden death functions
    public void decrementSDTimer(ShowdownSDTimer sdTimerTask) {

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

        updateBossbarManager();
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
        getGameManager().playGlobalSound(Sound.ENTITY_WITHER_DEATH, 200, 1);
        getCombatManager().disableAllHealPads();

        // Heal all players
        for (CBCPlayer player : getGameManager().getAlivePlayers()) {
            if (!player.isOnline()) continue;
            player.healToFull();
        }

        // If sudden death border is enabled activate the border
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

    @Override
    public void playerJoinServer(Player playerEntity) {

        super.playerJoinServer(playerEntity);

        ShowdownPlayer player = getPlayer(playerEntity);
        if (player != null) {

            ShowdownTeam team = getPlayerTeam(player);
            if (team == null) return;

            // If player joins before round starts, put them into game
            if (!roundInPlay && roundStartCountdown) {
                player.playerSetupRound();
                player.teleportPlayerToSpawn(team.getRoundSpawn(), getMap().getMapCentre());
            }

        }

    }

    public void createFooter () {

        int maxRounds = (roundsToWin - 1) * getTeams().size() + 1;

        Component footer = Component.newline().append(smallText("Round " + roundNumber + " ")
                .color(NamedTextColor.AQUA).decorate(TextDecoration.BOLD));

        for (int rd = 1; rd <= maxRounds; rd++) {
            if (roundWinOrder.size() >= rd) {
                footer = footer.append(smallText("■").color(roundWinOrder.get(rd - 1).getColor()));
            } else {
                if (rd == roundNumber) {
                    footer = footer.append(smallText("□").color(NamedTextColor.WHITE));
                } else {
                    footer = footer.append(smallText("□").color(NamedTextColor.GRAY));
                }
            }
        }

        getGameManager().setPlayerListFooter(footer);
    }

    public boolean isRoundStartCountdown () {
        return roundStartCountdown;
    }

    public ShowdownTeam getRoundWinner () {
        return roundWinner;
    }

    public List<ShowdownSpawn> sortSpawns() {

        List<ShowdownSpawn> roundSpawnList = new ArrayList<>(randomTeamSpawns);
        List<ShowdownSpawn> spawnOrder = new ArrayList<>();

        // Select the first spawn
        Comparator<ShowdownSpawn> byDistanceFromCenter =
                (ShowdownSpawn loc1, ShowdownSpawn loc2) -> Double.compare(loc1.distanceSquared(getMap().getMapCentre()), loc2.distanceSquared(getMap().getMapCentre()));
        roundSpawnList.sort(byDistanceFromCenter);
        Collections.reverse(roundSpawnList);

        spawnOrder.add(roundSpawnList.get(0));
        roundSpawnList.remove(0);

        while (spawnOrder.size() < randomTeamSpawns.size()) {
            double minDistanceFromSpawns = 0;
            ShowdownSpawn spawnSelected = null;
            for (ShowdownSpawn spawn : new ArrayList<>(roundSpawnList)) {
                double spawnMinDistanceFromSpawns = 300000;
                for (ShowdownSpawn spawnAlreadySelected : spawnOrder) {
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

    public boolean isPlayerGlowingEnabled () {
        return playersGlow;
    }

    public int getPlayersAlive () {
        return (int) getPlayers().stream().filter(ShowdownPlayer::isAlive).count();
    }

}
