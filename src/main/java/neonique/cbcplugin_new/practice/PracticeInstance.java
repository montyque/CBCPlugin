package neonique.cbcplugin_new.practice;

import neonique.cbcplugin_new.combat.CombatDisplay;
import neonique.cbcplugin_new.combat.CombatSession;
import neonique.cbcplugin_new.combat.DeathCause;
import neonique.cbcplugin_new.combat.DeathInfo;
import neonique.cbcplugin_new.core.CBCPlayer;
import neonique.cbcplugin_new.core.PlayerSession;
import neonique.cbcplugin_new.gamemodes.showdown.ShowdownGame;
import neonique.cbcplugin_new.gamemodes.showdown.ShowdownTeam;
import neonique.cbcplugin_new.managers.DeathMessageManager;
import neonique.cbcplugin_new.mapconfig.CBCMap;
import neonique.cbcplugin_new.scoreboard.CBCScoreboardManager;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.audience.ForwardingAudience;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scoreboard.ScoreboardManager;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class PracticeInstance implements PlayerSession<PracticePlayer>, ForwardingAudience, Listener {

    private final Map<UUID, PracticePlayer> players = new HashMap<>();

    private final Plugin plugin;
    private final World world;
    private final CBCScoreboardManager scoreboardManager;
    private final CombatSession combatSession;
    private final CombatDisplay combatDisplay;

    private CBCMap map;
    private List<Location> spawns;

    public PracticeInstance (Plugin plugin, World world, CBCScoreboardManager scoreboardManager) {
        this.plugin = plugin;
        this.world = world;
        this.scoreboardManager = scoreboardManager;
        this.combatSession = new CombatSession(plugin, world, scoreboardManager, this);
        this.combatDisplay = new CombatDisplay(this, new DeathMessageManager());
        combatSession.setCombatDisplay(combatDisplay);
    }

    public void playerJoin (Player playerEntity) {
        PracticePlayer player = createPlayer(playerEntity);
        addPlayer(player);

        player.playerSpawn();
        onPlayerSpawn(player);
    }

    public void playerLeave (Player playerEntity) {
        PracticePlayer player = players.get(playerEntity.getUniqueId());
        if (player == null) return;
        removePlayer(player);
        if (player.isAlive() && player.getLastPlayerHitBy() != null) {
            combatSession.playerDeath(player, DeathCause.LEAVE_PRACTICE);
        }
    }

    public void activate () {
        combatSession.activate();
        combatSession.setDeathListener(this::onPlayerDeath);
        combatSession.setJoinAfterDeathListener(this::joinAfterDeath);
        combatSession.setRespawnListener(this::onPlayerSpawn);

        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    public void setMap (CBCMap map) {
        this.map = map;
        combatSession.setupMap(map);
        spawns = map.defaultSpawns();
        for (PracticePlayer player : players()) {
            player.playerSpawn();
        }
    }

    public CBCMap map () {
        return map;
    }

    public void deactivate () {
        combatSession.deactivate();
        HandlerList.unregisterAll(this);
    }

    public void onPlayerDeath (DeathInfo deathInfo) {
        CBCPlayer victim = deathInfo.victim();
        if (victim.isOnline() && players.containsKey(victim.getUUID())) {
            victim.setRespawnTicks(80);
            victim.showTitle(victim.getRespawnTitle());
        }
    }

    public void onPlayerSpawn (CBCPlayer player) {
        Location spawn = spawns.get(new Random().nextInt(spawns.size()));
        player.teleportPlayerToSpawn(spawn, map.getMapCentre());
    }

    public void joinAfterDeath (CBCPlayer victim) {
        victim.setRespawnTicks(80);
        victim.showTitle(victim.getRespawnTitle());
    }

    @EventHandler
    public void playerLeaveServer (PlayerJoinEvent e) {
        Player playerEntity = e.getPlayer();
        if (hasPlayer(playerEntity)) {
            playerLeave(playerEntity);
        }
    }

    @Override
    public PracticePlayer createPlayer(Player playerEntity) {
        return new PracticePlayer(playerEntity, combatSession);
    }

    @Override
    public void addPlayer(PracticePlayer player) {
        players.put(player.getUUID(), player);
    }

    @Override
    public void removePlayer(PracticePlayer player) {
        players.remove(player.getUUID());
    }

    @Override
    public Collection<PracticePlayer> players() {
        return players.values();
    }

    @Override
    public Optional<PracticePlayer> getPlayerByUUID(UUID uuid) {
        return Optional.ofNullable(players.get(uuid));
    }

    @Override
    public @NotNull Iterable<? extends Audience> audiences() {
        return players.values();
    }
}
