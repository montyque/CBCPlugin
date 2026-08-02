package neonique.cbcplugin_new.combat;

import neonique.cbcplugin_new.combat.listeners.*;
import neonique.cbcplugin_new.combat.tasks.PlayerParticlesTask;
import neonique.cbcplugin_new.combat.tasks.ProjectileUpdateTask;
import neonique.cbcplugin_new.combat.tasks.RespawnTimerTask;
import neonique.cbcplugin_new.combat.tasks.WeaponReloadTask;
import neonique.cbcplugin_new.core.CBCPlayer;
import neonique.cbcplugin_new.core.PlayerStore;
import neonique.cbcplugin_new.mapconfig.CBCMap;
import neonique.cbcplugin_new.mapmechanics.MapMechanicsManager;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class CombatSession {

    private final Plugin plugin;
    private final World world;
    private final PlayerStore players;
    private CombatDisplay combatDisplay;

    private final MapMechanicsManager mapMechanicsManager;
    private final ProjectileManager projectileManager;

    private List<Listener> listeners = new ArrayList<>();
    private List<BukkitRunnable> tasks = new ArrayList<>();

    private int timer = 0;

    public CombatSession (Plugin plugin, World world, PlayerStore players) {

        this.plugin = plugin;
        this.world = world;
        this.players = players;

        this.mapMechanicsManager = new MapMechanicsManager(players);
        this.projectileManager = new ProjectileManager();

    }

    public void setCombatDisplay (CombatDisplay combatDisplay) {
        this.combatDisplay = combatDisplay;
    }

    public void activate () {
        setupListeners();
        setupTasks();
    }

    public void setupMap (CBCMap map) {
        mapMechanicsManager.setupMapMechanics(map);
    }

    public void deactivate () {

        for (Listener listener : listeners) {
            HandlerList.unregisterAll(listener);
        }

        for (BukkitRunnable task : tasks) {
            if (!task.isCancelled()) task.cancel();
        }

        projectileManager.clearAllProjectiles();

    }

    public void playerDeath (CBCPlayer playerKilled, DeathCause cause) {
        playerDeath(playerKilled, playerKilled.getLastPlayerHitBy(), cause, false);
    }

    // Runs when a player takes fatal damage
    public void playerDeath (CBCPlayer victim, CBCPlayer killer, DeathCause cause, boolean direct) {

        victim.playerDie();
        victim.playerAfterDeath(killer);

        if (killer != null) {
            killer.playerKill();
            killer.playerAfterKill(victim);
        }

        // Check if player that was killed is online
        if (victim.isOnline()) {

            // Get player entity of the player who was killed
            Player victimEntity = victim.getPlayer();
            victimEntity.setGameMode(GameMode.SPECTATOR);
            Location location = victimEntity.getLocation();

            // Show death title
            victimEntity.showTitle(victim.getDeathTitle());
            victim.updateActionBarDisplay(true);

        }

        if (combatDisplay != null) {
            combatDisplay.onPlayerDeath(victim, killer, cause, direct);
        }

    }

    // Respawn player
    public void playerRespawn (CBCPlayer player) {

        if (!player.isOnline()) return;

        Player playerEntity = player.getPlayer();

        player.playerSpawn();

        // Set gamemode of player to adventure and reset their stats
        playerEntity.setGameMode(GameMode.ADVENTURE);
        player.healToFull();
        player.setAlive(true); // Set player's state to alive

        // Show respawned title
        Component respawnedComponent = Component.text("Respawned!").color(NamedTextColor.GREEN)
                .decorate(TextDecoration.BOLD);
        Title respawnedTitle = Title.title(respawnedComponent, Component.empty(),Title.Times.times(
                Duration.ofMillis(0), Duration.ofMillis(250), Duration.ofMillis(250)));
        playerEntity.showTitle(respawnedTitle);
        playerEntity.playSound(playerEntity.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 5, 2);

    }

    private void setupListeners () {

        listeners = List.of(
                new EntityDamagePlayerListener(projectileManager, players),
                new CrossbowFiredListener(projectileManager, players),
                new PlayerItemListener(players),
                new PlayerMiscDamageListener(players),
                new PlayerDeathListener(players),
                new ArrowHitPlayerListener(projectileManager, players::getPlayer),
                new BlockInteractListener(p -> players.getPlayer(p) == null)
        );

        listeners.forEach(e -> plugin.getServer().getPluginManager().registerEvents(e, plugin));

    }

    private void setupTasks () {

        BukkitRunnable weaponReloadTask = new WeaponReloadTask(players::getPlayers);
        weaponReloadTask.runTaskTimer(plugin, 0, 1);

        // TODO: add respawning method
        BukkitRunnable respawnTimerTask = new RespawnTimerTask(players::getPlayers, null);
        respawnTimerTask.runTaskTimer(plugin, 0, 1);

        BukkitRunnable projectileUpdateTask = new ProjectileUpdateTask(players::getPlayers, projectileManager);
        projectileUpdateTask.runTaskTimer(plugin, 0, 1);

        BukkitRunnable weaponManagerTimerTask = new BukkitRunnable() {
            @Override
            public void run() {timer++;}
        };
        weaponManagerTimerTask.runTaskTimer(plugin, 0, 20);

        BukkitRunnable playerParticlesTask = new PlayerParticlesTask(players::getPlayers);
        playerParticlesTask.runTaskTimer(plugin, 0, 1);

        tasks = List.of(weaponReloadTask, respawnTimerTask, projectileUpdateTask,
                weaponManagerTimerTask, playerParticlesTask);

    }

    public void incrementTimer () {
        timer++;
    }

    public MapMechanicsManager mapMechanicsManager() {
        return mapMechanicsManager;
    }
}
