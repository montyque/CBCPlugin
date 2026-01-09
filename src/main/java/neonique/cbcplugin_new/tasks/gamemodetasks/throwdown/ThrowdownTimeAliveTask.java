package neonique.cbcplugin_new.tasks.gamemodetasks.throwdown;

import neonique.cbcplugin_new.gamemodes.throwdown.ThrowdownGame;
import neonique.cbcplugin_new.gamemodes.throwdown.ThrowdownPlayer;
import neonique.cbcplugin_new.managers.GameManager;
import neonique.cbcplugin_new.playerclasses.CBCPlayer;
import org.bukkit.scheduler.BukkitRunnable;

public class ThrowdownTimeAliveTask extends BukkitRunnable {

    private final GameManager gameManager;
    private final ThrowdownGame game;

    public ThrowdownTimeAliveTask(GameManager gameManager, ThrowdownGame game) {

        this.gameManager = gameManager;
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

        for (ThrowdownPlayer player : game.getPlayers()) {
            if (player.isAlive()) {
                player.incrementPlayerSecondsAlive();
            }
        }
    }

}
