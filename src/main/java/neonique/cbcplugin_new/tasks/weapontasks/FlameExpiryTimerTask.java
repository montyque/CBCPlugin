package neonique.cbcplugin_new.tasks.weapontasks;

import neonique.cbcplugin_new.managers.CombatManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.UUID;

public class FlameExpiryTimerTask extends BukkitRunnable {

    CombatManager combatManager;
    UUID arrowUUID;

    public FlameExpiryTimerTask(CombatManager combatManager, Arrow arrow) {
        this.combatManager = combatManager;
        this.arrowUUID = arrow.getUniqueId();
    }

    @Override
    public void run() {
        // Remove arrow from game and from list
        combatManager.flameZoneArrowSet.remove(arrowUUID);
        Entity arrow = Bukkit.getEntity(arrowUUID);
        if (arrow == null) return;
        arrow.remove();
    }
}
