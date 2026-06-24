package neonique.cbcplugin_new.combat.tasks;

import neonique.cbcplugin_new.mechanics.JumpPad;
import neonique.cbcplugin_new.managers.GameManager;
import neonique.cbcplugin_new.combat.CombatManager;
import neonique.cbcplugin_new.playerclasses.CBCPlayer;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Collection;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

public class JumpPadTask extends BukkitRunnable {

    GameManager gameManager;
    CombatManager combatManager;

    Set<CBCPlayer> playersOnJumpPads = new HashSet<>();

    public JumpPadTask (GameManager gameManager, CombatManager combatManager) {
        this.gameManager = gameManager;
        this.combatManager = combatManager;
    }

    @Override
    public void run() {

        if (!combatManager.isJumpPadsEnabled()) return;

        Set<CBCPlayer> playersOnJumpPadsNow = new HashSet<>();

        // Iterate through each jump pad
        for (JumpPad jumpPad : combatManager.getJumpPadList()) {

            // Make particles
            for (int i = 0; i < 2; i++) {
                Random random = new Random();
                gameManager.getWorld().spawnParticle(Particle.SNEEZE, jumpPad.clone().add(random.nextDouble() * 2 - 1, 0.5, random.nextDouble() * 2 - 1),
                        0, 0d, 0.5d, 0d, 0.5);
            }

            // Get list of nearby players
            Collection<Player> playersNearby = jumpPad.getNearbyEntitiesByType(Player.class, 5);
            for (Player playerEntity : playersNearby) {
                // Check if each player is in game
                if (!gameManager.hasPlayer(playerEntity)) continue;

                CBCPlayer player = gameManager.getPlayer(playerEntity);

                // Check if player is alive
                if (!player.isAlive()) continue;

                // Press on this heal pad
                jumpPad.jumpPadPressed(player);
                playersOnJumpPadsNow.add(player);
                playersOnJumpPads.add(player);
            }
        }

        for (CBCPlayer player : new HashSet<>(playersOnJumpPads)) {
            if (!playersOnJumpPadsNow.contains(player)) {
                playersOnJumpPads.remove(player);
                player.jumpPadOff();
            }
        }
    }

    public Set<CBCPlayer> getPlayersOnJumpPads() {
        return playersOnJumpPads;
    }
}
