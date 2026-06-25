package neonique.cbcplugin_new.weapons.projectiles;

import neonique.cbcplugin_new.core.CBCPlayer;
import org.bukkit.entity.Entity;

public abstract class PlayerProjectile extends Projectile {

    private final CBCPlayer source;

    public PlayerProjectile (CBCPlayer playerFired, Entity entity) {
        super(entity);
        source = playerFired;
    }

    public CBCPlayer getSource() {
        return source;
    }

}
