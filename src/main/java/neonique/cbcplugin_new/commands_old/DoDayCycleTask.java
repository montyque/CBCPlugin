package neonique.cbcplugin_new.commands_old;

import org.bukkit.World;
import org.bukkit.scheduler.BukkitRunnable;

public class DoDayCycleTask extends BukkitRunnable {

    World world;

    public DoDayCycleTask(World world) {
        this.world = world;
    }

    @Override
    public void run() {
        world.setTime(world.getTime() + 7);
    }
}
