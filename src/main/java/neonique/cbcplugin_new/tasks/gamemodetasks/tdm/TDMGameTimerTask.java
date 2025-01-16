package neonique.cbcplugin_new.tasks.gamemodetasks.tdm;

import neonique.cbcplugin_new.gamemodes.tdm.TDMGame;
import org.bukkit.scheduler.BukkitRunnable;

public class TDMGameTimerTask extends BukkitRunnable {

    private final TDMGame game;

    public TDMGameTimerTask (TDMGame game) {
        this.game = game;
    }

    @Override
    public void run() {

        if (game.isGameOver()) {
            this.cancel();
            return;
        }

        if (game.isGameByTimer()) {
            game.decrementTimer();
        }
    }
}
