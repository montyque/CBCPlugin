package neonique.cbcplugin_new.weapons.projectiles;

import neonique.cbcplugin_new.playerclasses.CBCPlayer;
import org.bukkit.Particle;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;

public class XbowArrow extends PlayerProjectile {

    public XbowArrow (CBCPlayer playerFired, Arrow arrowEntity) {
        super(playerFired, arrowEntity);
    }

    @Override
    public void update() {

        Arrow arrow = getArrow();
        if (arrow == null) return;

        playParticleTrail(arrow);

        if (arrow.isInBlock()) {
            markForRemoval();
            playHitParticles(arrow);
        }

    }

    public Arrow getArrow() {

        Entity projectileEntity = getProjectileEntity();
        if (projectileEntity == null) {
            markForRemoval();
            return null;
        }

        Arrow arrow = (Arrow) getProjectileEntity();

        if (arrow.isDead()) {
            markForRemoval();
            return null;
        }

        return arrow;

    }

    public void playParticleTrail (Arrow arrow) {

        arrow.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, arrow.getLocation(), 1, 0, 0, 0,
                0.05, null, true);

    }

    public void playHitParticles (Arrow arrow) {
        arrow.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, arrow.getLocation(), 8, 0.0, 0.0,
                0.0, 0.4, null, true);
    }

}
