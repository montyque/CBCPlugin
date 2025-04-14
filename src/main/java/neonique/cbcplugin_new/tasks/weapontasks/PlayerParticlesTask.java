package neonique.cbcplugin_new.tasks.weapontasks;

import neonique.cbcplugin_new.managers.CombatManager;
import neonique.cbcplugin_new.managers.GameManager;
import neonique.cbcplugin_new.playerclasses.CBCPlayer;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Set;

public class PlayerParticlesTask extends BukkitRunnable {

    private final GameManager gameManager;

    public PlayerParticlesTask (GameManager gameManager) {
        this.gameManager = gameManager;
    }

    @Override
    public void run() {

        World world = gameManager.getWorld();
        Set<CBCPlayer> playerSet = gameManager.getAlivePlayers();

        for (CBCPlayer player : playerSet) {

            if (!player.isOnline()) return;
            Player playerEntity = player.getPlayer();

            // If player is immune
            if (player.isImmune()) {
                world.spawnParticle(Particle.TOTEM_OF_UNDYING, player.getPlayer().getLocation().add(0, 2, 0),
                        1, 0.5, 0.5, 0.5, 0);
            }

            // If player is healing
            PotionEffect currentPotionEffect = playerEntity.getPotionEffect(PotionEffectType.REGENERATION);
            if (currentPotionEffect != null) {
                world.spawnParticle(Particle.TRIAL_SPAWNER_DETECTION_OMINOUS, player.getPlayer().getLocation().add(0, 0, 0),
                        5, 0.5, 0.5, 0.5, 0);
            }

        }
    }
}
