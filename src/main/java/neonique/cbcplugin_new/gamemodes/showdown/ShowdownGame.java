package neonique.cbcplugin_new.gamemodes.showdown;

import neonique.cbcplugin_new.combat.DeathInfo;
import neonique.cbcplugin_new.core.GameInitContext;
import neonique.cbcplugin_new.core.TeamGame;
import neonique.cbcplugin_new.core.TeamLike;
import neonique.cbcplugin_new.core.CBCGamemode;
import neonique.cbcplugin_new.core.TeamGameContext;
import neonique.cbcplugin_new.gamemodes.showdown.tasks.*;
import neonique.cbcplugin_new.mapconfig.CBCMap;
import neonique.cbcplugin_new.mapconfig.spawns.MapStartSpawn;
import neonique.cbcplugin_new.mapmechanics.HealthPadMechanic;
import neonique.cbcplugin_new.mapmechanics.VoidMechanic;
import neonique.cbcplugin_new.mechanics.DeathBorder;
import neonique.cbcplugin_new.core.CBCPlayer;
import neonique.cbcplugin_new.tasks.gamemodetasks.IncrementGameTimeTask;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import net.kyori.adventure.sound.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.time.Duration;
import java.util.*;

public class ShowdownGame extends TeamGame<ShowdownPlayer, ShowdownTeam> {

    public ShowdownGame(GameInitContext ctx) {
        super(ctx);
    }

    @Override
    public CBCGamemode getGamemode () {
        return CBCGamemode.SHOWDOWN;
    }

    @Override
    public ShowdownTeam createGamemodeTeam (TeamLike team, int teamNum) {
        return new ShowdownTeam(team, Integer.toString(teamNum));
    }

    @Override
    public ShowdownPlayer createPlayer(Player playerEntity) {
        return new ShowdownPlayer(playerEntity, combatSession());
    }

    public enum RoundState {PRE_ROUND, DURING_ROUND, AFTER_ROUND}

    // Map related variables
    private ShowdownMap map;
    private ShowdownSettings settings;

    // Game related variables
    private final List<ShowdownTeam> roundWinOrder = new ArrayList<>();
    private int roundNumber = 0;

    // Set up round related variables
    private Map<ShowdownTeam, List<MapStartSpawn>> roundSpawns = null;
    private RoundState roundState;
    private ShowdownTeam roundWinner = null;
    private boolean suddenDeath = false;
    private DeathBorder suddenDeathBorder = null;

    // Event listeners and tasks
    private ShowdownSDTimer suddenDeathTimer;

    // Statistical variables
    private int gameInRoundTime = 0;
    private int suddenDeathRounds = 0;

    @Override
    public CBCMap getMap () {
        return map.map();
    }

    @Override
    public void setupGame (TeamGameContext ctx) {

        // Create teams and players
        List<? extends TeamLike> teamTemplates = ctx.teams();
        createTeams(teamTemplates);

        // Set up settings and map
        settings = (ShowdownSettings) ctx.gameSettings();
        setupMap(ctx);

        // TODO: Set gamemode information
        // createHeaderTitle();

        // Activate combat manager
        setupCombat();

        // TODO: Setup game commands
        // setGameCommands(new TagGameCommands(this));

        // TODO: Create Bossbar/Sidebar managers
        // createUIManagers();

        // Start first round
        setupRound();

    }

    private void setupCombat () {
        combatSession().activate();
        combatSession().setupMap(getMap());
        combatSession().setDeathListener(this::onPlayerDeath);
        combatSession().setJoinAfterDeathListener(this::joinAfterDeath);
        combatSession().setRespawnListener(this::onPlayerRespawn);
    }

    private void setupMap (TeamGameContext ctx) {
        map = new ShowdownMap(world(), (ShowdownMapData) ctx.mapData());
    }

    public void setupRound () {

        roundState = RoundState.PRE_ROUND;
        roundWinner = null;
        roundNumber++;

        // Reset sudden death variables
        suddenDeath = false;
        suddenDeathBorder = null;

        // TODO: Update footer
        // createFooter();

        // Setup map for start of round
        combatSession().mapMechanicsManager().getMechanicsOfType(HealthPadMechanic.class).forEach(
                HealthPadMechanic::enableAll
        );

        // TODO: fill blocks at start
        // this.getMap().fillBlocksAtStart();

        // Setup spawns
        roundSpawns = map.roundSpawns(getTeams());
        for (List<MapStartSpawn> spawns : roundSpawns.values()) {
            spawns.forEach(MapStartSpawn::onSetup);
        }

        // Teleport players to spawns
        for (ShowdownTeam team : getTeams()) {
            team.teleportPlayers(roundSpawns.get(team), getMap().getMapCentre());
        }

        // Reset team variables
        for (ShowdownTeam team : getTeams()) {
            team.setupRound();
        }

        // Start countdown for start of next round
        startRoundTimer();

    }

    public void startRoundTimer () {
        new ShowdownStartRoundTimer(
                this,
                this::players,
                this::spectators,
                getMap().name(),
                () -> !isGameOver() && roundState == RoundState.PRE_ROUND,
                this::startRound,
                roundNumber
        ).start(plugin());
    }

    public void startRound () {

        roundState = RoundState.DURING_ROUND;

        // Activate void
        combatSession().mapMechanicsManager().getMechanicsOfType(VoidMechanic.class).forEach(
                VoidMechanic::deactivate
        );

        // Reset spawns
        for (List<MapStartSpawn> spawns : roundSpawns.values()) {
            spawns.forEach(MapStartSpawn::reset);
        }

        for (ShowdownPlayer player : players()) {
            player.playerStartRound();
        }

        startTimeAliveTracker();

        if (map.suddenDeathEnabled()) {
            startSuddenDeathTimer();
        }

        if (roundNumber == 1) {
            new IncrementGameTimeTask(this).runTaskTimer(plugin(), 20, 20);
        }

        updatePlayerCounts();

    }

    private void startTimeAliveTracker () {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (isGameOver()) cancel();
                if (roundState != RoundState.DURING_ROUND) cancel();
                for (ShowdownPlayer player : players()) {
                    if (player.isAlive()) player.incrementPlayerSecondsAlive();
                }
            }
        }.runTaskTimer(plugin(), 20, 20);
    }

    private void startSuddenDeathTimer () {
        suddenDeathTimer = new ShowdownSDTimer(
                this,
                map.suddenDeathTimer(),
                i -> {},
                () -> (!isGameOver() && roundState == RoundState.DURING_ROUND),
                this::startSuddenDeath
        );
        suddenDeathTimer.runTaskTimer(plugin(), 20, 20);
    }

    public void onPlayerDeath (DeathInfo deathInfo) {

        CBCPlayer victim = deathInfo.victim();
        victim.showTitle(getDeathTitle(deathInfo.victim(), deathInfo.killer()));

        // Check if round has ended
        if (roundState == RoundState.DURING_ROUND) {
            updatePlayerCounts();
        }

    }

    public void onPlayerRespawn (CBCPlayer player) {
        ShowdownTeam playerTeam = getPlayerTeam(getTypedPlayer(player));
        player.teleportPlayerToSpawn(roundSpawns.get(playerTeam).getFirst().location(), getMap().getMapCentre());
    }

    public void joinAfterDeath (CBCPlayer victim) {
        victim.showTitle(getDeathTitle(victim, null));
    }

    private Title getDeathTitle (CBCPlayer victim, CBCPlayer killer) {

        return Title.title(
            Component.text()
                    .content("YOU DIED!")
                    .color(NamedTextColor.RED)
                    .build(),
            Component.text()
                    .content("You've been eliminated!")
                    .color(NamedTextColor.YELLOW)
                    .build(),
            Title.Times.times(
                    Duration.ofMillis(0),
                    Duration.ofMillis(1500),
                    Duration.ofMillis(500)
            )
        );

    }

    public void updatePlayerCounts() {

        List<ShowdownTeam> teamsAlive = new ArrayList<>();
        for (ShowdownTeam team : getTeams()) {
            if (!team.isTeamAlive()) continue; // Make sure team is alive
            int teamPlayerCount = team.updatePlayersLeftAlive(true);
            if (teamPlayerCount > 0) {
                teamsAlive.add(team);
            } else {
                if (team.isTeamAlive()) {
                    eliminateTeam(team);
                }
            }
        }

        if (teamsAlive.size() == 1) {
            teamWonRound(teamsAlive.getFirst());
        }

    }

    public void eliminateTeam (ShowdownTeam team) {
        team.eliminateTeam();
        sendMessage(
                Component.text("TEAM ELIMINATED > ").decorate(TextDecoration.BOLD).color(NamedTextColor.WHITE)
                        .append(team.nameComponent().decorate(TextDecoration.BOLD))
                        .append(Component.text(" has been eliminated!").decoration(TextDecoration.BOLD, TextDecoration.State.FALSE).color(NamedTextColor.WHITE))
        );
    }

    public void reviveTeam (ShowdownTeam team) {
        team.reviveTeam();
        sendMessage(
                Component.text("TEAM ELIMINATED > ").decorate(TextDecoration.BOLD).color(NamedTextColor.WHITE)
                        .append(team.nameComponent().decorate(TextDecoration.BOLD))
                        .append(Component.text(" has been revived!").decoration(TextDecoration.BOLD, TextDecoration.State.FALSE).color(NamedTextColor.WHITE))
        );
    }

    public void teamWonRound (ShowdownTeam team) {

        // Increment team count
        team.teamWonRound();

        // Add team to list
        roundWinner = team;
        roundWinOrder.add(team);

        // Update footer
        updateFooter();

        // Check if team has reached the required amount of points
        if (team.getRoundsWon() >= settings.roundsToWin()) {
            gameWon(team);
            roundOver(false);
        } else {
            showRoundWinTitle(team);
            sendRoundWinMessage(team);
            playSound(Sound.sound(org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, Sound.Source.MASTER,200, 0));
            roundOver(true);
        }

    }

    private void showRoundWinTitle (ShowdownTeam team) {

        showTitle(Title.title(
            Component.text(team.name().toUpperCase() + " WINS ROUND " + roundNumber + "!")
                    .decorate(TextDecoration.BOLD)
                    .color(team.textColor()),
            Component.space(),
            Title.Times.times(Duration.ofMillis(0), Duration.ofMillis(3000), Duration.ofMillis(500))
        ));

    }

    private void sendRoundWinMessage (ShowdownTeam team) {
        sendMessage(
                Component.newline()
                        .append(Component.text("ROUND WIN > ").decorate(TextDecoration.BOLD).color(NamedTextColor.WHITE))
                        .append(Component.text(team.name()).decorate(TextDecoration.BOLD).color(team.textColor()))
                        .append(Component.text(" has won the round!").color(NamedTextColor.WHITE))
                        .append(Component.newline())
        );
    }

    public void roundOver (boolean startNextRound) {

        roundState = RoundState.AFTER_ROUND;

        // Set all alive players to immune
        for (ShowdownPlayer player : players()) {
            if (player.isAlive()) {
                player.setPermanentlyImmune(true);
            }
        }

        // Make the void do nothing
        combatSession().mapMechanicsManager().getMechanicsOfType(VoidMechanic.class).forEach(
                VoidMechanic::deactivate
        );

        cleanupRound();

        // Start timer for next round
        if (startNextRound) {
            startNextRoundTimer();
        }

    }

    private void startNextRoundTimer () {
        new ShowdownNextRoundTimer(
                this,
                10,
                () -> isGameOver() && roundState == RoundState.AFTER_ROUND,
                this::setupRound
        ).runTaskTimer(plugin(), 20, 20);
    }

    private void cleanupRound () {

        cancelTask(suddenDeathTimer);

        if (suddenDeathBorder != null) {
            if (suddenDeathBorder.isActive()) {
                suddenDeathBorder.deactivateBorder();
            }
            suddenDeathBorder = null;
        }

    }

    @Override
    public void gameWon (ShowdownTeam team) {

        super.gameWon(team);

        // Add bonus points for winning
        for (CBCPlayer player : team.players()) {
            player.addGamePoints(40);
        }

        // End round and do not start the next round
        roundOver(false);

    }

    @Override
    public void gameCleanup () {
        cleanupRound();
    }

    public int getRoundNumber() {
        return roundNumber;
    }

    public boolean isRoundNotInPlay() {
        return roundState == RoundState.DURING_ROUND;
    }

    public int getRoundsToWin() {
        return settings.roundsToWin();
    }

    public void startSuddenDeath () {

        suddenDeath = true;
        suddenDeathRounds++;

        // Title and sound
        showTitle(Title.title(
                Component.space(),
                Component.text("Sudden Death has started!").color(NamedTextColor.RED),
                Title.Times.times(Duration.ofMillis(0), Duration.ofMillis(1500), Duration.ofMillis(500))
        ));
        playSound(Sound.sound(org.bukkit.Sound.ENTITY_BEE_STING, Sound.Source.MASTER, 200, 1));

        // Disable heal pads
        combatSession().mapMechanicsManager().getMechanicsOfType(HealthPadMechanic.class).forEach(
                HealthPadMechanic::disableAll
        );

        // Heal all players
        for (CBCPlayer player : players()) {
            if (player.isAlive()) player.healToFull();
        }

        // If sudden death border is enabled activate the border
        if (map.deathBorderOptions() != null) {
            suddenDeathBorder = new DeathBorder(
                    plugin(),
                    this,
                    getMap().getMapCentre(),
                    map.deathBorderOptions()
            );
            suddenDeathBorder.activateBorder();
        }

    }

    public boolean isSuddenDeath() {
        return suddenDeath;
    }

    public int getSuddenDeathTimer() {
        return suddenDeathTimer != null ? suddenDeathTimer.getSecs() : 0;
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
        if (roundState == RoundState.DURING_ROUND) {
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
            if (roundState == RoundState.AFTER_ROUND) {
                player.playerSetupRound();
                player.teleportPlayerToSpawn(roundSpawns.get(team).getFirst().location(), this.getMap().getMapCentre());
            }
        }
    }

    public void updateFooter () {
        sendPlayerListFooter(createFooter());
    }

    public Component createFooter () {

        int maxRounds = (settings.roundsToWin() - 1) * getTeams().size() + 1;

        TextComponent.Builder footer = Component.text()
                .append(Component.newline())
                .append(smallText("Round " + roundNumber + " ").color(NamedTextColor.AQUA).decorate(TextDecoration.BOLD));

        for (int rd = 1; rd <= maxRounds; rd++) {
            if (roundWinOrder.size() >= rd) {
                footer.append(smallText("■").color(roundWinOrder.get(rd - 1).textColor()));
            } else {
                if (rd == roundNumber) {
                    footer.append(smallText("□").color(NamedTextColor.WHITE));
                } else {
                    footer.append(smallText("□").color(NamedTextColor.GRAY));
                }
            }
        }

        return footer.build();

    }

    public ShowdownTeam getRoundWinner () {
        return roundWinner;
    }

}
