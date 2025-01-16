package neonique.cbcplugin_new.tasks.gamemodetasks.throwdown;

import neonique.cbcplugin_new.gamemodes.throwdown.ThrowdownGame;
import neonique.cbcplugin_new.managers.GameManager;
import org.bukkit.scheduler.BukkitRunnable;

public class ThrowdownPlayerCounts extends BukkitRunnable {

    private final ThrowdownGame game;

    public ThrowdownPlayerCounts(GameManager gameManager, ThrowdownGame game) {

        this.game = game;

    }

    @Override
    public void run() {

        if (game.isGameOver()) {
            this.cancel();
            return;
        }

        // Constantly update player counts and check if teams are eliminated
        game.checkPlayerCounts();
    }
}
