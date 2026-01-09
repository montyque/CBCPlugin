package neonique.cbcplugin_new.listeners.lobby;

import neonique.cbcplugin_new.lobby.Lobby;
import neonique.cbcplugin_new.managers.GameManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerJoinEvent;

public class PlayerDamageListener implements Listener {

    private final Lobby lobby;
    private final GameManager gameManager;

    public PlayerDamageListener (Lobby lobby, GameManager gameManager) {
        this.lobby = lobby;
        this.gameManager = gameManager;
    }

    @EventHandler
    public void playerDamage(EntityDamageEvent e) {

        if (!(e.getEntity() instanceof Player player)) {
            return;
        }

        if (gameManager.isPracticeActive()) {
            if (gameManager.hasPlayer(player)) return;
        }

        e.setDamage(0);
        player.setHealth(20);
    }
}
