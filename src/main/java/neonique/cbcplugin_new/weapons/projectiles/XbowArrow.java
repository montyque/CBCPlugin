package neonique.cbcplugin_new.weapons.projectiles;

import neonique.cbcplugin_new.playerclasses.CBCPlayer;
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

        playParticleTrail();

        if (arrow.isInBlock()) {
            markForRemoval();
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

    public void playParticleTrail () {



    }

}
