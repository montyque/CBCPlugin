package neonique.cbcplugin_new.gamemodes.assassin;

import neonique.cbcplugin_new.CBCPlugin;
import neonique.cbcplugin_new.enums.CBCGamemode;
import neonique.cbcplugin_new.gamemodes._base.*;
import neonique.cbcplugin_new.listeners.gamemodes.PlayerNoMove;
import neonique.cbcplugin_new.lobby.LobbyPlayer;
import neonique.cbcplugin_new.lobby.LobbyTeam;
import neonique.cbcplugin_new.managers.GameBossBarManager;
import neonique.cbcplugin_new.managers.GameManager;
import neonique.cbcplugin_new.managers.CombatManager;
import neonique.cbcplugin_new.playerclasses.CBCPlayer;
import neonique.cbcplugin_new.resourcepack.ResourcePackManager;
import neonique.cbcplugin_new.tasks.gamemodetasks.IncrementGameTimeTask;
import neonique.cbcplugin_new.tasks.gamemodetasks.assassin.AssassinGlowUpdateTask;
import neonique.cbcplugin_new.tasks.gamemodetasks.assassin.AssassinStartGameTimer;
import neonique.cbcplugin_new.tasks.gamemodetasks.assassin.AssassinTargetChangeTimer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;
import org.bukkit.scoreboard.Team;

import java.util.*;

public class AssassinGame extends FFAGame {

    // Map related variables
    private AssassinMap map;
    private List<AssassinSpawn> spawns;
    private double targetSpawnDistance;

    // Game related variables
    private int targetsToKill;
    private int targetChangeTimer;

    // Other variables
    private Team inGameTeam;
    private ResourcePackManager resourcePackManager;

    // Listeners and tasks
    private PlayerNoMove noMoveListener;

    // Glow manager for making one player glow
    private AssassinGlowManager glowManager;
    private AssassinGlowUpdateTask glowUpdateTask;

    public AssassinGame(GameManager gameManager, CombatManager combatManager) {
        super(gameManager, combatManager);
    }

    @Override
    public void setupGame(CBCMap mapChosen, LinkedHashMap<String, LobbyTeam> teams, Collection<LobbyPlayer> players,
                             HashMap<String, Boolean> boolVars, HashMap<String, Integer> intVars, HashMap<String, String> stringVars) {

        resourcePackManager = CBCPlugin.getResourcePackManager();

        final GameManager gameManager = getGameManager();
        final CombatManager combatManager = getCombatManager();
        final World world = getWorld();

        // Setup map
        setupMap(mapChosen);
        // Setup default game variables
        setupDefaultGameVars(boolVars, intVars, stringVars);

        // Set gamemode information
        setGamemode(CBCGamemode.ASSASSIN);
        createHeaderTitle();

        // Enable weapons
        combatManager.activateWeapons();
        gameManager.resetPlayerList();

        // Setup gamemode game variables
        targetsToKill = intVars.getOrDefault("targetsToKill", 16);
        targetChangeTimer = intVars.getOrDefault("targetChangeTimer", 60);

        // setGameCommands(new ShowdownGameCommands(gameManager, weaponManager, this));
        // Create players
        createPlayers(players);
        teleportSpectators();

        // Teleport players to spawns
        map.fillBlocksAtStart();

        List<AssassinSpawn> gameStartSpawns = sortSpawns();
        List<AssassinPlayer> shuffledPlayers = new ArrayList<>(getAssassinPlayers());
        Collections.shuffle(shuffledPlayers);
        int spawnNum = 0;
        for (AssassinPlayer player : shuffledPlayers) {
            if (!player.isOnline()) continue;
            player.teleportPlayerToSpawn(gameStartSpawns.get(spawnNum), map.getMapCentre());
            player.playerSetupGame();
            spawnNum++;
        }

        // Create teams
        ScoreboardManager scoreboardManager = CBCPlugin.getPlugin().getServer().getScoreboardManager();
        // Team scoreboard object
        Scoreboard scoreboard = scoreboardManager.getMainScoreboard();

        // Create safe team
        inGameTeam = scoreboard.registerNewTeam("01players");
        inGameTeam.setCanSeeFriendlyInvisibles(false);
        inGameTeam.setAllowFriendlyFire(true); // Allow friendly fire
        inGameTeam.color(NamedTextColor.AQUA); // Set team color

        if (gameManager.getCbcScoreboardManager().isActive()) {
            gameManager.getCbcScoreboardManager().registerTeamForAllClients(inGameTeam);
        }

        updatePlacements();

        // Create Bossbar/Sidebar managers
        createUIManagers();

        // Set up glow manager
        glowManager = new AssassinGlowManager(world, this);
        glowManager.activate();

        glowUpdateTask = new AssassinGlowUpdateTask(this);
        glowUpdateTask.runTaskTimer(CBCPlugin.getPlugin(), 0, 10);

        // Set player's targets
        for (AssassinPlayer player : getAssassinPlayers()) {
            player.newTarget(false);
            getGameManager().getCbcScoreboardManager().addTeamEntry(player.getName(), inGameTeam);
        }

        // Setup listeners
        CBCPlugin plugin = CBCPlugin.getPlugin();

        // Make it so players cannot move
        noMoveListener = new PlayerNoMove(gameManager);
        plugin.getServer().getPluginManager().registerEvents(noMoveListener, plugin);

        // Start countdown timer
        new AssassinStartGameTimer(gameManager, this, 11).runTaskTimer(plugin, 0, 20);

    }

    public CBCPlayer createGamemodePlayer (Player playerEntity, int playerId) {
        return new AssassinPlayer(this, getGameManager(), getCombatManager(), playerEntity, playerId);
    }

    public GameSidebarManager createSidebarManager() {
        return new AssassinSidebarManager(this);
    }

    @Override
    public GameBossBarManager createBossbarManager() {
        return new AssassinBossbarManager(this);
    }

    public void startGame () {

        map.fillBlocksAtEnd();

        // Allow players to move
        PlayerMoveEvent.getHandlerList().unregister(noMoveListener);
        noMoveListener = null;

        // Initialise all players
        for (AssassinPlayer player : getAssassinPlayers()) {
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
                (AssassinSpawn loc1, AssassinSpawn loc2) -> Double.compare(loc1.distanceSquared(map.getMapCentre()), loc2.distanceSquared(map.getMapCentre()));
        roundSpawnList.sort(byDistanceFromCenter);
        Collections.reverse(roundSpawnList);

        spawnOrder.add(roundSpawnList.get(0));
        roundSpawnList.remove(0);

        while (spawnOrder.size() < getPlayers().size()) {
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

    public void setupMap (CBCMap generalMap) {
        super.setupMap(generalMap);
        this.map = (AssassinMap) generalMap;
        // Get spawns
        spawns = map.getAssassinSpawns();
        targetSpawnDistance = map.getTargetDistance();
    }

    public List<AssassinPlayer> getPlayersByTargets () {

        List<AssassinPlayer> sortedPlayerList = new ArrayList<>(getAssassinPlayers());
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
        CBCPlugin.getGameManager().getCbcScoreboardManager().unregisterTeamForAllClients(inGameTeam.getName());
        inGameTeam.unregister();

        glowManager.deactivate();

    }

    @Override
    public void playerLeaveServer(Player player) {
        super.playerLeaveServer(player);

        UUID offlinePlayerId = player.getUniqueId();

        // Check if game has already had a winner
        if (getWinner() != null) return;

        // Check if player is a player
        for (Player playerEntity : getGameManager().getPlayerEntities()) {
            if (playerEntity.getUniqueId().equals(offlinePlayerId)) {
                CBCPlayer cbcplayer = getGameManager().getPlayer(playerEntity);
                // Check if player is the target of any assassins, if so change their targets
                for (AssassinPlayer assassinPlayer : getAssassinPlayers()) {
                    if (assassinPlayer.getCurrentTarget() == cbcplayer) {
                        // Change player's target
                        if (assassinPlayer.isOnline()) {
                            Player assassinPlayerEntity = assassinPlayer.getPlayer();
                            assassinPlayerEntity.sendMessage(Component.text("Your target disconnected, so you've been given a new one!").color(NamedTextColor.YELLOW));
                        }
                        assassinPlayer.newTarget(true);
                    }
                }
            }
        }
    }

    @Override
    public PostGameStats getPostGameStats() {
        return new AssassinPostGameStats(this);
    }

    public Set<AssassinPlayer> getAssassinPlayers() {
        Set<AssassinPlayer> players = new HashSet<>();
        for (CBCPlayer player : getPlayers().values()) {
            players.add((AssassinPlayer) player);
        }
        return players;
    }

    public Set<AssassinPlayer> getOnlineAssassinPlayers () {
        Set<AssassinPlayer> players = new HashSet<>();
        for (CBCPlayer player : getPlayers().values()) {
            if (player.isOnline()) {
                players.add((AssassinPlayer) player);
            }
        }
        return players;
    }

    public Set<AssassinPlayer> getCurrentTargets() {

        Set<AssassinPlayer> targets = new HashSet<>();
        for (AssassinPlayer player : getAssassinPlayers()) {
            if (player.getCurrentTarget() != null) {
                targets.add(player.getCurrentTarget());
            }
        }

        return targets;

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
