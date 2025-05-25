package neonique.cbcplugin_new.gamemodes.assassin;

import neonique.cbcplugin_new.CBCPlugin;
import neonique.cbcplugin_new.enums.PlayerHeadType;
import neonique.cbcplugin_new.managers.GameManager;
import neonique.cbcplugin_new.managers.CombatManager;
import neonique.cbcplugin_new.playerclasses.CBCPlayer;
import neonique.cbcplugin_new.tasks.weapontasks.RespawnTimerTask;
import neonique.cbcplugin_new.tasks.weapontasks.TempImmunityTask;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

import static neonique.cbcplugin_new.resourcepack.ResourcePackManager.smallText;
import static neonique.cbcplugin_new.util.TextUtil.titleTimes;

public class AssassinPlayer extends CBCPlayer {

    private final AssassinGame game;

    // Game variables
    private AssassinPlayer currentTarget = null;
    private AssassinPlayer lastTarget = null;
    private final List<AssassinPlayer> targetOrder;
    private int targetsLeft;
    private int targetTimer;

    private int placement = 1;
    private boolean tied = false;

    // Game statistics
    private int targetDeaths = 0;
    private int targetsKilled = 0;

    public AssassinPlayer(AssassinGame game, GameManager gameManager, CombatManager combatManager, Player player,
                          Integer playerId) {
        super(gameManager, combatManager, player, playerId);
        this.game = game;

        targetsLeft = game.getTargetsToKill();
        targetOrder = new ArrayList<>();
    }

    public void newTarget (boolean showTitle) {

        if (game.getWinner() != null) return;

        currentTarget = selectNextTarget();
        lastTarget = currentTarget;
        targetOrder.add(currentTarget);

        // Reset player's target timer
        targetTimer = game.getTargetChangeTimer();

        if (isOnline() && currentTarget != null && showTitle) {
            newTargetTitle();
        }

        // Make player glow
        if (isOnline()) {
            game.getGlowManager().updateGlowingPlayer(getPlayer(), currentTarget.getPlayer());
        }
    }

    public void newTargetTitle () {

        Component titleComponent = Component.text("New target!").color(NamedTextColor.YELLOW).decorate(TextDecoration.BOLD);

        // Get head component of target player
        Component subtitleComponent = game.getResourcePackManager().getPlayerHeadComponent(
                PlayerHeadType.NORMAL, currentTarget.getOfflinePlayer()
        ).append(Component.text(" " + currentTarget.getName()).color(NamedTextColor.AQUA));

        getPlayer().showTitle(
                Title.title(titleComponent, subtitleComponent, titleTimes(300, 2000, 300))
        );

        getPlayer().playSound(getPlayer().getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, 200, 2);
    }

    public AssassinPlayer selectNextTarget() {

        List<AssassinPlayer> playerList = new ArrayList<>(game.getOnlineAssassinPlayers());
        Collections.shuffle(playerList);

        // Clear target order if length of target order is the same or greater as the amount of players minus 1
        if (targetOrder.size() >= playerList.size() - 1) {
            targetOrder.clear();
        }

        // List of players that are already targets
        final Set<AssassinPlayer> alreadyTargetedPlayers = game.getCurrentTargets();

        AssassinPlayer nextTarget = null;
        AssassinPlayer nextNewTarget = null;

        for (AssassinPlayer player : playerList) {

            // Make sure target cannot be yourself
            if (player.hasPlayerId(getPlayerId())) continue;

            // Make sure target cannot be repeated
            if (lastTarget == player && playerList.size() > 2) continue;

            nextTarget = player;

            // Check if player has already been a target
            if (!targetOrder.contains(player)) {
                nextNewTarget = player;

                // Check if player is not already a target of someone else
                if (!alreadyTargetedPlayers.contains(player)) {
                    return player;
                }
            }
        }

        if (nextNewTarget == null) {
            return nextTarget;
        }
        else {
            return nextNewTarget;
        }
    }

    public void playerKilledTarget () {

        targetsLeft--;
        targetsKilled++;
        Player playerEntity = getPlayer();

        // Play sound to player
        playerEntity.playSound(playerEntity.getLocation(), Sound.BLOCK_RESPAWN_ANCHOR_DEPLETE, 200, 2);
        currentTarget.killedAsTarget();
        currentTarget = null;

        // Check if the game has not ended yet with this target kill
        if (targetsLeft > 0) {
            // Show title after 1 tick, so it clears all current titles
            if (isOnline()) {

                Component titleComponent = Component.text("TARGET KILLED!").color(NamedTextColor.YELLOW).decorate(TextDecoration.BOLD);

                // Get head component of target player
                Component subtitleComponent = Component.text("New target in 3 seconds...").color(NamedTextColor.AQUA);

                playerEntity.showTitle(
                        Title.title(titleComponent, subtitleComponent, titleTimes(0, 3000, 500))
                );
            }

            // Give player new target after 3 seconds
            new BukkitRunnable() {
                @Override
                public void run() {
                    if (game.getWinner() == null) {
                        newTarget(true);
                    }
                }
            }.runTaskLater(CBCPlugin.getPlugin(), 60);
        }
        else {
            game.playerWonGame(this);
        }

        game.updatePlacements();
        game.updateServerSidebar();

    }

    public void decrementTargetChangeTimer() {

        if (!isOnline()) return;
        Player playerEntity = getPlayer();

        if (currentTarget == null) return;
        targetTimer--;

        // If target timer is 0, change target
        if (targetTimer == 0) {
            newTarget(true);
        }
        else {
            // Play sound to warn player about change if there's 10 seconds left
            if (targetTimer <= 10) {
                playerEntity.playSound(playerEntity.getLocation(), Sound.BLOCK_NOTE_BLOCK_BIT, 200, 2);
            }
        }
    }

    @Override
    public Component modifyDeathMessageAsKiller (CBCPlayer playerKilled, Component deathMessage) {
        // Modify the death message if the player was their target
        if (playerKilled == currentTarget) {
            // Add text to death message
            deathMessage = deathMessage.append(smallText(" TARGET KILLED!").color(NamedTextColor.RED).decorate(TextDecoration.BOLD));
        }
        return deathMessage;
    }

    @Override
    public void playerAfterKill (CBCPlayer playerKilled) {
        // Check if player has killed target
        if (playerKilled == currentTarget) {
            playerKilledTarget();
        }

        if (isOnline()) {
            game.getSidebarManager().updateClientBoard(getPlayer());
        }
    }

    @Override
    public void playerAfterDeath (CBCPlayer playerKiller) {

        // The player will respawn, so we are overriding the old method
        if (isOnline()) {

            // Remove potion effects
            for (PotionEffect effect : getPlayer().getActivePotionEffects()) {
                if (effect.getType() != PotionEffectType.NIGHT_VISION) getPlayer().removePotionEffect(effect.getType());
            }

            // Respawn player
            setRespawning(true);

            // Find the amount of time that it takes for the players to respawn
            int timeToRespawn = 4;
            // Set up respawn timer
            RespawnTimerTask respawnTimerTask = new RespawnTimerTask(getGameManager(), getCombatManager(), this, timeToRespawn + 1);
            respawnTimerTask.runTaskTimer(CBCPlugin.getPlugin(), 0L, 20L);

        }

        game.getBossbarManager().update();

    }

    @Override
    public void playerSpawn () {

        if (!isOnline()) return;

        if (game.getWinner() == null) {
            new TempImmunityTask(getGameManager(), getCombatManager(), this, 20).runTaskTimer(CBCPlugin.getPlugin(), 0, 3);
        }
        else {
            setImmune(true);
        }

        playerSetup();
        setReloadsBySecond(2);
        teleportOnRespawn();

    }

    public void teleportOnRespawn() {

        // Teleport player to spawn point
        AssassinSpawn spawn = selectSpawn();
        Location lookTo = game.getMap().getMapCentre();

        // If their target is alive, spawn them orientated towards the target
        // Otherwise, spawn them facing the map's center
        if (currentTarget != null) {
            if (currentTarget.isAlive()) {
                lookTo = currentTarget.getPlayer().getLocation();
            }
        }

        teleportPlayerToSpawn(spawn, lookTo);

    }

    // Runs when the game is being set up
    public void playerSetupGame () {

        if (!isOnline()) return;
        resetPlayer();
        getPlayer().addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 800000, 0, false, false, false));

    }

    // Runs when the game countdown timer stops
    public void playerStartGame () {

        if (!isOnline()) return;
        Player playerEntity = getPlayer();
        playerEntity.removePotionEffect(PotionEffectType.INVISIBILITY);

        playerSetup();
        setReloadsBySecond(2);

        // Show title for player's target
        newTargetTitle();

    }

    // Selecting spawn
    public AssassinSpawn selectSpawn () {

        List<AssassinSpawn> spawns = new ArrayList<>(game.getSpawns());
        List<AssassinSpawn> noEnemyNearbySpawns = new ArrayList<>();

        if (currentTarget != null) {
            if (currentTarget.isAlive()) {

                AssassinSpawn closestSpawn = null;
                double smallestDif = 3000000;

                for (AssassinSpawn spawn : spawns) {

                    double distance = spawn.calculateDistance(currentTarget.getPlayer().getLocation());
                    double difference = Math.abs(game.getTargetSpawnDistance() * game.getTargetSpawnDistance() - distance);

                    if (difference < smallestDif) {
                        closestSpawn = spawn;
                        smallestDif = difference;
                    }

                    if (!spawn.isEnemyNearbySpawn(this)) {
                        noEnemyNearbySpawns.add(spawn);
                        if (difference < 10) {
                            return spawn;
                        }
                    }
                }

                if (closestSpawn != null) {
                    return closestSpawn;
                }
            }
        }

        if (!noEnemyNearbySpawns.isEmpty()) {
            return noEnemyNearbySpawns.get(0);
        }
        else {
            return spawns.get(0);
        }
    }

    public void setPlacement(int currentPlacement, boolean tied) {
        placement = currentPlacement;
        this.tied = tied;
    }

    public int getPlacement() {
        return placement;
    }

    public boolean isTied() {
        return tied;
    }

    public AssassinPlayer getCurrentTarget() {
        return currentTarget;
    }

    public int getTargetsLeft() {
        return targetsLeft;
    }

    public void killedAsTarget () {
        // When player is killed by their assassin, increment their target deaths
        targetDeaths++;
    }

    public int getTargetDeaths () {
        return targetDeaths;
    }

    public int getTargetChangeTimer () {
        return targetTimer;
    }

    public int getTargetKills() {
        return targetsKilled;
    }
}
