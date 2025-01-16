package neonique.cbcplugin_new.tasks.gamemodetasks.ctf;

import neonique.cbcplugin_new.gamemodes.ctf.CTFGame;
import org.bukkit.scheduler.BukkitRunnable;

public class CTFSidebarUpdateTask extends BukkitRunnable {

    CTFGame ctfGame;

    public CTFSidebarUpdateTask(CTFGame ctfGame) {
        this.ctfGame = ctfGame;
    }

    @Override
    public void run() {

        if (ctfGame.isGameOver()) {
            this.cancel();
            return;
        }

        ctfGame.getSidebarManager().updateServerBoard();

    }

}
