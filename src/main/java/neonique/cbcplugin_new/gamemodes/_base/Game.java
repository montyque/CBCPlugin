package neonique.cbcplugin_new.gamemodes._base;

import neonique.cbcplugin_new.CBCPlugin;
import neonique.cbcplugin_new.enums.DeathCause;
import neonique.cbcplugin_new.enums.CBCGamemode;
import neonique.cbcplugin_new.enums.ResourcePackFont;
import neonique.cbcplugin_new.gamemodes.ctf.CTFGame;
import neonique.cbcplugin_new.lobby.LobbyPlayer;
import neonique.cbcplugin_new.lobby.LobbyTeam;
import neonique.cbcplugin_new.cbcevents.CBCEventManager;
import neonique.cbcplugin_new.managers.CBCScoreboardManager;
import neonique.cbcplugin_new.managers.GameBossBarManager;
import neonique.cbcplugin_new.managers.GameManager;
import neonique.cbcplugin_new.managers.CombatManager;
import neonique.cbcplugin_new.playerclasses.CBCPlayer;
import neonique.cbcplugin_new.resourcepack.ResourcePackManager;
import neonique.cbcplugin_new.tasks.gamemodetasks.UpdateBossbarsTask;
import neonique.cbcplugin_new.tasks.gamemodetasks.VictoryFireworkTask;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.GameMode;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

import static net.kyori.adventure.text.Component.newline;

public abstract class Game {

    private CBCGamemode gamemode;

    private Component headerTitle = Component.text("");

    private final GameManager gameManager;
    private final CombatManager combatManager;
    private final World world;

    private CBCMap generalMap;
    private int newestPlayerId;
    private BaseGameCommands gameCommands;

    private HashMap<UUID, CBCPlayer> playerList = new HashMap<>();
    private boolean gameOver = false;
    private boolean nightVisionDisabled = false;
    private int gameLength = 0;

    // If global kills is enabled
    private boolean globalKillsEnabled = true;

    // Sidebar manager
    private GameSidebarManager baseSidebarManager = null;

    // Bossbar manager
    private GameBossBarManager baseBossBarManager = null;

    public Game (GameManager gameManager, CombatManager combatManager) {
        this.gameManager = gameManager;
        this.combatManager = combatManager;
        this.world = gameManager.getWorld();
    }

    public void setupMap (CBCMap map) {
        generalMap = map;
        map.fillBlocksAtStart();
        getCombatManager().setupMap(map);
    }

    public void setGeneralMap (CBCMap map) {
        generalMap = map;
    }

    public abstract void setupGame (
            CBCMap mapChosen, LinkedHashMap<String, LobbyTeam> teams, Collection<LobbyPlayer> players,
            HashMap<String, Boolean> boolVars, HashMap<String, Integer> intVars, HashMap<String, String> stringVars);

    public abstract CBCPlayer createGamemodePlayer (Player playerEntity, int playerId);

    public abstract GameSidebarManager createSidebarManager ();

    public abstract GameBossBarManager createBossbarManager ();

    public abstract PostGameStats getPostGameStats();

    public void createUIManagers () {
        baseSidebarManager = createSidebarManager();
        baseSidebarManager.setupSidebar();
        baseSidebarManager.updateServerBoard();

        baseBossBarManager = createBossbarManager();
        gameManager.showGlobalBossbarManager(baseBossBarManager);
        new UpdateBossbarsTask(this).runTaskTimer(CBCPlugin.getPlugin(), 0, 5);
    }

    public void resetGame () {

        setGameOver(true);

        // Remove sidebar and bossbar
        baseSidebarManager.removeSidebar();
        gameManager.hideGlobalBossbarManager(baseBossBarManager);

        // Reset all players
        for (CBCPlayer player : playerList.values()) {
            player.resetPlayer();
            player.setRespawning(false);
            player.setAlive(false);
        }

        // Clear game manager player list
        gameManager.resetPlayerList();

        // Disable all combat functions
        getCombatManager().disableWeapons();

    }

    public CBCPlayer createPlayer (Player playerEntity) {

        // Create player, add them to player lists and game manager
        CBCPlayer player = createGamemodePlayer(playerEntity, newestPlayerId);
        playerList.put(playerEntity.getUniqueId(), player);
        gameManager.addPlayer(player, newestPlayerId);

        // Increment player id so that the next player does not have the same player id
        newestPlayerId++;

        return player;
    }

    public void createHeaderTitle () {

        String firstPart = "Crossbow Champions: ";

        if (gameManager.isThisGameCBCGame()) {
            CBCEventManager eventManager = gameManager.getEventManager();
            firstPart = eventManager.getEventName() + ": " + eventManager.getGameName() + " - ";
        }

        headerTitle = smallText("          " + firstPart).color(NamedTextColor.YELLOW)
            .append(
                    smallText(gamemode.getGamemodeName().toUpperCase() + "          ").color(NamedTextColor.AQUA)
            ).append(
                    newline()
            ).append(
                    smallText(generalMap.getMapName() + " - ").color(NamedTextColor.GRAY)
            ).append(
                    smallText(gameLengthToText()).color(NamedTextColor.GRAY)
            );

        gameManager.setPlayerListHeader(headerTitle);

    }

    public Component smallText(String target) {
        return ResourcePackManager.setTextFont(target, ResourcePackFont.SMALL_5X5);
    }

    public void setupDefaultGameVars (HashMap<String, Boolean> boolVars, HashMap<String, Integer> intVars, HashMap<String, String> stringVars) {

        if (boolVars.getOrDefault("beaconHeads", false)) {
            combatManager.setBeaconHeadsEnabled(true);
        }

        if (boolVars.getOrDefault("doDayCycle", false)) {
            combatManager.setDoDayCycleEnabled(true);
        }

        if (boolVars.getOrDefault("nightVisionDisabled", false)) {
            combatManager.setNightVisionDisabled(true);
            nightVisionDisabled = true;
        }

        globalKillsEnabled = boolVars.getOrDefault("globalKillsEnabled", true);

    }

    public BaseGameCommands getGameCommands() {
        if (gameCommands == null) {
            return new BaseGameCommands(gameManager, combatManager);
        }
        return gameCommands;
    }

    public void setGameCommands(BaseGameCommands gameCommands) {
        this.gameCommands = gameCommands;
    }


    public CBCPlayer getPlayer(Player player) {
        return playerList.getOrDefault(player.getUniqueId(), null);
    }

    public void replacePlayerEntityKey(Player origin, Player newPlayer) {
        if (playerList.containsKey(origin.getUniqueId())) {
            CBCPlayer cbcPlayer = playerList.get(origin.getUniqueId());
            playerList.remove(origin.getUniqueId());
            playerList.put(newPlayer.getUniqueId(), cbcPlayer);
            cbcPlayer.setNewPlayer(newPlayer);

            gameManager.replacePlayerEntityKey(origin, newPlayer);

            if (cbcPlayer.getTeam() != null) {
                cbcPlayer.getTeam().replacePlayerEntityKey(origin, newPlayer);
            }
        }
    }

    public HashMap<UUID, CBCPlayer> getPlayers() {
        return playerList;
    }

    public CBCPlayer addPlayer (Player playerEntity) {
        return createPlayer(playerEntity);
    }

    public void removePlayer (CBCPlayer player) {
        playerList.remove(player.getOfflinePlayer().getUniqueId());
        gameManager.removePlayer(player);
    }

    public void playerJoinServer(Player player) {

        UUID offlinePlayerId = player.getUniqueId();

        boolean isSpectator = true;

        // Check if player is a player
        for (Player playerEntity : gameManager.getPlayerEntities()) {
            if (playerEntity.getUniqueId().equals(offlinePlayerId)) {
                CBCPlayer cbcplayer = gameManager.getPlayer(playerEntity);
                if (cbcplayer == null) return;
                replacePlayerEntityKey(playerEntity, player);

                // Put player into spectator mode, they cannot come back into the game yet
                playerEntity.setGameMode(GameMode.SPECTATOR);
                cbcplayer.playerAfterDeath(null);

                isSpectator = false;

                break;
            }
        }

        if (isSpectator) {
            // Player is a spectator, so teleport them to map and let them spectate
            setPlayerSpectator(player);
        }

        if (baseBossBarManager != null) {
            baseBossBarManager.addPlayer(player);
        }

        getSidebarManager().addPlayerSidebar(player);
    }

    public void playerLeaveServer(Player player) {
        UUID offlinePlayerId = player.getUniqueId();

        // Check if player is a player
        for (Player playerEntity : gameManager.getPlayerEntities()) {
            if (playerEntity.getUniqueId().equals(offlinePlayerId)) {
                CBCPlayer cbcplayer = gameManager.getPlayer(playerEntity);

                // If player is alive kill them
                if (cbcplayer == null) return;
                if (cbcplayer.isAlive()) {
                    if (cbcplayer.getLastPlayerHitBy() == null) {
                        combatManager.playerDeath(cbcplayer, null, DeathCause.DISCONNECT, false);
                    } else {
                        combatManager.playerDeath(cbcplayer, cbcplayer.getLastPlayerHitBy(), DeathCause.DISCONNECT, false);
                    }
                }

                replacePlayerEntityKey(playerEntity, player.getPlayer());
                return;
            }
        }

        getSidebarManager().removePlayerSidebar(player);
    }

    // Firework celebration
    public void playVictoryFireworks(CBCTeam team) {

        // If team is null, this means this is a free for all game
        new VictoryFireworkTask(team, generalMap).runTaskTimer(CBCPlugin.getPlugin(), 0, 10);

    }

    public void setGameOver (boolean b) {
        gameOver = b;
    }

    public boolean isGameOver() {
        return gameOver;
    }

    public void cancelTask (BukkitRunnable task) {
        if (task == null) return;
        if (!task.isCancelled()) {
            task.cancel();
        }
    }

    public void incrementGameTime() {
        createHeaderTitle();
        gameManager.setPlayerListHeader(headerTitle);
        gameLength++;
    }

    public int getGameLength() {
        return gameLength;
    }

    public GameManager getGameManager() {
        return gameManager;
    }

    public CombatManager getCombatManager() {
        return combatManager;
    }

    public void setGamemode(CBCGamemode gamemode) {
        this.gamemode = gamemode;
    }

    public void setPlayerSpectator(Player player) {
        // Player is spectating, put player into spectator mode
        player.setGameMode(GameMode.SPECTATOR);
        player.teleport(generalMap.getMapCentre());
        player.sendMessage(
                Component.text("You are now spectating this " +
                        "Crossbow Champs - " + gamemode.getGamemodeName() + " game.").color(NamedTextColor.YELLOW).decorate(TextDecoration.BOLD)
        );

        // Remove night vision if needed
        if (combatManager.isNightVisionDisabled()) {
            player.addScoreboardTag("NVDisable");
            player.removePotionEffect(PotionEffectType.NIGHT_VISION);
        }
    }

    public CBCMap getMap() {
        return generalMap;
    }

    public String gameLengthToText() {
        return String.format("%d:%02d", gameLength / 60, gameLength % 60);
    }

    public boolean isGlobalKillsEnabled() {
        return globalKillsEnabled;
    }

    public GameBossBarManager getBossbarManager () {
        return baseBossBarManager;
    }

    public GameSidebarManager getSidebarManager () {
        return baseSidebarManager;
    }

    public void updateServerSidebar () {
        if (baseSidebarManager == null) return;
        baseSidebarManager.updateServerBoard();
    }

    public void updateBossbarManager () {
        if (baseBossBarManager == null) return;
        baseBossBarManager.update();
    }

    public void updateHeaderTitle() {
        gameManager.setPlayerListHeader(headerTitle);
    }

    public TextColor getGamemodeColor() {
        return gamemode.getColor();
    }

    public CBCGamemode getGamemode() {
        return gamemode;
    }

    public void teleportSpectators () {
        // Go through all players that aren't in the game
        for (Player player : world.getPlayers()) {
            if (!gameManager.hasPlayer(player)) {
                setPlayerSpectator(player);
            }
        }
    }

    public boolean isNightVisionDisabled() {
        return nightVisionDisabled;
    }

    public World getWorld() {
        return world;
    }

    public Set<String> getPlayerNames() {
        // Create set of names
        Set<String> names = new HashSet<>();
        // Add each player's name to set
        for (CBCPlayer player : getPlayers().values()) {
            names.add(player.getName());
        }
        return names;
    }

    public void setHeaderTitle (Component component) {
        headerTitle = component;
    }

    public CBCScoreboardManager getCBCScoreboardManager () {
        return gameManager.getCbcScoreboardManager();
    }

}
