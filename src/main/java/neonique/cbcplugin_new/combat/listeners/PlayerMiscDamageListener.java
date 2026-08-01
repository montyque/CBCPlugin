package neonique.cbcplugin_new.combat.listeners;

import neonique.cbcplugin_new.core.PlayerStore;
import neonique.cbcplugin_new.managers.GameManager;
import neonique.cbcplugin_new.combat.CombatManager;
import neonique.cbcplugin_new.core.CBCPlayer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;

import java.util.function.Function;

public class PlayerMiscDamageListener implements Listener {

    private final Function<Entity, CBCPlayer> playerGetter;

    public PlayerMiscDamageListener (PlayerStore playerStore) {
        this.playerGetter = playerStore::getPlayer;
    }

    @EventHandler
    public void onDamageTaken(EntityDamageEvent e) {

        // Check if damage was done by wither
        if (e.getCause() == EntityDamageEvent.DamageCause.WITHER) {

            CBCPlayer cbcPlayerHurt = playerGetter.apply(e.getEntity());
            if (cbcPlayerHurt == null) return;

            // Check if this player is alive
            if (!cbcPlayerHurt.isAlive()) {
                return;
            }

            // Cancel damage event
            e.setCancelled(true);
        }
    }
}
