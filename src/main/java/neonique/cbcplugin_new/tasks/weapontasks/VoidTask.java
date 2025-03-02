package neonique.cbcplugin_new.tasks.weapontasks;

import neonique.cbcplugin_new.enums.DeathCause;
import neonique.cbcplugin_new.managers.GameManager;
import neonique.cbcplugin_new.managers.CombatManager;
import neonique.cbcplugin_new.playerclasses.CBCPlayer;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Set;

public class VoidTask extends BukkitRunnable {

    GameManager gameManager;
    CombatManager combatManager;

    public VoidTask(GameManager gameManager, CombatManager combatManager) {
        this.gameManager = gameManager;
        this.combatManager = combatManager;
    }

    @Override
    public void run() {

        if (!combatManager.voidEnabled()) return;
        if (!combatManager.isVoidKill()) return;

        Set<CBCPlayer> playerSet = gameManager.getAlivePlayers();
        double voidPlane = combatManager.getVoidPlane();
        for (CBCPlayer player : playerSet) {

            if (!player.isOnline()) continue;

            // Check if player is below void line
            if (player.getPlayer().getLocation().getY() < voidPlane) {
                // Kill player
                if (player.getLastPlayerHitBy() != null) {
                    combatManager.playerDeath(player, player.getLastPlayerHitBy(), DeathCause.VOID, false);
                } else {
                    combatManager.playerDeath(player, null, DeathCause.VOID, false);
                }
            }
        }
    }
}
