package neonique.cbcplugin_new.tasks.weapontasks;

import neonique.cbcplugin_new.gameobjects.HealthPad;
import neonique.cbcplugin_new.managers.CombatManager;
import neonique.cbcplugin_new.managers.PlayerRegistry;
import neonique.cbcplugin_new.playerclasses.CBCPlayer;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

public class HealPadDetectionTask extends BukkitRunnable {

    private final CombatManager combatManager;
    private final PlayerRegistry playerRegistry;

    public HealPadDetectionTask (CombatManager combatManager, PlayerRegistry playerRegistry) {
        this.combatManager = combatManager;
        this.playerRegistry = playerRegistry;
    }

    @Override
    public void run() {

        for (HealthPad healPad : combatManager.getHealthPadList()) {

            // Check if this health pad is online
            if (!healPad.isOnline()) continue;
            if (!healPad.isEnabled()) continue;

            // Make particles
            healPad.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, healPad.clone().add(0, 1, 0),
                                3, 1d, 1d, 1d, 1);

            // Find players standing on the heal pad
            List<CBCPlayer> playersOnPad = getPlayersOnPad(healPad);
            if (!playersOnPad.isEmpty()) {
                healPad.healPadPressed(playersOnPad.get(0));
            }

        }
    }

    private List<CBCPlayer> getPlayersOnPad (HealthPad pad) {
        return pad.getNearbyEntitiesByType(Player.class, 3).stream()
                .filter(this::isOnPad)
                .sorted(Comparator.comparingDouble(p -> p.getLocation().distanceSquared(pad)))
                .map(playerRegistry::getPlayer)
                .filter(Objects::nonNull)
                .filter(CBCPlayer::isAlive)
                .toList();
    }

    private boolean isOnPad (Entity e) {
        Location playerLocation = e.getLocation();
        Block blockBelowPlayer = playerLocation.subtract(0, 1, 0).getBlock();
        return blockBelowPlayer.getType() == Material.EMERALD_BLOCK;
    }

}
