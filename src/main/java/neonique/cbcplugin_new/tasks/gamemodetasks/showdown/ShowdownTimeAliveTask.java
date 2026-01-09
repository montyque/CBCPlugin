package neonique.cbcplugin_new.tasks.gamemodetasks.showdown;

import neonique.cbcplugin_new.gamemodes.showdown.ShowdownGame;
import neonique.cbcplugin_new.gamemodes.showdown.ShowdownPlayer;
import neonique.cbcplugin_new.managers.GameManager;
import neonique.cbcplugin_new.playerclasses.CBCPlayer;
import org.bukkit.scheduler.BukkitRunnable;

public class ShowdownTimeAliveTask extends BukkitRunnable {

    private final GameManager gameManager;
    private final ShowdownGame game;

    public ShowdownTimeAliveTask(GameManager gameManager, ShowdownGame showdownGame) {

        this.gameManager = gameManager;
        this.game = showdownGame;

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

        for (CBCPlayer player : gameManager.getAlivePlayers()) {
            ((ShowdownPlayer) player).incrementPlayerSecondsAlive();
        }

        game.updateServerSidebar();
    }
}
