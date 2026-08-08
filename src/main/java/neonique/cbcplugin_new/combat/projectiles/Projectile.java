package neonique.cbcplugin_new.combat.projectiles;

import neonique.cbcplugin_new.CBCPlugin;
import org.bukkit.entity.Entity;

import javax.annotation.Nullable;
import java.util.UUID;

public abstract class Projectile {

    private final UUID projectileEntityUUID;
    private boolean removalMark = false;

    public Projectile(Entity entity) {
        projectileEntityUUID = entity.getUniqueId();
    }

    @Nullable
    public Entity getProjectileEntity() {
        return CBCPlugin.getPlugin().getServer().getEntity(projectileEntityUUID);
    }

    public UUID getProjectileEntityUUID () {
        return projectileEntityUUID;
    }

    public void markForRemoval() {
        removalMark = true;
    }

    public boolean markedForRemoval() {
        return removalMark;
    }

    public abstract void update();

}
