package neonique.cbcplugin_new.tasks.gamemodetasks;

import neonique.cbcplugin_new.core.Game;
import org.bukkit.scheduler.BukkitRunnable;

public class UpdateBossbarsTask extends BukkitRunnable {

    private Game game;

    public UpdateBossbarsTask (Game game) {
        this.game = game;
    }

    @Override
    public void run() {

        if (game.isGameOver()) {
            this.cancel();
            return;
        }

        game.updateBossbarManager();

    }
}
