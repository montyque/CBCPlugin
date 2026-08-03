package neonique.cbcplugin_new.combat;

import neonique.cbcplugin_new.combat.events.CBCPlayerDeathEvent;
import neonique.cbcplugin_new.combat.listeners.*;
import neonique.cbcplugin_new.combat.tasks.PlayerParticlesTask;
import neonique.cbcplugin_new.combat.tasks.ProjectileUpdateTask;
import neonique.cbcplugin_new.combat.tasks.RespawnTimerTask;
import neonique.cbcplugin_new.combat.tasks.WeaponReloadTask;
import neonique.cbcplugin_new.core.CBCPlayer;
import neonique.cbcplugin_new.core.PlayerStore;
import neonique.cbcplugin_new.mapconfig.CBCMap;
import neonique.cbcplugin_new.mapmechanics.MapMechanicsManager;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class CombatSession implements Listener {

    private final Plugin plugin;
    private final World world;
    private final PlayerStore players;
    private CombatDisplay combatDisplay;

    private final MapMechanicsManager mapMechanicsManager;
    private final ProjectileManager projectileManager;

    private List<Listener> listeners = new ArrayList<>();
    private List<BukkitRunnable> tasks = new ArrayList<>();

    private Consumer<DeathInfo> deathListener = (d) -> {};
    private Consumer<CBCPlayer> afterDeathListener = (d) -> {};
    private Consumer<CBCPlayer> respawnListener = (p) -> {};

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

    public void setAfterDeathListener (Consumer<CBCPlayer> afterDeathListener) {
        this.afterDeathListener = afterDeathListener;
    }

    public void setDeathListener (Consumer<DeathInfo> deathListener) {
        this.deathListener = deathListener;
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

    @EventHandler
    public void playerDeath (CBCPlayerDeathEvent e) {

        if (players.hasPlayer(e.victim())) return;
        if (e.killer() != null && players.hasPlayer(e.killer())) return;

        playerDeath(e.victim(), e.killer(), e.cause(), e.direct());

    }

    public void playerDeath (CBCPlayer playerKilled, DeathCause cause) {
        playerDeath(playerKilled, playerKilled.getLastPlayerHitBy(), cause, false);
    }

    // Runs when a player takes fatal damage
    public void playerDeath (CBCPlayer victim, CBCPlayer killer, DeathCause cause, boolean direct) {

        DeathInfo deathInfo = new DeathInfo(victim, killer, cause, direct, timer);

        victim.playerDie();
        victim.playerAfterDeath(killer);

        if (killer != null) {
            killer.playerKill(deathInfo);
            killer.playerAfterKill(victim);
        }

        deathListener.accept(deathInfo);
        if (combatDisplay != null) {
            combatDisplay.onPlayerDeath(victim, killer, cause, direct);
        }

    }

    public void playerAfterDeath (CBCPlayer player) {
        player.afterDeath();
        afterDeathListener.accept(player);
    }

    public void playerRespawn (CBCPlayer player) {
        if (!player.isOnline()) return;
        player.playerSpawn();
        respawnListener.accept(player);
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

        BukkitRunnable weaponReloadTask = new WeaponReloadTask(players::players);
        weaponReloadTask.runTaskTimer(plugin, 0, 1);

        // TODO: add respawning method
        BukkitRunnable respawnTimerTask = new RespawnTimerTask(players::players, this::playerRespawn);
        respawnTimerTask.runTaskTimer(plugin, 0, 1);

        BukkitRunnable projectileUpdateTask = new ProjectileUpdateTask(players::players, projectileManager);
        projectileUpdateTask.runTaskTimer(plugin, 0, 1);

        BukkitRunnable weaponManagerTimerTask = new BukkitRunnable() {
            @Override
            public void run() {timer++;}
        };
        weaponManagerTimerTask.runTaskTimer(plugin, 0, 20);

        BukkitRunnable playerParticlesTask = new PlayerParticlesTask(players::players);
        playerParticlesTask.runTaskTimer(plugin, 0, 1);

        BukkitRunnable combatTimerTask = new BukkitRunnable() {
            @Override
            public void run() {
                incrementTimer();
            }
        };
        combatTimerTask.runTaskTimer(plugin, 0, 1);

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
