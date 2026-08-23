package neonique.cbcplugin_new.combat;

import neonique.cbcplugin_new.combat.display.CombatDisplay;
import neonique.cbcplugin_new.combat.events.CBCPlayerDeathEvent;
import neonique.cbcplugin_new.combat.listeners.*;
import neonique.cbcplugin_new.combat.tasks.*;
import neonique.cbcplugin_new.core.CBCPlayer;
import neonique.cbcplugin_new.core.PlayerStore;
import neonique.cbcplugin_new.mapconfig.CBCMap;
import neonique.cbcplugin_new.mapmechanics.MapMechanicsManager;
import neonique.cbcplugin_new.scoreboard.CBCScoreboardManager;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

public class CombatSession implements CombatContext, Listener {

    private final Plugin plugin;
    private final World world;
    private final PlayerStore players;
    private CombatDisplay combatDisplay;

    private final MapMechanicsManager mapMechanicsManager;
    private final ProjectileManager projectileManager;

    private List<Listener> listeners = new ArrayList<>();
    private List<BukkitTask> tasks = new ArrayList<>();

    private Consumer<DeathInfo> deathListener = (d) -> {};
    private Consumer<CBCPlayer> joinAfterDeathListener = (d) -> {};
    private Consumer<CBCPlayer> respawnListener = (p) -> {};

    private final List<int[]> chunksLoaded = new ArrayList<>();

    private int timer = 0;

    public CombatSession (Plugin plugin, World world, CBCScoreboardManager scoreboardManager, PlayerStore players) {

        this.plugin = plugin;
        this.world = world;
        this.players = players;

        this.mapMechanicsManager = new MapMechanicsManager(world, this);
        this.projectileManager = new ProjectileManager(scoreboardManager, this);

    }

    public void setCombatDisplay (CombatDisplay combatDisplay) {
        this.combatDisplay = combatDisplay;
    }

    public void setDeathListener (Consumer<DeathInfo> deathListener) {
        this.deathListener = deathListener;
    }

    public void setJoinAfterDeathListener (Consumer<CBCPlayer> joinAfterDeathListener) {
        this.joinAfterDeathListener = joinAfterDeathListener;
    }

    public void setRespawnListener (Consumer<CBCPlayer> respawnListener) {
        this.respawnListener = respawnListener;
    }

    public void activate () {
        setupListeners();
        setupTasks();
        projectileManager.setup();
    }

    public void setupMap (CBCMap map) {
        unForceLoadChunks();
        loadChunks(map.lowerBound(), map.upperBound());
        mapMechanicsManager.setupMapMechanics(map);
        combatDisplay.setDeathMessageProvider(map.deathMessageProvider());
    }

    public void loadChunks (Location lowerBound, Location upperBound) {

        int minChunkX = lowerBound.getBlockX() >> 4;
        int maxChunkX = upperBound.getBlockX() >> 4;
        int minChunkZ = lowerBound.getBlockZ() >> 4;
        int maxChunkZ = upperBound.getBlockZ() >> 4;

        for (int x = minChunkX; x <= maxChunkX; x++) {
            for (int z = minChunkZ; z <= maxChunkZ; z++) {
                world.getChunkAt(x, z, false).setForceLoaded(true);
                chunksLoaded.add(new int[]{x, z});
            }
        }

    }

    public void unForceLoadChunks () {
        for (int[] chunkCoords : chunksLoaded) {
            if (chunkCoords.length == 2) {
                world.getChunkAt(chunkCoords[0], chunkCoords[1], false).setForceLoaded(false);
            }
        }
        chunksLoaded.clear();
    }

    public void deactivate () {

        for (Listener listener : listeners) {
            HandlerList.unregisterAll(listener);
        }

        for (BukkitTask task : tasks) {
            if (!task.isCancelled()) task.cancel();
        }

        mapMechanicsManager.unregisterAll();
        projectileManager.cleanup();
        unForceLoadChunks();

    }

    @EventHandler
    public void playerDeath (CBCPlayerDeathEvent e) {

        if (!players.hasPlayer(e.victim())) return;
        if (e.killer() != null && !players.hasPlayer(e.killer())) return;

        playerDeath(e.victim(), e.killer(), e.cause(), e.direct());

    }

    // Runs when a player takes fatal damage
    public void playerDeath (CBCPlayer victim, CBCPlayer killer, DeathCause cause, boolean direct) {

        DeathInfo deathInfo = new DeathInfo(victim, killer, cause, direct, timer);

        victim.playerDie();

        if (killer != null) {
            killer.playerKill();
            killer.playerAfterKill(victim);
        }

        deathListener.accept(deathInfo);
        if (combatDisplay != null) {
            combatDisplay.onPlayerDeath(victim, killer, cause, direct);
        }

    }

    public void playerJoinAfterDeath(CBCPlayer player) {
        joinAfterDeathListener.accept(player);
    }

    public void playerRespawn (CBCPlayer player) {
        if (!player.isOnline()) return;
        player.playerSpawn();
        player.playRespawnedTitle();
        respawnListener.accept(player);
    }

    private void setupListeners () {

        listeners = List.of(
                this,
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

        BukkitTask weaponReloadTask = new WeaponReloadTask(players::players)
                .runTaskTimer(plugin, 0, 1);

        BukkitTask respawnTimerTask = new RespawnTimerTask(players::players, this::playerRespawn)
                .runTaskTimer(plugin, 0, 1);

        BukkitTask projectileUpdateTask = new ProjectileUpdateTask(players::players, projectileManager)
                .runTaskTimer(plugin, 0, 1);

        BukkitTask weaponManagerTimerTask = new BukkitRunnable() {
            @Override
            public void run() {timer++;}
        }
                .runTaskTimer(plugin, 0, 20);

        BukkitTask playerParticlesTask = new PlayerParticlesTask(players::players)
                .runTaskTimer(plugin, 0, 1);

        BukkitTask combatTimerTask = new BukkitRunnable() {
            @Override
            public void run() {
                incrementTimer();
            }
        }
                .runTaskTimer(plugin, 0, 1);

        BukkitTask tempImmunityTask = new TempImmunityTask(players, 5)
                .runTaskTimer(plugin, 0, 5);

        tasks = List.of(weaponReloadTask, respawnTimerTask, projectileUpdateTask,
                weaponManagerTimerTask, playerParticlesTask, combatTimerTask, tempImmunityTask);

    }

    public void incrementTimer () {
        timer++;
    }

    public MapMechanicsManager mapMechanicsManager() {
        return mapMechanicsManager;
    }

    public PlayerStore players () {
        return players;
    }

    public int timer () {
        return timer;
    }

    public Plugin plugin () {
        return plugin;
    }

}
