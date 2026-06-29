package neonique.cbcplugin_new.combat;

import neonique.cbcplugin_new.CBCPlugin;
import neonique.cbcplugin_new.combat.listeners.*;
import neonique.cbcplugin_new.combat.tasks.*;
import neonique.cbcplugin_new.mapconfig.CBCMap;
import neonique.cbcplugin_new.managers.DeathMessageManager;
import neonique.cbcplugin_new.managers.GameManager;
import neonique.cbcplugin_new.mapmechanics.*;
import neonique.cbcplugin_new.core.CBCPlayer;

import neonique.cbcplugin_new.scoreboard.CBCScoreboardManager;
import neonique.cbcplugin_new.scoreboard.CBCScoreboardTeam;
import neonique.cbcplugin_new.weapons.EquipmentFactory;
import neonique.cbcplugin_new.weapons.WeaponFactory;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.time.Duration;

public class CombatManager {

    // Weapons active
    private boolean active = false;
    private final GameManager gameManager;

    private final ProjectileManager projectileManager;
    private final MapMechanicsManager mapMechanicsManager;
    private final EquipmentFactory equipmentFactory;
    private final WeaponFactory weaponFactory;

    // Death message manager
    private final DeathMessageManager deathMessageManager;

    // Colors for glowing arrows
    private CBCScoreboardTeam flameZoneArrowTeam;
    private CBCScoreboardTeam xbowArrowTeam;

    // Amount of times to run reload task
    public final static int RELOAD_TASK_FREQUENCY = 20; // Per second
    public final static int RELOAD_TASK_PERIOD = 1; // In ticks

    // Other weapon manager related stats
    private boolean allPlayersImmune = false;

    // Game mechanic options
    private boolean canTrapdoorsOpen = true;

    // Miscellaneous game variables
    private boolean beaconHeads = false;
    private boolean doDayCycle = false;
    private boolean nightVisionDisabled = false;

    // Event listeners used
    private final CrossbowFiredListener crossbowFiredListener;
    private final EntityDamagePlayerListener entityDamagePlayerListener;
    private final PlayerItemListener playerItemListener;
    private final PlayerMiscDamageListener playerMiscDamageListener;
    private final PlayerDeathListener playerDeathListener;
    private final PlayerJumpListener playerJumpListener;
    private final LavaDamageListener lavaDamageListener;
    private final ArrowHitPlayerListener arrowHitPlayerListener;
    private final BlockInteractListener blockInteractListener;

    // Tasks used
    private WeaponReloadTask weaponReloadTask;
    private ProjectileUpdateTask projectileUpdateTask;

    private ResetPlayerLastHitTask resetPlayerLastHitTask;
    private WeaponManagerTimerTask weaponManagerTimerTask;
    private DayCycleTask dayCycleTask;
    private PlayerParticlesTask playerParticlesTask;
    private RespawnTimerTask respawnTimerTask;

    // Time tracking variable
    private int timer;

    public CombatManager(GameManager gameManager) {

        CBCPlugin plugin = CBCPlugin.getPlugin();

        this.gameManager = gameManager;

        // Load death messages
        deathMessageManager = new DeathMessageManager();
        boolean success = deathMessageManager.loadDeathMessages();

        if (success) {
            CBCPlugin.getPlugin().getLogger().info("Successfully loaded death messages!");
        } else {
            CBCPlugin.getPlugin().getLogger().warning("Did not successfully load death messages!");
        }

        // Create factories
        projectileManager = new ProjectileManager();
        mapMechanicsManager = new MapMechanicsManager(gameManager.getPlayerRegistry(), this);
        equipmentFactory = new EquipmentFactory(plugin.getTrimService());
        weaponFactory = new WeaponFactory();
        weaponFactory.resetWeaponPresetsToDefault();

        // Create instances of listeners
        entityDamagePlayerListener = new EntityDamagePlayerListener(gameManager, this);
        crossbowFiredListener = new CrossbowFiredListener(gameManager.getPlayerRegistry(), projectileManager);
        playerItemListener = new PlayerItemListener(gameManager.getPlayerRegistry());
        playerMiscDamageListener = new PlayerMiscDamageListener(gameManager, this);
        playerDeathListener = new PlayerDeathListener(gameManager, this);
        lavaDamageListener = new LavaDamageListener(gameManager, this);
        playerJumpListener = new PlayerJumpListener(gameManager, this);
        arrowHitPlayerListener = new ArrowHitPlayerListener(gameManager, this);
        blockInteractListener = new BlockInteractListener(gameManager, this);

    }

    public void activateWeapons () {

        active = true;
        allPlayersImmune = false;

        // Activate weapon listeners
        CBCPlugin plugin = CBCPlugin.getPlugin();
        plugin.getServer().getPluginManager().registerEvents(crossbowFiredListener, plugin);
        plugin.getServer().getPluginManager().registerEvents(entityDamagePlayerListener, plugin);
        plugin.getServer().getPluginManager().registerEvents(playerItemListener, plugin);
        plugin.getServer().getPluginManager().registerEvents(playerMiscDamageListener, plugin);
        plugin.getServer().getPluginManager().registerEvents(playerDeathListener, plugin);
        plugin.getServer().getPluginManager().registerEvents(lavaDamageListener, plugin);
        plugin.getServer().getPluginManager().registerEvents(playerJumpListener, plugin);
        plugin.getServer().getPluginManager().registerEvents(arrowHitPlayerListener, plugin);
        plugin.getServer().getPluginManager().registerEvents(blockInteractListener, plugin);

        if (nightVisionDisabled) {
            for (Player player : gameManager.getWorld().getPlayers()) {
                player.removePotionEffect(PotionEffectType.NIGHT_VISION);
            }
        }

        // Create glowing arrow teams
        CBCScoreboardManager scoreboardManager = gameManager.getCbcScoreboardManager();
        flameZoneArrowTeam = scoreboardManager.registerNewTeam("flameArrows");
        flameZoneArrowTeam.setColor(NamedTextColor.GOLD);
        xbowArrowTeam = scoreboardManager.registerNewTeam("xbowArrows");
        xbowArrowTeam.setColor(NamedTextColor.AQUA);

        // Activate tasks
        weaponReloadTask = new WeaponReloadTask(gameManager.getPlayerRegistry());
        respawnTimerTask = new RespawnTimerTask(gameManager.getPlayerRegistry(), this);
        projectileUpdateTask = new ProjectileUpdateTask(gameManager.getPlayerRegistry(), projectileManager);

        resetPlayerLastHitTask = new ResetPlayerLastHitTask(gameManager);
        weaponManagerTimerTask = new WeaponManagerTimerTask(this);
        playerParticlesTask = new PlayerParticlesTask(gameManager);

        weaponReloadTask.runTaskTimer(plugin, 0, RELOAD_TASK_PERIOD);
        respawnTimerTask.runTaskTimer(plugin, 0, 1L);
        projectileUpdateTask.runTaskTimer(plugin, 0, 1L);

        resetPlayerLastHitTask.runTaskTimer(plugin, 0, 20L);
        weaponManagerTimerTask.runTaskTimer(plugin, 0, 1L);
        playerParticlesTask.runTaskTimer(plugin, 0, 1L);

        if (doDayCycle) {
            dayCycleTask = new DayCycleTask(this, gameManager.getWorld(), 8);
            dayCycleTask.runTaskTimer(plugin, 0, 1);
        }

        timer = 0;

    }

    public void setupMap (CBCMap map) {

        mapMechanicsManager.setupMapMechanics(map);
        canTrapdoorsOpen = map.isTrapdoorsOpening();
        deathMessageManager.setOverrides(map.getDeathMessageOverrides());

    }

    public void disableWeapons () {

        active = false;

        // Disable all map mechanics
        mapMechanicsManager.unregisterAll();

        // Disable weapon listeners
        CBCPlugin plugin = CBCPlugin.getPlugin();
        plugin.unregisterListener(crossbowFiredListener);
        plugin.unregisterListener(entityDamagePlayerListener);
        plugin.unregisterListener(playerItemListener);
        plugin.unregisterListener(playerMiscDamageListener);
        plugin.unregisterListener(playerDeathListener);
        plugin.unregisterListener(playerJumpListener);
        plugin.unregisterListener(arrowHitPlayerListener);
        plugin.unregisterListener(blockInteractListener);

        gameManager.getCbcScoreboardManager().unregisterTeam(flameZoneArrowTeam);
        gameManager.getCbcScoreboardManager().unregisterTeam(xbowArrowTeam);

        // Disable tasks
        cancelTask(weaponReloadTask);
        cancelTask(projectileUpdateTask);
        cancelTask(resetPlayerLastHitTask);
        cancelTask(weaponManagerTimerTask);
        cancelTask(playerParticlesTask);
        cancelTask(respawnTimerTask);

        if (dayCycleTask != null) {
            dayCycleTask.cancel();
        }

        // Set weapon presets back to default
        weaponFactory.resetWeaponPresetsToDefault();
        projectileManager.clearAllProjectiles();

        beaconHeads = false;
        doDayCycle = false;
        nightVisionDisabled = false;

    }

    public void playerDeath (CBCPlayer playerKilled, DeathCause cause) {
        playerDeath(playerKilled, playerKilled.getLastPlayerHitBy(), cause, false);
    }

    // Runs when a player takes fatal damage
    public void playerDeath (CBCPlayer playerKilled, CBCPlayer playerKiller, DeathCause cause, boolean direct) {

        // Make sure that the player who is killed is still alive
        if (!playerKilled.isAlive()) {
            return;
        }

        // Get death message
        Component deathMessage = deathMessageManager.getDeathMessage(playerKilled, playerKiller, cause, direct);

        // Send death message to everyone
        deathMessage = playerKilled.modifyDeathMessage(playerKiller, deathMessage);
        if (playerKiller != null) {
            deathMessage = playerKiller.modifyDeathMessageAsKiller(playerKilled, deathMessage);
        }
        gameManager.sendGlobalMessage(deathMessage);

        playerKilled.playerDie();
        playerKilled.playerAfterDeath(playerKiller);

        if (playerKiller != null) {
            playerKiller.playerKill();
            playerKiller.playerAfterKill(playerKilled);
            playerKiller.playerKillTitle(playerKilled, cause);
        }

        // Check if player that was killed is online
        if (playerKilled.isOnline()) {

            // Get player entity of the player who was killed
            Player playerKilledEntity = playerKilled.getPlayer();
            playerKilledEntity.setGameMode(GameMode.SPECTATOR);

            Location location = playerKilledEntity.getLocation();
            cause.playDeathEffect(gameManager, location, playerKilled);

            // Show death title
            playerKilledEntity.showTitle(playerKilled.getDeathTitle());
            playerKilled.updateActionBarDisplay(true);

        }

        // Get kill streak message
        if (playerKiller != null) {
            if (playerKiller.getKillStreak() >= 5) {
                // Get kill streak message
                Component killStreakMessage = deathMessageManager.getKillStreakMessage(playerKiller);
                // Send death message to everyone and play sound
                if (killStreakMessage != null) {
                    gameManager.sendGlobalMessage(killStreakMessage);
                    gameManager.playGlobalSound(Sound.ENTITY_ELDER_GUARDIAN_CURSE, 100, 1);
                }
            }
        }

        // Get kill streak ended message
        if (playerKilled.getKillStreak() >= 5) {
            Component killStreakEndMessage = deathMessageManager.getKillStreakEndedMessage(playerKilled, playerKiller);
            gameManager.sendGlobalMessage(killStreakEndMessage);
            gameManager.playGlobalSound(Sound.BLOCK_BEACON_DEACTIVATE, 100, 2);
        }
        playerKilled.playerKillStreakEnd();

    }

    // Respawn player
    public void playerRespawn (CBCPlayer playerRespawning) {

        if (!playerRespawning.isOnline()) {
            return;
        }
        Player playerEntity = playerRespawning.getPlayer();

        playerRespawning.playerSpawn();

        // Set gamemode of player to adventure and reset their stats
        playerEntity.setGameMode(GameMode.ADVENTURE);
        playerRespawning.healToFull();
        playerRespawning.setAlive(true); // Set player's state to alive

        // Show respawned title
        Component respawnedComponent = Component.text("Respawned!").color(NamedTextColor.GREEN)
                .decorate(TextDecoration.BOLD);
        Title respawnedTitle = Title.title(respawnedComponent, Component.empty(),Title.Times.times(
                Duration.ofMillis(0), Duration.ofMillis(250), Duration.ofMillis(250)));
        playerEntity.showTitle(respawnedTitle);
        playerEntity.playSound(playerEntity.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 5, 2);

        playerRespawning.updateActionBarDisplay(true);
    }

    public void setVoidKill(boolean b) {
        mapMechanicsManager.getMechanicsOfType(VoidMechanic.class).forEach(v -> v.setKillOnVoid(b));
    }

    public void enableAllHealPads() {
        // Enable heal pads
        mapMechanicsManager.getMechanicsOfType(HealthPadMechanic.class).forEach(HealthPadMechanic::enableAll);
    }

    public void disableAllHealPads() {
        // Enable heal pads
        mapMechanicsManager.getMechanicsOfType(HealthPadMechanic.class).forEach(HealthPadMechanic::disableAll);
    }

    public int getTimer () {
        return timer;
    }

    public void incrementTimer () {
        timer++;
    }

    public boolean isBeaconHeads() {
        return beaconHeads;
    }

    public void setBeaconHeadsEnabled (boolean b) {
        this.beaconHeads = b;
    }

    public void setDoDayCycleEnabled (boolean b) {
        this.doDayCycle = b;
    }

    public boolean isDayCycleEnabled() {
        return doDayCycle;
    }

    public void setNightVisionDisabled(boolean b) {
        this.nightVisionDisabled = b;
    }

    public boolean isNightVisionDisabled() {
        return nightVisionDisabled;
    }

    private void cancelTask (BukkitRunnable task) {
        if (task == null) return;
        if (task.isCancelled()) return;
        task.cancel();
    }

    public boolean isCanTrapdoorsOpen() {
        return canTrapdoorsOpen;
    }

    public DeathMessageManager getDeathMessageManager () {
        return deathMessageManager;
    }

    public boolean isActive () {
        return active;
    }

    public ProjectileManager getProjectileManager() {
        return projectileManager;
    }

    public void setAllPlayersImmune (boolean b) {
        allPlayersImmune = b;
        for (CBCPlayer player : gameManager.getPlayerRegistry().getPlayers()) {
            if (player.isAlive()) player.setImmune(allPlayersImmune);
        }
    }

    public boolean isAllPlayersImmune () {
        return allPlayersImmune;
    }

    public EquipmentFactory getEquipmentFactory () {
        return equipmentFactory;
    }

    public WeaponFactory getWeaponFactory() {
        return weaponFactory;
    }

    public CBCScoreboardTeam xbowArrowTeam() {
        return xbowArrowTeam;
    }

    public CBCScoreboardTeam flameZoneArrowTeam() {
        return flameZoneArrowTeam;
    }

    public MapMechanicsManager mapMechanicsManager () {
        return mapMechanicsManager;
    }
}
