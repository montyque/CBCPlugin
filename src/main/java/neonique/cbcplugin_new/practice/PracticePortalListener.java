package neonique.cbcplugin_new.practice;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.function.Consumer;

public class PracticePortalListener implements Listener {

    private final Plugin plugin;
    private final Location portalLocation;
    private final Consumer<Player> enterPortalListener;

    public PracticePortalListener(Plugin plugin, Location portalLocation, Consumer<Player> enterPortalListener) {
        this.plugin = plugin;
        this.portalLocation = portalLocation;
        this.enterPortalListener = enterPortalListener;
    }

    @EventHandler
    public void onPlayerTeleport (PlayerTeleportEvent e) {
        if (e.getCause() != PlayerTeleportEvent.TeleportCause.END_GATEWAY) return;
        if (e.getFrom().distance(portalLocation) < 5) {
            new BukkitRunnable() {
                @Override
                public void run() {
                    enterPortalListener.accept(e.getPlayer());
                }
            }.runTaskLater(plugin, 5);
            e.setCancelled(true);
        }
    }

}
