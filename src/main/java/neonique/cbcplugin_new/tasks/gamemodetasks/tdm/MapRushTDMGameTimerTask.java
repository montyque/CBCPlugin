package neonique.cbcplugin_new.tasks.gamemodetasks.tdm;

import neonique.cbcplugin_new.gamemodes.tdm.MapRushTDMGame;
import org.bukkit.scheduler.BukkitRunnable;

public class MapRushTDMGameTimerTask extends BukkitRunnable {

    private final MapRushTDMGame game;

    public MapRushTDMGameTimerTask (MapRushTDMGame game) {
        this.game = game;
    }

    @Override
    public void run() {

        if (game.isGameOver()) {
            this.cancel();
            return;
        }

        game.decrementMapTimer(this);
    }

}
