package neonique.cbcplugin_new.listeners.gamemodes;

import neonique.cbcplugin_new.managers.GameManager;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

public class PlayerNoMove implements Listener {

    private final GameManager gameManager;

    public PlayerNoMove (GameManager gameManager) {
        this.gameManager = gameManager;
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent e) {

        if (gameManager.getCurrentGame() == null) {
            PlayerMoveEvent.getHandlerList().unregister(this);
            return;
        }

        // Check that the player is in the game
        if (!gameManager.hasPlayer(e.getPlayer())) return;

        Location from = e.getFrom().clone();
        Location to = e.getTo().clone();

        if (from.getX() == to.getX() && from.getZ() == to.getZ()) return;

        e.setTo(from);

    }
}
