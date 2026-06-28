package neonique.cbcplugin_new.combat.listeners;

import neonique.cbcplugin_new.combat.DeathCause;
import neonique.cbcplugin_new.managers.GameManager;
import neonique.cbcplugin_new.combat.CombatManager;
import neonique.cbcplugin_new.core.CBCPlayer;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByBlockEvent;
import org.bukkit.event.entity.EntityDamageEvent;

public class LavaDamageListener implements Listener {

    private final GameManager gameManager;
    private final CombatManager combatManager;

    public LavaDamageListener(GameManager gameManager, CombatManager combatManager) {
        this.gameManager = gameManager;
        this.combatManager = combatManager;
    }

    @EventHandler
    public void onEntityDamagePlayer(EntityDamageByBlockEvent e) {

        // Check if player died due to creeper
        Entity entity = e.getEntity();

        // Check if the entity damaged is a player
        if (!(entity instanceof Player playerEntity)) {
            return;
        }

        // Check if the player damaged is in the game
        if (!(gameManager.hasPlayer(playerEntity))) {
            return;
        }

        if (e.getDamager() != null) {
            if (e.getDamager().getType() == Material.MAGMA_BLOCK) {
                e.setCancelled(true);
            }
        }

    }
}
