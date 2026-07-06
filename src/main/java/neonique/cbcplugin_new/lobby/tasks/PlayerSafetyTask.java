package neonique.cbcplugin_new.lobby.tasks;

import neonique.cbcplugin_new.lobby.Lobby;
import neonique.cbcplugin_new.managers.GameManager;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class PlayerSafetyTask extends BukkitRunnable {

    Lobby lobby;
    GameManager gameManager;

    public PlayerSafetyTask(GameManager gameManager, Lobby lobby) {
        this.gameManager = gameManager;
        this.lobby = lobby;
    }

    @Override
    public void run() {
        for (Player player : gameManager.getWorld().getPlayers()) {
            if (player.getLocation().getY() < 0) {
                player.teleport(lobby.getLobbySpawn());
            }
        }
    }
}
