package neonique.cbcplugin_new.managers;

import com.destroystokyo.paper.event.player.PlayerJumpEvent;
import neonique.cbcplugin_new.CBCPlugin;
import neonique.cbcplugin_new.enums.DeathCause;
import neonique.cbcplugin_new.gamemodes._base.CBCMap;
import neonique.cbcplugin_new.gameobjects.*;
import neonique.cbcplugin_new.listeners.combat.*;
import neonique.cbcplugin_new.playerclasses.CBCPlayer;

import neonique.cbcplugin_new.tasks.weapontasks.*;
import neonique.cbcplugin_new.weapons.EquipmentFactory;
import neonique.cbcplugin_new.weapons.WeaponFactory;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.*;
import org.bukkit.Color;
import org.bukkit.entity.*;
import org.bukkit.event.entity.*;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;
import org.bukkit.scoreboard.Team;

import java.time.Duration;
import java.util.*;

public class CombatManager {

    // Weapons active
    private boolean active = false;
    private final GameManager gameManager;

    private final ProjectileManager projectileManager;
    private final EquipmentFactory equipmentFactory;
    private final WeaponFactory weaponFactory;

    // Death message manager
    private final DeathMessageManager deathMessageManager;

    // Colors for glowing arrows
    private Team flameZoneArrowTeam;
    private Team xbowArrowTeam;

    // Amount of times to run reload task
    public final static int RELOAD_TASK_FREQUENCY = 20; // Per second
    public final static int RELOAD_TASK_PERIOD = 1; // In ticks

    // Other weapon manager related stats
    private int healPadTimer = 10;
    private boolean allPlayersImmune = false;

    // Game mechanic options
    private double voidPlane = 0; // If set to zero, there is no void plane
    private boolean voidKill = true;
    private boolean lavaInstaKill = false;
    private boolean swimTimerEnabled;
    private int swimTimerLength;
    private boolean canTrapdoorsOpen = true;

    // Miscellaneous game variables
    private boolean beaconHeads = false;
    private boolean doDayCycle = false;
    private boolean nightVisionDisabled = false;

    // Map variables
    private Location voidTeleport = null;
    private Set<HealthPad> healthPadList = new HashSet<>();
    private boolean jumpPadsEnabled = false;
    private Set<JumpPad> jumpPadList = new HashSet<>();
    private boolean dashPadsEnabled = false;
    private Set<DashPad> dashPadList = new HashSet<>();

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
    private VoidTask voidTask;
    private HealPadDetectionTask healPadTask;
    private ResetPlayerLastHitTask resetPlayerLastHitTask;
    private WeaponManagerTimerTask weaponManagerTimerTask;
    private JumpPadTask jumpPadTask;
    private DashPadTask dashPadTask;
    private SwimTimerTask swimTimerTask;
    private DayCycleTask dayCycleTask;
    private PlayerParticlesTask playerParticlesTask;

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
        equipmentFactory = new EquipmentFactory(plugin.getTrimService());
        weaponFactory = new WeaponFactory();
        weaponFactory.resetWeaponPresetsToDefault();

        // Create instances of listeners
        entityDamagePlayerListener = new EntityDamagePlayerListener(gameManager, this);
        crossbowFiredListener = new CrossbowFiredListener(gameManager, this);
        playerItemListener = new PlayerItemListener(gameManager, this);
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
        // Create teams
        ScoreboardManager scoreboardManager = CBCPlugin.getPlugin().getServer().getScoreboardManager();
        // Team scoreboard object
        Scoreboard scoreboard = scoreboardManager.getMainScoreboard();

        // Create flame zone arrow color team
        flameZoneArrowTeam = scoreboard.getTeam("flameArrows");
        if (flameZoneArrowTeam == null) {
            flameZoneArrowTeam = scoreboard.registerNewTeam("flameArrows");
        }
        flameZoneArrowTeam.color(NamedTextColor.GOLD); // Set team color

        // Create xbow arrow color team
        xbowArrowTeam = scoreboard.getTeam("xbowArrows");
        if (xbowArrowTeam == null) {
            xbowArrowTeam = scoreboard.registerNewTeam("xbowArrows");
        }
        xbowArrowTeam.color(NamedTextColor.AQUA); // Set team color

        if (gameManager.getCbcScoreboardManager().isActive()) {
            gameManager.getCbcScoreboardManager().registerTeamForAllClients(flameZoneArrowTeam);
            gameManager.getCbcScoreboardManager().registerTeamForAllClients(xbowArrowTeam);
        }

        // Activate tasks
        weaponReloadTask = new WeaponReloadTask(gameManager, this);
        projectileUpdateTask = new ProjectileUpdateTask(gameManager, projectileManager);
        voidTask = new VoidTask(gameManager, this);
        healPadTask = new HealPadDetectionTask(gameManager, this);
        resetPlayerLastHitTask = new ResetPlayerLastHitTask(gameManager);
        weaponManagerTimerTask = new WeaponManagerTimerTask(this);
        playerParticlesTask = new PlayerParticlesTask(gameManager);


        weaponReloadTask.runTaskTimer(plugin, 0, RELOAD_TASK_PERIOD);
        projectileUpdateTask.runTaskTimer(plugin, 0, 1L);
        voidTask.runTaskTimer(plugin, 0, 1L);
        healPadTask.runTaskTimer(plugin, 0, 2L);
        resetPlayerLastHitTask.runTaskTimer(plugin, 0, 20L);
        weaponManagerTimerTask.runTaskTimer(plugin, 0, 1L);
        playerParticlesTask.runTaskTimer(plugin, 0, 1L);

        dashPadTask = new DashPadTask(gameManager, this);
        dashPadTask.runTaskTimer(plugin, 0, 2L);

        jumpPadTask = new JumpPadTask(gameManager, this);
        jumpPadTask.runTaskTimer(plugin, 0, 2L);

        swimTimerTask = new SwimTimerTask(gameManager, this);
        swimTimerTask.runTaskTimer(plugin, 0, 1L);

        if (doDayCycle) {
            dayCycleTask = new DayCycleTask(this, gameManager.getWorld(), 8);
            dayCycleTask.runTaskTimer(plugin, 0, 1);
        }

        voidKill = true;

        // Enable all heal pads
        healPadTimer = 20;
        enableAllHealPads();

        timer = 0;

    }

    public void setupMap (CBCMap map) {

        if (active) {

            // Disable all current jump pads, heal pads and dash pads, along with other mechanics
            clearHealthPadList();
            clearJumpPadList();
            clearDashPadList();

            jumpPadsEnabled = false;
            swimTimerEnabled = false;
            dashPadsEnabled = false;

        }

        setHealthPadList(map.getHealthPads());

        if (active) {
            enableAllHealPads();
        }

        setGameMechanics(map.getVoidPlane());
        if (map.isJumpPadsEnabled()) {
            setJumpPadsEnabled(true);
            setJumpPadList(map.getJumpPads());
        }

        if (map.isDashPadsEnabled()) {
            setDashPadsEnabled(true);
            setDashPadList(map.getDashPads());
        }

        if (map.isSwimTimerEnabled()) {
            setSwimTimerEnabled(true);
            setSwimTimerLength(120);
        }

        if (map.isInstaKillLava()) {
            lavaInstaKill = true;
        }

        voidTeleport = map.getMapCentre();
        nightVisionDisabled = map.isNightVisionAlwaysDisabled();
        canTrapdoorsOpen = map.isTrapdoorsOpening();
        deathMessageManager.setOverrides(map.getDeathMessageOverrides());

    }

    public void disableWeapons () {

        active = false;

        // Disable weapon listeners
        EntityShootBowEvent.getHandlerList().unregister(crossbowFiredListener);
        EntityDamageByEntityEvent.getHandlerList().unregister(entityDamagePlayerListener);
        PlayerDropItemEvent.getHandlerList().unregister(playerItemListener);
        EntityPickupItemEvent.getHandlerList().unregister(playerItemListener);
        PlayerSwapHandItemsEvent.getHandlerList().unregister(playerItemListener);
        InventoryClickEvent.getHandlerList().unregister(playerItemListener);
        PlayerItemHeldEvent.getHandlerList().unregister(playerItemListener);
        EntityDamageEvent.getHandlerList().unregister(playerMiscDamageListener);
        PlayerDeathEvent.getHandlerList().unregister(playerDeathListener);
        EntityDamageByBlockEvent.getHandlerList().unregister(lavaDamageListener);
        PlayerJumpEvent.getHandlerList().unregister(playerJumpListener);
        ProjectileHitEvent.getHandlerList().unregister(arrowHitPlayerListener);
        PlayerInteractEvent.getHandlerList().unregister(blockInteractListener);

        flameZoneArrowTeam.unregister();
        xbowArrowTeam.unregister();

        // Disable tasks
        cancelTask(weaponReloadTask);
        cancelTask(projectileUpdateTask);
        cancelTask(voidTask);
        cancelTask(healPadTask);
        cancelTask(resetPlayerLastHitTask);
        cancelTask(weaponManagerTimerTask);
        cancelTask(jumpPadTask);
        cancelTask(swimTimerTask);
        cancelTask(dashPadTask);
        cancelTask(playerParticlesTask);

        if (dayCycleTask != null) {
            dayCycleTask.cancel();
        }

        // Set weapon presets back to default
        weaponFactory.resetWeaponPresetsToDefault();
        projectileManager.clearAllProjectiles();

        // Disable heal pads
        clearHealthPadList();
        clearJumpPadList();
        clearDashPadList();

        jumpPadsEnabled = false;
        swimTimerEnabled = false;
        dashPadsEnabled = false;
        beaconHeads = false;
        doDayCycle = false;
        nightVisionDisabled = false;

    }

    // Runs when a player takes fatal damage
    public void playerDeath (CBCPlayer playerKilled, CBCPlayer playerKiller, DeathCause cause, boolean direct) {

        World world = gameManager.getWorld();

        // Make sure that the player who is killed is still alive
        if (!playerKilled.isAlive()) {
            return;
        }

        // Get death message
        Component deathMessage = deathMessageManager.getDeathMessage(playerKilled, playerKiller, cause, direct);

        // Send death message to everyone
        if (deathMessage != null) {
            deathMessage = playerKilled.modifyDeathMessage(playerKiller, deathMessage);
            if (playerKiller != null) {
                deathMessage = playerKiller.modifyDeathMessageAsKiller(playerKilled, deathMessage);
            }
            gameManager.sendGlobalMessage(deathMessage);
        }

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
            Location particleLocation = location.clone().add(0, 1, 0);

            // Create effect or sound depending on player death cause
            if (cause == DeathCause.CREEPER) {

                // Summon a firework
                Firework firework = (Firework) world.spawnEntity(location.clone().add(0, 2, 0),
                        EntityType.FIREWORK_ROCKET, CreatureSpawnEvent.SpawnReason.COMMAND);
                FireworkMeta fireworkMeta = firework.getFireworkMeta();

                Color fireworkColor = Color.WHITE;

                if (playerKilled.getTeam() != null) {
                    fireworkColor = Color.fromRGB(playerKilled.getTeam().getColor().value());
                }

                fireworkMeta.addEffect(FireworkEffect.builder().withColor(fireworkColor)
                        .with(FireworkEffect.Type.BALL).build());

                fireworkMeta.setPower(0);

                firework.setFireworkMeta(fireworkMeta);
                firework.detonate();

            } else if (cause == DeathCause.FLAMEZONE) {
                gameManager.playSound(location, Sound.ITEM_FIRECHARGE_USE, 4, 1);
                world.spawnParticle(Particle.TRIAL_SPAWNER_DETECTION, particleLocation, 15, 0.4, 0, 0.4, 0.01);
            } else if (cause == DeathCause.XBOW || cause == DeathCause.XBOW_PIGLIN) {
                gameManager.playSound(location, Sound.BLOCK_GLASS_BREAK, 4, 0);
                world.spawnParticle(Particle.SOUL_FIRE_FLAME, particleLocation, 20, 0, 0, 0, 0.5);
            } else if (cause == DeathCause.VOID) {
                gameManager.playSound(location, Sound.ENTITY_ITEM_PICKUP, 4, 1);
                world.spawnParticle(Particle.INSTANT_EFFECT, particleLocation, 80, 0, 1, 0, 1);
            } else if (cause == DeathCause.MELEE) {
                gameManager.playSound(location, Sound.ENTITY_ITEM_BREAK, 4, 1);
            } else if (cause == DeathCause.DROWN) {
                world.spawnParticle(Particle.BUBBLE_POP, particleLocation, 150, 0.5, 0.5, 0.5, 0.5);
                gameManager.playSound(location, Sound.ENTITY_ZOMBIE_CONVERTED_TO_DROWNED, 3F, (float) 1);
            } else if (cause == DeathCause.LAVA) {
                world.spawnParticle(Particle.LAVA, particleLocation, 40, 0.5, 0.5, 0.5, 0.5);
                gameManager.playSound(location, Sound.ENTITY_PLAYER_HURT_ON_FIRE, 3F, (float) 1);
            } else if (cause == DeathCause.COMMAND) {
                world.spawnParticle(Particle.INSTANT_EFFECT, particleLocation, 80, 0.5, 0.5, 0.5, 0.5);
                gameManager.playSound(location, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, 3F, (float) 1);
                gameManager.playSound(location, Sound.ENTITY_LIGHTNING_BOLT_IMPACT, 3F, (float) 1);
            }

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

    public void setGameMechanics(int voidPlane) {
        this.voidPlane = voidPlane;
    }

    public boolean voidEnabled() {
        return voidPlane > 0;
    }

    public void setVoidKill(boolean b) {
        voidKill = b;
    }

    public double getVoidPlane() {
        return voidPlane;
    }

    // Heal pad related methods
    public void setHealthPadList(Set<HealthPad> newHealthPadList) {
        clearHealthPadList();
        healthPadList = newHealthPadList;
    }

    public void clearHealthPadList() {
        disableAllHealPads();
        healthPadList.clear();
    }

    public void enableAllHealPads() {
        // Enable heal pads
        for (HealthPad healPad : healthPadList) {
            healPad.enable(true);
        }
    }

    public void disableAllHealPads() {
        // Enable heal pads
        for (HealthPad healPad : healthPadList) {
            healPad.disable();
        }
    }

    public Set<HealthPad> getHealthPadList() {
        return healthPadList;
    }

    public int getHealPadTimer() {
        return healPadTimer;
    }

    public int getHealPadHealing() {
        return 6;
    }

    public Set<Entity> getHealthPadItemList () {
        Set<Entity> itemList = new HashSet<>();
        for (HealthPad pad : getHealthPadList()) {
            Entity item = pad.getItem();
            if (item != null) {
                itemList.add(item);
            }
        }
        return itemList;
    }

    public boolean isVoidKill() {
        return voidKill;
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

    // Jump pad related methods
    public boolean isJumpPadsEnabled() {
        return jumpPadsEnabled;
    }

    public void setJumpPadList(Set<JumpPad> newJumpPadList) {
        clearJumpPadList();
        jumpPadList = newJumpPadList;
    }

    public void clearJumpPadList() {
        jumpPadList.clear();
    }

    public Set<JumpPad> getJumpPadList() {
        return jumpPadList;
    }

    public void setJumpPadsEnabled(boolean b) {
        jumpPadsEnabled = b;
    }

    // Dash pad related methods
    public boolean isDashPadsEnabled() {
        return dashPadsEnabled;
    }

    public void setDashPadList(Set<DashPad> newDashPadList) {
        clearDashPadList();
        dashPadList = newDashPadList;
    }

    public void clearDashPadList() {
        dashPadList.clear();
    }

    public Set<DashPad> getDashPadList() {
        return dashPadList;
    }

    public void setDashPadsEnabled(boolean b) {
        dashPadsEnabled = b;
    }

    public boolean isSwimTimerEnabled() {
        return swimTimerEnabled;
    }

    public int getSwimTimerLength() {
        return swimTimerLength;
    }

    public void setSwimTimerEnabled(boolean b) {
        swimTimerEnabled = b;
    }

    public void setSwimTimerLength(int swimTimerLength) {
        this.swimTimerLength = swimTimerLength;
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

    // Lava kills
    public boolean isLavaInstaKill() {
        return lavaInstaKill;
    }

    private void cancelTask (BukkitRunnable task) {
        if (task == null) return;
        if (task.isCancelled()) return;
        task.cancel();
    }

    public JumpPadTask getJumpPadTask() {
        return jumpPadTask;
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
        for (CBCPlayer player : gameManager.getAlivePlayers()) {
            player.setImmune(allPlayersImmune);
        }
    }

    public boolean isAllPlayersImmune () {
        return allPlayersImmune;
    }

    public Location getVoidTeleport () {
        return voidTeleport;
    }

    public EquipmentFactory getEquipmentFactory () {
        return equipmentFactory;
    }

    public WeaponFactory getWeaponFactory() {
        return weaponFactory;
    }
}
