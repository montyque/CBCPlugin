package neonique.cbcplugin_new.combat.listeners;

import neonique.cbcplugin_new.managers.GameManager;
import neonique.cbcplugin_new.combat.CombatManager;
import neonique.cbcplugin_new.playerclasses.CBCPlayer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;

public class PlayerMiscDamageListener implements Listener {
    private final GameManager gameManager;
    private final CombatManager combatManager;

    public PlayerMiscDamageListener(GameManager gameManager, CombatManager combatManager) {
        this.gameManager = gameManager;
        this.combatManager = combatManager;
    }

    @EventHandler
    public void onDamageTaken(EntityDamageEvent e) {

        // Check if damage was done by wither
        if (e.getCause() == EntityDamageEvent.DamageCause.WITHER) {

            // Check if the entity who was damaged is a player
            Entity entityHurt = e.getEntity();
            if (!(entityHurt instanceof Player playerHurt)) {
                return;
            }

            // Check if player is in players list
            if (!(this.gameManager.hasPlayer(playerHurt))) {
                return;
            }

            CBCPlayer cbcPlayerHurt = this.gameManager.getPlayer(playerHurt);

            // Check if this player is alive
            if (!cbcPlayerHurt.isAlive()) {
                return;
            }

            // Cancel damage event
            e.setCancelled(true);
        }
    }
}
