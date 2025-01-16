package neonique.cbcplugin_new.tasks.lobbytasks;

import neonique.cbcplugin_new.lobby.Lobby;
import org.bukkit.scheduler.BukkitRunnable;

public class LobbySidebarManagerTask extends BukkitRunnable {

    Lobby lobby;

    public LobbySidebarManagerTask(Lobby lobby) {
        this.lobby = lobby;
    }

    @Override
    public void run() {

        if (!lobby.getSidebarManager().isActive()) {
            this.cancel();
            return;
        }

        lobby.getSidebarManager().updateAllClientBoards();

    }
}
