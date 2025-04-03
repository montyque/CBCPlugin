package neonique.cbcplugin_new.tasks.gametasks;

import neonique.cbcplugin_new.gameobjects.DeathBorder;
import neonique.cbcplugin_new.managers.GameManager;
import neonique.cbcplugin_new.managers.CombatManager;
import neonique.cbcplugin_new.playerclasses.CBCPlayer;
import org.bukkit.Location;
import org.bukkit.scheduler.BukkitRunnable;

public class DeathBorderDamageTask extends BukkitRunnable {

    GameManager gameManager;
    CombatManager combatManager;
    DeathBorder border;

    private final int WARN_DISTANCE = 8;

    public DeathBorderDamageTask (GameManager gameManager, DeathBorder border) {
        this.border = border;
        this.gameManager = gameManager;
        this.combatManager = gameManager.combatManager;
    }

    @Override
    public void run() {

        if (!border.isActive()) {
            return;
        }

        if (!combatManager.isActive()) {
            border.deactivateBorder();
            return;
        }

        for (CBCPlayer player : gameManager.getAlivePlayers()) {
            if (player.isOnline()) {
                border.checkIfPlayerOutsideBorder(player);
            }
        }
    }
}