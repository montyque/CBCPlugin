package neonique.cbcplugin_new.listeners.combat;

import neonique.cbcplugin_new.managers.CombatManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.potion.PotionEffectType;

public class DisableNightVisionListener implements Listener {

    private final CombatManager combatManager;

    public DisableNightVisionListener(CombatManager combatManager) {
        this.combatManager = combatManager;
    }

    @EventHandler
    public void playerEffect (EntityPotionEffectEvent e) {

        if (!combatManager.isNightVisionDisabled()) return;
        if (!(e.getEntity() instanceof Player)) return;

        Player player = (Player) e.getEntity();

        if (e.getNewEffect() == null) return;
        if (e.getNewEffect().getType() == PotionEffectType.NIGHT_VISION) {
            e.setCancelled(true);
        }
    }
}
