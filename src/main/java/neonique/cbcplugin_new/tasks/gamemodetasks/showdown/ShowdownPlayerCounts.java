package neonique.cbcplugin_new.tasks.gamemodetasks.showdown;

import neonique.cbcplugin_new.gamemodes.showdown.ShowdownGame;
import neonique.cbcplugin_new.managers.GameManager;
import org.bukkit.scheduler.BukkitRunnable;

public class ShowdownPlayerCounts extends BukkitRunnable {

    private final GameManager gameManager;
    private final ShowdownGame showdownGame;

    public ShowdownPlayerCounts(GameManager gameManager, ShowdownGame showdownGame) {

        this.gameManager = gameManager;
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
            return;
        }

        // Constantly update player counts and check if teams are eliminated
        showdownGame.checkPlayerCounts();


    }
}
