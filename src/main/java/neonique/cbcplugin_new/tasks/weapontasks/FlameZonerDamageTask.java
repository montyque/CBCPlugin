package neonique.cbcplugin_new.tasks.weapontasks;

import neonique.cbcplugin_new.enums.DeathCauses;
import neonique.cbcplugin_new.managers.GameManager;
import neonique.cbcplugin_new.managers.CombatManager;
import neonique.cbcplugin_new.playerclasses.CBCPlayer;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

public class FlameZonerDamageTask extends BukkitRunnable {

    private CBCPlayer player;
    private GameManager gameManager;
    private CombatManager combatManager;
    private CBCPlayer lastDamageSource;

    public FlameZonerDamageTask(GameManager gameManager, CombatManager combatManager, CBCPlayer player) {
        this.gameManager = gameManager;
        this.combatManager = combatManager;
        this.player = player;
    }

    @Override
    public void run() {

        if (!gameManager.hasPlayer(player.getPlayer())) {
            this.cancel();
            return;
        }

        if (!player.isOnline()) return;
        Player playerEntity = player.getPlayer();

        // Check if player is still alive, and if not cancel
        if (!player.isAlive()) {
            if (playerEntity.hasPotionEffect(PotionEffectType.WITHER)) {
                playerEntity.removePotionEffect(PotionEffectType.WITHER);
            }
            player.flamezoneFireTicks = 0;
            this.cancel();
            return;
        }

        // Check if player is immune
        if (player.isImmune()) {
            if (playerEntity.hasPotionEffect(PotionEffectType.WITHER)) {
                playerEntity.removePotionEffect(PotionEffectType.WITHER);
            }
            player.flamezoneFireTicks = 0;
            this.cancel();
            return;
        }

        if (player.getInFlameZoneOfPlayer() != null) {
            lastDamageSource = player.getInFlameZoneOfPlayer();
        }

        // Give player damage effect if on fire
        if (player.flamezoneFireTicks > 0) {
            if (!playerEntity.hasPotionEffect(PotionEffectType.WITHER)) {
                playerEntity.addPotionEffect(new PotionEffect(
                        PotionEffectType.WITHER, 10, 1, false, false, false
                ));
            }

            playerEntity.getWorld().spawnParticle(Particle.FLAME,
                    playerEntity.getLocation().clone().add(0, 1, 0), 3,
                    0F, 0F, 0F, 0.4);

            // Check if player dies from the damage
            if (playerEntity.getHealth() <= 1) {
                playerEntity.removePotionEffect(PotionEffectType.WITHER);
                player.flamezoneFireTicks = 0;
                combatManager.playerDeath(player, lastDamageSource, DeathCauses.FLAMEZONE, true);
                this.cancel();
                return;
            } else {
                playerEntity.damage(1);
                player.setLastPlayerHitBy(lastDamageSource);
                player.addPlayerDamaged(lastDamageSource);
            }
        }

        if (!player.isInFlameZoner()) {
            player.flamezoneFireTicks--;
        }
        else {
            player.flamezoneFireTicks = 1;
        }

        if (player.flamezoneFireTicks == 0) {
            if (playerEntity.hasPotionEffect(PotionEffectType.WITHER)) {
                playerEntity.removePotionEffect(PotionEffectType.WITHER);
            }
            this.cancel();
        }
    }
}
