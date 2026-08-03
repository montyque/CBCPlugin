package neonique.cbcplugin_new.gamemodes.showdown.tasks;

import neonique.cbcplugin_new.gamemodes.showdown.ShowdownGame;
import neonique.cbcplugin_new.gamemodes.showdown.ShowdownPlayer;
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

        for (ShowdownPlayer player : game.players()) {
            if (player.isAlive()) continue;
            player.incrementPlayerSecondsAlive();
        }

        game.updateServerSidebar();

    }
}
