package neonique.cbcplugin_new.tasks.gamemodetasks.holdthegold;

import neonique.cbcplugin_new.gamemodes.holdthegold.HTGGame;
import neonique.cbcplugin_new.gamemodes.holdthegold.HTGPlayer;
import org.bukkit.scheduler.BukkitRunnable;

public class HTGScoreTask extends BukkitRunnable {

    private final HTGGame game;
    private final HTGPlayer playerScoring;

    public HTGScoreTask (HTGGame game, HTGPlayer playerScoring) {
        this.game = game;
        this.playerScoring = playerScoring;
    }

    @Override
    public void run() {

        if (game.isGameOver()) {
            this.cancel();
            return;
        }

        game.score(playerScoring);

    }
}
