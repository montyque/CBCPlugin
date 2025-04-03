package neonique.cbcplugin_new.managers;

import com.destroystokyo.paper.event.player.PlayerJumpEvent;
import neonique.cbcplugin_new.CBCPlugin;
import neonique.cbcplugin_new.enums.DeathCause;
import neonique.cbcplugin_new.enums.WeaponType;
import neonique.cbcplugin_new.enums.WeaponsState;
import neonique.cbcplugin_new.gamemodes._base.CBCTeam;
import neonique.cbcplugin_new.gamemodes._base.CBCMap;
import neonique.cbcplugin_new.gameobjects.*;
import neonique.cbcplugin_new.listeners.combat.*;
import neonique.cbcplugin_new.playerclasses.CBCPlayer;

import neonique.cbcplugin_new.tasks.weapontasks.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;

import net.kyori.adventure.title.Title;
import org.bukkit.*;
import org.bukkit.Color;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.*;
import org.bukkit.event.entity.*;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.CrossbowMeta;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.ScoreboardManager;
import org.bukkit.scoreboard.Team;
import org.bukkit.util.Vector;

import java.io.File;
import java.time.Duration;
import java.util.*;

public class CombatManager {

    // Weapons active
    private boolean active = false;

    private final GameManager gameManager;
    // Death message manager
    private final DeathMessageManager deathMessageManager;

    // Crossbow templates
    private static ItemStack creeperCannon;
    private static ItemStack loadedCreeperCannon;

    private static ItemStack flameZoner;
    private static ItemStack loadedFlameZoner;

    private static ItemStack xBow;
    private static ItemStack loadedXBow;

    // Colors for glowing arrows
    private Team flameZoneArrowTeam;
    private Team xbowArrowTeam;
    private Team healthPadItemTeam;

    // Weapon variables
    private CreeperPreset creeperWeaponVariables;
    private FlamePreset flameWeaponVariables;
    private XbowPreset xbowWeaponVariables;

    private HashMap<String, CreeperPreset> creeperPresetTeamOverrides;
    private HashMap<String, FlamePreset> flamePresetTeamOverrides;
    private HashMap<String, XbowPreset> xbowPresetTeamOverrides;

    // Amount of times to run reload task
    private int reloadTaskFrequency = 10; // Per second
    private int reloadTaskPeriod = 2; // In ticks

    // Other weapon variables that can be chosen from
    private HashMap<String, CreeperPreset> creeperPresets;
    private HashMap<String, FlamePreset> flamePresets;
    private HashMap<String, XbowPreset> xbowPresets;
    private HashMap<String, OverallPreset> overallPresets;

    // Set of current projectiles for game to keep track of
    public Set<UUID> creeperProjectileSet = new HashSet<>();
    public Set<UUID> flameZoneArrowSet = new HashSet<>();
    public Set<UUID> xbowArrowSet = new HashSet<>();

    // Other weapon manager related stats
    private int healPadTimer = 10;

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

    // Tasks/Runnables used
    private WeaponReloadTask weaponReloadTask;
    private CreeperTrackingTask creeperTrackingTask;
    private ArrowParticlesTask arrowParticlesTask;
    private ArrowHitGroundTask arrowHitGroundTask;
    private FlameZonerTask flameZonerTask;
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

        this.gameManager = gameManager;

        // Load death messages
        deathMessageManager = new DeathMessageManager();
        boolean success = deathMessageManager.loadDeathMessages();

        if (success) {
            CBCPlugin.getPlugin().getLogger().info("Successfully loaded death messages!");
        }
        else {
            CBCPlugin.getPlugin().getLogger().warning("Did not successfully load death messages!");
        }

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

        // CREATING WEAPON TEMPLATES TO COPY FOR EFFICIENCY
        // ******************************************
        // Creating creeper cannon as template
        creeperCannon = new ItemStack(Material.CROSSBOW);
        ItemMeta creeperCannonMeta = creeperCannon.getItemMeta();
        Component creeperTitle = Component.text("Creeper Cannon").color(TextColor.color(91, 183, 34))
                .decoration(TextDecoration.ITALIC, TextDecoration.State.FALSE);
        creeperCannonMeta.displayName(creeperTitle);
        creeperCannonMeta.addEnchant(Enchantment.QUICK_CHARGE, 10, true);
        PersistentDataContainer creeperCannonTags = creeperCannonMeta.getPersistentDataContainer();
        creeperCannonTags.set(new NamespacedKey(CBCPlugin.getPlugin(), "cbc_crossbow"), PersistentDataType.STRING, "CREEPER");
        creeperCannonTags.set(new NamespacedKey(CBCPlugin.getPlugin(), "cbc_loaded"), PersistentDataType.INTEGER, 0);
        creeperCannonMeta.setCustomModelData(1);
        creeperCannon.setItemMeta(creeperCannonMeta);


        // Creating loaded creeper cannon as template
        loadedCreeperCannon = creeperCannon.clone();
        CrossbowMeta loadedCreeperCannonMeta = (CrossbowMeta) loadedCreeperCannon.getItemMeta();
        ItemStack ccProjectile = new ItemStack(Material.TIPPED_ARROW);
        ItemMeta ccProjectileMeta = ccProjectile.getItemMeta();
        PotionMeta ccPotionMeta = (PotionMeta) ccProjectileMeta;
        ccPotionMeta.addCustomEffect(new PotionEffect(
                PotionEffectType.INSTANT_DAMAGE, 30, 0), true);
        ccPotionMeta.setColor(Color.GREEN);
        ccProjectile.setItemMeta(ccPotionMeta);
        loadedCreeperCannonMeta.addChargedProjectile(ccProjectile);
        PersistentDataContainer loadedCreeperCannonTags = loadedCreeperCannonMeta.getPersistentDataContainer();
        loadedCreeperCannonTags.set(new NamespacedKey(CBCPlugin.getPlugin(), "cbc_loaded"), PersistentDataType.INTEGER, 1);
        loadedCreeperCannonMeta.setCustomModelData(1);
        loadedCreeperCannon.setItemMeta(loadedCreeperCannonMeta);

        // ******************************************
        // Creating flame zoner as template
        flameZoner = new ItemStack(Material.CROSSBOW);
        ItemMeta flameZoneMeta = flameZoner.getItemMeta();
        Component flameTitle = Component.text("Flame Zoner").color(TextColor.color(232, 98, 58))
                .decoration(TextDecoration.ITALIC, TextDecoration.State.FALSE);
        flameZoneMeta.displayName(flameTitle);
        flameZoneMeta.addEnchant(Enchantment.QUICK_CHARGE, 10, true);
        PersistentDataContainer flameZonerTags = flameZoneMeta.getPersistentDataContainer();
        flameZonerTags.set(new NamespacedKey(CBCPlugin.getPlugin(), "cbc_crossbow"), PersistentDataType.STRING, "FLAME");
        flameZonerTags.set(new NamespacedKey(CBCPlugin.getPlugin(), "cbc_loaded"), PersistentDataType.INTEGER, 0);
        flameZoneMeta.setCustomModelData(5);
        flameZoner.setItemMeta(flameZoneMeta);


        // Creating loaded flame zoner as template
        loadedFlameZoner = flameZoner.clone();
        CrossbowMeta loadedFlameZonerMeta = (CrossbowMeta) loadedFlameZoner.getItemMeta();
        ItemStack fzProjectile = new ItemStack(Material.TIPPED_ARROW);
        ItemMeta fzProjectileMeta = fzProjectile.getItemMeta();
        PotionMeta fzPotionMeta = (PotionMeta) fzProjectileMeta;
        fzPotionMeta.addCustomEffect(new PotionEffect(
                PotionEffectType.FIRE_RESISTANCE, 30, 0), true);
        fzPotionMeta.setColor(Color.RED);
        fzProjectile.setItemMeta(fzPotionMeta);
        loadedFlameZonerMeta.addChargedProjectile(fzProjectile);
        PersistentDataContainer loadedFlameZonerTags = loadedFlameZonerMeta.getPersistentDataContainer();
        loadedFlameZonerTags.set(new NamespacedKey(CBCPlugin.getPlugin(), "cbc_loaded"), PersistentDataType.INTEGER, 1);
        flameZoneMeta.setCustomModelData(5);
        loadedFlameZoner.setItemMeta(loadedFlameZonerMeta);

        // ******************************************
        // Creating X-Bow as template
        xBow = new ItemStack(Material.CROSSBOW);
        ItemMeta xBowMeta = xBow.getItemMeta();
        Component xbowTitle = Component.text("X-Bow").color(TextColor.color(124, 226, 226))
                .decoration(TextDecoration.ITALIC, TextDecoration.State.FALSE);
        flameZoneMeta.displayName(flameTitle);
        xBowMeta.displayName(xbowTitle);
        xBowMeta.addEnchant(Enchantment.QUICK_CHARGE, 10, true);
        PersistentDataContainer xBowTags = xBowMeta.getPersistentDataContainer();
        xBowTags.set(new NamespacedKey(CBCPlugin.getPlugin(), "cbc_crossbow"), PersistentDataType.STRING, "XBOW");
        xBowTags.set(new NamespacedKey(CBCPlugin.getPlugin(), "cbc_loaded"), PersistentDataType.INTEGER, 0);
        xBowMeta.setCustomModelData(9);
        xBow.setItemMeta(xBowMeta);

        // Creating loaded X Bow as template
        loadedXBow = xBow.clone();
        CrossbowMeta loadedXBowMeta = (CrossbowMeta) loadedXBow.getItemMeta();
        ItemStack xbProjectile = new ItemStack(Material.ARROW);
        loadedXBowMeta.addChargedProjectile(xbProjectile);
        PersistentDataContainer loadedXBowTags = loadedXBowMeta.getPersistentDataContainer();
        loadedXBowTags.set(new NamespacedKey(CBCPlugin.getPlugin(), "cbc_loaded"), PersistentDataType.INTEGER, 1);
        loadedXBowMeta.setCustomModelData(9);
        loadedXBow.setItemMeta(loadedXBowMeta);

        // Load weapon presets
        loadWeaponPresets();

        // Reset weapon presets to default
        resetWeaponPresetsToDefault();

    }

    public void loadWeaponPresets () {

        creeperPresets = new HashMap<>();
        flamePresets = new HashMap<>();
        xbowPresets = new HashMap<>();

        // Attempt to find weapons folder
        File weaponsFolderFile = new File(CBCPlugin.getPlugin().getDataFolder(), "weapons");
        // Attempt to make this a directory
        if (!weaponsFolderFile.exists()) {
            boolean folderMade = weaponsFolderFile.mkdir();
            if (!folderMade) {
                return;
            }
        }

        // Get config file
        File file = new File(weaponsFolderFile, "weaponpresets.yml");
        if (!file.exists()) return;

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);

        // Creeper presets
        ConfigurationSection creeperSection = config.getConfigurationSection("CreeperPresets");
        if (creeperSection != null) {
            System.out.println("Creeper preset section found");
            for (String key : creeperSection.getKeys(false)) {
                ConfigurationSection creeperPresetSection = creeperSection.getConfigurationSection(key);
                if (creeperPresetSection != null) {
                    CreeperPreset preset = CreeperPreset.newPreset(key.toUpperCase(), creeperPresetSection);
                    creeperPresets.put(key, preset);
                }
            }
        }

        // Flame presets
        ConfigurationSection flameSection = config.getConfigurationSection("FlamePresets");
        if (flameSection != null) {
            System.out.println("Flame preset section found");
            for (String key : flameSection.getKeys(false)) {
                ConfigurationSection flamePresetSection = flameSection.getConfigurationSection(key);
                if (flamePresetSection != null) {
                    FlamePreset preset = FlamePreset.newPreset(key.toUpperCase(), flamePresetSection);
                    flamePresets.put(key, preset);
                }
            }
        }

        // Xbow presets
        ConfigurationSection xbowSection = config.getConfigurationSection("XbowPresets");
        if (xbowSection != null) {
            System.out.println("Flame preset section found");
            for (String key : xbowSection.getKeys(false)) {
                ConfigurationSection xbowPresetSection = xbowSection.getConfigurationSection(key);
                if (xbowPresetSection != null) {
                    XbowPreset preset = XbowPreset.newPreset(key.toUpperCase(), xbowPresetSection);
                    xbowPresets.put(key, preset);
                }
            }
        }

        // Put overall presets
        overallPresets = new HashMap<>();

        ConfigurationSection overallSection = config.getConfigurationSection("OverallPresets");
        if (overallSection != null) {
            System.out.println("Overall preset section found");
            for (String key : overallSection.getKeys(false)) {
                ConfigurationSection overallPresetSection = overallSection.getConfigurationSection(key);
                if (overallPresetSection != null) {

                    String creeperPreset = overallPresetSection.getString("Creeper");
                    String flamePreset = overallPresetSection.getString("Flame");
                    String xbowPreset = overallPresetSection.getString("Xbow");

                    if (!creeperPresets.containsKey(creeperPreset)) {
                        continue;
                    }
                    if (!flamePresets.containsKey(flamePreset)) {
                        continue;
                    }
                    if (!xbowPresets.containsKey(xbowPreset)) {
                        continue;
                    }

                    // Add overall preset
                    OverallPreset overallPreset = new OverallPreset(key, creeperPresets.get(creeperPreset), flamePresets.get(flamePreset),
                            xbowPresets.get(xbowPreset));

                    overallPresets.put(key, overallPreset);

                }
            }
        }

    }

    public void resetWeaponPresetsToDefault () {
        creeperWeaponVariables = CreeperPreset.getDefaultPreset();
        flameWeaponVariables = FlamePreset.getDefaultPreset();
        xbowWeaponVariables = XbowPreset.getDefaultPreset();

        creeperPresetTeamOverrides = new HashMap<>();
        flamePresetTeamOverrides = new HashMap<>();
        xbowPresetTeamOverrides = new HashMap<>();
    }

    public void setCreeperWeaponVariables(CreeperPreset creeperWeaponVariables) {
        this.creeperWeaponVariables = creeperWeaponVariables;
    }

    public void setFlameWeaponVariables(FlamePreset flameWeaponVariables) {
        this.flameWeaponVariables = flameWeaponVariables;
        if (arrowParticlesTask != null) {
            arrowParticlesTask.updateFlameRadius();
        }
    }

    public void setXbowWeaponVariables(XbowPreset xbowWeaponVariables) {
        this.xbowWeaponVariables = xbowWeaponVariables;
    }

    public CreeperPreset getCreeperPresetById (String presetId) {
        return creeperPresets.get(presetId);
    }

    public FlamePreset getFlamePresetById (String presetId) {
        return flamePresets.get(presetId);
    }

    public XbowPreset getXbowPresetById (String presetId) {
        return xbowPresets.get(presetId);
    }

    public OverallPreset getOverallPreset (String presetId) {
        return overallPresets.get(presetId);
    }

    public List<String> getPresetIds (WeaponType weaponType) {

        if (weaponType == WeaponType.CREEPER) {
            return new ArrayList<>(creeperPresets.keySet());
        }
        else if (weaponType == WeaponType.FLAME) {
            return new ArrayList<>(flamePresets.keySet());
        }
        else if (weaponType == WeaponType.XBOW) {
            return new ArrayList<>(xbowPresets.keySet());
        }
        return null;

    }

    public List<String> getOverallPresetIds () {
        return new ArrayList<>(overallPresets.keySet());
    }

    public static ItemStack fetchWeapon(WeaponType weapon, boolean loaded) {
        if (weapon == WeaponType.CREEPER) {if (loaded) {return loadedCreeperCannon;} else {return creeperCannon;}}
        else if (weapon == WeaponType.FLAME) {if (loaded) {return loadedFlameZoner;} else {return flameZoner;}}
        else {if (loaded) {return loadedXBow;} else {return xBow;}}
    }

    public void fireCreeper (CBCPlayer player, EntityShootBowEvent e) {

        Entity projFired = e.getProjectile();
        assert projFired instanceof Arrow;
        Arrow arrowFired = (Arrow) projFired;
        World world = arrowFired.getWorld();

        arrowFired.setDamage(0);
        Vector arrowVelocity = arrowFired.getVelocity();
        Location creeperSpawnLocation = arrowFired.getLocation();
        Entity creeperFired = world.spawnEntity(new Location(world, 0, 100, 0), EntityType.CREEPER, CreatureSpawnEvent.SpawnReason.COMMAND,
                creeper -> {
                    creeper.setVelocity(arrowVelocity.multiply(creeperWeaponVariables.getLaunchVelocityModifier()));
                    creeper.setInvulnerable(true);
                    }
                );
        Creeper creeperProjectile = (Creeper) creeperFired;
        // Modify creeper projectile
        creeperProjectile.setPowered(true);
        creeperProjectile.setExplosionRadius((int) creeperWeaponVariables.getCreeperExplosionRadius());
        PersistentDataContainer creeperTags = creeperProjectile.getPersistentDataContainer();
        creeperTags.set(new NamespacedKey(CBCPlugin.getPlugin(), "playerId"),
                PersistentDataType.INTEGER, player.getPlayerId());
        // Setting creeper team ID here - not yet done so
        // Adding tag to identify it as a creeper
        creeperProjectile.addScoreboardTag("firedCreeper");
        // Adding creeper to list to track it
        creeperProjectileSet.add(creeperProjectile.getUniqueId());
        // Teleport creeper to arrow
        creeperFired.teleport(creeperSpawnLocation);
        // Remove the arrow
        arrowFired.remove();

        // Rename creeper depending on team
        if (player.getTeam() != null) {
            String teamName = player.getTeam().getTeamName();
            creeperProjectile.customName(Component.text(teamName + "Creeper"));
        }

        // Add a timer before it can explode off walls
        new CreeperWallTimerTask(creeperProjectile.getUniqueId()).runTaskLater(CBCPlugin.getPlugin(), 2L);

    }

    public void fireFlame (CBCPlayer player, EntityShootBowEvent e) {

        Entity projFired = e.getProjectile();
        assert projFired instanceof Arrow;
        Arrow arrowFired = (Arrow) projFired;
        arrowFired.addScoreboardTag("flameArrow");
        arrowFired.setPickupStatus(AbstractArrow.PickupStatus.DISALLOWED);
        arrowFired.setInvulnerable(true);
        arrowFired.setDamage(1);
        arrowFired.setPierceLevel(20);

        PersistentDataContainer arrowTags = arrowFired.getPersistentDataContainer();
        arrowTags.set(new NamespacedKey(CBCPlugin.getPlugin(), "playerId"),
                PersistentDataType.INTEGER, player.getPlayerId());

        flameZoneArrowTeam.addEntry(arrowFired.getUniqueId().toString());
        gameManager.getCbcScoreboardManager().addTeamEntry(arrowFired.getUniqueId().toString(), flameZoneArrowTeam);
        arrowFired.setGlowing(true);

        // Adding arrow to list to track it
        flameZoneArrowSet.add(arrowFired.getUniqueId());

    }

    public void fireXBow (CBCPlayer player, EntityShootBowEvent e) {
        Entity projFired = e.getProjectile();
        assert projFired instanceof Arrow;
        Arrow arrowFired = (Arrow) projFired;
        arrowFired.setVelocity(arrowFired.getVelocity().multiply(xbowWeaponVariables.getArrowVelocityModifier()));
        arrowFired.addScoreboardTag("xbowArrow");
        arrowFired.setPickupStatus(AbstractArrow.PickupStatus.DISALLOWED);
        arrowFired.setInvulnerable(true);
        arrowFired.setDamage(1);
        arrowFired.setPierceLevel(20);

        xbowArrowTeam.addEntry(arrowFired.getUniqueId().toString());
        gameManager.getCbcScoreboardManager().addTeamEntry(arrowFired.getUniqueId().toString(), xbowArrowTeam);
        arrowFired.setGlowing(true);

        // Adding arrow to list to track it
        xbowArrowSet.add(arrowFired.getUniqueId());
    }

    public void fireXBowPiglin (EntityShootBowEvent e) {

        // Xbow fired
        Entity projFired = e.getProjectile();
        assert projFired instanceof Arrow;
        Arrow arrowFired = (Arrow) projFired;
        arrowFired.setVelocity(arrowFired.getVelocity().multiply(xbowWeaponVariables.getArrowVelocityModifier()));
        arrowFired.addScoreboardTag("xbowArrowPiglin");
        arrowFired.setPickupStatus(AbstractArrow.PickupStatus.DISALLOWED);
        arrowFired.setInvulnerable(true);
        arrowFired.setDamage(1);
        arrowFired.setPierceLevel(20);

        xbowArrowTeam.addEntry(arrowFired.getUniqueId().toString());
        gameManager.getCbcScoreboardManager().addTeamEntry(arrowFired.getUniqueId().toString(), xbowArrowTeam);
        arrowFired.setGlowing(true);

        // Adding arrow to list to track it
        xbowArrowSet.add(arrowFired.getUniqueId());
    }

    public void removeXbowArrow(Arrow arrow) {
        xbowArrowSet.remove(arrow);
        arrow.remove();
    }

    public void clearProjectiles() {
        for (UUID creeperUUID : new HashSet<>(creeperProjectileSet)) {
            Entity entity = gameManager.getWorld().getEntity(creeperUUID);
            if (entity == null) continue;
            entity.remove();
        }
        creeperProjectileSet.clear();

        for (UUID arrowUUID : new HashSet<>(flameZoneArrowSet)) {
            Entity entity = gameManager.getWorld().getEntity(arrowUUID);
            if (entity == null) continue;
            entity.remove();
        }
        flameZoneArrowSet.clear();

        for (UUID arrowUUID : new HashSet<>(xbowArrowSet)) {
            Entity entity = gameManager.getWorld().getEntity(arrowUUID);
            if (entity == null) continue;
            entity.remove();
        }
        xbowArrowSet.clear();
    }

    public void weaponFired (CBCPlayer player, EntityShootBowEvent e, WeaponType weapon) {
        if (weapon == WeaponType.CREEPER) {this.fireCreeper(player, e);}
        else if (weapon == WeaponType.FLAME) {this.fireFlame(player, e);}
        else if (weapon == WeaponType.XBOW) {this.fireXBow(player, e);}
        player.startReload(weapon);
    }

    public void activateWeapons () {

        active = true;

        // Setup any projectile sets
        this.creeperProjectileSet = new HashSet<>();
        this.flameZoneArrowSet = new HashSet<>();
        this.xbowArrowSet = new HashSet<>();

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
        flameZoneArrowTeam = scoreboard.registerNewTeam("flameArrows");
        flameZoneArrowTeam.color(NamedTextColor.GOLD); // Set team color

        // Create heal pad color team
        xbowArrowTeam = scoreboard.registerNewTeam("xbowArrows");
        xbowArrowTeam.color(NamedTextColor.AQUA); // Set team color

        // Create heal pad color team
        healthPadItemTeam = scoreboard.registerNewTeam("healPadItemTeam");
        healthPadItemTeam.color(NamedTextColor.GREEN); // Set team color

        if (gameManager.getCbcScoreboardManager().isActive()) {
            gameManager.getCbcScoreboardManager().registerTeamForAllClients(flameZoneArrowTeam);
            gameManager.getCbcScoreboardManager().registerTeamForAllClients(xbowArrowTeam);
            gameManager.getCbcScoreboardManager().registerTeamForAllClients(healthPadItemTeam);
        }

        // Activate tasks
        weaponReloadTask = new WeaponReloadTask(gameManager, this);
        creeperTrackingTask = new CreeperTrackingTask(gameManager, this);
        arrowParticlesTask = new ArrowParticlesTask(gameManager, this);
        arrowHitGroundTask = new ArrowHitGroundTask(this);
        flameZonerTask = new FlameZonerTask(gameManager, this);
        voidTask = new VoidTask(gameManager, this);
        healPadTask = new HealPadDetectionTask(gameManager, this);
        resetPlayerLastHitTask = new ResetPlayerLastHitTask(gameManager);
        weaponManagerTimerTask = new WeaponManagerTimerTask(this);
        playerParticlesTask = new PlayerParticlesTask(gameManager);

        weaponReloadTask.runTaskTimer(plugin, 0, reloadTaskPeriod);
        creeperTrackingTask.runTaskTimer(plugin, 0, 1L);
        arrowParticlesTask.runTaskTimer(plugin, 0, 3L);
        arrowHitGroundTask.runTaskTimer(plugin, 0, 5L);
        flameZonerTask.runTaskTimer(plugin, 0, 5L);
        voidTask.runTaskTimer(plugin, 0, 4L);
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

        // Set any numbers
        healPadTimer = 20;

        // Enable all heal pads
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
        healthPadItemTeam.unregister();

        // Disable tasks
        cancelTask(weaponReloadTask);
        cancelTask(creeperTrackingTask);
        cancelTask(arrowParticlesTask);
        cancelTask(flameZonerTask);
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
        resetWeaponPresetsToDefault();

        // Clear all projectiles
        clearProjectiles();

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
        playerEntity.setHealth(20);
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

    public double getCreeperAllyDamageRatio () {
        return creeperWeaponVariables.getCreeperAllyDamageRatio();
    }

    public double getHorizontalKnockbackCoefficient () {
        return creeperWeaponVariables.getHorizontalKnockbackCoefficient();
    }

    public double getVerticalKnockbackCoefficient () {
        return creeperWeaponVariables.getVerticalKnockbackCoefficient();
    }

    public double getFlameRadius () {
        return flameWeaponVariables.getZoneRadius();
    }

    public double getFlameZoneLife () {
        return flameWeaponVariables.getZoneLife();
    }

    public int getCreeperReloadTime (CBCPlayer player) {
        return (int) Math.round(getCreeperWeaponVariables(player).getReloadTimer() * reloadTaskFrequency);
    }

    public int getFlameReloadTime (CBCPlayer player) {
        return (int) Math.round(getFlameWeaponVariables(player).getReloadTimer() * reloadTaskFrequency);
    }

    public int getXbowReloadTime (CBCPlayer player) {
        return (int) Math.round(getXbowWeaponVariables(player).getReloadTimer() * reloadTaskFrequency);
    }

    public CreeperPreset getCreeperWeaponVariables(CBCPlayer player) {
        CBCTeam team = player.getTeam();
        if (team != null) {
            return creeperPresetTeamOverrides.getOrDefault(team.getTeamId(), creeperWeaponVariables);
        }
        return creeperWeaponVariables;
    }

    public FlamePreset getFlameWeaponVariables(CBCPlayer player) {
        CBCTeam team = player.getTeam();
        if (team != null) {
            return flamePresetTeamOverrides.getOrDefault(team.getTeamId(), flameWeaponVariables);
        }
        return flameWeaponVariables;
    }

    public XbowPreset getXbowWeaponVariables(CBCPlayer player) {
        CBCTeam team = player.getTeam();
        if (team != null) {
            return xbowPresetTeamOverrides.getOrDefault(team.getTeamId(), xbowWeaponVariables);
        }
        return xbowWeaponVariables;
    }

    public WeaponPreset getWeaponVariables (WeaponType weaponType) {
        if (weaponType == WeaponType.CREEPER) {
            return creeperWeaponVariables;
        }
        else if (weaponType == WeaponType.FLAME) {
            return flameWeaponVariables;
        }
        else if (weaponType == WeaponType.XBOW) {
            return xbowWeaponVariables;
        }
        return null;
    }

    public void addTeamCreeperOverrides(String teamId, CreeperPreset preset) {
        if (creeperPresetTeamOverrides.containsKey(teamId) && preset == creeperWeaponVariables) {
            creeperPresetTeamOverrides.remove(teamId);
        }
        creeperPresetTeamOverrides.put(teamId, preset);
    }

    public void addTeamFlameOverrides(String teamId, FlamePreset preset) {
        if (flamePresetTeamOverrides.containsKey(teamId) && preset == flameWeaponVariables) {
            flamePresetTeamOverrides.remove(teamId);
        }
        flamePresetTeamOverrides.put(teamId, preset);
    }

    public void addTeamXbowOverrides(String teamId, XbowPreset preset) {
        if (xbowPresetTeamOverrides.containsKey(teamId) && preset == xbowWeaponVariables) {
            xbowPresetTeamOverrides.remove(teamId);
        }
        xbowPresetTeamOverrides.put(teamId, preset);
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
}
