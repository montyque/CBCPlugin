package neonique.cbcplugin_new.tasks.gamemodetasks.tdm;

import neonique.cbcplugin_new.gamemodes.tdm.TDMGame;
import org.bukkit.scheduler.BukkitRunnable;

public class TDMSidebarUpdateTask extends BukkitRunnable {

    TDMGame game;

    public TDMSidebarUpdateTask (TDMGame game) {
        this.game = game;
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
