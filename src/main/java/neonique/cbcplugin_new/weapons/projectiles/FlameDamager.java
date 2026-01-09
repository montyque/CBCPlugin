package neonique.cbcplugin_new.weapons.projectiles;

import neonique.cbcplugin_new.enums.DeathCause;
import neonique.cbcplugin_new.playerclasses.CBCPlayer;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashSet;
import java.util.Set;

public class FlameDamager {

    private final CBCPlayer player;
    private int flameTicks = 0;
    private int afterburnTicks = 0;
    private FlameArrow lastFlameArrow;

    private final Set<FlameArrow> damagingFlameArrows = new HashSet<>();

    public FlameDamager (CBCPlayer player) {
        this.player = player;
    }

    public void update () {

        if ((player.isImmune() || !player.isAlive()) && flameTicks > 0) {
            resetFlameDamager();
            return;
        }

        if (damagingFlameArrows.isEmpty()) {
            if (afterburnTicks > 0) {
                afterburnTicks--;
                damageCheck(lastFlameArrow);
            } else {
                resetFlameDamager();
            }
            return;
        }

        if (!damagingFlameArrows.contains(lastFlameArrow)) {
            lastFlameArrow = findClosestArrow(damagingFlameArrows);
        }
        afterburnTicks = 10;
        damageCheck(lastFlameArrow);

        damagingFlameArrows.clear();

    }

    public void damageCheck (FlameArrow source) {

        final int damageRate = 10;

        if (flameTicks % damageRate == 0) {
            damagePlayer(source);
        }
        flameTicks++;

    }

    public void damagePlayer (FlameArrow source) {

        Player playerEntity = player.getPlayer();
        playerEntity.addPotionEffect(new PotionEffect(
                PotionEffectType.WITHER, 10, 1, false, false, false
        ));

        playerEntity.getWorld().spawnParticle(Particle.FLAME,
                playerEntity.getLocation().clone().add(0, 1, 0), 8,
                0.3F, 0.7F, 0.3F, 0.01);

        // Check if player dies from the damage
        CBCPlayer playerSource = source.getSource();
        if (playerEntity.getHealth() <= 1) {
            playerEntity.removePotionEffect(PotionEffectType.WITHER);
            player.getCombatManager().playerDeath(player, playerSource, DeathCause.FLAMEZONE, true);
        } else {
            playerEntity.setNoDamageTicks(0);
            playerEntity.damage(1);
            player.setLastPlayerHitBy(playerSource);
            player.addPlayerDamaged(playerSource);
        }

    }

    public FlameArrow findClosestArrow (Set<FlameArrow> arrows) {

        FlameArrow closestArrow = null;
        Location playerLocation = player.getPlayer().getLocation();
        double smallestDif = Double.POSITIVE_INFINITY;

        for (FlameArrow arrow : arrows) {
            Location arrowLocation = arrow.getArrow().getLocation();
            if (playerLocation.distanceSquared(arrowLocation) < smallestDif) {
                closestArrow = arrow;
            }
        }

        return closestArrow;

    }

    public void checkNewFlameArrow (FlameArrow flameArrow) {
        damagingFlameArrows.add(flameArrow);
    }

    public void resetFlameDamager () {

        if (flameTicks > 0) {
            if (player.isOnline()) {
                Player playerEntity = player.getPlayer();
                if (playerEntity.hasPotionEffect(PotionEffectType.WITHER)) {
                    playerEntity.removePotionEffect(PotionEffectType.WITHER);
                }
            }
        }

        afterburnTicks = 0;
        flameTicks = 0;
        lastFlameArrow = null;
        damagingFlameArrows.clear();

    }



}
