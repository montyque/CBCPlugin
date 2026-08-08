package neonique.cbcplugin_new.combat;

import neonique.cbcplugin_new.combat.projectiles.FlameArrow;
import neonique.cbcplugin_new.combat.projectiles.XbowArrow;
import neonique.cbcplugin_new.core.PlayerStore;
import neonique.cbcplugin_new.scoreboard.CBCScoreboardManager;
import neonique.cbcplugin_new.scoreboard.CBCScoreboardTeam;
import neonique.cbcplugin_new.combat.projectiles.Projectile;
import org.bukkit.entity.Player;

import javax.annotation.Nullable;
import java.util.*;

public class ProjectileManager {

    private final CBCScoreboardManager scoreboardManager;
    private final CombatContext combatContext;
    private final Map<UUID, Projectile> projectiles = new HashMap<>();

    private CBCScoreboardTeam flameZoneArrowTeam;
    private CBCScoreboardTeam xbowArrowTeam;

    public ProjectileManager (CBCScoreboardManager scoreboardManager, CombatContext combatContext) {
        this.scoreboardManager = scoreboardManager;
        this.combatContext = combatContext;
    }

    public void setup () {
        this.flameZoneArrowTeam = scoreboardManager.registerNewTeam("flameArrows");
        this.xbowArrowTeam = scoreboardManager.registerNewTeam("xbowArrows");
    }

    public void cleanup () {
        flameZoneArrowTeam.unregister();
        xbowArrowTeam.unregister();
    }

    /**
     * Updates the state of every projectile in the projectiles list.
     * <p>If a projectile's entity is found to no longer be alive, it will be removed from the list.
     */
    public void updateProjectiles() {
        Set<Projectile> markedForRemoval = new HashSet<>();
        for (Projectile projectile : projectiles.values()) {
            if (!projectile.markedForRemoval()) {
                projectile.update(combatContext);
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

        if (projectile instanceof FlameArrow) {
            flameZoneArrowTeam.addEntityUUID(projectile.getProjectileEntityUUID());
        } else if (projectile instanceof XbowArrow) {
            xbowArrowTeam.addEntityUUID(projectile.getProjectileEntityUUID());
        }

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

        flameZoneArrowTeam.removeEntityUUID(projectile.getProjectileEntityUUID());
        xbowArrowTeam.removeEntityUUID(projectile.getProjectileEntityUUID());
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

    public CBCScoreboardTeam xbowArrowTeam() {
        return xbowArrowTeam;
    }

    public CBCScoreboardTeam flameZoneArrowTeam() {
        return flameZoneArrowTeam;
    }
}
