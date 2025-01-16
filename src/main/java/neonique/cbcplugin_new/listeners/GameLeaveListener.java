package neonique.cbcplugin_new.listeners;

import neonique.cbcplugin_new.managers.GameManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public class GameLeaveListener implements Listener {

    GameManager gameManager;

    public GameLeaveListener(GameManager gameManager) {
        this.gameManager = gameManager;
    }

    @EventHandler
    public void playerLeaveServer(PlayerQuitEvent e) {
        gameManager.playerLeaveServer(e.getPlayer());
    }
}
