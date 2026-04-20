package neonique.cbcplugin_new.tasks.weapontasks;

import neonique.cbcplugin_new.managers.GameManager;
import neonique.cbcplugin_new.managers.CombatManager;
import neonique.cbcplugin_new.managers.PlayerRegistry;
import neonique.cbcplugin_new.playerclasses.CBCPlayer;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Collection;

public class WeaponReloadTask extends BukkitRunnable {

    private final PlayerRegistry playerRegistry;
    private boolean updateItems = false;

    public WeaponReloadTask(PlayerRegistry playerRegistry) {
        this.playerRegistry = playerRegistry;
    }

    @Override
    public void run() {

        updateItems = !updateItems;

        for (CBCPlayer player : playerRegistry.getPlayers()) {
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
