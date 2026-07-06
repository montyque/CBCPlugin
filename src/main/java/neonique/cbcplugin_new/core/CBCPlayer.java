package neonique.cbcplugin_new.core;

import neonique.cbcplugin_new.CBCPlugin;
import neonique.cbcplugin_new.combat.DeathCause;
import neonique.cbcplugin_new.mapmechanics.SwimTimer;
import neonique.cbcplugin_new.resourcepack.ResourcePackFont;
import neonique.cbcplugin_new.managers.GameManager;
import neonique.cbcplugin_new.combat.CombatManager;
import neonique.cbcplugin_new.combat.tasks.TempImmunityTask;
import neonique.cbcplugin_new.weapons.*;
import neonique.cbcplugin_new.weapons.projectiles.FlameDamager;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.audience.ForwardingAudience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

import static neonique.cbcplugin_new.resourcepack.ResourcePackManager.*;

public class CBCPlayer implements TeamPlayerLike, ForwardingAudience {

    private final GameManager gameManager;

    public GameManager getGameManager() {
        return gameManager;
    }

    private final CombatManager combatManager;

    public CombatManager getCombatManager() {
        return combatManager;
    }

    private final UUID playerUUID;

    private int kills;
    private int deaths;
    private int killStreak;
    private int maxKillStreak = 0;
    private int multiKill = 0;
    private int lastKillTime = 0;
    private int gamePoints = 0;

    // Team variables
    private CBCTeam<?> team = null;

    // Combat variables
    private boolean alive = false;
    private boolean immune = false;
    private TempImmunityTask tempImmunityTask = null;
    private int respawnTicks = 0;
    private CBCPlayer lastPlayerHitBy = null;
    private int lastPlayerHitByReset = 0;
    private final HashMap<CBCPlayer, Integer> timeDamaged = new HashMap<>();

    private final CBCInventory inventory;

    // Important stats for fighting
    private final FlameDamager flameDamager = new FlameDamager(this);

    // Other fields
    private SwimTimer swimTimer = null;

    // Display in player list
    private List<Component> playerListSuffixes;

    public CBCPlayer(GameManager gameManager, CombatManager combatManager, Player player) {

        this.gameManager = gameManager;
        this.combatManager = combatManager;
        this.playerUUID = player.getUniqueId();

        this.inventory = new CBCInventory(this, combatManager.getWeaponFactory(), combatManager.getEquipmentFactory());

        playerListSuffixes = new ArrayList<>();

    }

    public UUID uuid () {
        return playerUUID;
    }

    // Set player to alive or dead
    public void setAlive(boolean alive) {
        this.alive = alive;
    }

    // Return true or false if this player is alive
    public boolean isAlive() {
        return alive;
    }

    // Return true or false if this player is respawning
    public boolean isRespawning () {
        return respawnTicks > 0;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////
    // TEAM RELATED FUNCTIONS
    ////////////////////////////////////////////////////////////////////////////////////////////

    // Get player's team
    public CBCTeam<?> team() {
        return team;
    }

    // Check if player is an ally
    public boolean isAlly (CBCPlayer player) {
        if (player == this) return true;
        else if (this.team != null) {
            return team.isAlly(player);
        }
        return false;
    }

    public boolean isInSameTeam (CBCPlayer player) {
        return (player.team() != null && player.team() == this.team);
    }

    public boolean isPlayerEntityAliveEnemy (Player playerEntity) {
        CBCPlayer player = gameManager.getPlayer(playerEntity);
        if (player == null) {
            return false;
        }
        if (!player.isAlive()) {
            return false;
        }
        return !isAlly(player);
    }

    public boolean isPlayerEntityAlly (Player playerEntity) {
        CBCPlayer player = gameManager.getPlayer(playerEntity);
        if (player == null) return false;
        return isAlly(player);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////
    // PLAYER WEAPONS AND PLAYER RESET FUNCTIONS
    ////////////////////////////////////////////////////////////////////////////////////////////

    public void resetPlayer() {

        if (!isOnline()) return;
        Player playerEntity = getPlayer();

        playerEntity.setLevel(0);
        playerEntity.setExp(0);
        playerEntity.setGameMode(GameMode.ADVENTURE);
        playerEntity.getInventory().clear();
        playerEntity.updateInventory();
        resetAllAttributes();
        healToFull();
        flameDamager.resetFlameDamager();
        setLastPlayerHitBy(null);

        if (alive) {
            giveEffects();
        } else {
            clearEffects();
            checkNightVision();
        }

    }

    public void playerSetup () {
        setAlive(true);
        setRespawnTicks(0);
        resetPlayer();
        inventory.setWeapons();
        inventory.loadEquipment();
        giveEffects();
    }

    public void playerSetup (double weaponReloadTimer) {
        playerSetup();
        inventory.setReloadsBySecond(weaponReloadTimer);
    }

    public void clearEffects () {
        for (PotionEffect effect : getPlayer().getActivePotionEffects()) {
            if (effect.getType() != PotionEffectType.NIGHT_VISION) getPlayer().removePotionEffect(effect.getType());
        }
    }

    public void giveEffects () {

        if (!isOnline()) return;
        Player player = getPlayer();

        if (!player.hasPotionEffect(PotionEffectType.JUMP_BOOST)) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.JUMP_BOOST, -1, 4, false, false, false));
        }

        if (!player.hasPotionEffect(PotionEffectType.SPEED)) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, -1, 4, false, false, false));
        }

        if (!player.hasPotionEffect(PotionEffectType.DOLPHINS_GRACE)) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.DOLPHINS_GRACE, -1, 0, false, false, false));
        }

        // Remove night vision effect if needed
        checkNightVision();

    }

    public void checkNightVision () {

        Player player = getPlayer();
        if (combatManager.isNightVisionDisabled()) {
            player.addScoreboardTag("NVDisable");
            player.removePotionEffect(PotionEffectType.NIGHT_VISION);
        }
        else {
            player.removeScoreboardTag("NVDisable");
        }

    }

    public void teleportPlayerToSpawn (Location spawn, Location faceLocation) {
        if (!isOnline()) return;
        Player playerEntity = getPlayer();
        playerEntity.teleport(spawn);
        faceToLocation(faceLocation, true);
    }

    public void faceToLocation (Location targetLocation, boolean eyeLevel) {

        if (!isOnline()) return;
        Player playerEntity = getPlayer();
        Location targetLoc = targetLocation.clone();

        // Make it so player faces forward, not upwards or downwards
        if (eyeLevel) {
            targetLoc.setY(playerEntity.getLocation().getY() + 1.5);
        }

        Vector dir = targetLocation.clone().subtract(getPlayer().getEyeLocation()).toVector();
        Location loc = playerEntity.getLocation().setDirection(dir);
        playerEntity.teleport(loc);

    }

    ////////////////////////////////////////////////////////////////////////////////////////////
    // DAMAGING, PLAYER KILL AND PLAYER DEATH FUNCTIONS
    ////////////////////////////////////////////////////////////////////////////////////////////

    // Return the player last hit
    public CBCPlayer getLastPlayerHitBy() {
        return lastPlayerHitBy;
    }

    // Set the player last hit
    public void setLastPlayerHitBy(CBCPlayer player) {
        lastPlayerHitBy = player; lastPlayerHitByReset = 7;
    }

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

    public void playerDie () {

        this.deaths++;
        this.alive = false;

        // Reset variables
        resetSwimTimer();
        flameDamager.resetFlameDamager();
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

    public void playerKillStreakEnd () {
        this.killStreak = 0;
    }

    public void playerAfterDeath (CBCPlayer playerKiller) {
        if (isOnline()) {
            setRespawnTicks(80);
        }
    }

    public void playerAfterKill(CBCPlayer playerKilled) {}

    public void playerKillTitle (CBCPlayer playerKilled, DeathCause directDeathCause) {

        // Set player's title
        if (isOnline()) {

            // Update multi kill
            updateMultiKill();

            Player player = getPlayer();
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

            TextColor color = playerKilled.nameColor();

            Component deathCauseIcon = directDeathCause.deathIconComponent(playerKilled, this);

            Component subtitle = smallText(multiKillShow).color(NamedTextColor.AQUA).append(
                    deathCauseIcon
            ).append(Component.space()).append(
                    playerKilled.nameComponent(ResourcePackFont.DEFAULT)
            );

            Title killTitle = Title.title(titleComponent, subtitle, Title.Times.times(Duration.ofMillis(0), Duration.ofMillis(2000), Duration.ofMillis(500)));
            player.showTitle(killTitle);
        }
    }

    // This is used to compound healing - e.g. if a player already has the effect make the duration last longer
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
        respawnTicks = 0;
        immune = true;
        new TempImmunityTask(gameManager, combatManager, this, 20).runTaskTimer(CBCPlugin.getPlugin(), 0, 3);
        playerSetup();
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

        if (tempImmunityTask != null) {
            if (!tempImmunityTask.isCancelled()) {
                tempImmunityTask.cancel();
            }
        }

        setImmune(true);
        tempImmunityTask = new TempImmunityTask(gameManager, combatManager, this, (int) (duration / 5));
        tempImmunityTask.runTaskTimer(CBCPlugin.getPlugin(), 0, 5);

    }

    public boolean isImmune() {
        return immune;
    }

    public void setImmune(boolean b) {
        immune = b;
        if (!b) {
            tempImmunityTask = null;
        }
    }

    public void setTeam(CBCTeam<?> cbcTeam) {
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

    public void updateMultiKill() {

        if (combatManager.getTimer() - lastKillTime > 120) {
            this.multiKill = 0;
        }

    }

    public void startSwimTimer (int length) {
        swimTimer = new SwimTimer(length, 5);
    }

    public SwimTimer getSwimTimer () {
        return swimTimer;
    }

    public boolean hasSwimTimer () {
        return swimTimer != null;
    }

    public void resetSwimTimer () {
        swimTimer = null;
    }

    public void updateSwimTimerBubbles () {
        if (!isOnline()) return;
        Player player = getPlayer();
        if (swimTimer != null) {
            if (swimTimer.empty()) player.setRemainingAir(-50);
            else player.setRemainingAir(Math.round(300 * swimTimer.getFraction()));
        } else player.setRemainingAir(300);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////
    // DISPLAY FUNCTIONS
    ////////////////////////////////////////////////////////////////////////////////////////////

    // Name display in list
    public void updatePlayerListName () {

        if (playerListSuffixes.isEmpty()) {
            resetPlayerListName();
        }

        Component playerListName = Component.text("");
        playerListName = playerListName.append(nameComponentWithTeamPrefix());

        for (Component suffix : playerListSuffixes) {
            playerListName = playerListName.append(Component.text(" "));
            playerListName = playerListName.append(suffix);
        }

        if (isOnline()) {
            getPlayer().playerListName(playerListName);
        }
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

    public CBCInventory getInventory () {
        return inventory;
    }

    // Action bar display
    public void updateActionBarDisplay (boolean showIcon) {

        if (!isOnline()) return;

        Player playerEntity = getPlayer();
        PlayerInventory entityInventory = getPlayer().getInventory();

        // Check if player is in creative or spectator mode, if so remove the action bar
        if (playerEntity.getGameMode() == GameMode.SPECTATOR || playerEntity.getGameMode() == GameMode.CREATIVE) {
            playerEntity.sendActionBar(Component.text(""));
            return;
        }

        Component actionBarDisplay = null;


        if (isAlive()) {

            // Check which slot player is using
            int slotNum = entityInventory.getHeldItemSlot();
            InventorySlot slot = inventory.getSlot(slotNum);

            if (slot instanceof WeaponSlot weaponSlot) {
                CrossbowWeapon weapon = weaponSlot.getWeapon();
                actionBarDisplay = weapon.getXPBarComponent();
            }

        }


        // Show icon if needing to show icon
        if (showIcon) {
            // Remove shadow from crossbow hot bar icon
            Component hotbarIcon = noShadowText(getHotbarIcon(team(), actionBarDisplay != null));
            if (actionBarDisplay == null) {
                actionBarDisplay = hotbarIcon;
            } else {
                actionBarDisplay = actionBarDisplay.append(hotbarIcon);
            }
        }

        playerEntity.sendActionBar(Objects.requireNonNullElseGet(actionBarDisplay, () -> Component.text("")));

    }

    public Set<CBCPlayer> damagingPlayersInLastTime (int ticks) {
        int currentTime = combatManager.getTimer();
        return timeDamaged.keySet().stream()
                .filter(p -> (currentTime - timeDamaged.get(p)) < ticks)
                .collect(Collectors.toSet());
    }

    public void resetAllAttributes () {
        Player playerEntity = getPlayer();
        if (playerEntity == null) return;

        AttributeInstance scaleAttr = playerEntity.getAttribute(Attribute.GENERIC_SCALE);
        if (scaleAttr != null) {
            scaleAttr.setBaseValue(1.0);
        }

        AttributeInstance jumpAttr = playerEntity.getAttribute(Attribute.GENERIC_JUMP_STRENGTH);
        if (jumpAttr != null) {
            jumpAttr.setBaseValue(0.42);
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

    public Component modifyDeathMessage (CBCPlayer playerKiller, Component deathMessage) {
        return deathMessage;
    }

    public Component modifyDeathMessageAsKiller (CBCPlayer playerKilled, Component deathMessage) {
        return deathMessage;
    }

    public FlameDamager getFlameDamager() {
        return flameDamager;
    }

    public UUID getUUID() {
        return getOfflinePlayer().getUniqueId();
    }

    public int getRespawnTicks () {
        return respawnTicks;
    }

    public void setRespawnTicks (int ticks) {
        this.respawnTicks = ticks;
    }

    public void respawnTick () {
        respawnTicks--;
    }

    public Title getDeathTitle () {
        return getRespawnTitle();
    }

    public Title getRespawnTitle () {
        return Title.title(
                Component.text("YOU DIED!").color(NamedTextColor.RED),
                Component.text("Respawning in " + (((respawnTicks - 1) / 20) + 1)).color(NamedTextColor.YELLOW),
                Title.Times.times(Duration.ofMillis(0), Duration.ofMillis(1500), Duration.ofMillis(250)));
    }

    @Override
    public @NotNull Iterable<? extends Audience> audiences() {
        if (!isOnline()) return List.of();
        return List.of(getPlayer());
    }
}
