package neonique.cbcplugin_new.tasks.weapontasks;

import neonique.cbcplugin_new.combat.CombatManager;
import org.bukkit.scheduler.BukkitRunnable;

public class WeaponManagerTimerTask extends BukkitRunnable {

    CombatManager combatManager;

    public WeaponManagerTimerTask (CombatManager w) {
        combatManager = w;
    }

    @Override
    public void run() {
        combatManager.incrementTimer();
    }
}
