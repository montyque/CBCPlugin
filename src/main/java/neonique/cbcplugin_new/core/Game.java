package neonique.cbcplugin_new.core;

import neonique.cbcplugin_new.CBCPlugin;
import neonique.cbcplugin_new.combat.CombatSession;
import neonique.cbcplugin_new.combat.DeathCause;
import neonique.cbcplugin_new.mapconfig.CBCMap;
import neonique.cbcplugin_new.resourcepack.ResourcePackFont;
import neonique.cbcplugin_new.resourcepack.ResourcePackManager;
import neonique.cbcplugin_new.scoreboard.CBCScoreboardManager;
import neonique.cbcplugin_new.tasks.gamemodetasks.VictoryFireworkTask;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.audience.ForwardingAudience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

import static neonique.cbcplugin_new.util.TextUtil.timerToText;

public abstract class Game<P extends CBCPlayer> implements PlayerSession<P>, ForwardingAudience {

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

    // If global kills is enabled
    private boolean globalKillsEnabled = true;

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
        audiences.add(player.getUUID());
    }

    public Optional<P> getPlayerByUUID (UUID uuid) {
        return Optional.ofNullable(playerList.get(uuid));
    }

    public void removePlayer (P player) {
        playerList.remove(player.getUUID());
        audiences.remove(player.getUUID());
    }

    public P getTypedPlayer (CBCPlayer player) {
        if (player == null) return null;
        return playerList.get(player.getUUID());
    }

    public void resetGame () {
        combatSession.deactivate();
        setGameOver(true);
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

    public Component smallText(String target) {
        return ResourcePackManager.setTextFont(target, ResourcePackFont.SMALL_5X5);
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

    public void playerJoinServer(Player playerEntity) {

        // Check if player is a player
        if (hasPlayer(playerEntity)) {
            P player = getPlayer(playerEntity);
            combatSession.playerJoinAfterDeath(player);
        } else if (spectatorUUIDs.contains(playerEntity.getUniqueId())) {
            teleportSpectator(playerEntity);
        }

    }

    public void playerLeaveServer(Player playerEntity) {

        if (hasPlayer(playerEntity)) {
            P player = getPlayer(playerEntity);
            if (player.isAlive()) {
                combatSession.playerDeath(player, DeathCause.DISCONNECT);
            }
        }

    }

    // Firework celebration
    public void playVictoryFireworks (CBCTeam<?> team) {

        // If team is null, this means this is a free for all game
        new VictoryFireworkTask(team, getMap()).runTaskTimer(CBCPlugin.getPlugin(), 0, 10);

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
        gameLength++;
        sendPlayerListHeader(getHeaderTitle());
    }

    public int getGameLength() {
        return gameLength;
    }

    public void setPlayerSpectator(Player player) {

        // Player is spectating, put player into spectator mode
        player.setGameMode(GameMode.SPECTATOR);
        player.teleport(getMap().getMapCentre());
        player.sendMessage(
                Component.text("You are now spectating this " +
                        "Crossbow Champions - " + getGamemode().getGamemodeName() + " game.").color(NamedTextColor.YELLOW).decorate(TextDecoration.BOLD)
        );

    }

    public void teleportSpectator (Player player) {

        // Player is spectating, put player into spectator mode
        player.setGameMode(GameMode.SPECTATOR);
        player.teleport(getMap().getMapCentre());
        player.sendMessage(
                Component.text("You are now spectating this " +
                        "Crossbow Champions - " + getGamemode().getGamemodeName() + " game.").color(NamedTextColor.YELLOW).decorate(TextDecoration.BOLD)
        );

    }

    public boolean isGlobalKillsEnabled() {
        return globalKillsEnabled;
    }

    public TextColor getGamemodeColor() {
        return getGamemode().getColor();
    }

    public void addSpectator (Player player) {
        teleportSpectator(player);
        spectatorUUIDs.add(player.getUniqueId());
        audiences.add(player.getUniqueId());
    }

    public void removeSpectator (Player player) {
        teleportSpectator(player);
        spectatorUUIDs.remove(player.getUniqueId());
        audiences.remove(player.getUniqueId());
    }

    public Collection<Player> spectators () {
        return spectatorUUIDs.stream()
                .map(Bukkit::getPlayer)
                .filter(Objects::nonNull)
                .filter(Player::isOnline)
                .toList();
    }

    public void teleportSpectators () {
        // Go through all players that aren't in the game
        for (Player player : world.getPlayers()) {
            if (!hasPlayer(player)) {
                setPlayerSpectator(player);
            }
        }
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
                .toList();
    }

    public boolean hasSpectator (Player player) {
        return spectatorUUIDs.contains(player.getUniqueId());
    }
}
