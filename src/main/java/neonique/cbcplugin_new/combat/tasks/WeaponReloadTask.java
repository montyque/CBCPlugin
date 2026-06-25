package neonique.cbcplugin_new.combat.tasks;

import neonique.cbcplugin_new.managers.PlayerRegistry;
import neonique.cbcplugin_new.core.CBCPlayer;
import org.bukkit.scheduler.BukkitRunnable;

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
