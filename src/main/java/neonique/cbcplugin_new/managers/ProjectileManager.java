package neonique.cbcplugin_new.managers;

import neonique.cbcplugin_new.weapons.projectiles.Projectile;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class ProjectileManager {

    private final HashMap<UUID, Projectile> projectiles = new HashMap<>();

    /**
     * Updates the state of every projectile in the projectiles list.
     * <p>If a projectile's entity is found to no longer be alive, it will be removed from the list.
     */
    public void updateProjectiles() {
        Set<Projectile> markedForRemoval = new HashSet<>();
        for (Projectile projectile : projectiles.values()) {
            if (!projectile.markedForRemoval()) {
                projectile.update();
            }
            if (projectile.markedForRemoval()) {
                markedForRemoval.add(projectile);
            }
        }

        for (Projectile remove : markedForRemoval) {
            removeProjectile(remove);
        }
    }

    /**
     * Adds the projectile to the projectiles list for tracking.
     *
     * @param projectile - the projectile to begin tracking.
     */
    public void addProjectile(Projectile projectile) {
        projectiles.put(projectile.getProjectileEntityUUID(), projectile);
    }

    /**
     * Removes the projectile from tracking.
     * <p>This will automatically remove the projectile entity if not already done.
     *
     * @param projectile - the projectile to remove.
     */
    public void removeProjectile(Projectile projectile) {
        projectiles.remove(projectile.getProjectileEntityUUID());
        if (projectile.getProjectileEntity() != null) {
            if (!projectile.getProjectileEntity().isDead()) {
                projectile.getProjectileEntity().remove();
            }
        }
    }

    /**
     * Removes all projectiles from tracking.
     * <p>This will automatically mark all projectile entities for removal if not already done.
     */
    public void clearAllProjectiles() {
        for (Projectile projectile : new HashSet<>(projectiles.values())) {
            removeProjectile(projectile);
        }
        projectiles.clear();
    }

    /**
     * Get a Projectile from the projectiles list given its entity UUID.
     * @param uuid the UUID of the projectile entity
     * @return the Projectile associated with the entity, or null if not found
     */
    @Nullable
    public Projectile getProjectile(UUID uuid) {
        return projectiles.getOrDefault(uuid, null);
    }

}
