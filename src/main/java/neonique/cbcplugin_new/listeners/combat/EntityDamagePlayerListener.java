package neonique.cbcplugin_new.listeners.combat;

// This listener is used to check for both creeper, X-Bow and melee damage

import neonique.cbcplugin_new.CBCPlugin;
import neonique.cbcplugin_new.enums.DeathCause;
import neonique.cbcplugin_new.managers.GameManager;
import neonique.cbcplugin_new.managers.CombatManager;
import neonique.cbcplugin_new.managers.ProjectileManager;
import neonique.cbcplugin_new.playerclasses.CBCPlayer;
import neonique.cbcplugin_new.weapons.projectiles.*;
import neonique.cbcplugin_new.weapons.projectiles.Projectile;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class EntityDamagePlayerListener implements Listener {

    private final GameManager gameManager;
    private final CombatManager combatManager;

    private final static double CREEPER_DAMAGE_MULTIPLIER = 0.22;

    public EntityDamagePlayerListener(GameManager gameManager, CombatManager combatManager) {
        this.gameManager = gameManager;
        this.combatManager = combatManager;
    }

    @EventHandler
    public void onEntityDamagePlayer(EntityDamageByEntityEvent e) {

        ProjectileManager projectileManager = combatManager.getProjectileManager();

        // Check if player died due to creeper
        Entity entity = e.getEntity();
        Entity damageSource = e.getDamager();

        // Check if the entity damaged is a player
        if (!(entity instanceof Player playerEntity)) {
            return;
        }

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
        if (damageSource instanceof Creeper creeper) {

            Projectile projectile = projectileManager.getProjectile(creeper.getUniqueId());
            if (projectile == null) {
                e.setCancelled(true);
                return;
            }

            if (!(projectile instanceof CBCCreeper creeperProjectile)) {
                e.setCancelled(true);
                return;
            }

            CBCPlayer sourcePlayer = creeperProjectile.getSource();
            double finalDamage = e.getDamage() * CREEPER_DAMAGE_MULTIPLIER;

            // Check if this player is an ally
            if (!player.isAlly(sourcePlayer)) {
                if (player.isImmune()) {
                    e.setCancelled(true);
                    return;
                }
                player.addPlayerDamaged(sourcePlayer);
                player.setLastPlayerHitBy(sourcePlayer);
            } else {
                // Change creeper damage if creeper fired by ally
                finalDamage *= combatManager.getCreeperAllyDamageRatio();
                if (sourcePlayer != player) {
                    if (player.isImmune() || !player.isInSameTeam(sourcePlayer)) {
                        e.setCancelled(true);
                        return;
                    }
                }
            }

            e.setDamage(finalDamage);

            // Check if this explosion kills the player
            if (e.getFinalDamage() >= playerEntity.getHealth()) {
                if (player.isAlly(sourcePlayer)) {
                    // If the killing blow is from an ally, check if player has been hit in last 6 seconds
                    if (player.getLastPlayerHitBy() == null) {
                        combatManager.playerDeath(player, null, DeathCause.CREEPER, false);
                    } else {
                        combatManager.playerDeath(player, player.getLastPlayerHitBy(), DeathCause.CREEPER, false);
                    }
                } else {
                    // The killing blow was from an enemy
                    combatManager.playerDeath(player, sourcePlayer, DeathCause.CREEPER, true);
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

        if (damageSource instanceof Arrow) {

            Projectile projectile = projectileManager.getProjectile(damageSource.getUniqueId());
            if (projectile == null) {
                e.setCancelled(true);
                return;
            }

            if (!(projectile instanceof PlayerProjectile playerProjectile)) {
                e.setCancelled(true);
                return;
            }

            CBCPlayer sourcePlayer = playerProjectile.getSource();

            // Check if the shooter and the player are not allied
            if (player.isAlly(sourcePlayer) && player.getTeam() != sourcePlayer.getTeam()) {
                // Prevent the player from taking damage
                e.setCancelled(true);
                return;
            }

            if (playerProjectile instanceof XbowArrow) {
                // Run player death function
                player.addPlayerDamaged(sourcePlayer);
                combatManager.playerDeath(player, sourcePlayer, DeathCause.XBOW, true);
            }
            else if (playerProjectile instanceof FlameArrow) {
                player.addPlayerDamaged(sourcePlayer);
                if (e.getFinalDamage() >= playerEntity.getHealth()) {
                    combatManager.playerDeath(player, sourcePlayer, DeathCause.FLAMEZONE, true);
                }
            }

        } else if (damageSource instanceof Player) {

            // Check if damage is in game
            if (!gameManager.hasPlayer((Player) damageSource)) {
                // Cancel damage
                e.setCancelled(true);
                return;
            }

            // Find player who damaged other player
            CBCPlayer sourcePlayer = gameManager.getPlayer((Player) damageSource);

            // See if players are allies
            if (sourcePlayer.isAlly(player)) {
                e.setCancelled(true);
                return;
            }

            player.setLastPlayerHitBy(sourcePlayer);
            // Check if the hit kills the player
            if (e.getFinalDamage() > playerEntity.getHealth()) {
                combatManager.playerDeath(player, sourcePlayer, DeathCause.MELEE, true);
            }

        } else if (damageSource instanceof Firework) {
            e.setCancelled(true);
        }
    }
}
