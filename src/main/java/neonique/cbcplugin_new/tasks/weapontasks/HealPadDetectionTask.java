package neonique.cbcplugin_new.tasks.weapontasks;

import neonique.cbcplugin_new.gameobjects.HealthPad;
import neonique.cbcplugin_new.managers.GameManager;
import neonique.cbcplugin_new.managers.CombatManager;
import neonique.cbcplugin_new.playerclasses.CBCPlayer;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Collection;

public class HealPadDetectionTask extends BukkitRunnable {

    GameManager gameManager;
    CombatManager combatManager;

    public HealPadDetectionTask (GameManager gameManager, CombatManager combatManager) {
        this.gameManager = gameManager;
        this.combatManager = combatManager;
    }

    @Override
    public void run() {

        // Iterate through each heal pad
        for (HealthPad healPad : combatManager.getHealthPadList()) {
            // Check if this health pad is online
            if (!healPad.isOnline()) continue;
            if (!healPad.isEnabled()) continue;

            // Make particles
            gameManager.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, healPad.clone().add(0, 1, 0),
                                3, 1d, 1d, 1d, 1);

            // Get list of nearby players
            Collection<Player> playersNearby = healPad.getNearbyEntitiesByType(Player.class, 3);
            for (Player playerEntity : playersNearby) {
                // Check if each player is in game
                if (!gameManager.hasPlayer(playerEntity)) continue;

                CBCPlayer player = gameManager.getPlayer(playerEntity);

                // Check if player is alive
                if (!player.isAlive()) continue;

                // Check if player is stepping on emerald block
                Location playerLocation = playerEntity.getLocation();
                Block blockBelowPlayer = playerLocation.subtract(0, 1, 0).getBlock();
                if (blockBelowPlayer.getType() != Material.EMERALD_BLOCK) continue;

                // Press on this heal pad
                healPad.healPadPressed(player);

            }
        }
    }
}
