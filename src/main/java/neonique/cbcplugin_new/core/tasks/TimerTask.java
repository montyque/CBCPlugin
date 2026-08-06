package neonique.cbcplugin_new.core.tasks;

import org.bukkit.scheduler.BukkitRunnable;

import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class TimerTask extends BukkitRunnable {

    private final Consumer<Integer> onTick;
    private final Supplier<Boolean> runCondition;
    private final Runnable onEnd;

    private int secs;

    public TimerTask (int length,
                      Consumer<Integer> onTick,
                      Supplier<Boolean> runCondition,
                      Runnable onEnd) {
        this.secs = length;
        this.onTick = onTick;
        this.runCondition = runCondition;
        this.onEnd = onEnd;
    }

    public TimerTask (int length,
                      Consumer<Integer> onTick,
                      Runnable onEnd) {
        this(length, onTick, () -> true, onEnd);
    }

    @Override
    public void run () {

        if (!runCondition.get()) {
            this.cancel();
        }

        secs--;

        // Run any task scheduled when the timer hits this value
        if (timerEventsMap().containsKey(secs)) {
            timerEventsMap().get(secs).accept(secs);
        }

        // Run a task scheduled to occur every trip
        onTick.accept(secs);

        // Run a task when the timer hits 0, and cancel if still 0 after
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

    public Map<Integer, Consumer<Integer>> timerEventsMap () {
        return Map.of();
    }

}
