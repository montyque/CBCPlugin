package neonique.cbcplugin_new.tasks.weapontasks;

import neonique.cbcplugin_new.combat.CombatManager;
import neonique.cbcplugin_new.managers.PlayerRegistry;
import neonique.cbcplugin_new.playerclasses.CBCPlayer;
import org.bukkit.scheduler.BukkitRunnable;

public class RespawnTimerTask extends BukkitRunnable {

    private final PlayerRegistry playerRegistry;
    private final CombatManager combatManager;

    public RespawnTimerTask (PlayerRegistry playerRegistry, CombatManager combatManager) {
        this.playerRegistry = playerRegistry;
        this.combatManager = combatManager;
    }

    @Override
    public void run () {

        for (CBCPlayer player : gameManager.getPlayers()) {

            if (!player.isOnline()) continue;
            if (player.isAlive()) continue;

            if (player.getRespawnTicks() <= 0) continue;

            // Decrement player's respawn timer, and respawn if their timer reaches 0
            player.respawnTick();
            if (player.getRespawnTicks() == 0) {
                combatManager.playerRespawn(player);
            } else {
                // Redisplay respawn timer on screen
                if (player.getRespawnTicks() % 20 == 0) {
                    player.getPlayer().showTitle(player.getRespawnTitle());
                }
            }

        }

    }
}
