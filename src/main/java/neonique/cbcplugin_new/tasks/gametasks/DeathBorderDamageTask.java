package neonique.cbcplugin_new.tasks.gametasks;

import neonique.cbcplugin_new.mechanics.DeathBorder;
import neonique.cbcplugin_new.managers.CombatManager;
import neonique.cbcplugin_new.managers.PlayerRegistry;
import neonique.cbcplugin_new.playerclasses.CBCPlayer;
import org.bukkit.scheduler.BukkitRunnable;

public class DeathBorderDamageTask extends BukkitRunnable {

    private final PlayerRegistry playerRegistry;
    private final CombatManager combatManager;
    private final DeathBorder border;

    private final int WARN_DISTANCE = 8;

    public DeathBorderDamageTask (PlayerRegistry playerRegistry, CombatManager combatManager, DeathBorder border) {
        this.playerRegistry = playerRegistry;
        this.combatManager = combatManager;
        this.border = border;
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

        for (CBCPlayer player : playerRegistry.getPlayers()) {
            if (player.isOnline()) {
                border.checkIfPlayerOutsideBorder(player);
            }
        }
    }
}