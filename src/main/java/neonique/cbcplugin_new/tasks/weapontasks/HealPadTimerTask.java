package neonique.cbcplugin_new.tasks.weapontasks;

import neonique.cbcplugin_new.gameobjects.HealthPad;
import org.bukkit.scheduler.BukkitRunnable;

public class HealPadTimerTask extends BukkitRunnable {

    HealthPad healPad;

    public HealPadTimerTask(HealthPad healPad) {
        this.healPad = healPad;
    }

    @Override
    public void run() {

        // Check if heal pad is still reloading
        if (!healPad.isEnabled()) {
            this.cancel();
            return;
        }

        // Run heal pad timer
        healPad.decrementTimer(this);

    }
}
