package neonique.cbcplugin_new.tasks.gamemodetasks.throwdown;

import neonique.cbcplugin_new.gamemodes.throwdown.ThrowdownGame;
import neonique.cbcplugin_new.managers.GameManager;
import org.bukkit.scheduler.BukkitRunnable;

public class ThrowdownSDTimer extends BukkitRunnable {

    private final ThrowdownGame game;

    public ThrowdownSDTimer(GameManager gameManager, ThrowdownGame game) {

        this.game = game;

    }

    @Override
    public void run() {

        if (game.isGameOver()) {
            this.cancel();
            return;
        }

        // Check if round is in play
        if (game.isRoundNotInPlay()) {
            this.cancel();
            return;
        }

        game.decrementSDTimer();
    }

}
