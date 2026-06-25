package neonique.cbcplugin_new.gamemodes.ctf;

import neonique.cbcplugin_new.CBCPlugin;
import neonique.cbcplugin_new.core.TeamGame;
import neonique.cbcplugin_new.gamemodes.CBCGamemode;
import neonique.cbcplugin_new.gamemodes.GameContext;
import neonique.cbcplugin_new.gamemodes._base.*;
import neonique.cbcplugin_new.gamemodes.ctf.tasks.CTFGlowManagerTask;
import neonique.cbcplugin_new.gamemodes.ctf.tasks.CTFPlayersNearbyFlags;
import neonique.cbcplugin_new.gamemodes.ctf.tasks.CTFStartGameTimer;
import neonique.cbcplugin_new.mechanics.DeathBorder;
import neonique.cbcplugin_new.listeners.gamemodes.CTFTeleportListener;
import neonique.cbcplugin_new.listeners.gamemodes.PlayerNoMove;
import neonique.cbcplugin_new.lobby.LobbyTeam;
import neonique.cbcplugin_new.managers.GameBossBarManager;
import neonique.cbcplugin_new.managers.GameManager;
import neonique.cbcplugin_new.combat.CombatManager;
import neonique.cbcplugin_new.playerclasses.CBCPlayer;
import neonique.cbcplugin_new.tasks.gamemodetasks.IncrementGameTimeTask;
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

public class CTFGame extends TeamGame<CTFPlayer, CTFMap, CTFTeam> {

    // Game related variables
    private int flagsStart = 4;
    private boolean canCaptureOrTake = true;
    private int flagsRemovedTimer = 1800;
    private int flagsRemovedTimerIncrement = 600;
    private boolean allFlagsTaken = false;

    // Sudden death variables
    private boolean suddenDeath = false;
    private DeathBorder suddenDeathBorder = null;

    private boolean randomBases;
    private List<Location> randomFlagLocations;
    private HashMap<String, Location> nonRandomFlagLocations;
    private List<Set<Location>> randomBaseSpawns;
    private HashMap<String, Set<Location>> nonRandomBaseSpawns;

    private int defensiveKillRadius;
    private HashMap<Integer, Integer> respawnTimes;

    // Game event listeners and tasks
    private CTFPlayersNearbyFlags playersNearbyFlagsTask;
    private CTFGlowManagerTask updateGlowTask;
    private PlayerNoMove playerNoMoveListener;
    private CTFTeleportListener teleportListener;

    private CTFGlowManager glowManager;

    public CTFGame(GameManager gameManager) {
        super(gameManager);
    }

    @Override
    public CBCGamemode getGamemode () {
        return CBCGamemode.CTF;
    }

    @Override
    public CTFTeam createGamemodeTeam (LobbyTeam team, int teamNum) {
        return new CTFTeam(this, team.getTeamId(),
                Integer.toString(teamNum), team.getTeamName(), team.getColor(),
                team.getPrefix(), team.getItem(), team.getGlassHead()
        );
    }

    @Override
    public CTFPlayer createPlayer(Player playerEntity) {
        return new CTFPlayer(this, getGameManager(), getCombatManager(), playerEntity);
    }

    @Override
    public GameSidebarManager createSidebarManager () {
        return new CTFSidebarManager(this);
    }

    @Override
    public GameBossBarManager createBossbarManager () {
        return new CTFBossbarManager(this);
    }

    @Override
    public void setupGame (GameContext ctx) {

        final GameManager gameManager = getGameManager();
        final CombatManager combatManager = getCombatManager();
        final World world = getWorld();

        // Setup map
        CTFMap map = (CTFMap) ctx.getMap();
        setupMap(map);

        // Setup default game variables
        setupDefaultGameVars(ctx.getBoolVars(), ctx.getIntVars(), ctx.getStringVars());

        // Set gamemode information
        createHeaderTitle();

        // Enable weapons
        combatManager.activateWeapons();

        // Setup gamemode game variables
        this.flagsStart = ctx.getIntVars().getOrDefault("flagsStart", 4);
        this.flagsRemovedTimer = ctx.getIntVars().getOrDefault("initialFlagsRemovedTimer", 1800);
        this.flagsRemovedTimerIncrement = ctx.getIntVars().getOrDefault("flagsRemovedTimerAfterFirst", 600);

        // Setup game commands
        setGameCommands(new CTFGameCommands(this));

        // Setup teams/players
        createTeams(ctx.getTeams());
        teleportSpectators();

        // Create random list of integers for randomizing team bases
        List<Integer> randomIntegerList = new ArrayList<>();
        if (randomBases) {
            for (int i = 0; i < randomFlagLocations.size(); i++) randomIntegerList.add(i);
            Collections.shuffle(randomIntegerList);
        }

        int teamNum = 0;
        for (CTFTeam team : getTeams()) {

            // Set team's base variables
            if (randomBases) {
                int index = randomIntegerList.get(teamNum);
                team.setBaseVariables(randomFlagLocations.get(index), randomBaseSpawns.get(index));
            }
            else {
                team.setBaseVariables(nonRandomFlagLocations.get(team.getTeamId()), nonRandomBaseSpawns.get(team.getTeamId()));
            }

            // Teleport all players to their spawn
            for (CTFPlayer player : team.getPlayers()) {
                player.resetPlayer();
                player.teleportPlayerToSpawn(team.getPlayerSpawn(), map.getMapCentre());
            }

            teamNum++;
        }

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

        // Start countdown for the game
        new CTFStartGameTimer(gameManager, this, 11).runTaskTimer(CBCPlugin.getPlugin(), 0, 20);

    }

    @Override
    public void gameWon (CTFTeam team) {
        super.gameWon(team);
        for (CTFPlayer player : team.getPlayers()) {
            player.addGamePoints(40);
        }
        canCaptureOrTake = false;
        if (suddenDeathBorder != null) {
            suddenDeathBorder.deactivateBorder();
        }
    }

    @Override
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

        // Disable glow manager
        glowManager.deactivate();

    }

    @Override
    public PostGameStats getPostGameStats() {
        return new CTFPostGameStats(this);
    }

    public void setupMap (CTFMap map) {

        super.setupMap(map);

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

    public void startGame () {

        getMap().fillBlocksAtEnd();

        if (!getMap().isCanMoveAtGameStart()) {
            PlayerMoveEvent.getHandlerList().unregister(playerNoMoveListener);
            playerNoMoveListener = null;
        }

        // Initialise all players
        for (CTFTeam team : getTeams()) {
            for (CTFPlayer player : team.getPlayers()) {
                player.playerSetup(2);
                player.setTempImmune(60);

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

        // Remove one flag from each team that still has more than 0 flags
        for (CTFTeam team : getTeams()) {
            if (team.getFlagsLeft() > 0) {
                team.removeFlag(null);
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
        for (CTFTeam team : getTeams()) {
            if (team.getFlagsLeft() > 0) {
                flagsRemaining = true;
                break;
            }
        }

        // Check if sudden death should begin
        if (!allFlagsTaken && !flagsRemaining) {
            if (getMap().isSuddenDeathEnabled()) {
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
        for (CBCPlayer player : getPlayers()) {
            if (!player.isAlive()) continue;
            player.healToFull();
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
        for (CTFTeam team : getTeams()) {
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
        for (CTFTeam team : getTeams()) {
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

}
