package neonique.cbcplugin_new.gamemodes.ctf;

import neonique.cbcplugin_new.CBCPlugin;
import neonique.cbcplugin_new.enums.CBCGamemode;
import neonique.cbcplugin_new.gamemodes._base.*;
import neonique.cbcplugin_new.gameobjects.DeathBorder;
import neonique.cbcplugin_new.listeners.gamemodes.CTFTeleportListener;
import neonique.cbcplugin_new.listeners.gamemodes.PlayerNoMove;
import neonique.cbcplugin_new.lobby.LobbyPlayer;
import neonique.cbcplugin_new.lobby.LobbyTeam;
import neonique.cbcplugin_new.managers.GameBossBarManager;
import neonique.cbcplugin_new.managers.GameManager;
import neonique.cbcplugin_new.managers.CombatManager;
import neonique.cbcplugin_new.playerclasses.CBCPlayer;
import neonique.cbcplugin_new.tasks.gamemodetasks.IncrementGameTimeTask;
import neonique.cbcplugin_new.tasks.gamemodetasks.ctf.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

import java.time.Duration;
import java.util.*;

import static neonique.cbcplugin_new.util.StatsUtil.sortPlayerStatList;

public class CTFGame extends TeamGame {

    protected final List<CTFTeam> teams = new ArrayList<>();

    // Game related variables
    private int flagsStart = 4;
    protected boolean canCaptureOrTake = true;
    private int flagsRemovedTimer = 1800;
    private int flagsRemovedTimerIncrement = 600;
    private boolean allFlagsTaken = false;

    // Sudden death variables
    private boolean suddenDeath = false;
    private DeathBorder suddenDeathBorder = null;

    // Map related variables
    protected CTFMap map;

    protected boolean randomBases;
    protected List<Location> randomFlagLocations;
    protected HashMap<String, Location> nonRandomFlagLocations;
    protected List<Set<Location>> randomBaseSpawns;
    protected HashMap<String, Set<Location>> nonRandomBaseSpawns;

    protected int defensiveKillRadius;
    protected HashMap<Integer, Integer> respawnTimes;

    // Game event listeners and tasks
    protected CTFStartGameTimer startGameTimer;
    protected CTFPlayersNearbyFlags playersNearbyFlagsTask;
    protected CTFGlowManagerTask updateGlowTask;
    protected CTFPlayerTrackingTask playerTrackingTask;
    protected PlayerNoMove playerNoMoveListener;
    private CTFTeleportListener teleportListener;

    protected CTFGlowManager glowManager;

    // Current leaderboards
    private List<PlayerStatObject> topKills;
    private List<PlayerStatObject> topGameScore;
    private List<PlayerStatObject> topCaptures;
    private List<PlayerStatObject> topDKills;

    public CTFGame(GameManager gameManager, CombatManager combatManager) {
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
        setGamemode(CBCGamemode.CTF);
        createHeaderTitle();

        // Enable weapons
        combatManager.activateWeapons();
        gameManager.resetPlayerList();

        // Setup gamemode game variables
        this.flagsStart = intVars.getOrDefault("flagsStart", 4);
        this.flagsRemovedTimer = intVars.getOrDefault("initialFlagsRemovedTimer", 1800);
        this.flagsRemovedTimerIncrement = intVars.getOrDefault("flagsRemovedTimerAfterFirst", 600);

        // Setup game commands
        setGameCommands(new CTFGameCommands(gameManager, combatManager, this));

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
                CTFPlayer ctfPlayer = (CTFPlayer) player;
                ctfPlayer.resetPlayer();
                ctfPlayer.teleportPlayerToSpawn();
            }

            teamNum++;
        }

        // Update stat leaderboards
        updateTopKillsList();
        updateTopGameScoreList();
        updateTopCapturesList();
        updateTopDKillsList();

        if (!map.isCanMoveAtGameStart()) {
            playerNoMoveListener = new PlayerNoMove(gameManager);
            CBCPlugin.getPlugin().getServer().getPluginManager().registerEvents(playerNoMoveListener, CBCPlugin.getPlugin());
        }

        // Create Bossbar/Sidebar managers
        createUIManagers();

        // Teleport listener -- players cannot teleport to others in spectator mode unless eliminated
        teleportListener = new CTFTeleportListener(this);
        CBCPlugin.getPlugin().getServer().getPluginManager().registerEvents(teleportListener, CBCPlugin.getPlugin());

        glowManager = new CTFGlowManager(world, this);
        glowManager.activate();

        updateGlowTask = new CTFGlowManagerTask(this);
        updateGlowTask.runTaskTimer(CBCPlugin.getPlugin(), 0, 15);

        playerTrackingTask = new CTFPlayerTrackingTask(this);
        playerTrackingTask.runTaskTimer(CBCPlugin.getPlugin(), 0, 6);

        // Start countdown for the game
        startGameTimer = new CTFStartGameTimer(gameManager, this, 11);
        startGameTimer.runTaskTimer(CBCPlugin.getPlugin(), 0, 20);
    }

    public void setupMap (CBCMap generalMap) {

        super.setupMap(generalMap);
        this.map = (CTFMap) generalMap;

        // Get spawns
        randomBases = map.isRandomBases();
        if (randomBases) {
            randomFlagLocations = map.getFlagLocations();
            randomBaseSpawns = map.getBaseSpawns();
        } else {
            nonRandomFlagLocations = map.getFlagLocationsWithKeys();
            nonRandomBaseSpawns = map.getBaseSpawnsWithKeys();
        }

        // Set other variables
        respawnTimes = map.getRespawnTimes();
        defensiveKillRadius = map.getdKillRadius();

        // Setup sudden death
        if (map.isSuddenDeathEnabled()) {
            suddenDeathBorder = new DeathBorder(
                    getGameManager(), map.getMapCentre(), map.getBorderShape(), map.getStartingBorderRadius(), map.getFinalBorderRadius(),
                    map.getBorderTopY(), map.getBorderBottomY(), map.getBorderShrinkRate()
            );
        }
    }

    public CBCTeam createGamemodeTeam (LobbyTeam team, int teamNum) {
        CTFTeam createdTeam = new CTFTeam(this, team.getTeamId(),
                Integer.toString(teamNum), team.getTeamName(), team.getColor(),
                team.getPrefix(), team.getItem(), team.getGlassHead()
        );
        teams.add(createdTeam);
        return createdTeam;
    }

    public CBCPlayer createGamemodePlayer (Player playerEntity, int playerId) {
        return new CTFPlayer(this, getGameManager(), getCombatManager(), playerEntity, playerId);
    }

    public GameSidebarManager createSidebarManager() {
        return new CTFSidebarManager(getGameManager(), getCombatManager(), this);
    }

    @Override
    public GameBossBarManager createBossbarManager() {
        return new CTFBossbarManager(this);
    }

    public void startGame () {

        map.fillBlocksAtEnd();

        if (!map.isCanMoveAtGameStart()) {
            PlayerMoveEvent.getHandlerList().unregister(playerNoMoveListener);
            playerNoMoveListener = null;
        }

        // Initialise all players
        for (CTFTeam team : teams) {
            for (CBCPlayer player : team.getPlayers()) {
                CTFPlayer ctfPlayer = (CTFPlayer) player;

                ctfPlayer.playerSetup();
                ctfPlayer.setReloadsBySecond(1);

                ctfPlayer.setTempImmune(60);
            }
        }

        // Start important tasks
        playersNearbyFlagsTask = new CTFPlayersNearbyFlags(this);
        playersNearbyFlagsTask.runTaskTimer(CBCPlugin.getPlugin(), 3, 2);

        canCaptureOrTake = true;

        // Start game length timer
        new IncrementGameTimeTask(this).runTaskTimer(CBCPlugin.getPlugin(), 20, 20);

        updateServerSidebar();
    }

    public void decrementRemoveFlagTimer () {

        final GameManager gameManager = getGameManager();

        flagsRemovedTimer--;

        // Check if timer has run out
        if (flagsRemovedTimer == 0) {
            removeFlagsFromTeams();
            // Set flags removed back to the increment
            flagsRemovedTimer = flagsRemovedTimerIncrement;
        }
        else {
            // Send warnings for timer
            if (flagsRemovedTimer == 300) {
                gameManager.sendGlobalMessage(Component.text("5 minutes until all teams lose a flag!")
                        .color(NamedTextColor.YELLOW).decorate(TextDecoration.BOLD));
                gameManager.playGlobalSound(Sound.BLOCK_NOTE_BLOCK_BIT, 200, 1);
            }
            else if (flagsRemovedTimer == 180) {
                gameManager.sendGlobalMessage(Component.text("3 minutes until all teams lose a flag!")
                        .color(NamedTextColor.GOLD).decorate(TextDecoration.BOLD));
                gameManager.playGlobalSound(Sound.BLOCK_NOTE_BLOCK_BIT, 200, 1);
            }
            else if (flagsRemovedTimer == 120) {
                gameManager.sendGlobalMessage(Component.text("2 minutes until all teams lose a flag!")
                        .color(NamedTextColor.GOLD).decorate(TextDecoration.BOLD));
                gameManager.playGlobalSound(Sound.BLOCK_NOTE_BLOCK_BIT, 200, 1);
            }
            else if (flagsRemovedTimer == 60) {
                gameManager.sendGlobalMessage(Component.text("1 minute until all teams lose a flag!")
                        .color(NamedTextColor.RED).decorate(TextDecoration.BOLD));
                gameManager.playGlobalSound(Sound.BLOCK_NOTE_BLOCK_BIT, 200, 2);
            }
            else if (flagsRemovedTimer == 30) {
                gameManager.sendGlobalMessage(Component.text("30 seconds until all teams lose a flag!")
                        .color(NamedTextColor.RED).decorate(TextDecoration.BOLD));
                gameManager.playGlobalSound(Sound.BLOCK_NOTE_BLOCK_BIT, 200, 2);
            }
            else if (flagsRemovedTimer == 10) {
                gameManager.sendGlobalMessage(Component.text("10 seconds until all teams lose a flag!")
                        .color(NamedTextColor.RED).decorate(TextDecoration.BOLD));
                gameManager.playGlobalSound(Sound.BLOCK_NOTE_BLOCK_BIT, 200, 2);
            }
        }

        updateBossbarManager();


    }

    public void removeFlagsFromTeams () {

        // Remove flags from teams
        for (CTFTeam team : teams) {
            // Check if team has any flags left
            if (team.getFlagsLeft() > 0) {
                // Remove a flag from their team
                team.removeFlag();
            }
        }

        Component flagCaptureComponent = Component.newline().append(Component.text("FLAGS REMOVED > ").color(NamedTextColor.WHITE).decorate(TextDecoration.BOLD))
                .append(Component.text("All remaining teams have lost one flag!").color(NamedTextColor.WHITE))
                .append(Component.newline());

        getGameManager().sendGlobalMessage(flagCaptureComponent);

        updateServerSidebar();
        checkIfFlagsLeft();
    }

    public void checkIfFlagsLeft () {

        // Remove flags from teams
        boolean flagsRemaining = false;
        for (CTFTeam team : teams) {
            if (team.getFlagsLeft() > 0) {
                flagsRemaining = true;
                break;
            }
        }

        // Check if game should be sent to sudden death
        if (!allFlagsTaken && !flagsRemaining) {
            if (map.isSuddenDeathEnabled()) {
                startSuddenDeath();
            }
        }

        allFlagsTaken = !flagsRemaining;
    }

    public void startSuddenDeath () {

        final GameManager gameManager = getGameManager();
        suddenDeath = true;

        // Create border
        if (suddenDeathBorder != null) {
            suddenDeathBorder.activateBorder();
        }

        // Disable all health pads
        getCombatManager().disableAllHealPads();

        // Heal all players
        for (CBCPlayer player : getGameManager().getAlivePlayers()) {
            if (!player.isOnline()) continue;
            player.getPlayer().setHealth(player.getMaxHealth());
        }

        // Display title to everyone
        Title title = Title.title(
                Component.text("SUDDEN DEATH").color(NamedTextColor.RED).decorate(TextDecoration.BOLD), Component.space(),
                Title.Times.times(Duration.ofMillis(0), Duration.ofMillis(2000), Duration.ofMillis(500))
        );
        gameManager.sendGlobalTitle(title);

        // Play sound to everyone
        gameManager.playGlobalSound(Sound.ENTITY_WITHER_DEATH, 200, 1);

        // Send a message to everyone
        gameManager.sendGlobalMessage(Component.newline().append(
                    Component.text("SUDDEN DEATH > ").color(NamedTextColor.WHITE).decorate(TextDecoration.BOLD)
                ).append(
                    Component.text("ONE LIFE LEFT! ").color(NamedTextColor.RED).decorate(TextDecoration.BOLD)
                ).append(
                    Component.text("A damaging border is beginning" +
                            " to close in towards the center of the map!").color(NamedTextColor.RED)
                            .decoration(TextDecoration.BOLD, TextDecoration.State.FALSE)
                )
                .append(Component.newline())
        );


    }

    public void checkIfWinner() {
        if (getWinner() != null) return;

        // Go through every team and see if they are eliminated
        CTFTeam teamAlive = null;
        for (CTFTeam team : teams) {
            if (!team.isTeamEliminated()) {
                if (teamAlive != null) {
                    return;
                }
                teamAlive = team;
            }
        }

        if (teamAlive == null) return;

        // End the game
        gameWon(teamAlive);

    }

    @Override
    public void gameWon (CBCTeam team) {

        super.gameWon(team);

        // Add bonus points for winning
        for (CBCPlayer player : team.getPlayers()) {
            player.addGamePoints(40);
        }

        // Set all alive players to immune
        for (CBCPlayer player : getPlayers().values()) {
            CTFPlayer ctfPlayer = (CTFPlayer) player;
            if (!ctfPlayer.isEliminated()) {
                ctfPlayer.setImmune(true);
            }
        }

        // Make the void do nothing
        getCombatManager().setVoidKill(false);

        // Players can no longer capture or take
        canCaptureOrTake = false;

        if (suddenDeathBorder != null) {
            suddenDeathBorder.deactivateBorder();
        }
    }

    public void resetGame() {

        super.resetGame();

        // Deactivate sudden death border if existent
        if (suddenDeathBorder != null) {
            suddenDeathBorder.deactivateBorder();
        }

        // If players are unable to move, disable the function
        if (playerNoMoveListener != null) {
            PlayerMoveEvent.getHandlerList().unregister(playerNoMoveListener);
        }
        PlayerTeleportEvent.getHandlerList().unregister(teleportListener);

        // Cancel all tasks
        cancelTask(playersNearbyFlagsTask);
        cancelTask(updateGlowTask);
        cancelTask(playerTrackingTask);

        // Disable glow manager
        glowManager.deactivate();

    }

    @Override
    public PostGameStats getPostGameStats() {
        return new CTFPostGameStats(this);
    }

    public List<CTFTeam> getTeams() {
        return teams;
    }

    public int getFlagsStart() {
        return flagsStart;
    }

    public int getDefensiveKillRadius() {
        return defensiveKillRadius;
    }

    public int getRespawnTime(int teamAmount) {
        if (teamAmount > 5) {
            return respawnTimes.get(5);
        } else {
            return respawnTimes.get(teamAmount);
        }
    }

    @Override
    public void incrementGameTime() {
        super.incrementGameTime();
        for (CTFTeam team : teams) {
            team.incrementTimeTeamAlive();
        }

        // Decrement flag remove timer
        if (anyFlagsLeft() && getWinner() == null) {
            decrementRemoveFlagTimer();
        }
    }

    public CTFGlowManager getGlowManager() {
        return glowManager;
    }

    public boolean canCaptureOrTake() {
        return canCaptureOrTake;
    }

    public int getFlagsRemovedTimer() {
        return flagsRemovedTimer;
    }

    public boolean anyFlagsLeft() {
        return !allFlagsTaken;
    }

    public boolean isSuddenDeath() {
        return suddenDeath;
    }

    public int getBorderDiameter() {
        return (int) Math.round(suddenDeathBorder.getCurrentRadius() * 2);
    }

    public void updateTopKillsList () {
        // Create new top kills list
        topKills = new ArrayList<>();
        for (CTFPlayer player : getCTFPlayers()) {
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
        for (CTFPlayer player : getCTFPlayers()) {
            // Add player's game score to the list
            topGameScore.add(new PlayerStatObject(player, player.getGamePoints()));
        }
        // Sort list
        sortPlayerStatList(topGameScore, true);
    }

    public List<PlayerStatObject> getTopGameScoreList () {
        return topGameScore;
    }

    public void updateTopCapturesList () {
        // Create new top flag captures list
        topCaptures = new ArrayList<>();
        for (CTFPlayer player : getCTFPlayers()) {
            // Add player's flag captures to the list
            topCaptures.add(new PlayerStatObject(player, player.getFlagsCaptured()));
        }
        // Sort list
        sortPlayerStatList(topCaptures, true);
    }

    public List<PlayerStatObject> getTopCapturesList () {
        return topCaptures;
    }

    public void updateTopDKillsList () {
        // Create new top defensive kills list
        topDKills = new ArrayList<>();
        for (CTFPlayer player : getCTFPlayers()) {
            // Add player's defensive kills to the list
            topDKills.add(new PlayerStatObject(player, player.getDefensiveKills()));
        }
        // Sort list
        sortPlayerStatList(topDKills, true);
    }

    public List<PlayerStatObject> getTopDKills () {
        return topDKills;
    }

    public List<CTFPlayer> getCTFPlayers () {
        // Get all players as CTFPlayer objects
        List<CTFPlayer> playersList = new ArrayList<>();
        for (CBCPlayer player : getPlayers().values()) {
            if (player instanceof CTFPlayer) {
                playersList.add((CTFPlayer) player);
            }
        }
        return playersList;
    }
}
