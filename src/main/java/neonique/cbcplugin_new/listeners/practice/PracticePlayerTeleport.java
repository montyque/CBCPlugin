package neonique.cbcplugin_new.listeners.practice;

import neonique.cbcplugin_new.CBCPlugin;
import neonique.cbcplugin_new.managers.GameManager;
import neonique.cbcplugin_new.managers.PracticeManager;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerTeleportEvent;

public class PracticePlayerTeleport implements Listener {

    private final GameManager gameManager;
    private final PracticeManager practiceManager;

    public PracticePlayerTeleport (GameManager gameManager, PracticeManager practiceManager) {

        this.gameManager = gameManager;
        this.practiceManager = practiceManager;

    }

    @EventHandler
    public void onPlayerTeleport (PlayerTeleportEvent e) {

        if (e.getCause() != PlayerTeleportEvent.TeleportCause.END_GATEWAY) return;
        if (!practiceManager.isEnabled()) return;

        CBCPlugin.getPlugin().getLogger().info("Player " + e.getPlayer().getName() + " has entered the practice portal.");

        Location practicePortalLocation = new Location(gameManager.getWorld(), -1069.5, 126.5, -1656.5);
        if (e.getFrom().distance(practicePortalLocation) < 5) {
            if (gameManager.hasPlayer(e.getPlayer())) return;
            practiceManager.addPlayer(e.getPlayer());
        }

        e.setCancelled(true);

    }

}
