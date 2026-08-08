package neonique.cbcplugin_new.combat.listeners;

import neonique.cbcplugin_new.combat.projectiles.FlameArrow;
import neonique.cbcplugin_new.combat.projectiles.PlayerProjectile;
import neonique.cbcplugin_new.combat.projectiles.XbowArrow;
import neonique.cbcplugin_new.combat.ProjectileManager;
import neonique.cbcplugin_new.core.CBCPlayer;
import neonique.cbcplugin_new.combat.projectiles.Projectile;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileHitEvent;

import java.util.function.Function;

public class ArrowHitPlayerListener implements Listener {

    private final ProjectileManager projManager;
    private final Function<Entity, CBCPlayer> playerGetter;

    public ArrowHitPlayerListener (ProjectileManager projManager, Function<Entity, CBCPlayer> playerGetter) {
        this.projManager = projManager;
        this.playerGetter = playerGetter;
    }

    @EventHandler
    public void onArrowHit(ProjectileHitEvent e) {

        // Check if it was an arrow that hit
        Entity projectileEntity = e.getEntity();
        if (!(projectileEntity instanceof Arrow)) {
            return;
        }

        Projectile projectile = projManager.getProjectile(projectileEntity.getUniqueId());
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

        CBCPlayer player = playerGetter.apply(e.getHitEntity());

        // Check if player is immune
        if (player.isImmune()) {
            return;
        }

        CBCPlayer arrowSource = playerProjectile.getSource();

        // Check if the shooter and the player are not the same person
        if (arrowSource == player) {
            return;
        }

        player.getPlayer().setNoDamageTicks(0);

    }
}
