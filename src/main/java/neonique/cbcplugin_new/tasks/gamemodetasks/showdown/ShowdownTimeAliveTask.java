package neonique.cbcplugin_new.tasks.gamemodetasks.showdown;

import neonique.cbcplugin_new.gamemodes.showdown.ShowdownGame;
import neonique.cbcplugin_new.gamemodes.showdown.ShowdownPlayer;
import neonique.cbcplugin_new.managers.GameManager;
import neonique.cbcplugin_new.playerclasses.CBCPlayer;
import org.bukkit.scheduler.BukkitRunnable;

public class ShowdownTimeAliveTask extends BukkitRunnable {

    private final ShowdownGame game;

    public ShowdownTimeAliveTask(ShowdownGame showdownGame) {

        this.game = showdownGame;

    }

    @Override
    public void run() {

        if (game.isGameOver()) {
            this.cancel();
            return;
        }

        if (game.isRoundNotInPlay()) {
            this.cancel();
            return;
        }

        for (ShowdownPlayer player : game.getPlayers()) {
            if (player.isAlive()) continue;
            player.incrementPlayerSecondsAlive();
        }

        game.updateServerSidebar();

    }
}
