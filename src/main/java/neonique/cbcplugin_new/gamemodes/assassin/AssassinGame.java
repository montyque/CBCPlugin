package neonique.cbcplugin_new.gamemodes.assassin;

import neonique.cbcplugin_new.CBCPlugin;
import neonique.cbcplugin_new.core.FFAGame;
import neonique.cbcplugin_new.core.CBCGamemode;
import neonique.cbcplugin_new.core.FFAGameContext;
import neonique.cbcplugin_new.gamemodes._base.*;
import neonique.cbcplugin_new.listeners.gamemodes.PlayerNoMove;
import neonique.cbcplugin_new.managers.GameBossBarManager;
import neonique.cbcplugin_new.managers.GameManager;
import neonique.cbcplugin_new.combat.CombatManager;
import neonique.cbcplugin_new.mapconfig.CBCMap;
import neonique.cbcplugin_new.resourcepack.ResourcePackManager;
import neonique.cbcplugin_new.scoreboard.CBCScoreboardManager;
import neonique.cbcplugin_new.scoreboard.CBCScoreboardTeam;
import neonique.cbcplugin_new.tasks.gamemodetasks.IncrementGameTimeTask;
import neonique.cbcplugin_new.gamemodes.assassin.tasks.AssassinGlowUpdateTask;
import neonique.cbcplugin_new.gamemodes.assassin.tasks.AssassinStartGameTimer;
import neonique.cbcplugin_new.gamemodes.assassin.tasks.AssassinTargetChangeTimer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.*;

public class AssassinGame extends FFAGame<AssassinPlayer> {

    // Map related variables
    private AssassinMapData mapData;
    private List<AssassinSpawn> spawns;
    private double targetSpawnDistance;

    // Game related variables
    private int targetsToKill;
    private int targetChangeTimer;

    // Scoreboard variables
    private CBCScoreboardTeam inGameTeam;
    private final ResourcePackManager resourcePackManager;

    // Listeners and tasks
    private PlayerNoMove noMoveListener;

    // Glow manager for making one player glow
    private AssassinGlowManager glowManager;
    private AssassinGlowUpdateTask glowUpdateTask;

    public AssassinGame(GameManager gameManager) {
        super(gameManager);
        resourcePackManager = CBCPlugin.getResourcePackManager();
    }

    @Override
    public CBCGamemode getGamemode () {
        return CBCGamemode.ASSASSIN;
    }

    @Override
    public AssassinPlayer createPlayer(Player playerEntity) {
        return new AssassinPlayer(this, getGameManager(), getCombatManager(), playerEntity);
    }

    @Override
    public GameSidebarManager createSidebarManager() {
        return new AssassinSidebarManager(this);
    }

    @Override
    public GameBossBarManager createBossbarManager() {
        return new AssassinBossbarManager(this);
    }

    @Override
    public CBCMap getMap() {
        return mapData.mapData();
    }

    @Override
    public void setupGame (FFAGameContext ctx) {

        final GameManager gameManager = getGameManager();
        final CombatManager combatManager = getCombatManager();
        final World world = getWorld();

        // Create players
        createPlayers(ctx.players());

        // Set up game settings
        AssassinSettings gameSettings = (AssassinSettings) ctx.gameSettings();
        targetsToKill = gameSettings.targetsToKill();
        targetChangeTimer = gameSettings.targetChangeTimer();

        // Setup map
        setupMap(ctx);

        // Set gamemode information
        createHeaderTitle();

        // Enable weapons
        combatManager.activate(this);

        // Teleport players to spawns
        setupPlayers();
        updatePlacements();

        // Create scoreboard team to mark players in game
        CBCScoreboardManager sbManager = gameManager.getCbcScoreboardManager();
        inGameTeam = new CBCScoreboardTeam(sbManager, "01players");
        inGameTeam.setColor(NamedTextColor.AQUA);
        inGameTeam.setSeeFriendlyInvisiblesEnabled(false);
        inGameTeam.setFriendlyFireEnabled(true);
        sbManager.registerTeam(inGameTeam);

        // Create Bossbar/Sidebar managers
        createUIManagers();

        // Set up glow manager
        glowManager = new AssassinGlowManager(world, this);
        glowManager.activate();
        glowUpdateTask = new AssassinGlowUpdateTask(this);
        glowUpdateTask.runTaskTimer(CBCPlugin.getPlugin(), 0, 10);

        // Set player's targets
        for (AssassinPlayer player : this.players()) {
            player.newTarget(false);
            inGameTeam.addEntityUUID(player.getUUID());
        }

        // Setup listeners
        CBCPlugin plugin = CBCPlugin.getPlugin();

        // Make it so players cannot move
        noMoveListener = new PlayerNoMove(gameManager);
        plugin.getServer().getPluginManager().registerEvents(noMoveListener, plugin);

        // Start countdown timer
        new AssassinStartGameTimer(gameManager, this, 11).runTaskTimer(plugin, 0, 20);

    }

    private void setupPlayers () {

        List<AssassinSpawn> gameStartSpawns = sortSpawns();
        List<AssassinPlayer> shuffledPlayers = this.players();
        Collections.shuffle(shuffledPlayers);
        for (int spawnNum = 0; spawnNum < shuffledPlayers.size(); spawnNum++) {
            AssassinSpawn spawn = gameStartSpawns.get(spawnNum);
            AssassinPlayer player = shuffledPlayers.get(spawnNum);
            player.teleportPlayerToSpawn(spawn, getMap().getMapCentre());
            player.playerSetupGame();
        }

    }

    private void setupMap (FFAGameContext ctx) {
        mapData = (AssassinMapData) ctx.mapData();
        spawns = mapData.getAssassinSpawns(this);
        targetSpawnDistance = mapData.spawnTargetRadius();
    }

    public void startGame () {

        // Allow players to move
        noMoveListener = null;

        // Initialise all players
        for (AssassinPlayer player : this.players()) {
            if (!player.isOnline()) return;
            player.playerStartGame();
        }

        new IncrementGameTimeTask(this).runTaskTimer(CBCPlugin.getPlugin(), 20, 20);

        // Timer that changes target of players if they fail to kill their target within a certain time period
        AssassinTargetChangeTimer targetChangeTimerTask = new AssassinTargetChangeTimer(this);
        targetChangeTimerTask.runTaskTimer(CBCPlugin.getPlugin(), 20, 20);

    }

    public List<AssassinSpawn> sortSpawns() {

        List<AssassinSpawn> roundSpawnList = new ArrayList<>(spawns);
        List<AssassinSpawn> spawnOrder = new ArrayList<>();

        // Select the first spawn
        Comparator<AssassinSpawn> byDistanceFromCenter =
                (AssassinSpawn loc1, AssassinSpawn loc2) -> Double.compare(loc1.distanceSquared(this.getMap().getMapCentre()), loc2.distanceSquared(this.getMap().getMapCentre()));
        roundSpawnList.sort(byDistanceFromCenter);
        Collections.reverse(roundSpawnList);

        spawnOrder.add(roundSpawnList.get(0));
        roundSpawnList.remove(0);

        while (spawnOrder.size() < this.players().size()) {
            double minDistanceFromSpawns = 0;
            AssassinSpawn spawnSelected = null;
            for (AssassinSpawn spawn : new ArrayList<>(roundSpawnList)) {
                double spawnMinDistanceFromSpawns = 300000;
                for (AssassinSpawn spawnAlreadySelected : spawnOrder) {
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

    public List<AssassinPlayer> getPlayersByTargets () {

        List<AssassinPlayer> sortedPlayerList = new ArrayList<>(this.players());
        sortedPlayerList.sort(Comparator.comparingInt(AssassinPlayer::getTargetsLeft));

        return sortedPlayerList;

    }

    public void updatePlacements () {

        List<AssassinPlayer> playersByTargets = getPlayersByTargets();

        int placement = 0;
        int currentScore = -1;
        int i = 0;

        for (AssassinPlayer player : playersByTargets) {

            boolean tied = false;
            if (player.getTargetsLeft() > currentScore) {
                placement = i + 1;
                currentScore = player.getTargetsLeft();
                if (playersByTargets.size() - 1 != i) {
                    if (playersByTargets.get(i + 1).getTargetsLeft() == currentScore) {
                        tied = true;
                    }
                }
            }
            else if (currentScore == player.getTargetsLeft()) {
                tied = true;
            }

            player.setPlacement(placement, tied);
            i++;
        }
    }

    public void resetGame() {

        super.resetGame();

        PlayerMoveEvent.getHandlerList().unregister(noMoveListener);

        // Cancel tasks
        cancelTask(glowUpdateTask);

        // Unregister teams
        CBCScoreboardManager sbManager = getGameManager().getCbcScoreboardManager();
        sbManager.unregisterTeam(inGameTeam);

        glowManager.deactivate();

    }

    @Override
    public void playerLeaveServer(Player playerEntity) {
        super.playerLeaveServer(playerEntity);

        if (getWinner() != null) return;

        AssassinPlayer player = getPlayer(playerEntity);
        if (player != null) {
            for (AssassinPlayer p : this.players()) {
                if (p.getCurrentTarget() == player) {
                    if (p.isOnline()) {
                        Player assassinPlayerEntity = p.getPlayer();
                        assassinPlayerEntity.sendMessage(Component.text("Your current target disconnected, so you've been given a new one.").color(NamedTextColor.YELLOW));
                    }
                    p.newTarget(true);
                }
            }
        }
    }

    @Override
    public PostGameStats getPostGameStats() {
        return new AssassinPostGameStats(this);
    }

    public int getTargetsToKill() {
        return targetsToKill;
    }

    public ResourcePackManager getResourcePackManager () {
        return resourcePackManager;
    }

    public List<AssassinSpawn> getSpawns() {
        return spawns;
    }

    public double getTargetSpawnDistance() {
        return targetSpawnDistance;
    }

    public AssassinGlowManager getGlowManager () {
        return glowManager;
    }

    public int getTargetChangeTimer() {
        return targetChangeTimer;
    }
}
