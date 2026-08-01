package neonique.cbcplugin_new.combat.tasks;

import neonique.cbcplugin_new.core.PlayerStore;
import neonique.cbcplugin_new.managers.PlayerRegistry;
import neonique.cbcplugin_new.combat.ProjectileManager;
import neonique.cbcplugin_new.core.CBCPlayer;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Collection;
import java.util.function.Supplier;


public class ProjectileUpdateTask extends BukkitRunnable {

    private final Supplier<Collection<? extends CBCPlayer>> players;
    private final ProjectileManager projectileManager;

    public ProjectileUpdateTask (Supplier<Collection<? extends CBCPlayer>> players, ProjectileManager projectileManager) {
        this.players = players;
        this.projectileManager = projectileManager;
    }

    @Override
    public void run() {
        projectileManager.updateProjectiles();
        updateFlames();
    }

    public void updateFlames () {
        for (CBCPlayer player : players.get()) {
            player.getFlameDamager().update();
        }
    }

}
