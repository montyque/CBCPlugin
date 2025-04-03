package neonique.cbcplugin_new.tasks.weapontasks;

import neonique.cbcplugin_new.enums.WeaponsState;
import neonique.cbcplugin_new.managers.CombatManager;
import org.bukkit.World;
import org.bukkit.scheduler.BukkitRunnable;

public class DayCycleTask extends BukkitRunnable {

    private final CombatManager combatManager;
    private final int speed;
    private final World world;

    public DayCycleTask (CombatManager combatManager, World world, int speed) {
        this.combatManager = combatManager;
        this.speed = speed;
        this.world = world;
    }

    @Override
    public void run() {

        if (combatManager.isActive()) {
            world.setTime(world.getTime() + speed);
        }
        else {
            this.cancel();
        }

    }
}
