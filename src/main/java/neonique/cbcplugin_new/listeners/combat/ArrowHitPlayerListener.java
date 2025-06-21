package neonique.cbcplugin_new.listeners.combat;

import neonique.cbcplugin_new.managers.GameManager;
import neonique.cbcplugin_new.managers.CombatManager;
import neonique.cbcplugin_new.managers.ProjectileManager;
import neonique.cbcplugin_new.playerclasses.CBCPlayer;
import neonique.cbcplugin_new.weapons.projectiles.*;
import neonique.cbcplugin_new.weapons.projectiles.Projectile;
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

        ProjectileManager projectileManager = combatManager.getProjectileManager();

        // Check if it was an arrow that hit
        Entity projectileEntity = e.getEntity();
        if (!(projectileEntity instanceof Arrow)) {
            return;
        }

        Projectile projectile = projectileManager.getProjectile(projectileEntity.getUniqueId());
        if (projectile == null) {
            return;
        }

        if (!(projectile instanceof PlayerProjectile playerProjectile)) {
            return;
        }

        if (!(playerProjectile instanceof XbowArrow) && !(playerProjectile instanceof FlameArrow)) {
            return;
        }

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

        CBCPlayer arrowSource = playerProjectile.getSource();

        // Check if the shooter and the player are not the same person
        if (arrowSource == player) {
            return;
        }

        hitPlayer.setNoDamageTicks(0);
    }
}
