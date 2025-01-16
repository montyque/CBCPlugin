package neonique.cbcplugin_new.tasks.weapontasks;

import neonique.cbcplugin_new.CBCPlugin;
import neonique.cbcplugin_new.managers.CombatManager;
import org.bukkit.Particle;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashSet;
import java.util.UUID;

public class ArrowHitGroundTask extends BukkitRunnable {

    CombatManager combatManager;

    public ArrowHitGroundTask(CombatManager combatManager) {
        this.combatManager = combatManager;
    }

    @Override
    public void run() {
        for (UUID flameArrowUUID : new HashSet<>(this.combatManager.flameZoneArrowSet)) {

            Entity entity = CBCPlugin.getPlugin().getServer().getEntity(flameArrowUUID);
            if (entity == null) continue;
            if (!(entity instanceof Arrow)) continue;
            Arrow flameArrow = (Arrow) entity;

            if (flameArrow.isDead()) continue;
            if (!flameArrow.isInBlock()) continue;
            if (flameArrow.getScoreboardTags().contains("expiring")) continue;

            new FlameExpiryTimerTask(combatManager, flameArrow).runTaskLater(CBCPlugin.getPlugin(),
                    Math.round(combatManager.getFlameZoneLife() * 20));
            flameArrow.addScoreboardTag("expiring");
        }

        for (UUID xbowArrowUUID : new HashSet<>(this.combatManager.xbowArrowSet)) {

            Entity entity = CBCPlugin.getPlugin().getServer().getEntity(xbowArrowUUID);
            if (entity == null) continue;
            if (!(entity instanceof Arrow)) continue;
            Arrow xbowArrow = (Arrow) entity;

            if (xbowArrow.isDead()) continue;
            if (!xbowArrow.isInBlock()) continue;

            xbowArrow.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, xbowArrow.getLocation(), 12, 0.0, 0.0, 0.0, 0.2, null, false);
            combatManager.removeXbowArrow(xbowArrow);
        }
    }
}
