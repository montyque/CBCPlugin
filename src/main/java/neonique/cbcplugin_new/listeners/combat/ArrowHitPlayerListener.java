package neonique.cbcplugin_new.listeners.combat;

import neonique.cbcplugin_new.managers.GameManager;
import neonique.cbcplugin_new.managers.CombatManager;
import neonique.cbcplugin_new.playerclasses.CBCPlayer;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileHitEvent;

public class ArrowHitPlayerListener implements Listener {

    private final GameManager gameManager;
    private final CombatManager combatManager;

    public ArrowHitPlayerListener (GameManager gameManager, CombatManager combatManager) {

        this.gameManager = gameManager;
        this.combatManager = combatManager;

    }

    @EventHandler
    public void onArrowHit(ProjectileHitEvent e) {

        // Check if it was an arrow that hit
        Projectile projectile = e.getEntity();
        if (!(projectile instanceof Arrow)) {
            return;
        }

        Arrow arrow = (Arrow) projectile;

        // Check if arrow hit block
        if (e.getHitEntity() == null) {
            return;
        }

        Entity hitEntity = e.getHitEntity();

        // Make sure hit entity is a player
        if (hitEntity.getType() != EntityType.PLAYER) return;

        Player hitPlayer = (Player) hitEntity;
        // Check if the player damaged is in the game
        if (!(gameManager.hasPlayer(hitPlayer))) {
            return;
        }

        CBCPlayer player = gameManager.getPlayer(hitPlayer);
        // Check if the player damaged is alive
        if (!player.isAlive()) {
            return;
        }

        // Check if player is immune
        if (player.isImmune()) {
            return;
        }

        // Check if the arrow was fired by a player and if the arrow was shot by a crossbow
        if (!((arrow.getShooter() instanceof Player && arrow.isShotFromCrossbow()))) {
            return;
        }

        Player shooterEntity = (Player) arrow.getShooter();
        // Check if the shooter and the player are not the same person
        if (shooterEntity == hitPlayer) {
            return;
        }
        // Check if both player and shooter are in the game
        if (!(gameManager.hasPlayer(shooterEntity))) {
            return;
        }

        // Check if arrow is an X-Bow arrow
        if (arrow.getScoreboardTags().contains("xbowArrow") || arrow.getScoreboardTags().contains("xbowArrowPiglin")
                || arrow.getScoreboardTags().contains("flameArrow")) {
            // Set no damage ticks so that player will be tagged
            hitPlayer.setNoDamageTicks(0);
        }
    }
}
