package neonique.cbcplugin_new.gamemodes.crossbowtag.tasks;

import neonique.cbcplugin_new.gamemodes.crossbowtag.TagGame;
import org.bukkit.scheduler.BukkitRunnable;

public class TagRoundTimer extends BukkitRunnable {

    private final TagGame game;

    public TagRoundTimer(TagGame game) {

        this.game = game;

    }

    @Override
    public void run() {

        if (game.isGameOver()) {
            this.cancel();
            return;
        }

        // Check if round is in play
        if (!game.isRoundInPlay()) {
            this.cancel();
            return;
        }

        game.decrementRoundTimer();
    }

}
