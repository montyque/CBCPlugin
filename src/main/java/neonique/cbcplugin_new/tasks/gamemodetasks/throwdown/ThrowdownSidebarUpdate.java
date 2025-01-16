package neonique.cbcplugin_new.tasks.gamemodetasks.throwdown;

import neonique.cbcplugin_new.gamemodes.throwdown.ThrowdownGame;
import org.bukkit.scheduler.BukkitRunnable;

public class ThrowdownSidebarUpdate extends BukkitRunnable {

    ThrowdownGame game;

    public ThrowdownSidebarUpdate(ThrowdownGame game) {
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
