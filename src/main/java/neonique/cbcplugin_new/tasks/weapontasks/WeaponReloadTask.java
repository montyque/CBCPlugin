package neonique.cbcplugin_new.tasks.weapontasks;

import neonique.cbcplugin_new.managers.GameManager;
import neonique.cbcplugin_new.managers.CombatManager;
import neonique.cbcplugin_new.playerclasses.CBCPlayer;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Collection;

public class WeaponReloadTask extends BukkitRunnable {

    GameManager gameManager;
    CombatManager combatManager;

    private boolean updateItems = true;

    public WeaponReloadTask(GameManager gameManager, CombatManager combatManager) {
        this.gameManager = gameManager;
        this.combatManager = combatManager;
    }

    @Override
    public void run() {

        Collection<CBCPlayer> players = gameManager.getPlayers();

        if (updateItems) {
            updateItems = false;
        }
        else {
            updateItems = true;
        }

        for (CBCPlayer player : players) {
            if (player.isAlive()) {
                player.updateWeaponReloads();
                if (updateItems) {
                    player.updateAllWeaponItems();
                }
            }
        }
     }
}
