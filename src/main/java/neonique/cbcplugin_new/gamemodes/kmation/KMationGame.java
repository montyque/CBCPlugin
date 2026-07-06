package neonique.cbcplugin_new.gamemodes.kmation;

import neonique.cbcplugin_new.CBCPlugin;
import neonique.cbcplugin_new.core.FFAGame;
import neonique.cbcplugin_new.gamemodes.CBCGamemode;
import neonique.cbcplugin_new.gamemodes.FFAGameContext;
import neonique.cbcplugin_new.gamemodes._base.*;
import neonique.cbcplugin_new.listeners.gamemodes.PlayerNoMove;
import neonique.cbcplugin_new.managers.GameBossBarManager;
import neonique.cbcplugin_new.managers.GameManager;
import neonique.cbcplugin_new.combat.CombatManager;
import neonique.cbcplugin_new.scoreboard.CBCScoreboardManager;
import neonique.cbcplugin_new.scoreboard.CBCScoreboardTeam;
import neonique.cbcplugin_new.tasks.gamemodetasks.IncrementGameTimeTask;
import neonique.cbcplugin_new.gamemodes.kmation.tasks.KMationCycleTimerTask;
import neonique.cbcplugin_new.gamemodes.kmation.tasks.KMationStartGameTimer;
import neonique.cbcplugin_new.util.StringUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerMoveEvent;

import java.time.Duration;
import java.util.*;

public class KMationGame extends FFAGame<KMationPlayer, KMationMap> {

    // Game related variables
    private int cycleNumber;
    private int cycleTimer;
    private boolean cycleTimerEnabled = false;
    private boolean finalCycle = false;
    private List<KMationPlayer> playersInLast = new ArrayList<>();
    private HashMap<Integer, Set<KMationPlayer>> placements = new HashMap<>();

    // Overtime related variables
    private boolean overtime;
    private int overtimeThreshold;

    // Game related constants
    private int maxPlayersInFinalCycle;
    private int cycleLength;

    // Map related variables
    private List<KMationSpawn> spawns;

    // Teams - for display only
    private CBCScoreboardTeam safeTeam;
    private CBCScoreboardTeam dangerTeam;
    private CBCScoreboardTeam eliminatedTeam;

    // Listeners and tasks
    private KMationCycleTimerTask cycleTimerTask;
    private PlayerNoMove noMoveListener;

    public KMationGame (GameManager gameManager) {
        super(gameManager);
    }

    @Override
    public CBCGamemode getGamemode () {
        return CBCGamemode.KMATION;
    }

    @Override
    public KMationPlayer createPlayer(Player playerEntity) {
        return new KMationPlayer(this, getGameManager(), getCombatManager(), playerEntity);
    }

    @Override
    public GameSidebarManager createSidebarManager () {
        return new KMationSidebarManager(getGameManager(), getCombatManager(), this);
    }

    @Override
    public GameBossBarManager createBossbarManager () {
        return new KMationBossbarManager(this);
    }

    @Override
    public void setupGame (FFAGameContext ctx) {

        final GameManager gameManager = getGameManager();
        final CombatManager combatManager = getCombatManager();

        // Setup map
        setupMap((KMationMap) ctx.getMap());

        // Setup default game variables
        setupDefaultGameVars(ctx.getBoolVars(), ctx.getIntVars(), ctx.getStringVars());

        // Set gamemode information
        createHeaderTitle();

        // Enable weapons
        combatManager.activate(this);

        // Setup gamemode game variables
        this.maxPlayersInFinalCycle = ctx.getIntVars().getOrDefault("maxPlayersInFinalCycle", 4);
        this.cycleLength = ctx.getIntVars().getOrDefault("cycleLength", 60);

        // Set timer
        this.cycleTimer = this.cycleLength;
        // Set cycle number to 1
        this.cycleNumber = 1;

        // setGameCommands(new ShowdownGameCommands(gameManager, weaponManager, this));
        // Create players
        createPlayers(ctx.getPlayers());
        teleportSpectators();

        List<KMationSpawn> gameStartSpawns = sortSpawns();
        List<KMationPlayer> shuffledPlayers = getPlayers();
        Collections.shuffle(shuffledPlayers);
        int spawnNum = 0;

        for (KMationPlayer player : shuffledPlayers) {
            if (!player.isOnline()) continue;
            player.teleportPlayerToSpawn(gameStartSpawns.get(spawnNum), this.getMap().getMapCentre());
            player.playerSetupGame();
            spawnNum++;
        }

        // Create teams
        CBCScoreboardManager scoreboardManager = gameManager.getCbcScoreboardManager();

        // Create safe team
        safeTeam = scoreboardManager.registerNewTeam("01safe");
        safeTeam.setSeeFriendlyInvisiblesEnabled(false);
        safeTeam.setFriendlyFireEnabled(true); // Do not allow friendly fire
        safeTeam.setColor(NamedTextColor.GREEN); // Set team color

        // Create danger team
        dangerTeam = scoreboardManager.registerNewTeam("02danger");
        dangerTeam.setSeeFriendlyInvisiblesEnabled(false);
        dangerTeam.setFriendlyFireEnabled(true); // Do not allow friendly fire
        dangerTeam.setColor(NamedTextColor.YELLOW); // Set team color

        // Create eliminated team
        eliminatedTeam = scoreboardManager.registerNewTeam("03dead");
        eliminatedTeam.setSeeFriendlyInvisiblesEnabled(false);
        eliminatedTeam.setFriendlyFireEnabled(true); // Do not allow friendly fire
        eliminatedTeam.setColor(NamedTextColor.RED); // Set team color

        // Check if game starts in final cycle
        if (getPlayers().size() <= maxPlayersInFinalCycle) finalCycle = true;

        // Set list of players in last
        updatePlayersInLast();
        updatePlacements();

        // Create sidebar manager
        createUIManagers();

        // Setup listeners
        CBCPlugin plugin = CBCPlugin.getPlugin();

        // Make it so players cannot move
        noMoveListener = new PlayerNoMove(gameManager);
        plugin.getServer().getPluginManager().registerEvents(noMoveListener, plugin);

        // Start countdown timer
        new KMationStartGameTimer(gameManager, this, 11).runTaskTimer(plugin, 0, 20);

    }

    @Override
    public void resetGame() {

        super.resetGame();

        PlayerMoveEvent.getHandlerList().unregister(noMoveListener);

        // Cancel tasks
        cancelTask(cycleTimerTask);

        // Unregister teams
        CBCScoreboardManager.getInstance().unregisterTeam(safeTeam);
        CBCScoreboardManager.getInstance().unregisterTeam(dangerTeam);
        CBCScoreboardManager.getInstance().unregisterTeam(eliminatedTeam);

    }

    @Override
    public PostGameStats getPostGameStats() {
        return new KMationPostGameStats(this);
    }

    public void startGame () {

        this.getMap().fillBlocksAtEnd();

        // Allow players to move
        PlayerMoveEvent.getHandlerList().unregister(noMoveListener);
        noMoveListener = null;

        // Initialise all players
        for (KMationPlayer player : getPlayers()) {
            if (!player.isOnline()) return;
            player.playerStartGame();
        }

        // Start cycle timer
        cycleTimerEnabled = true;
        cycleTimerTask = new KMationCycleTimerTask(this);
        cycleTimerTask.runTaskTimer(CBCPlugin.getPlugin(), 20, 20);

        // Set list of players in last
        updatePlayersInLast();
        updatePlacements();

        // Update sidebar
        updateServerSidebar();

        new IncrementGameTimeTask(this).runTaskTimer(CBCPlugin.getPlugin(), 20, 20);
    }

    public void decrementCycleTimer() {

        if (!cycleTimerEnabled) {
            return;
        }

        cycleTimer--;

        if (cycleTimer == 30) {
            timerMessage(Component.text("30 seconds remain!").color(NamedTextColor.RED).decorate(TextDecoration.BOLD), 1);
        } else if (cycleTimer == 15) {
            timerMessage(Component.text("15 SECONDS REMAIN!").color(NamedTextColor.RED).decorate(TextDecoration.BOLD), 2);
        } else if (cycleTimer <= 5 && cycleTimer > 1) {
            timerMessage(Component.text(cycleTimer + " SECONDS REMAIN!").color(NamedTextColor.RED).decorate(TextDecoration.BOLD), 2);
        } else if (cycleTimer == 1) {
            timerMessage(Component.text("1 SECOND REMAINS!").color(NamedTextColor.RED).decorate(TextDecoration.BOLD), 2);
        } else if (cycleTimer == 0) {
            // Check for winner of game
            endCycle();
        }

        updateBossbarManager();
    }

    public void endCycle() {

        cycleTimerEnabled = false;

        // Get players currently in last
        int playersInCycle = getKMationPlayersInGame().size();
        playersInLast = getPlayersInLast();
        placements = getPlacements();

        if (!overtime) {
            if (playersInLast.size() == getKMationPlayersInGame().size() || playersInLast.isEmpty()) {
                startOvertime();
                return;
            }
        }

        // Eliminate players
        for (KMationPlayer player : playersInLast) {
            eliminatePlayer(player);
        }

        // Send message
        Component endOfCycleMessage = Component.text("Cycle " + cycleNumber + " has ended! ").color(NamedTextColor.GREEN).decorate(TextDecoration.BOLD);
        endOfCycleMessage = endOfCycleMessage.append(Component.newline());
        if (playersInLast.size() == 1) {
            endOfCycleMessage = endOfCycleMessage.append(
                    Component.text("").append(
                            Component.text(playersInLast.size()).color(NamedTextColor.RED).decorate(TextDecoration.BOLD)
                    ).append(
                            Component.text(" player was eliminated.").color(NamedTextColor.WHITE)
                    )
            );
        } else {
            endOfCycleMessage = endOfCycleMessage.append(
                    Component.text("").append(
                            Component.text(playersInLast.size()).color(NamedTextColor.RED).decorate(TextDecoration.BOLD)
                    ).append(
                            Component.text(" players were eliminated.").color(NamedTextColor.WHITE)
                    )
            );
        }
        getGameManager().sendGlobalMessage(endOfCycleMessage.append(Component.newline()));

        // Send message to all players in the cycle
        for (Integer placement : getPlacements().keySet()) {
            Set<KMationPlayer> playersInPlacement = getPlacements().get(placement);
            for (KMationPlayer player : playersInPlacement) {

                if (!player.isOnline()) continue;

                NamedTextColor highlightColor = NamedTextColor.GREEN;
                String survivedOrEliminated = "SURVIVED > ";
                if (playersInLast.contains(player)) {
                    highlightColor = NamedTextColor.RED;
                    survivedOrEliminated = "ELIMINATED > ";
                }

                // Create placement string
                String tieString = "";
                String placementString = StringUtil.getPlacementString(placement);

                if (playersInPlacement.size() > 1) {
                    tieString = " (" + playersInPlacement.size() + " way tie) ";
                }

                String killString = " kills.";
                if (player.getKills() == 1) {
                    killString = " kill.";
                }

                // Send message
                player.getPlayer().sendMessage(
                        Component.text("").append(
                                Component.text(survivedOrEliminated).color(highlightColor).decorate(TextDecoration.BOLD)
                        ).append(
                                Component.text("You placed ").color(NamedTextColor.WHITE)
                        ).append(
                                Component.text(placementString).color(highlightColor).decorate(TextDecoration.BOLD)
                        ).append(
                                Component.text(tieString).color(highlightColor).decorate(TextDecoration.ITALIC)
                        ).append(
                                Component.text(" out of ").color(NamedTextColor.WHITE)
                        ).append(
                                Component.text(playersInCycle).color(highlightColor).decorate(TextDecoration.BOLD)
                        ).append(
                                Component.text(" with ").color(NamedTextColor.WHITE)
                        ).append(
                                Component.text(player.getKills()).color(highlightColor).decorate(TextDecoration.BOLD)
                        ).append(
                                Component.text(killString).color(NamedTextColor.WHITE)
                        ).append(Component.newline())
                );
            }
        }

        // Eliminate players if not in final cycle
        if (!finalCycle) {
            // Play sound
            getGameManager().playGlobalSound(Sound.BLOCK_END_PORTAL_SPAWN, 100, 2);
        }

        // Check if there is only one player left
        if (getKMationPlayersInGame().size() == 1) {
            playerWonGame(getKMationPlayersInGame().iterator().next());
        } else {
            startNextCycle();
        }
    }

    public void startNextCycle() {

        cycleTimerEnabled = true;

        cycleNumber++;
        this.cycleTimer = this.cycleLength;

        for (KMationPlayer player : getKMationPlayersInGame()) {
            player.playerResetCycle();
        }

        if (getKMationPlayersInGame().size() <= maxPlayersInFinalCycle) {
            finalCycle = true;
        }

        // Send message
        getGameManager().sendGlobalMessage(
                Component.text("Cycle " + cycleNumber + " has begun! ").color(NamedTextColor.GREEN).decorate(TextDecoration.BOLD).append(
                        Component.text(getKMationPlayersInGame().size()).color(NamedTextColor.GREEN)
                ).append(
                        Component.text( " players remain.").color(NamedTextColor.WHITE)
                )
        );

        if (finalCycle) {
            getGameManager().sendGlobalMessage(
                    Component.text( "This is the ").color(NamedTextColor.WHITE).append(
                            Component.text("FINAL CYCLE").color(NamedTextColor.RED)
                    ).append(
                            Component.text( ". The player in first place at the end of the cycle ").color(NamedTextColor.WHITE)
                    ).append(
                            Component.text( "wins the game!").color(NamedTextColor.WHITE).decorate(TextDecoration.BOLD)
                    )
            );
        }

        // Update players in last and placements
        updatePlayersInLast();
        updatePlacements();

        // Update sidebar
        updateServerSidebar();

    }

    public void startOvertime() {

        final GameManager gameManager = getGameManager();

        overtime = true;

        // Find the highest kill amount, set overtime threshold to that plus two
        int highestKills = 0;
        for (KMationPlayer player : getKMationPlayersInGame()) {
            if (player.getCycleKills() > highestKills) {
                highestKills = player.getCycleKills();
            }
        }

        overtimeThreshold = highestKills + 2;

        // Play sound
        gameManager.playGlobalSound(Sound.ENTITY_WITHER_DEATH, 100, 1);

        // Play title
        Title overtimeTitle = Title.title(
                Component.text("OVERTIME").color(NamedTextColor.RED).decorate(TextDecoration.BOLD),
                Component.text("First player to reach ").color(NamedTextColor.WHITE).append(
                        Component.text(overtimeThreshold).color(NamedTextColor.RED).decorate(TextDecoration.BOLD)
                ).append(
                        Component.text(" wins!").color(NamedTextColor.WHITE)
                ),
                Title.Times.times(Duration.ofMillis(0), Duration.ofMillis(1250), Duration.ofMillis(500))
        );

        gameManager.sendGlobalTitle(overtimeTitle);

        // Send message of game win
        gameManager.sendGlobalMessage(
                Component.newline()
                        .append(Component.text("OVERTIME > ").decorate(TextDecoration.BOLD).color(NamedTextColor.WHITE))
                        .append(Component.text("First player to reach ").color(NamedTextColor.WHITE))
                        .append(Component.text(overtimeThreshold).color(NamedTextColor.RED).decorate(TextDecoration.BOLD))
                        .append(Component.text(" wins the game!").color(NamedTextColor.WHITE))
                        .append(Component.newline())
        );
    }

    public void eliminatePlayer(KMationPlayer player) {

        player.eliminatePlayer();
        eliminatedTeam.addEntityUUID(player.getUUID());

        // Send message
        getGameManager().sendGlobalMessage(
                Component.text("").append(
                        Component.text("❌ > ").decorate(TextDecoration.BOLD)
                ).append(
                        Component.text(player.name()).color(NamedTextColor.RED)
                ).append(
                        Component.text(" has been eliminated.").color(NamedTextColor.WHITE)
                )
        );

    }

    public void updatePlayersInLast() {
        List<KMationPlayer> oldPlayersInLast = new ArrayList<>(playersInLast);
        playersInLast = findPlayersInLast();

        // Change teams
        for (KMationPlayer player : oldPlayersInLast) {
            if (!playersInLast.contains(player)) {
                player.setInDanger(false);
                if (player.isEliminated()) {
                    eliminatedTeam.addEntityUUID(player.getUUID());
                } else {
                    safeTeam.addEntityUUID(player.getUUID());
                }
            }
        }

        for (KMationPlayer player : playersInLast) {
            if (!oldPlayersInLast.contains(player)) {
                player.setInDanger(true);
                if (player.isEliminated()) {
                    eliminatedTeam.addEntityUUID(player.getUUID());
                } else {
                    dangerTeam.addEntityUUID(player.getUUID());
                }
            }
        }
    }

    public List<KMationPlayer> findPlayersInLast() {

        List<KMationPlayer> playersInGame = getSortedPlayers(true, false);
        List<KMationPlayer> playersInLast = new ArrayList<>();
        // Sort players by round kills
        if (!finalCycle) {
            // Get the amount of kills the person in last place has
            int killsInLast = playersInGame.get(0).getCycleKills();
            // Get all players with the same amount of kills as killsInLast
            for (KMationPlayer player : playersInGame) {
                if (player.getCycleKills() > killsInLast) break;
                playersInLast.add(player);
            }
        } else {
            Collections.reverse(playersInGame);
            int killsInFirst = playersInGame.get(0).getCycleKills();
            playersInLast = new ArrayList<>(playersInGame);
            if (playersInGame.size() == 1) return new ArrayList<>();
            if (playersInGame.get(0).getCycleKills() == playersInGame.get(1).getCycleKills()) return playersInLast;
            for (KMationPlayer player : playersInGame) {
                if (player.getCycleKills() < killsInFirst) break;
                playersInLast.remove(player);
            }
        }
        return playersInLast;
    }

    public HashMap<Integer, Set<KMationPlayer>> findPlayerPlacements() {

        HashMap<Integer, Set<KMationPlayer>> placements = new HashMap<>();

        // Sort players, including eliminated players
        List<KMationPlayer> playersSorted = getSortedPlayers(false, true);

        int placement = 1;
        int currentKills = 100000;
        int currentTotalKills = 100000;
        int currentCyclesSurvived = 100000;
        boolean currentlyEliminatedPlayers = false;
        int i = 0;

        for (KMationPlayer player : playersSorted) {

            boolean tied = false;

            boolean isCurrentKillsTied = false;
            boolean isCurrentTotalKillsTied = false;
            boolean isCurrentCyclesSurvivedTied = false;

            KMationPlayer nextPlayer;

            if (currentKills == player.getCycleKills()) {
                isCurrentKillsTied = true;
            }
            if (currentTotalKills == player.getKills()) {
                isCurrentTotalKillsTied = true;
            }
            if (currentCyclesSurvived == player.getCyclesSurvived()) {
                isCurrentCyclesSurvivedTied = true;
            }

            // Check if player is eliminated
            if (player.isEliminated() && !currentlyEliminatedPlayers) {
                // First player eliminated
                placement = i + 1;
                currentlyEliminatedPlayers = true;
            }
            else {
                if (isCurrentTotalKillsTied && isCurrentKillsTied && isCurrentCyclesSurvivedTied) {
                    tied = true;
                }
                else {
                    placement = i + 1;
                    if (playersSorted.size() > i + 1) {
                        try {
                            nextPlayer = playersSorted.get(i + 1);
                            if (nextPlayer.getCycleKills() == player.getCycleKills()) {
                                isCurrentKillsTied = true;
                            }
                            if (nextPlayer.getKills() == player.getKills()) {
                                isCurrentTotalKillsTied = true;
                            }
                            if (nextPlayer.getCyclesSurvived() == player.getCyclesSurvived()) {
                                isCurrentCyclesSurvivedTied = true;
                            }
                        } catch (IndexOutOfBoundsException ignored) {}
                        if (isCurrentTotalKillsTied && isCurrentKillsTied && isCurrentCyclesSurvivedTied) {
                            tied = true;
                        }
                    }
                }
            }

            currentKills = player.getCycleKills();
            currentTotalKills = player.getKills();
            currentCyclesSurvived = player.getCyclesSurvived();

            i++;
            player.setPlacement(placement, tied);

        }

        return placements;
    }

    public List<KMationPlayer> getSortedPlayers(boolean descending, boolean includeEliminatedPlayers) {

        // Sort players, including eliminated players
        List<KMationPlayer> playersSorted;

        if (includeEliminatedPlayers) {
            playersSorted = new ArrayList<>(getPlayers());
        } else {
            playersSorted = new ArrayList<>(getKMationPlayersInGame());
        }

        playersSorted.sort(Comparator.comparing(KMationPlayer::isEliminated)
                .thenComparing(Comparator.comparingInt(KMationPlayer::getCyclesSurvived).reversed())
                .thenComparing(Comparator.comparingInt(KMationPlayer::getCycleKills).reversed())
                .thenComparing(Comparator.comparingInt(KMationPlayer::getKills).reversed())
                .thenComparing(KMationPlayer::name)
        );

        if (descending) {
            Collections.reverse(playersSorted);
        }

        return playersSorted;
    }

    public void updatePlacements() {
        placements = findPlayerPlacements();
    }

    public List<KMationPlayer> getPlayersInLast() {
        return playersInLast;
    }

    public HashMap<Integer, Set<KMationPlayer>> getPlacements() {
        return placements;
    }

    public void timerMessage(Component message, float pitch) {

        getGameManager().sendGlobalMessage(message);
        getGameManager().playGlobalSound(Sound.BLOCK_NOTE_BLOCK_BIT, 100, pitch);

    }

    public void setupMap (KMationMap map) {

        super.setupMap(map);
        getCombatManager().setupMap(map);

        // Get spawns
        spawns = map.getKMationSpawns(this);

    }

    public List<KMationSpawn> sortSpawns() {

        List<KMationSpawn> roundSpawnList = new ArrayList<>(spawns);
        List<KMationSpawn> spawnOrder = new ArrayList<>();

        // Select the first spawn
        Comparator<KMationSpawn> byDistanceFromCenter =
                (KMationSpawn loc1, KMationSpawn loc2) -> Double.compare(loc1.distanceSquared(this.getMap().getMapCentre()), loc2.distanceSquared(this.getMap().getMapCentre()));
        roundSpawnList.sort(byDistanceFromCenter);
        Collections.reverse(roundSpawnList);

        spawnOrder.add(roundSpawnList.get(0));
        roundSpawnList.remove(0);

        while (spawnOrder.size() < getPlayers().size()) {
            double minDistanceFromSpawns = 0;
            KMationSpawn spawnSelected = null;
            for (KMationSpawn spawn : new ArrayList<>(roundSpawnList)) {
                double spawnMinDistanceFromSpawns = 300000;
                for (KMationSpawn spawnAlreadySelected : spawnOrder) {
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

    public List<KMationPlayer> getKMationPlayersInGame() {
        return getPlayers().stream().filter(p -> !p.isEliminated()).toList();
    }

    public int getCycleTimer() {
        return cycleTimer;
    }

    public String timerToText() {
        return String.format("%d:%02d", cycleTimer / 60, cycleTimer % 60);
    }

    public int getCycleNumber() {
        return cycleNumber;
    }

    public boolean isFinalCycle() {
        return finalCycle;
    }

    // Get list of spawns
    public List<KMationSpawn> getSpawns() {
        return spawns;
    }

    public int getOvertimeThreshold() {
        return overtimeThreshold;
    }

    public boolean isOvertime() {
        return overtime;
    }

}
