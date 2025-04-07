package neonique.cbcplugin_new.tasks.weapontasks;

import neonique.cbcplugin_new.managers.GameManager;
import neonique.cbcplugin_new.managers.ProjectileManager;
import neonique.cbcplugin_new.playerclasses.CBCPlayer;
import org.bukkit.scheduler.BukkitRunnable;

public class ProjectileUpdateTask extends BukkitRunnable {

    private final GameManager gameManager;
    private final ProjectileManager projectileManager;

    public ProjectileUpdateTask (GameManager gameManager, ProjectileManager projectileManager) {
        this.gameManager = gameManager;
        this.projectileManager = projectileManager;
    }

    @Override
    public void run() {
        projectileManager.updateProjectiles();
        updateFlames();
    }

    public void updateFlames () {
        for (CBCPlayer player : gameManager.getPlayers()) {
            player.getFlameDamager().update();
        }
    }

}
