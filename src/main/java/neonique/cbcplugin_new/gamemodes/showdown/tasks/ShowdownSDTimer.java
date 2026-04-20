package neonique.cbcplugin_new.gamemodes.showdown.tasks;

import neonique.cbcplugin_new.gamemodes.showdown.ShowdownGame;
import neonique.cbcplugin_new.managers.GameManager;
import org.bukkit.scheduler.BukkitRunnable;

public class ShowdownSDTimer extends BukkitRunnable {

    private final ShowdownGame showdownGame;

    public ShowdownSDTimer(GameManager gameManager, ShowdownGame showdownGame) {

        this.showdownGame = showdownGame;

    }

    @Override
    public void run() {

        if (showdownGame.isGameOver()) {
            this.cancel();
            return;
        }

        // Check if round is in play
        if (showdownGame.isRoundNotInPlay()) {
            this.cancel();
            return;
        }

        showdownGame.decrementSDTimer(this);
    }
}
