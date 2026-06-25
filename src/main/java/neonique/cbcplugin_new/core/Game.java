package neonique.cbcplugin_new.core;

import neonique.cbcplugin_new.CBCPlugin;
import neonique.cbcplugin_new.combat.DeathCause;
import neonique.cbcplugin_new.gamemodes.CBCGamemode;
import neonique.cbcplugin_new.gamemodes._base.GameSidebarManager;
import neonique.cbcplugin_new.gamemodes._base.PostGameStats;
import neonique.cbcplugin_new.resourcepack.ResourcePackFont;
import neonique.cbcplugin_new.gamemodes.GameContext;
import neonique.cbcplugin_new.cbcevents.CBCEventManager;
import neonique.cbcplugin_new.managers.PlayerSession;
import neonique.cbcplugin_new.scoreboard.CBCScoreboardManager;
import neonique.cbcplugin_new.managers.GameBossBarManager;
import neonique.cbcplugin_new.managers.GameManager;
import neonique.cbcplugin_new.combat.CombatManager;
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
import java.util.stream.Collectors;

import static net.kyori.adventure.text.Component.newline;

public abstract class Game<P extends CBCPlayer, M extends CBCMap> implements PlayerSession<P> {

    private Component headerTitle = Component.text("");

    private final GameManager gameManager;
    private final CombatManager combatManager;
    private final World world;

    private M map;
    private BaseGameCommands gameCommands;

    private final Map<UUID, P> playerList = new HashMap<>();
    private boolean gameOver = false;
    private boolean nightVisionDisabled = false;
    private int gameLength = 0;

    // If global kills is enabled
    private boolean globalKillsEnabled = true;

    // Sidebar manager
    private GameSidebarManager baseSidebarManager = null;

    // Bossbar manager
    private GameBossBarManager baseBossBarManager = null;

    public Game (GameManager gameManager) {
        this.gameManager = gameManager;
        this.combatManager = gameManager.getCombatManager();
        this.world = gameManager.getWorld();
    }

    public P getTypedPlayer (CBCPlayer player) {
        if (player == null) return null;
        return playerList.get(player.getUUID());
    }

    public void setupMap (M map) {
        this.map = map;
        map.fillBlocksAtStart();
        getCombatManager().setupMap(map);
    }

    public M getMap () {
        return map;
    }

    public void addPlayer (P player) {
        playerList.put(player.getUUID(), player);
    }

    public Optional<P> getPlayerByUUID (UUID uuid) {
        return Optional.ofNullable(playerList.get(uuid));
    }

    public void removePlayer (P player) {
        playerList.remove(player.getUUID());
    }

    public abstract void setupGame (GameContext ctx);

    public abstract GameSidebarManager createSidebarManager ();

    public abstract GameBossBarManager createBossbarManager ();

    public abstract PostGameStats getPostGameStats ();

    public abstract CBCGamemode getGamemode ();

    public void createUIManagers () {

        baseSidebarManager = createSidebarManager();
        baseSidebarManager.setupSidebar(world.getPlayers());
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
            player.setRespawnTicks(0);
            player.setAlive(false);
        }

        // Clear game manager player list
        gameManager.resetPlayerList();

        // Disable all combat functions
        getCombatManager().disableWeapons();

    }

    public void createHeaderTitle () {

        String firstPart = "Crossbow Champions: ";

        if (gameManager.isEventGame()) {
            CBCEventManager eventManager = gameManager.getEventManager();
            firstPart = eventManager.getEventName() + ": " + eventManager.getGameName() + " - ";
        }

        headerTitle = smallText("          " + firstPart).color(NamedTextColor.YELLOW)
            .append(
                    smallText(getGamemode().getGamemodeName().toUpperCase() + "          ").color(NamedTextColor.AQUA)
            ).append(
                    newline()
            ).append(
                    smallText(map.getMapName() + " - ").color(NamedTextColor.GRAY)
            ).append(
                    smallText(gameLengthToText()).color(NamedTextColor.GRAY)
            );

        gameManager.setPlayerListHeader(headerTitle);

    }

    public Component smallText(String target) {
        return ResourcePackManager.setTextFont(target, ResourcePackFont.SMALL_5X5);
    }

    public void setupDefaultGameVars (Map<String, Boolean> boolVars, Map<String, Integer> intVars, Map<String, String> stringVars) {

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
            return new BaseGameCommands(this);
        }
        return gameCommands;
    }

    public void setGameCommands(BaseGameCommands gameCommands) {
        this.gameCommands = gameCommands;
    }

    public List<P> getPlayers () {
        return List.copyOf(playerList.values());
    }

    public List<? extends CBCPlayer> getBasePlayers () {
        return List.copyOf(playerList.values());
    }

    public void playerJoinServer(Player playerEntity) {

        // Check if player is a player
        if (hasPlayer(playerEntity)) {
            P player = getPlayer(playerEntity);
            playerEntity.setGameMode(GameMode.SPECTATOR);
            playerEntity.showTitle(player.getDeathTitle());
            player.playerAfterDeath(null);
        } else {
            setPlayerSpectator(playerEntity);
        }

        if (baseBossBarManager != null) {
            baseBossBarManager.addPlayer(playerEntity);
        }

        baseSidebarManager.addPlayerSidebar(playerEntity);

    }

    public void playerLeaveServer(Player playerEntity) {

        if (hasPlayer(playerEntity)) {
            P player = getPlayer(playerEntity);
            if (player.isAlive()) {
                combatManager.playerDeath(player, player.getLastPlayerHitBy(), DeathCause.DISCONNECT, false);
            }
        }

        baseSidebarManager.removePlayerSidebar(playerEntity);

    }

    // Firework celebration
    public void playVictoryFireworks (CBCTeam<?> team) {

        // If team is null, this means this is a free for all game
        new VictoryFireworkTask(team, map).runTaskTimer(CBCPlugin.getPlugin(), 0, 10);

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

    public void setPlayerSpectator(Player player) {

        // Player is spectating, put player into spectator mode
        player.setGameMode(GameMode.SPECTATOR);
        player.teleport(map.getMapCentre());
        player.sendMessage(
                Component.text("You are now spectating this " +
                        "Crossbow Champions - " + getGamemode().getGamemodeName() + " game.").color(NamedTextColor.YELLOW).decorate(TextDecoration.BOLD)
        );

        // Remove night vision if needed
        if (combatManager.isNightVisionDisabled()) {
            player.addScoreboardTag("NVDisable");
            player.removePotionEffect(PotionEffectType.NIGHT_VISION);
        }

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

    public void updateClientSidebar (Player client) {
        baseSidebarManager.updateClientBoard(client);
    }

    public void updateBossbarManager () {
        if (baseBossBarManager == null) return;
        baseBossBarManager.update();
    }

    public void updateHeaderTitle() {
        gameManager.setPlayerListHeader(headerTitle);
    }

    public TextColor getGamemodeColor() {
        return getGamemode().getColor();
    }

    public void teleportSpectators () {
        // Go through all players that aren't in the game
        for (Player player : world.getPlayers()) {
            if (!hasPlayer(player)) {
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
        return playerList.values().stream().map(CBCPlayer::getName).collect(Collectors.toSet());
    }

    public void setHeaderTitle (Component component) {
        headerTitle = component;
    }

    public CBCScoreboardManager getCBCScoreboardManager () {
        return gameManager.getCbcScoreboardManager();
    }

}
