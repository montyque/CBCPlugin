package neonique.cbcplugin_new.misc;

import org.bukkit.scheduler.BukkitRunnable;

public class LambdaRunnable extends BukkitRunnable {
    private final Runnable function;

    public LambdaRunnable(Runnable function) {
        this.function = function;
    }

    @Override
    public void run() {
        function.run();
    }
}