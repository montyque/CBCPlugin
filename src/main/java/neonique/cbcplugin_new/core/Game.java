package neonique.cbcplugin_new.core;

import neonique.cbcplugin_new.CBCPlugin;
import neonique.cbcplugin_new.combat.CombatSession;
import neonique.cbcplugin_new.combat.DeathCause;
import neonique.cbcplugin_new.hud.SidebarProvider;
import neonique.cbcplugin_new.mapconfig.CBCMap;
import neonique.cbcplugin_new.resourcepack.ResourcePackFont;
import neonique.cbcplugin_new.scoreboard.CBCScoreboardManager;
import neonique.cbcplugin_new.hud.SidebarManager;
import neonique.cbcplugin_new.tasks.gamemodetasks.VictoryFireworkTask;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.audience.ForwardingAudience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

import static neonique.cbcplugin_new.util.TextUtil.timerToText;

public abstract class Game<P extends CBCPlayer> implements PlayerSession<P>, ForwardingAudience, Listener {

    private final Plugin plugin;
    private final CBCScoreboardManager scoreboardManager;
    private final World world;
    private final CombatSession combatSession;

    private BaseGameCommands gameCommands;

    private final Map<UUID, P> playerList = new HashMap<>();
    private final Set<UUID> spectatorUUIDs = new HashSet<>();
    private final Set<UUID> audiences = new HashSet<>();

    private boolean gameOver = false;
    private int gameLength = 0;

    // UI elements
    private SidebarManager sidebarManager;

    public Game (GameInitContext ctx) {
        this.plugin = ctx.plugin();
        this.scoreboardManager = ctx.scoreboardManager();
        this.world = ctx.world();
        this.combatSession = new CombatSession(plugin, world, scoreboardManager, this);
    }

    public abstract CBCGamemode getGamemode ();

    public abstract CBCMap getMap();

    public abstract void setupGame (GameContext context);

    /**
     * Cleans up the game when the game ends.
     * This should clean up all dependencies a gamemode uses, such as listeners, tasks, or teams.
     */
    public abstract void gameCleanup();

    public void attachSidebarManager (SidebarManager manager) {
        sidebarManager = manager;
        sidebarManager.setProvider(sidebarProvider());
        sidebarManager.activate();
    }

    public SidebarProvider sidebarProvider () {
        return SidebarProvider.hidden();
    }

    public void updateSidebarForAll () {
        if (sidebarManager != null) sidebarManager.updateAll();
    }

    public void updateSidebarForPlayer (Player player) {
        if (sidebarManager != null) sidebarManager.updatePlayer(player);
    }

    public String getGameSubtitle() {
        return getMap().name();
    }

    public Plugin plugin () {
        return plugin;
    }

    public World world () {
        return world;
    }

    public CBCScoreboardManager scoreboardManager () {
        return scoreboardManager;
    }

    public CombatSession combatSession () {
        return combatSession;
    }

    public void addPlayer (P player) {
        playerList.put(player.getUUID(), player);
        addToAudience(player.getUUID());
    }

    public Optional<P> getPlayerByUUID (UUID uuid) {
        return Optional.ofNullable(playerList.get(uuid));
    }

    public void removePlayer (P player) {
        playerList.remove(player.getUUID());
        removeFromAudience(player.getUUID());
    }

    public P getTypedPlayer (CBCPlayer player) {
        if (player == null) return null;
        return playerList.get(player.getUUID());
    }

    public void setup (GameContext context) {
        combatSession().activate();
        gameOver = false;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        setupGame(context);
    }

    public void stop() {

        combatSession.deactivate();
        gameOver = true;
        HandlerList.unregisterAll(this);

        if (sidebarManager != null) {
            sidebarManager.deactivate();
        }

        gameCleanup();
    }

    public Component getHeaderTitle () {

        return Component.text()
                .append(Component.text()
                        .content("CROSSBOW CHAMPIONS: ")
                        .color(NamedTextColor.YELLOW))
                .append(Component.text()
                        .content(getGamemode().getGamemodeName())
                        .color(NamedTextColor.AQUA))
                .append(Component.newline())
                .append(Component.text()
                        .content(getGameSubtitle() + " - " + timerToText(getGameLength())))
                .font(ResourcePackFont.SMALL_5X5.getFontKey())
                .build();

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

    public List<P> players() {
        return List.copyOf(playerList.values());
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent e) {

        Player playerEntity = e.getPlayer();
        if (hasPlayer(playerEntity)) {
            playerJoinServer(getPlayer(playerEntity));
        } else if (spectatorUUIDs.contains(playerEntity.getUniqueId())) {
            teleportSpectator(playerEntity);
        }

    }

    public void playerJoinServer (P player) {
        combatSession.playerJoinAfterDeath(player);
    }

    @EventHandler
    public void onPlayerLeave(PlayerQuitEvent e) {

        Player playerEntity = e.getPlayer();
        if (hasPlayer(playerEntity)) {
            playerLeaveServer(getPlayer(playerEntity));
        }

    }

    public void playerLeaveServer (P player) {
        if (player.isAlive()) {
            combatSession.playerDeath(player, DeathCause.DISCONNECT);
        }
    }

    public void playVictoryFireworks (CBCTeam<?> team) {
        new VictoryFireworkTask(team, getMap()).runTaskTimer(CBCPlugin.getPlugin(), 0, 10);
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
        gameLength++;
        sendPlayerListHeader(getHeaderTitle());
    }

    public int getGameLength() {
        return gameLength;
    }

    public void teleportSpectator (Player player) {
        player.setGameMode(GameMode.SPECTATOR);
        player.teleport(getMap().getMapCentre());
    }

    public void addSpectator (Player player) {
        teleportSpectator(player);
        spectatorUUIDs.add(player.getUniqueId());
        addToAudience(player.getUniqueId());
        player.sendMessage(
                Component.text("You are now spectating this " +
                        "Crossbow Champions - " + getGamemode().getGamemodeName() + " game.").color(NamedTextColor.YELLOW)
        );
    }

    public void removeSpectator (Player player) {
        teleportSpectator(player);
        spectatorUUIDs.remove(player.getUniqueId());
        removeFromAudience(player.getUniqueId());
    }

    public void addToAudience (UUID playerUUID) {
        audiences.add(playerUUID);
        if (sidebarManager != null) sidebarManager.registerPlayer(playerUUID);
    }

    public void removeFromAudience (UUID playerUUID) {
        audiences.remove(playerUUID);
        if (sidebarManager != null) sidebarManager.removePlayerUUID(playerUUID);
    }

    public Collection<Player> spectators () {
        return spectatorUUIDs.stream()
                .map(Bukkit::getPlayer)
                .filter(Objects::nonNull)
                .filter(Player::isOnline)
                .toList();
    }

    public void teleportSpectators () {
        spectators().forEach(this::teleportSpectator);
    }

    public World getWorld() {
        return world;
    }

    public Set<String> getPlayerNames() {
        return playerList.values().stream().map(CBCPlayer::name).collect(Collectors.toSet());
    }

    @Override
    public @NotNull Iterable<? extends Audience> audiences () {
        return audiences.stream()
                .map(Bukkit::getPlayer)
                .filter(Objects::nonNull)
                .filter(Player::isOnline)
                .toList();
    }

    public boolean hasSpectator (Player player) {
        return spectatorUUIDs.contains(player.getUniqueId());
    }
}
