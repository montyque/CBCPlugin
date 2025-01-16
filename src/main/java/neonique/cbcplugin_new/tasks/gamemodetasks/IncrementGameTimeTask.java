package neonique.cbcplugin_new.tasks.gamemodetasks;

import neonique.cbcplugin_new.gamemodes._base.Game;
import org.bukkit.scheduler.BukkitRunnable;

public class IncrementGameTimeTask extends BukkitRunnable {

    Game game;

    public IncrementGameTimeTask(Game game) {
        this.game = game;
    }

    @Override
    public void run() {

        if (game.isGameOver()) {
            this.cancel();
            return;
        }

        game.incrementGameTime();

    }
}
