package neonique.cbcplugin_new.mapmechanics;

import neonique.cbcplugin_new.CBCPlugin;
import neonique.cbcplugin_new.combat.CombatManager;
import neonique.cbcplugin_new.combat.DeathCause;
import neonique.cbcplugin_new.core.CBCPlayer;
import neonique.cbcplugin_new.managers.PlayerRegistry;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class SwimTimerMechanic implements MapMechanic {

    private static final int UPDATE_RATE = 2;

    private final int length;

    private PlayerRegistry registry;
    private CombatManager combatManager;
    private BukkitRunnable updateTask;

    public SwimTimerMechanic (int length) {
        this.length = length / UPDATE_RATE;
    }

    @Override
    public void activate(PlayerRegistry registry, CombatManager combatManager) {
        this.registry = registry;
        this.combatManager = combatManager;

        updateTask = new BukkitRunnable() {
            @Override
            public void run() {
                update();
            }
        };
        updateTask.runTaskTimer(CBCPlugin.getPlugin(), 0, UPDATE_RATE);
    }

    @Override
    public void deactivate() {
        updateTask.cancel();
    }

    public void update() {

        for (CBCPlayer player : registry.getPlayers()) {
            if (!player.isAlive()) continue;
            if (player.isImmune()) continue;
            Player entity = player.getPlayer();
            SwimTimer timer = player.getSwimTimer();

            if (inWater(entity)) {
                // Player is in water, begin lowering their swim timer
                if (!player.hasSwimTimer()) {
                    player.startSwimTimer(length);
                    timer = player.getSwimTimer();
                }
                timer.decrement();
                player.updateSwimTimerBubbles();

                // Check if player should be damaged via drowning
                if (player.getSwimTimer().empty()) {
                    timer.decrementDamage();
                    if (timer.damageEmpty()) {
                        // Damage player
                        damagePlayer(player);
                        timer.resetDamage();
                    }
                }

            } else if (player.hasSwimTimer()) {
                // Player is not in water but has a timer, increase their swim timer
                timer.increment();
                timer.resetDamageZero();
                player.updateSwimTimerBubbles();
                if (timer.full()) player.resetSwimTimer();

            }
        }
    }

    private void damagePlayer (CBCPlayer player) {

        Player entity = player.getPlayer();
        entity.getWorld().playSound(entity.getLocation(), Sound.ENTITY_PLAYER_HURT_DROWN, 1, 1);
        entity.getWorld().spawnParticle(Particle.BUBBLE, entity.getLocation(), 10, null);
        if (entity.getHealth() <= 1) {
            combatManager.playerDeath(player, player.getLastPlayerHitBy(), DeathCause.DROWN, false);
        } else {
            entity.setHealth(entity.getHealth() - 1);
        }

    }

    @SuppressWarnings("deprecation")
    private boolean inWater (Player entity) {
        Block blockBelow = entity.getLocation().subtract(0, 0.25, 0).getBlock();
        if (entity.isOnGround() && !entity.isInWater()) return false;
        return entity.isInWater() || blockBelow.getType() == Material.WATER;
    }

}
