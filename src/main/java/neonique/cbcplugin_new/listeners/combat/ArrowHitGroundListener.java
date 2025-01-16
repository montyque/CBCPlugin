package neonique.cbcplugin_new.listeners.combat;

import neonique.cbcplugin_new.CBCPlugin;
import neonique.cbcplugin_new.managers.CombatManager;
import neonique.cbcplugin_new.tasks.weapontasks.FlameExpiryTimerTask;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileHitEvent;

public class ArrowHitGroundListener implements Listener {

    private final CombatManager combatManager;

    public ArrowHitGroundListener(CombatManager combatManager) {
        this.combatManager = combatManager;
    }

    @EventHandler
    public void onArrowHit(ProjectileHitEvent e) {

        // Check if it was an arrow that hit
        Projectile projectile = e.getEntity();
        if (!(projectile instanceof Arrow)) {
            return;
        }

        Arrow arrow = (Arrow) projectile;

        // Check if arrow hit block
        if (e.getHitBlock() == null) {
            return;
        }

        // Check if arrow was either X-Bow or Flame, or not
        if (arrow.getScoreboardTags().contains("xbowArrow")) {
            // Remove projectile immediately
            arrow.remove();
        } else if (arrow.getScoreboardTags().contains("flameArrow")) {
            // Remove projectile in a certain time
            new FlameExpiryTimerTask(combatManager, arrow).runTaskLater(CBCPlugin.getPlugin(),
                    Math.round(combatManager.getFlameZoneLife()));
        }

    }
}
