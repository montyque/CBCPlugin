package neonique.cbcplugin_new.tasks.gametasks;

import neonique.cbcplugin_new.gameobjects.DeathBorder;
import org.bukkit.scheduler.BukkitRunnable;

public class DeathBorderShrinkTask extends BukkitRunnable {

    private final DeathBorder border;

    public DeathBorderShrinkTask (DeathBorder border) {
        this.border = border;
    }

    @Override
    public void run () {

        if (!border.isActive()) {
            return;
        }

        border.shrinkBorder();

    }

}
