package neonique.cbcplugin_new.tasks;

import neonique.cbcplugin_new.misc.Cutscene;
import org.bukkit.scheduler.BukkitRunnable;

public class CutsceneRunTask extends BukkitRunnable {

    private final Cutscene cutscene;

    public CutsceneRunTask (Cutscene cutscene) {
        this.cutscene = cutscene;
    }


    @Override
    public void run() {

        if (!cutscene.isActive()) {
            this.cancel();
            return;
        }

        // Run cutscene tick
        cutscene.tick(false);

    }
}
