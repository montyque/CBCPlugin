package neonique.cbcplugin_new.tasks.gamemodetasks.kmation;

import neonique.cbcplugin_new.gamemodes.kmation.KMationGame;
import neonique.cbcplugin_new.gamemodes.showdown.ShowdownGame;
import org.bukkit.scheduler.BukkitRunnable;

public class KMationSidebarUpdate extends BukkitRunnable {

    KMationGame game;

    public KMationSidebarUpdate(KMationGame game) {
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
