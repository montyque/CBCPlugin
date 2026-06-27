package neonique.cbcplugin_new.combat.tasks;

import neonique.cbcplugin_new.mapmechanics.DashPad;
import neonique.cbcplugin_new.managers.GameManager;
import neonique.cbcplugin_new.combat.CombatManager;
import neonique.cbcplugin_new.core.CBCPlayer;
import org.bukkit.scheduler.BukkitRunnable;

/*
public class DashPadTask extends BukkitRunnable {

    GameManager gameManager;
    CombatManager combatManager;

    public DashPadTask (GameManager gameManager, CombatManager combatManager) {
        this.gameManager = gameManager;
        this.combatManager = combatManager;
    }

    @Override
    public void run() {

        if (!combatManager.isDashPadsEnabled()) return;

        // Iterate through each jump pad
        for (DashPad dashPad : combatManager.getDashPadList()) {
            dashPad.updateCooldwns();
            for (CBCPlayer player : dashPad.getPlayersOnPad(gameManager.getPlayerRegistry())) {
                dashPad.launchPlayer(player);
            }
        }
    }
}*/
