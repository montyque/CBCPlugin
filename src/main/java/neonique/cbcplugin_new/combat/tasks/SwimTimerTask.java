package neonique.cbcplugin_new.combat.tasks;

import neonique.cbcplugin_new.managers.GameManager;
import neonique.cbcplugin_new.combat.CombatManager;
import neonique.cbcplugin_new.playerclasses.CBCPlayer;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.scheduler.BukkitRunnable;

public class SwimTimerTask extends BukkitRunnable {

    private GameManager gameManager;
    private CombatManager combatManager;

    public SwimTimerTask(GameManager gameManager, CombatManager combatManager) {
        this.gameManager = gameManager;
        this.combatManager = combatManager;
    }

    @Override
    public void run() {

        if (!combatManager.isSwimTimerEnabled()) return;

        for (CBCPlayer player : gameManager.getAlivePlayers()) {

            if (!player.isOnline() || player.isImmune()) continue;
            Block playerLocBelowBlock = player.getPlayer().getLocation().subtract(0, 0.25, 0).getBlock();

            if (((Entity) player.getPlayer()).isOnGround() && !player.getPlayer().isInWater()) {
                player.swimTimerIncrement();
            } else {
                if (player.getPlayer().isInWater()
                        || playerLocBelowBlock.getType() == Material.WATER
                        || playerLocBelowBlock.getType() == Material.SEAGRASS
                        || playerLocBelowBlock.getType() == Material.TALL_SEAGRASS
                        || playerLocBelowBlock.getType() == Material.KELP) {
                    // Decrement swim timer
                    player.swimTimerDecrement();
                } else {
                    // Increment swim timer
                    player.swimTimerIncrement();
                }
            }
        }
    }
}
