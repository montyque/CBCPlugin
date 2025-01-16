package neonique.cbcplugin_new.listeners;

import neonique.cbcplugin_new.managers.GameManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class GameJoinListener implements Listener {

    GameManager gameManager;

    public GameJoinListener(GameManager gameManager) {
        this.gameManager = gameManager;
    }

    @EventHandler
    public void playerJoinServer(PlayerJoinEvent e) {
        gameManager.playerJoinServer(e.getPlayer());
    }
}
