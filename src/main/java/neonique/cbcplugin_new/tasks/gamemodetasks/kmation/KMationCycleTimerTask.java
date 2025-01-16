package neonique.cbcplugin_new.tasks.gamemodetasks.kmation;

import neonique.cbcplugin_new.gamemodes.kmation.KMationGame;
import org.bukkit.scheduler.BukkitRunnable;

public class KMationCycleTimerTask extends BukkitRunnable {
    private final KMationGame game;

    public KMationCycleTimerTask (KMationGame game) {
        this.game = game;
    }

    @Override
    public void run() {

        if (game.isGameOver()) {
            this.cancel();
            return;
        }

        game.decrementCycleTimer();
    }
}
