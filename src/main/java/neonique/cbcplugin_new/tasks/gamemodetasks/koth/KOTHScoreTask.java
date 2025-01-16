package neonique.cbcplugin_new.tasks.gamemodetasks.koth;

import neonique.cbcplugin_new.gamemodes.holdthegold.HTGGame;
import neonique.cbcplugin_new.gamemodes.holdthegold.HTGPlayer;
import neonique.cbcplugin_new.gamemodes.koth.KOTHGame;
import org.bukkit.scheduler.BukkitRunnable;

public class KOTHScoreTask extends BukkitRunnable {

    private final KOTHGame game;

    public KOTHScoreTask (KOTHGame game) {
        this.game = game;
    }

    @Override
    public void run() {

        if (game.isGameOver()) {
            this.cancel();
            return;
        }

        game.teamScore();

    }
}