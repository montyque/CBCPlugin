package neonique.cbcplugin_new.listeners.combat;

// This listener is used to check for both creeper, X-Bow and melee damage

import neonique.cbcplugin_new.CBCPlugin;
import neonique.cbcplugin_new.enums.DeathCause;
import neonique.cbcplugin_new.managers.GameManager;
import neonique.cbcplugin_new.managers.CombatManager;
import neonique.cbcplugin_new.playerclasses.CBCPlayer;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class EntityDamagePlayerListener implements Listener {

    private final GameManager gameManager;
    private final CombatManager combatManager;

    private final double CREEPER_DAMAGE_MULTIPLIER = 0.22;

    public EntityDamagePlayerListener(GameManager gameManager, CombatManager combatManager) {
        this.gameManager = gameManager;
        this.combatManager = combatManager;
    }

    @EventHandler
    public void onEntityDamagePlayer(EntityDamageByEntityEvent e) {

        // Check if player died due to creeper
        Entity entity = e.getEntity();
        Entity damager = e.getDamager();

        // Check if the entity damaged is a player
        if (!(entity instanceof Player)) {
            return;
        }

        Player playerEntity = (Player) entity;

        // Check if the player damaged is in the game
        if (!(gameManager.hasPlayer(playerEntity))) {
            return;
        }

        CBCPlayer player = gameManager.getPlayer(playerEntity);
        // Check if the player damaged is alive
        if (!player.isAlive()) {
            return;
        }

        // **********************************************************
        // CREEPER CHECKING
        if (damager instanceof Creeper) {

            Creeper creeper = (Creeper) damager;
            if (!creeper.getScoreboardTags().contains("firedCreeper")) {
                return;
            }
            // Find the creeper's player id
            PersistentDataContainer creeperTags = creeper.getPersistentDataContainer();
            Integer creeperId = creeperTags.get(new NamespacedKey(CBCPlugin.getPlugin(), "playerId"),
                    PersistentDataType.INTEGER);
            // Check if creeperId is null
            if (creeperId == null) {
                // Cancel damage done
                e.setCancelled(true);
                return;
            }

            double finalDamage = e.getDamage() * CREEPER_DAMAGE_MULTIPLIER;

            // Find player who fired creeper
            CBCPlayer damagerPlayer = gameManager.getPlayerById(creeperId);
            // Check if this player is an ally
            if (!player.isAlly(damagerPlayer)) {
                if (player.isImmune()) {
                    e.setCancelled(true);
                    return;
                }

                player.addPlayerDamaged(damagerPlayer);
                player.setLastPlayerHitBy(damagerPlayer);
            } else {
                // Change creeper damage if creeper fired by ally
                finalDamage *= combatManager.getCreeperAllyDamageRatio();
                if (!damagerPlayer.hasPlayerId(player.getPlayerId()) && player.isImmune()) {
                    e.setCancelled(true);
                    return;
                }
            }

            e.setDamage(finalDamage);

            // Check if this explosion kills the player
            if (e.getFinalDamage() >= playerEntity.getHealth()) {
                if (player.isAlly(damagerPlayer)) {
                    // If the killing blow is from an ally, check if player has been hit in last 6 seconds
                    if (player.getLastPlayerHitBy() == null) {
                        combatManager.playerDeath(player, null, DeathCause.CREEPER, false);
                    } else {
                        combatManager.playerDeath(player, player.getLastPlayerHitBy(), DeathCause.CREEPER, false);
                    }
                } else {
                    // The killing blow was from an enemy
                    combatManager.playerDeath(player, damagerPlayer, DeathCause.CREEPER, true);
                }
            }

            // Dampen the vertical velocity of creeper knockback
            Vector velocity = playerEntity.getVelocity().clone();
            velocity.setY(velocity.getY() * combatManager.getVerticalKnockbackCoefficient());
            playerEntity.setVelocity(velocity);

            new BukkitRunnable() {
                @Override
                public void run() {
                    Vector velocity = playerEntity.getVelocity().clone();
                    velocity.setX(velocity.getX() * combatManager.getHorizontalKnockbackCoefficient());
                    velocity.setZ(velocity.getZ() * combatManager.getHorizontalKnockbackCoefficient());
                    playerEntity.setVelocity(velocity);
                }
            }.runTaskLater(CBCPlugin.getPlugin(), 0);

            return;
        }

        // Check if player is immune
        if (player.isImmune()) {
            e.setCancelled(true);
            return;
        }

        // **********************************************************
        // X-BOW ARROW CHECKING
        if (damager instanceof Arrow) {

            Arrow arrow = (Arrow) damager;
            // Check if the arrow was fired by a player and if the arrow was shot by a crossbow
            if (!((arrow.isShotFromCrossbow()))) {
                return;
            }

            if (arrow.getShooter() instanceof Piglin) {
                if (arrow.getScoreboardTags().contains("xbowArrowPiglin")) {
                    // Run player death function
                    combatManager.playerDeath(player, null, DeathCause.XBOW_PIGLIN, true);
                }
                return;
            }

            if (!(arrow.getShooter() instanceof Player)) {
                return;
            }

            Player shooterEntity = (Player) arrow.getShooter();

            // Check if the shooter and the player are not the same person
            if (shooterEntity == playerEntity) {
                // Prevent the player from taking damage
                e.setCancelled(true);
                return;
            }

            // Check if both player and shooter are in the game
            if (!(gameManager.hasPlayer(shooterEntity))) {
                return;
            }
            CBCPlayer shooterPlayer = gameManager.getPlayer(shooterEntity);

            // Check if the shooter and the player are not allied
            if (player.isAlly(shooterPlayer) && player.getTeam() != shooterPlayer.getTeam()) {
                // Prevent the player from taking damage
                e.setCancelled(true);
                return;
            }

            // Check if arrow is an X-Bow arrow
            if (arrow.getScoreboardTags().contains("xbowArrow")) {
                // Run player death function
                player.addPlayerDamaged(shooterPlayer);
                combatManager.playerDeath(player, shooterPlayer, DeathCause.XBOW, true);
            }
            // Check if arrow is a Flame Zoner arrow
            if (arrow.getScoreboardTags().contains("flameArrow")) {
                player.addPlayerDamaged(shooterPlayer);

                if (e.getFinalDamage() >= playerEntity.getHealth()) {
                    // Run player death function
                    combatManager.playerDeath(player, shooterPlayer, DeathCause.FLAMEZONE, true);
                }
            }

        } else if (damager instanceof Player) {

            // Check if damager is in game
            if (!gameManager.hasPlayer((Player) damager)) {
                // Cancel damage
                e.setCancelled(true);
                return;
            }

            // Find player who damaged other player
            CBCPlayer damagerPlayer = gameManager.getPlayer((Player) damager);

            // See if players are allies
            if (damagerPlayer.isAlly(player)) {
                e.setCancelled(true);
                return;
            }

            player.setLastPlayerHitBy(damagerPlayer);
            // Check if the hit kills the player
            if (e.getFinalDamage() > playerEntity.getHealth()) {
                combatManager.playerDeath(player, damagerPlayer, DeathCause.MELEE, true);
            }
        } else if (damager instanceof Firework) {
            e.setCancelled(true);
        }
    }
}
