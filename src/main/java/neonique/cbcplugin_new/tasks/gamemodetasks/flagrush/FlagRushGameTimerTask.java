package neonique.cbcplugin_new.tasks.gamemodetasks.flagrush;

import neonique.cbcplugin_new.gamemodes.flagrush.FlagRushGame;
import org.bukkit.scheduler.BukkitRunnable;

public class FlagRushGameTimerTask extends BukkitRunnable {

    private final FlagRushGame game;

    public FlagRushGameTimerTask (FlagRushGame game) {
        this.game = game;
    }

    @Override
    public void run() {

        if (game.isGameOver()) {
            this.cancel();
            return;
        }

        game.decrementTimer();
    }
}
