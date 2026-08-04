package neonique.cbcplugin_new.combat.tasks;

import neonique.cbcplugin_new.core.PlayerStore;
import neonique.cbcplugin_new.managers.GameManager;
import neonique.cbcplugin_new.combat.CombatManager;
import neonique.cbcplugin_new.core.CBCPlayer;
import org.bukkit.scheduler.BukkitRunnable;

public class TempImmunityTask extends BukkitRunnable {

    private final PlayerStore players;
    private final int frequency;

    public TempImmunityTask (PlayerStore players, int frequency) {
        this.players = players;
        this.frequency = frequency;
    }

    @Override
    public void run() {

        for (CBCPlayer player : players.players()) {
            if (player.isAlive() && player.getTempImmunityTicks() > 0) {
                player.decrementImmunityTicks(frequency);
            }
        }

    }

}
