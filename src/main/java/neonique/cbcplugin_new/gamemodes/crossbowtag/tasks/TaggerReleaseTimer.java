package neonique.cbcplugin_new.gamemodes.crossbowtag.tasks;

import neonique.cbcplugin_new.gamemodes.crossbowtag.TagGame;
import org.bukkit.scheduler.BukkitRunnable;

public class TaggerReleaseTimer extends BukkitRunnable {

    private final TagGame game;

    public TaggerReleaseTimer (TagGame game) {
        this.game = game;
    }

    @Override
    public void run() {

        if (game.isGameOver()) {
            this.cancel();
            return;
        }

        game.decrementTaggerReleaseTimer();

    }
}
