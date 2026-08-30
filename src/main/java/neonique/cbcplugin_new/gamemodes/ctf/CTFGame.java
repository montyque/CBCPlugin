package neonique.cbcplugin_new.gamemodes.ctf;

import neonique.cbcplugin_new.CBCPlugin;
import neonique.cbcplugin_new.core.TeamGame;
import neonique.cbcplugin_new.core.TeamLike;
import neonique.cbcplugin_new.core.CBCGamemode;
import neonique.cbcplugin_new.core.TeamGameContext;
import neonique.cbcplugin_new.gamemodes._base.*;
import neonique.cbcplugin_new.gamemodes.ctf.tasks.CTFGlowManagerTask;
import neonique.cbcplugin_new.gamemodes.ctf.tasks.CTFPlayersNearbyFlags;
import neonique.cbcplugin_new.gamemodes.ctf.tasks.CTFStartGameTimer;
import neonique.cbcplugin_new.mapconfig.CBCMap;
import neonique.cbcplugin_new.mechanics.DeathBorder;
import neonique.cbcplugin_new.listeners.gamemodes.CTFTeleportListener;
import neonique.cbcplugin_new.listeners.gamemodes.PlayerNoMove;
import neonique.cbcplugin_new.managers.GameBossBarManager;
import neonique.cbcplugin_new.managers.GameManager;
import neonique.cbcplugin_new.combat.CombatManager;
import neonique.cbcplugin_new.core.CBCPlayer;
import neonique.cbcplugin_new.tasks.gamemodetasks.IncrementGameTimeTask;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

import java.time.Duration;
import java.util.*;

public class CTFGame extends TeamGame<CTFPlayer, CTFTeam> {

    // Game related variables
    private int flagsStart = 4;
    private boolean canCaptureOrTake = true;
    private int flagsRemovedTimer = 1800;
    private int flagsRemovedTimerIncrement = 600;
    private boolean allFlagsTaken = false;

    // Map information
    private CTFMapData mapData;

    // Sudden death variables
    private boolean suddenDeath = false;
    private DeathBorder suddenDeathBorder = null;

    private double defensiveKillRadius;
    private List<Integer> respawnTimes;

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
    public CTFTeam createGamemodeTeam (TeamLike team, int teamNum) {
        return new CTFTeam(this, team, Integer.toString(teamNum));
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
    public CBCMap getMap () {
        return mapData.mapData();
    }

    @Override
    public void setupGame (TeamGameContext ctx) {

        final GameManager gameManager = getGameManager();
        final CombatManager combatManager = getCombatManager();
        final World world = getWorld();

        // Create teams and players
        List<TeamLike> teamTemplates = ctx.teams();
        createTeams(teamTemplates);

        // Set up game settings
        CTFSettings gameSettings = (CTFSettings) ctx.gameSettings();
        flagsStart = gameSettings.startingFlags();
        flagsRemovedTimer = gameSettings.firstFlagRemovalTimer();
        flagsRemovedTimerIncrement = gameSettings.nextFlagRemovalTimer();

        // Set gamemode information
        // Set up game commands
        setGameCommands(new CTFGameCommands(this));

        // Set up map
        setupMap(ctx);

        // Set gamemode information
        createHeaderTitle();

        // Activate combat manager
        combatManager.activate(this);
        combatManager.setupMap(getMap());

        // Setup teams/players
        setupTeams();
        teleportSpectators();

        playerNoMoveListener = new PlayerNoMove(gameManager);
        CBCPlugin.getPlugin().getServer().getPluginManager().registerEvents(playerNoMoveListener, CBCPlugin.getPlugin());

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

    private void setupMap (TeamGameContext ctx) {
        mapData = (CTFMapData) ctx.mapData();
        defensiveKillRadius = mapData.defensiveKillRadius();
        respawnTimes = mapData.respawnTimers();
        if (mapData.deathBorderInfo() != null) {
            suddenDeathBorder = new DeathBorder(getGameManager(), getMap().getMapCentre(), mapData.deathBorderInfo());
        }
    }

    private void setupTeams () {
        for (CTFTeam team : getTeams()) {
            CTFBase base = mapData.getBase(team.teamColor());
            team.setBaseVariables(base.flagLocation(), base.spawns());
            for (CTFPlayer player : team.players()) {
                player.resetPlayer();
                player.teleportPlayerToSpawn(team.getPlayerSpawn(), getMap().getMapCentre());
            }
        }
    }

    @Override
    public void gameWon (CTFTeam team) {
        super.gameWon(team);
        for (CTFPlayer player : team.players()) {
            player.addGamePoints(40);
        }
        canCaptureOrTake = false;
        if (suddenDeathBorder != null) {
            suddenDeathBorder.deactivateBorder();
        }
    }

    @Override
    public void stop() {

        super.stop();

        // Deactivate sudden death border if existent
        if (suddenDeathBorder != null) {
            suddenDeathBorder.deactivateBorder();
        }

        // If players are unable to move, disable the function
        PlayerMoveEvent.getHandlerList().unregister(playerNoMoveListener);
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

    public void startGame () {

        this.getMap().fillBlocksAtEnd();

        PlayerMoveEvent.getHandlerList().unregister(playerNoMoveListener);

        // Initialise all players
        for (CTFTeam team : getTeams()) {
            for (CTFPlayer player : team.players()) {
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
            startSuddenDeath();
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
        for (CBCPlayer player : this.players()) {
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

    public double getDefensiveKillRadius() {
        return defensiveKillRadius;
    }

    public int getRespawnTime(int teamAmount) {
        return respawnTimes.get(Math.min(respawnTimes.size() - 1, teamAmount - 1));
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
