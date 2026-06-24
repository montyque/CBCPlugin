package neonique.cbcplugin_new.combat.tasks;

import neonique.cbcplugin_new.managers.PlayerRegistry;
import neonique.cbcplugin_new.combat.ProjectileManager;
import neonique.cbcplugin_new.playerclasses.CBCPlayer;
import org.bukkit.scheduler.BukkitRunnable;

public class ProjectileUpdateTask extends BukkitRunnable {

    private final PlayerRegistry playerRegistry;
    private final ProjectileManager projectileManager;

    public ProjectileUpdateTask (PlayerRegistry playerRegistry, ProjectileManager projectileManager) {
        this.playerRegistry = playerRegistry;
        this.projectileManager = projectileManager;
    }

    @Override
    public void run() {
        projectileManager.updateProjectiles();
        updateFlames();
    }

    public void updateFlames () {
        for (CBCPlayer player : playerRegistry.getPlayers()) {
            player.getFlameDamager().update();
        }
    }

}
