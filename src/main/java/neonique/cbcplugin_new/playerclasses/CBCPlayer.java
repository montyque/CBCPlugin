package neonique.cbcplugin_new.playerclasses;

import neonique.cbcplugin_new.CBCPlugin;
import neonique.cbcplugin_new.enums.DeathCause;
import neonique.cbcplugin_new.enums.ResourcePackFont;
import neonique.cbcplugin_new.enums.WeaponType;
import neonique.cbcplugin_new.gamemodes._base.CBCTeam;
import neonique.cbcplugin_new.gamemodes.showdown.ShowdownSpawn;
import neonique.cbcplugin_new.managers.GameManager;
import neonique.cbcplugin_new.managers.CombatManager;
import neonique.cbcplugin_new.resourcepack.ResourcePackManager;
import neonique.cbcplugin_new.tasks.weapontasks.FlameZonerDamageTask;
import neonique.cbcplugin_new.tasks.weapontasks.RespawnTimerTask;
import neonique.cbcplugin_new.tasks.weapontasks.TempImmunityTask;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ArmorMeta;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.trim.ArmorTrim;
import org.bukkit.inventory.meta.trim.TrimMaterial;
import org.bukkit.inventory.meta.trim.TrimPattern;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.time.Duration;
import java.util.*;

import static neonique.cbcplugin_new.resourcepack.ResourcePackManager.*;

public class CBCPlayer {

    private CBCPlugin plugin;

    private final int playerId;

    private final GameManager gameManager;

    public GameManager getGameManager() {
        return gameManager;
    }

    private final CombatManager combatManager;

    public CombatManager getWeaponManager() {
        return combatManager;
    }

    private UUID playerUUID;

    private int kills;
    private int deaths;
    private int killStreak;
    private int maxKillStreak = 0;
    private int multiKill = 0;
    private int lastKillTime = 0;

    private int gamePoints = 0;

    // Team variables
    private CBCTeam team = null;

    // Combat variables
    private boolean alive = false;
    private boolean immune = false;
    private boolean respawning = false;
    private CBCPlayer lastPlayerHitBy = null;
    private int lastPlayerHitByReset = 0;
    private HashMap<CBCPlayer, Integer> timeDamaged = new HashMap<>();

    private int creeperCooldown = 0;
    private int flameCooldown = 0;
    private int xbowCooldown = 0;

    private boolean creeperLoaded = false;
    private boolean flameLoaded = false;
    private boolean xbowLoaded = false;

    // Important stats for fighting
    public int flamezoneFireTicks = 0;
    private CBCPlayer inFlameZoneOfPlayer = null;
    private FlameZonerDamageTask flameZonerDamageTask;

    // Other fields

    private int swimTimer = 0;
    private int swimTimerDamageTick = 0;
    private boolean onJumpPad = false;
    private boolean overrideGlassHelmet = false;

    // Display in player list
    private List<Component> playerListPrefixes;
    private List<Component> playerListSuffixes;

    private static NamespacedKey playerIdNamespacedKey;

    static {
        playerIdNamespacedKey = new NamespacedKey(CBCPlugin.getPlugin(), "playerId");
    }

    public CBCPlayer(GameManager gameManager, CombatManager combatManager, Player player, Integer playerId) {
        this.gameManager = gameManager;
        this.combatManager = combatManager;
        this.playerUUID = player.getUniqueId();
        this.playerId = playerId;

        playerListPrefixes = new ArrayList<>();
        playerListSuffixes = new ArrayList<>();

        this.plugin = CBCPlugin.getPlugin();

        if (combatManager.isSwimTimerEnabled()) {
            swimTimer = combatManager.getSwimTimerLength();
        }
    }

    // Set player to alive or dead
    public void setAlive(boolean alive) {
        this.alive = alive;
    }

    // Check if the given player Id belongs to this player
    public boolean hasPlayerId (Integer id) {
        return (id == this.playerId);
    }

    // Check if the entity Player object given belongs to this player
    public boolean isPlayer (Player player) {
        return (player.getUniqueId() == playerUUID);
    }

    // Return true or false if this player is alive
    public boolean isAlive() {
        return (this.alive);
    }

    // Return true or false if this player is respawning
    public boolean isRespawning() {return (this.respawning);}

    // Return the player's player id
    public Integer getPlayerId () {
        return this.playerId;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////
    // PLAYER ENTITY RELATED FUNCTIONS
    ////////////////////////////////////////////////////////////////////////////////////////////

    // Return the entity Player object
    public Player getPlayer () {
        OfflinePlayer player = Bukkit.getOfflinePlayer(playerUUID);
        if (player.getPlayer() != null) {
            return player.getPlayer();
        } else {
            return null;
        }
    }

    public OfflinePlayer getOfflinePlayer () {
        return Bukkit.getOfflinePlayer(playerUUID);
    }

    public boolean isOnline () {
        OfflinePlayer player = Bukkit.getOfflinePlayer(playerUUID);
        return player.isOnline();
    }

    ////////////////////////////////////////////////////////////////////////////////////////////
    // TEAM RELATED FUNCTIONS
    ////////////////////////////////////////////////////////////////////////////////////////////
    // Check if player is an ally
    public boolean isAlly (CBCPlayer player) {
        if (player == this) return true;
        else if (this.team != null) {
            return team.isAlly(player);
        }
        return false;
    }

    // Use tags to find out if an entity (arrow, creeper) is allied with this player
    public boolean isEntityAlly (Entity entity) {
        PersistentDataContainer entityTags = entity.getPersistentDataContainer();
        Integer entityPlayerId = entityTags.get(playerIdNamespacedKey, PersistentDataType.INTEGER);
        if (entityPlayerId != null) {
            if (entityPlayerId.equals(playerId)) {
                return true;
            } else {
                if (team != null) {
                    return team.isAllyPlayerId(entityPlayerId);
                } else {
                    return false;
                }
            }
        } else {
            return false;
        }
    }

    // Get player's team
    public CBCTeam getTeam () {
        return team;
    }

    public void startReload (WeaponType weaponType) {
        if (weaponType == WeaponType.CREEPER) {
            this.creeperCooldown = combatManager.getCreeperReloadTime(this);
            creeperLoaded = false;
            this.playerReload(false);
        }
        else if (weaponType == WeaponType.FLAME) {
            flameLoaded = false;
            this.flameCooldown = combatManager.getFlameReloadTime(this);
            this.playerReload(false);
        }
        else if (weaponType == WeaponType.XBOW) {
            xbowLoaded = false;
            this.xbowCooldown = combatManager.getXbowReloadTime(this);
            this.playerReload(false);
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////
    // PLAYER WEAPONS AND PLAYER RESET FUNCTIONS
    ////////////////////////////////////////////////////////////////////////////////////////////

    public void resetPlayer() {

        if (!isOnline()) return;
        Player player = getPlayer();

        // Set exp levels to 0
        getPlayer().setLevel(0);
        getPlayer().setExp(0);

        // Get max health of player and set player's health to max
        double maxHealth = getMaxHealth();
        player.setHealth(maxHealth);

        // Set gamemode of player
        player.setGameMode(GameMode.ADVENTURE);
        player.getInventory().clear();
        player.updateInventory();

        resetAllAttributes();
        setReloadsBySecond(2);

        inFlameZoneOfPlayer = null;
        flamezoneFireTicks = 0;
        setLastPlayerHitBy(null);

        // Remove all effects from player
        for (PotionEffect effect : getPlayer().getActivePotionEffects()) {
            if (effect.getType() != PotionEffectType.NIGHT_VISION) getPlayer().removePotionEffect(effect.getType());
            // Remove night vision effect if needed
            if (combatManager.isNightVisionDisabled()) {
                player.addScoreboardTag("NVDisable");
                player.removePotionEffect(PotionEffectType.NIGHT_VISION);
            }
            else {
                player.removeScoreboardTag("NVDisable");
            }
        }
    }

    public void teleportPlayerToSpawn (Location spawn, Location faceLocation) {

        if (!isOnline()) return;
        Player playerEntity = getPlayer();

        // Teleport player to spawn
        playerEntity.teleport(spawn);

        // Make them face location
        faceToLocation(faceLocation, true);

    }

    public void faceToLocation (Location targetLocation, boolean eyeLevel) {

        if (!isOnline()) return;
        Player playerEntity = getPlayer();
        Location targetLoc = targetLocation.clone();

        // Make it so player faces forward, not upwards or downwards
        if (eyeLevel) {
            targetLoc.setY(playerEntity.getLocation().getY() + 2);
        }

        Vector dir = targetLocation.clone().subtract(getPlayer().getEyeLocation()).toVector();
        Location loc = playerEntity.getLocation().setDirection(dir);
        playerEntity.teleport(loc);

    }

    public void loadout() {

        if (!isOnline()) return;
        Player player = getPlayer();
        PlayerInventory inventory = getPlayer().getInventory();

        // Create netherite chestplate
        ItemStack chestplate = new ItemStack(Material.NETHERITE_CHESTPLATE);
        ItemMeta chestplateMeta = chestplate.getItemMeta();
        ArmorMeta armorChestplateMeta = (ArmorMeta) chestplateMeta;
        chestplateMeta.addEnchant(Enchantment.BINDING_CURSE, 1, true);

        // Check if player has team
        if (team != null && !overrideGlassHelmet) {
            chestplateMeta.displayName(Component.text(team.getTeamName() + " CBC Chestplate").color(team.getColor()));
            inventory.setHelmet(team.getGlassHead());

            // Add trim to netherite chestplate
            TrimMaterial material = team.getTrimMaterial();
            TrimPattern pattern = plugin.getTrimManager().getCBCPlayerTrim(this);

            ArmorTrim armorTrim = new ArmorTrim(material, pattern);
            armorChestplateMeta.setTrim(armorTrim);
        }

        chestplateMeta.setUnbreakable(true);
        chestplate.setItemMeta(chestplateMeta);
        // Set chestplate slot to item meta
        inventory.setChestplate(chestplate);

        // If beacon head is on, set player's head to beacon
        if (combatManager.isBeaconHeads() && !overrideGlassHelmet) {
            ItemStack beacon = new ItemStack(Material.BEACON);
            ItemMeta itemMeta = beacon.getItemMeta();
            // Set item title
            Component itemTitle = Component.text("CBC Lamp Head")
                    .decorate(TextDecoration.BOLD).decoration(TextDecoration.ITALIC, TextDecoration.State.FALSE);
            itemMeta.displayName(itemTitle);
            itemMeta.addEnchant(Enchantment.BINDING_CURSE, 1, false);
            beacon.setItemMeta(itemMeta);
            inventory.setHelmet(beacon);
        }

        player.updateInventory();
    }

    public void playerReload (boolean decrement) {

        if (!isOnline()) return;

        boolean updateCreeper = false;
        boolean updateFlame = false;
        boolean updateXBow = false;

        // Set numbers
        if (creeperCooldown == 0) {
            if (!creeperLoaded) {
                updateCreeper = true;
            }
            creeperLoaded = true;
        } else {
            creeperLoaded = false;
            if (decrement) {
                creeperCooldown--;
            }
        }

        if (flameCooldown == 0) {
            if (!flameLoaded) {
                updateFlame = true;
            }
            flameLoaded = true;
        } else {
            flameLoaded = false;
            if (decrement) {
                flameCooldown--;
            }
        }

        if (xbowCooldown == 0) {
            if (!xbowLoaded) {
                updateXBow = true;
            }
            xbowLoaded = true;
        } else {
            xbowLoaded = false;
            if (decrement) {
                xbowCooldown--;
            }
        }

        if (updateCreeper || updateFlame || updateXBow) {
            playerReloadItems(updateCreeper, updateFlame, updateXBow);
        }

        // Update action bar if required
        updateActionBarDisplay(true);
    }

    public void playerReloadItems (boolean creeperReload, boolean flameReload, boolean xbowReload) {

        if (!isOnline()) return;
        Player player = getPlayer();

        PlayerInventory inventory = getPlayer().getInventory();

        // Check if player is missing effects
        if (!player.hasPotionEffect(PotionEffectType.JUMP_BOOST)) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, 30000000, 4, false, false, false));
        }

        if (!player.hasPotionEffect(PotionEffectType.SPEED)) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 30000000, 4, false, false, false));
        }

        if (!player.hasPotionEffect(PotionEffectType.DOLPHINS_GRACE)) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.DOLPHINS_GRACE, 30000000, 0, false, false, false));
        }

        boolean invUpdateNeeded = false;

        // Check if loadout of chestplate is needed
        if (inventory.getChestplate() == null) {
            loadout();
        } else {
            ItemStack chestplate = inventory.getChestplate();
            if (chestplate.getType() != Material.NETHERITE_CHESTPLATE) {
                loadout();
            }
        }

        // Check if player is alive, and if so, reload
        if (!alive) {
            return;
        }

        // Check if player is in a team
        List<Component> loreList = new ArrayList<>();
        if (team != null) {
            loreList.add(Component.text("Certified " + team.getTeamName() + " Team Crossbow"));
        }

        // Check if player's creeper is fully loaded
        if (creeperReload) {
            ItemStack ccLoaded = CombatManager.fetchWeapon(WeaponType.CREEPER, true);
            if (creeperCooldown == 0) {
                // Check if player does not have ccLoaded in their inventory, and if add it
                if (!inventory.contains(ccLoaded)) {
                    inventory.setItem(0, ccLoaded);
                    invUpdateNeeded = true;
                }
            } else {
                // Get an unloaded creeper cannon
                ItemStack cc = CombatManager.fetchWeapon(WeaponType.CREEPER, false);
                // Damage creeper cannon to show reloading
                ItemMeta ccItemMeta = cc.getItemMeta();

                ccItemMeta.lore(loreList);

                float reloadPercentageLeft = (float) (creeperCooldown + 1) / (float) combatManager.getCreeperReloadTime(this);
                int customModelData = 1;
                ccItemMeta.setCustomModelData(customModelData + getAddedCustomModelData(reloadPercentageLeft));

                if (ccItemMeta instanceof Damageable) {
                    Damageable ccMetaDamage = (Damageable) ccItemMeta;
                    ccMetaDamage.setDamage(Math.round(reloadPercentageLeft * 465.0f));
                    cc.setItemMeta((ItemMeta) ccMetaDamage);
                }
                inventory.setItem(0, cc);
                invUpdateNeeded = true;
            }
        }

        // Check if player's flame zoner is fully loaded
        if (flameReload) {
            ItemStack fzLoaded = CombatManager.fetchWeapon(WeaponType.FLAME, true);
            if (flameCooldown == 0) {
                // Check if player does not have ccLoaded in their inventory, and if add it
                if (!inventory.contains(fzLoaded)) {
                    inventory.setItem(1, fzLoaded);
                    invUpdateNeeded = true;
                }
            } else {
                // Get an unloaded creeper cannon
                ItemStack fz = CombatManager.fetchWeapon(WeaponType.FLAME, false);

                // Damage creeper cannon to show reloading
                ItemMeta fzItemMeta = fz.getItemMeta();

                fzItemMeta.lore(loreList);

                float reloadPercentageLeft = (float) (flameCooldown + 1) / (float) combatManager.getFlameReloadTime(this);
                int customModelData = 5;
                fzItemMeta.setCustomModelData(customModelData + getAddedCustomModelData(reloadPercentageLeft));

                if (fzItemMeta instanceof Damageable) {

                    Damageable fzMetaDamage = (Damageable) fzItemMeta;
                    fzMetaDamage.setDamage(Math.round(reloadPercentageLeft * 465.0f));
                    fz.setItemMeta((ItemMeta) fzMetaDamage);
                }
                inventory.setItem(1, fz);
                invUpdateNeeded = true;
            }
        }

        // Check if player's flame zoner is fully loaded
        if (xbowReload) {
            ItemStack xbLoaded = CombatManager.fetchWeapon(WeaponType.XBOW, true);
            if (xbowCooldown == 0) {
                // Check if player does not have ccLoaded in their inventory, and if add it
                if (!inventory.contains(xbLoaded)) {
                    inventory.setItem(2, xbLoaded);
                    invUpdateNeeded = true;
                }
            } else {
                // Get an unloaded creeper cannon
                ItemStack xb = CombatManager.fetchWeapon(WeaponType.XBOW, false);
                // Damage creeper cannon to show reloading
                ItemMeta xbItemMeta = xb.getItemMeta();

                xbItemMeta.lore(loreList);

                float reloadPercentageLeft = (float) (xbowCooldown + 1) / (float) combatManager.getXbowReloadTime(this);
                int customModelData = 9;
                xbItemMeta.setCustomModelData(customModelData + getAddedCustomModelData(reloadPercentageLeft));

                if (xbItemMeta instanceof Damageable) {
                    Damageable xbMetaDamage = (Damageable) xbItemMeta;
                    xbMetaDamage.setDamage(Math.round(reloadPercentageLeft * 465.0f));
                    xb.setItemMeta((ItemMeta) xbMetaDamage);
                }
                inventory.setItem(2, xb);
                invUpdateNeeded = true;
            }
        }

        // Update exp bar to show weapon cooldown
        player.setExp(0);

        // Update inventory if required
        if (invUpdateNeeded) {player.updateInventory();}

    }

    public int getAddedCustomModelData (float reloadPercentageLeft) {

        if (reloadPercentageLeft < 0.3) {
            return 3;
        }
        else if (reloadPercentageLeft < 0.6) {
            return 2;
        }
        else if (reloadPercentageLeft < 0.9) {
            return 1;
        }
        else {
            return 0;
        }

    }

    public void resetReloads() {
        this.creeperCooldown = combatManager.getCreeperReloadTime(this);
        this.flameCooldown = combatManager.getFlameReloadTime(this);
        this.xbowCooldown = combatManager.getXbowReloadTime(this);
        updateActionBarDisplay(true);
        playerReloadItems(true, true, true);
    }

    public void setReloadsBySecond(int seconds) {
        this.creeperCooldown = seconds * 4;
        this.flameCooldown = seconds * 4;
        this.xbowCooldown = seconds * 4;

        if (creeperCooldown > combatManager.getCreeperReloadTime(this)) {
            this.creeperCooldown = combatManager.getCreeperReloadTime(this);
        }

        if (flameCooldown > combatManager.getCreeperReloadTime(this)) {
            this.flameCooldown = combatManager.getFlameReloadTime(this);
        }

        if (flameCooldown > combatManager.getCreeperReloadTime(this)) {
            this.flameCooldown = combatManager.getXbowReloadTime(this);
        }

        updateActionBarDisplay(true);
        playerReloadItems(true, true, true);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////
    // JUMP PAD FUNCTIONS
    ////////////////////////////////////////////////////////////////////////////////////////////
    public void jumpPadPressed () {

        if (!isOnline()) return;
        Player player = getPlayer();

        player.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, 10, 11, false, false, false));

        if (!onJumpPad) {
            onJumpPad = true;
        }
    }

    public void jumpPadOff () {
        onJumpPad = false;

        if (!isOnline()) return;
        Player player = getPlayer();

        player.removePotionEffect(PotionEffectType.JUMP_BOOST);
        player.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, 10, 4, false, false, false));
    }

    ////////////////////////////////////////////////////////////////////////////////////////////
    // DAMAGING, PLAYER KILL AND PLAYER DEATH FUNCTIONS
    ////////////////////////////////////////////////////////////////////////////////////////////

    // Return the player last hit
    public CBCPlayer getLastPlayerHitBy() {return lastPlayerHitBy;}

    // Set the player last hit
    public void setLastPlayerHitBy(CBCPlayer player) {lastPlayerHitBy = player; lastPlayerHitByReset = 7;}

    public void addPlayerDamaged (CBCPlayer player) {
        if (!player.isAlly(this)) {
            timeDamaged.put(player, combatManager.getTimer());
        }
    }

    public void playerKill () {

        this.kills++;
        this.killStreak++;

        if (killStreak > maxKillStreak) {
            maxKillStreak = killStreak;
        }

        this.multiKill++;
        this.lastKillTime = combatManager.getTimer();

        if (isOnline()) {

            Player player = getPlayer();

            addHealing(6);
            player.playSound(player.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 200, 1);
        }
    }

    public void playerDie() {
        this.deaths++;
        this.alive = false;
        // Reset variables

        if (combatManager.isSwimTimerEnabled()) {
            this.swimTimer = combatManager.getSwimTimerLength();
        }

        inFlameZoneOfPlayer = null;
        flamezoneFireTicks = 0;
        lastPlayerHitBy = null;
        timeDamaged.clear();
        // Clear player inventory and effects
        if (isOnline()) {
            Player player = getPlayer();
            for (PotionEffect effect : getPlayer().getActivePotionEffects()) {
                if (effect.getType() != PotionEffectType.NIGHT_VISION) getPlayer().removePotionEffect(effect.getType());
            }
            player.getInventory().clear();
            player.setLevel(0);
            player.setExp(0);
            player.updateInventory();
        }
    }

    public void playerKillStreakEnd() {
        this.killStreak = 0;
    }

    public void playerAfterDeath(CBCPlayer playerKiller) {

        // Set player's title
        if (isOnline()) {
            Player player = getPlayer();
            this.respawning = true;
            Component titleComponent = Component.text("YOU DIED!").color(NamedTextColor.RED)
                    .decorate(TextDecoration.BOLD);
            Title diedTitle = Title.title(titleComponent, Component.text("Respawning in 4")
                    .color(NamedTextColor.YELLOW), Title.Times.times(Duration.ofMillis(0), Duration.ofMillis(1500), Duration.ofMillis(250)));
            player.showTitle(diedTitle);
            // Set up respawn timer
            RespawnTimerTask respawnTimerTask = new RespawnTimerTask(gameManager, combatManager, this, 4);
            respawnTimerTask.runTaskTimer(CBCPlugin.getPlugin(), 20L, 20L);
        }
    }

    public void playerAfterKill(CBCPlayer playerKilled) {}

    public void playerKillTitle (CBCPlayer playerKilled, DeathCause directDeathCause) {

        // Set player's title
        if (isOnline()) {

            // Update multi kill
            updateMultiKill();

            Player player = getPlayer();
            this.respawning = true;
            Component titleComponent = Component.text("");

            String multiKillShow = "";

            if (multiKill == 2) {
                multiKillShow = "II ";
            } else if (multiKill == 3) {
                multiKillShow = "III ";
            } else if (multiKill == 4) {
                multiKillShow = "IV ";
            } else if (multiKill >= 5) {
                multiKillShow = "V ";
            }

            NamedTextColor color = NamedTextColor.WHITE;
            if (playerKilled.getTeam() != null) {
                color = playerKilled.getTeam().getColor();
            }

            Component deathCauseIcon = getDeathCauseIcon(directDeathCause, true, color);

            Component subtitle = smallText(multiKillShow).color(NamedTextColor.AQUA).append(
                    deathCauseIcon
            ).append(Component.space()).append(
                    playerKilled.getNameComponent(ResourcePackFont.DEFAULT)
            );

            Title killTitle = Title.title(titleComponent, subtitle, Title.Times.times(Duration.ofMillis(0), Duration.ofMillis(2000), Duration.ofMillis(500)));
            player.showTitle(killTitle);
        }
    }

    // This is used to compound healing - eg if a player already has the effect make the duration last longer
    public void addHealing (int healthPoints) {
        if (isOnline() && alive) {

            Player player = getPlayer();
            PotionEffect currentPotionEffect = player.getPotionEffect(PotionEffectType.REGENERATION);
            if (currentPotionEffect != null) {
                int duration = currentPotionEffect.getDuration();
                player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, duration + healthPoints, 5, false, false, true));
            } else {
                player.addPotionEffect(new PotionEffect(PotionEffectType.REGENERATION, healthPoints, 5, false, false, true));
            }
        }
    }

    public void playerSpawn() {

        immune = true;
        new TempImmunityTask(gameManager, combatManager, this, 20).runTaskTimer(CBCPlugin.getPlugin(), 0, 3);

        // Teleport player to spawn point
        //FFASpawnpoint spawnpointSelected = gameManager.ffaSelectSpawn(this);
        //player.teleport(spawnpointSelected);

        this.alive = true;
        this.respawning = false;

        loadout();
    }

    public void decrementLastPlayerHit() {

        if (lastPlayerHitByReset > 0) {
            lastPlayerHitByReset--;
            if (lastPlayerHitByReset == 0) {
                lastPlayerHitBy = null;
            }
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////
    // IMMUNITY FUNCTIONS
    ////////////////////////////////////////////////////////////////////////////////////////////

    public void setTempImmune(long duration) {
        setImmune(true);
        new TempImmunityTask(gameManager, combatManager, this, (int) (duration / 5)).runTaskTimer(CBCPlugin.getPlugin(), 0, 5);
    }

    public boolean isImmune() {
        return immune;
    }

    public void setImmune(boolean b) {
        immune = b;
    }

    public void setRespawning(boolean b) {
        respawning = b;
    }

    public void setNewPlayer(Player player) {
        this.playerUUID = player.getUniqueId();
    }

    public void setTeam(CBCTeam cbcTeam) {
        team = cbcTeam;
    }

    public int getKills() {
        return kills;
    }

    public int getMultiKill () {
        return multiKill;
    }

    public int getDeaths() {
        return deaths;
    }

    public int getKillStreak() {
        return killStreak;
    }

    public int getMaxKillStreak() {
        return maxKillStreak;
    }

    public void setoverrideGlassHelmet(boolean b) {
        overrideGlassHelmet = b;
    }

    public void updateMultiKill() {

        if (combatManager.getTimer() - lastKillTime > 120) {
            this.multiKill = 0;
        }

    }
    public void swimTimerDecrement() {

        if (!isOnline()) return;
        Player player = getPlayer();

        if (swimTimer > 0) {
            swimTimer--;
        } else {
            // Damage player
            if (swimTimerDamageTick == 0) {
                swimTimerDamageTick = 10;

                player.getWorld().playSound(
                        player.getLocation(), Sound.ENTITY_PLAYER_HURT_DROWN, 1, 1
                );

                // Damage player
                if (player.getHealth() <= 1) {
                    if (lastPlayerHitBy != null) {
                        combatManager.playerDeath(this, lastPlayerHitBy, DeathCause.DROWN, false);
                    } else {
                        combatManager.playerDeath(this, null, DeathCause.DROWN, false);
                    }
                } else {
                    // Damage player
                    player.setHealth(player.getHealth() - 1);
                }
            } else {
                swimTimerDamageTick--;
            }
        }
        updateSwimTimerBubbles();
    }

    public void swimTimerIncrement() {
        if (swimTimer < combatManager.getSwimTimerLength()) {
            swimTimer += 2;
            updateSwimTimerBubbles();
            if (swimTimer < combatManager.getSwimTimerLength()) {
                swimTimer = combatManager.getSwimTimerLength();
            }
        }
    }

    public void updateSwimTimerBubbles() {

        if (!isOnline()) return;
        Player player = getPlayer();

        if (swimTimer > 0) {
            player.setRemainingAir(Math.round(300 * ((float) swimTimer / (float) combatManager.getSwimTimerLength())));
        } else {
            player.setRemainingAir(-50);
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////
    // DISPLAY FUNCTIONS
    ////////////////////////////////////////////////////////////////////////////////////////////
    public String getName() {
        return getOfflinePlayer().getName();
    }

    public Component getNameComponent() {
        NamedTextColor textColor = NamedTextColor.WHITE;
        if (team != null) {
            textColor = team.getColor();
        }
        return Component.text(getName()).color(textColor);
    }

    public Component getNameComponent(ResourcePackFont font) {
        NamedTextColor textColor = NamedTextColor.WHITE;
        if (team != null) {
            textColor = team.getColor();
        }
        return setTextFont(getName(), font).color(textColor);
    }

    public Component getNameComponentWithTeamPrefix() {
        Component prefix = Component.text("");
        NamedTextColor textColor = NamedTextColor.WHITE;
        if (team != null) {
            textColor = team.getColor();
            prefix = Component.text(team.getPrefix() + " ").color(textColor).decorate(TextDecoration.BOLD);
        }
        return prefix.append(Component.text(getName()).color(textColor).decoration(TextDecoration.BOLD,
                TextDecoration.State.FALSE));
    }

    // Name display in list
    public void updatePlayerListName () {
        Component playerListName = Component.text("");

        for (Component prefix : playerListPrefixes) {
            playerListName = playerListName.append(prefix);
            playerListName = playerListName.append(Component.text(" "));
        }

        playerListName = playerListName.append(getNameComponentWithTeamPrefix());

        for (Component suffix : playerListSuffixes) {
            playerListName = playerListName.append(Component.text(" "));
            playerListName = playerListName.append(suffix);
        }

        if (isOnline()) {
            getPlayer().playerListName(playerListName);
        }
    }

    public void setPlayerListPrefixes (List<Component> newPlayerListPrefixes) {
        playerListPrefixes = newPlayerListPrefixes;
        updatePlayerListName();
    }

    public void clearPlayerListPrefixes () {
        playerListPrefixes.clear();
        updatePlayerListName();
    }

    public void setPlayerListSuffixes (List<Component> newPlayerListSuffixes) {
        playerListSuffixes = newPlayerListSuffixes;
        updatePlayerListName();
    }

    public void clearPlayerListSuffixes () {
        playerListSuffixes.clear();
        updatePlayerListName();
    }

    public void resetPlayerListName () {
        if (isOnline()) {
            getPlayer().playerListName(null);
        }
    }

    // Action bar display
    public void updateActionBarDisplay (boolean showIcon) {

        if (!isOnline()) return;

        Player playerEntity = getPlayer();
        PlayerInventory inventory = getPlayer().getInventory();

        // Check if player is in creative or spectator mode, if so remove the action bar
        if (playerEntity.getGameMode() == GameMode.SPECTATOR || playerEntity.getGameMode() == GameMode.CREATIVE) {
            playerEntity.sendActionBar(Component.text(""));
            return;
        }

        Component actionBarDisplay = null;

        if (isAlive()) {
            // Check which slot player is using
            int slot = inventory.getHeldItemSlot();

            Component reloadBarComponent = null;

            if (slot == 0) {
                // Display creeper cannon cooldown
                float progress = 1 - ((float) creeperCooldown / (float) combatManager.getCreeperReloadTime(this));
                if (combatManager.getCreeperReloadTime(this) == 0) {
                    progress = 1;
                }
                reloadBarComponent = ResourcePackManager.getReloadBarComponent(WeaponType.CREEPER, progress);
            }
            else if (slot == 1) {
                // Display flame zoner cooldown
                float progress = 1 - ((float) flameCooldown / (float) combatManager.getFlameReloadTime(this));
                if (combatManager.getFlameReloadTime(this) == 0) {
                    progress = 1;
                }
                reloadBarComponent = ResourcePackManager.getReloadBarComponent(WeaponType.FLAME, progress);
            }
            else if (slot == 2) {
                // Display x bow cooldown
                float progress = 1 - ((float) xbowCooldown / (float) combatManager.getXbowReloadTime(this));
                if (combatManager.getXbowReloadTime(this) == 0) {
                    progress = 1;
                }
                reloadBarComponent = ResourcePackManager.getReloadBarComponent(WeaponType.XBOW, progress);
            }

            if (reloadBarComponent != null) {
                // Remove shadow from action bar
                actionBarDisplay = noShadowText(reloadBarComponent);
            }
        }

        // Show icon if needing to show icon
        if (showIcon) {
            // Remove shadow from crossbow hotbar icon
            Component hotbarIcon = noShadowText(getHotbarIcon(getTeam(), actionBarDisplay != null));
            if (actionBarDisplay == null) {
                actionBarDisplay = hotbarIcon;
            }
            else {
                actionBarDisplay = actionBarDisplay.append(hotbarIcon);
            }
        }

        if (actionBarDisplay != null) {
            playerEntity.sendActionBar(actionBarDisplay);
        }
        else {
            playerEntity.sendActionBar(Component.text(""));
        }
    }

    public Set<CBCPlayer> damagingPlayersInLastTime (int ticks) {

        int currentTime = combatManager.getTimer();

        Set<CBCPlayer> damagedInPeriod = new HashSet<>();
        for (CBCPlayer player : timeDamaged.keySet()) {
            int time = timeDamaged.get(player);
            int timeSinceDamaged = currentTime - time;
            if (timeSinceDamaged < ticks) {
                damagedInPeriod.add(player);
            }
        }

        return damagedInPeriod;

    }

    public void resetAllAttributes () {
        Player playerEntity = getPlayer();
        if (playerEntity == null) return;

        AttributeInstance attr = playerEntity.getAttribute(Attribute.GENERIC_SCALE);
        if (attr != null) {
            attr.setBaseValue(1.0);
        }
    }

    public void healToFull () {

        if (!isOnline()) return;
        Player playerEntity = getPlayer();
        playerEntity.setHealth(getMaxHealth());

    }

    public double getMaxHealth () {
        if (!isOnline()) return 20;

        Player playerEntity = getPlayer();
        AttributeInstance maxHealthAttribute = playerEntity.getAttribute(Attribute.GENERIC_MAX_HEALTH);

        if (maxHealthAttribute == null) return 20;
        return maxHealthAttribute.getValue();
    }

    public int getGamePoints() {
        return gamePoints;
    }

    public void addGamePoints(int pts) {
        gamePoints += pts;
    }

    public void setGamePoints(int pts) {
        gamePoints = pts;
    }

    public boolean isInFlameZoner () {
        return inFlameZoneOfPlayer != null;
    }

    public CBCPlayer getInFlameZoneOfPlayer () {
        return inFlameZoneOfPlayer;
    }

    public void setFlameZonerDamageSource (CBCPlayer player) {
        inFlameZoneOfPlayer = player;
    }

    public String getLowercaseName () {
        return getName().toLowerCase();
    }

    public Component modifyDeathMessage (CBCPlayer playerKiller, Component deathMessage) {
        return deathMessage;
    }

    public Component modifyDeathMessageAsKiller (CBCPlayer playerKilled, Component deathMessage) {
        return deathMessage;
    }


}
