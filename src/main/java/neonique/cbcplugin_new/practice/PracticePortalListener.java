package neonique.cbcplugin_new.practice;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerTeleportEvent;

import java.util.function.Consumer;

public class PracticePortalListener implements Listener {

    private final Location portalLocation;
    private final Consumer<Player> enterPortalListener;

    public PracticePortalListener(Location portalLocation, Consumer<Player> enterPortalListener) {
        this.portalLocation = portalLocation;
        this.enterPortalListener = enterPortalListener;
    }

    @EventHandler
    public void onPlayerTeleport (PlayerTeleportEvent e) {
        if (e.getCause() != PlayerTeleportEvent.TeleportCause.END_GATEWAY) return;
        if (e.getFrom().distance(portalLocation) < 5) {
            enterPortalListener.accept(e.getPlayer());
        }
        e.setCancelled(true);
    }

}
