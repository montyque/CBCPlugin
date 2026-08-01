package neonique.cbcplugin_new.combat.tasks;

import neonique.cbcplugin_new.core.PlayerStore;
import neonique.cbcplugin_new.managers.GameManager;
import neonique.cbcplugin_new.core.CBCPlayer;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Collection;
import java.util.Set;
import java.util.function.Supplier;

public class PlayerParticlesTask extends BukkitRunnable {

    private final Supplier<Collection<? extends CBCPlayer>> players;

    public PlayerParticlesTask (Supplier<Collection<? extends CBCPlayer>> players) {
        this.players = players;
    }

    @Override
    public void run() {

        for (CBCPlayer player : players.get()) {

            if (!player.isOnline()) return;
            Player playerEntity = player.getPlayer();

            // If player is immune
            if (player.isImmune()) {

                Particle.TOTEM_OF_UNDYING.builder()
                        .location(player.getPlayer().getLocation().add(0, 2, 0))
                        .offset(0.5, 0.5, 0.5)
                        .count(1)
                        .extra(0)
                        .receivers(128, true);

            }

            // If player is healing
            PotionEffect currentPotionEffect = playerEntity.getPotionEffect(PotionEffectType.REGENERATION);
            if (currentPotionEffect != null) {

                Particle.TRIAL_SPAWNER_DETECTION_OMINOUS.builder()
                        .location(player.getPlayer().getLocation())
                        .offset(0.5, 0.5, 0.5)
                        .count(5)
                        .extra(0)
                        .receivers(128, true);

            }

        }
    }
}
