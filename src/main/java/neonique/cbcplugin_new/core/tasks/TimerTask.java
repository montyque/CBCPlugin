package neonique.cbcplugin_new.core.tasks;

import org.bukkit.scheduler.BukkitRunnable;

import java.util.Map;
import java.util.function.Consumer;

public class TimerTask extends BukkitRunnable {

    private final Consumer<Integer> onTick;
    private final Runnable onEnd;
    private final Map<Integer, Consumer<Integer>> onTimerEquals;

    private int secs;

    public TimerTask (Consumer<Integer> onTick,
                      Runnable onEnd,
                      Map<Integer, Consumer<Integer>> onTimerEquals) {
        this.onTick = onTick;
        this.onEnd = onEnd;
        this.onTimerEquals = onTimerEquals;
    }

    @Override
    public void run () {

        secs--;
        onTick.accept(secs);
        if (secs == 0) {
            onEnd.run();
            if (secs == 0) this.cancel();
        }

    }

    public int getSecs () {
        return secs;
    }

    public void setSecs (int i) {
        this.secs = i;
    }

}
