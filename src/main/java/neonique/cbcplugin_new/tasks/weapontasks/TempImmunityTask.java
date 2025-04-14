package neonique.cbcplugin_new.tasks.weapontasks;

import neonique.cbcplugin_new.managers.GameManager;
import neonique.cbcplugin_new.managers.CombatManager;
import neonique.cbcplugin_new.playerclasses.CBCPlayer;
import org.bukkit.Particle;
import org.bukkit.scheduler.BukkitRunnable;

public class TempImmunityTask extends BukkitRunnable {

    private final GameManager gameManager;
    private final CombatManager combatManager;
    private final CBCPlayer player;
    private int tick = 0;
    private final int tickLimit;

    public TempImmunityTask(GameManager gameManager, CombatManager combatManager, CBCPlayer player, int tickLimit) {
        this.gameManager = gameManager;
        this.combatManager = combatManager;
        this.player = player;
        this.tickLimit = tickLimit;
    }

    @Override
    public void run() {

        if (!player.isOnline()) return;

        // If all players are immune, make sure player is permanently immune
        if (combatManager.isAllPlayersImmune()) {
            this.cancel();
            return;
        }

        // Make sure player is still alive
        if (!player.isAlive()) {
            player.setImmune(false);
            this.cancel();
            return;
        }

        tick++;
        if (tick >= tickLimit) {
            player.setImmune(false);
            this.cancel();
        }
    }
}
