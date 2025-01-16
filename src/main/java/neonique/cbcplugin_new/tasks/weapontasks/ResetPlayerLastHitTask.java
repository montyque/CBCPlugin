package neonique.cbcplugin_new.tasks.weapontasks;

import neonique.cbcplugin_new.managers.GameManager;
import neonique.cbcplugin_new.playerclasses.CBCPlayer;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Set;

public class ResetPlayerLastHitTask extends BukkitRunnable {

    GameManager gameManager;

    public ResetPlayerLastHitTask (GameManager gameManager) {

        this.gameManager = gameManager;


    }

    @Override
    public void run() {

        Set<CBCPlayer> playerSet = gameManager.getAlivePlayers();

        for (CBCPlayer player : playerSet) {
            player.decrementLastPlayerHit();
        }
    }
}
