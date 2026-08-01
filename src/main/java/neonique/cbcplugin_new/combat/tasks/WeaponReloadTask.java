package neonique.cbcplugin_new.combat.tasks;

import neonique.cbcplugin_new.managers.PlayerRegistry;
import neonique.cbcplugin_new.core.CBCPlayer;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Collection;
import java.util.function.Supplier;

public class WeaponReloadTask extends BukkitRunnable {

    private final Supplier<Collection<CBCPlayer>> players;
    private boolean updateItems = false;

    public WeaponReloadTask(Supplier<Collection<CBCPlayer>> players) {
        this.players = players;
    }

    @Override
    public void run() {

        updateItems = !updateItems;

        for (CBCPlayer player : players.get()) {
            if (player.isAlive()) {
                player.getInventory().updateWeaponReloads();
                player.updateActionBarDisplay(true);
                if (updateItems) {
                    player.getInventory().loadSlots();
                }
            }
        }
     }
}
