package neonique.cbcplugin_new.tasks.gamemodetasks.holdthegold;

import neonique.cbcplugin_new.gamemodes.holdthegold.HTGGame;
import org.bukkit.scheduler.BukkitRunnable;

public class HTGSidebarUpdateTask extends BukkitRunnable {

    HTGGame game;

    public HTGSidebarUpdateTask(HTGGame htgGame) {
        this.game = htgGame;
    }

    @Override
    public void run() {

        if (game.isGameOver()) {
            this.cancel();
            return;
        }

        game.getSidebarManager().updateServerBoard();

    }

}
