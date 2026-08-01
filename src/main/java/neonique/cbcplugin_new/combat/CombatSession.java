package neonique.cbcplugin_new.combat;

import neonique.cbcplugin_new.combat.listeners.*;
import neonique.cbcplugin_new.combat.tasks.PlayerParticlesTask;
import neonique.cbcplugin_new.combat.tasks.ProjectileUpdateTask;
import neonique.cbcplugin_new.combat.tasks.RespawnTimerTask;
import neonique.cbcplugin_new.combat.tasks.WeaponReloadTask;
import neonique.cbcplugin_new.core.PlayerStore;
import neonique.cbcplugin_new.mapmechanics.MapMechanicsManager;
import net.kyori.adventure.audience.Audience;
import org.bukkit.World;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;

public class CombatSession {

    private final Plugin plugin;
    private final World world;
    private final PlayerStore players;
    private final Audience audience;

    private final MapMechanicsManager mapMechanicsManager;
    private final ProjectileManager projectileManager;

    private List<Listener> listeners = new ArrayList<>();
    private List<BukkitRunnable> tasks = new ArrayList<>();

    private int timer = 0;

    public CombatSession (Plugin plugin, World world, Audience audience, PlayerStore players) {

        this.plugin = plugin;
        this.world = world;
        this.players = players;
        this.audience = audience;

        this.mapMechanicsManager = new MapMechanicsManager(players);
        this.projectileManager = new ProjectileManager();

    }

    public void activate () {
        setupListeners();
        setupTasks();
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

}
