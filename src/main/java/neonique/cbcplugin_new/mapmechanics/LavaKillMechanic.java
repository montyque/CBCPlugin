package neonique.cbcplugin_new.mapmechanics;

import neonique.cbcplugin_new.combat.CombatContext;
import neonique.cbcplugin_new.combat.DeathCause;
import neonique.cbcplugin_new.core.CBCPlayer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByBlockEvent;
import org.bukkit.event.entity.EntityDamageEvent;

public class LavaKillMechanic implements MapMechanic, Listener {

    private CombatContext combatContext;

    @Override
    public void activate (CombatContext combatContext) {
        this.combatContext = combatContext;
        combatContext.plugin().getServer().getPluginManager().registerEvents(this, combatContext.plugin());
    }

    @Override
    public void deactivate() {
        HandlerList.unregisterAll(this);
    }

    @EventHandler
    public void lavaKillCheck (EntityDamageByBlockEvent e) {

        // Check if the entity damaged is a player
        Entity entity = e.getEntity();
        if (!(entity instanceof Player playerEntity)) {
            return;
        }

        if (!combatContext.players().hasPlayer(playerEntity)) return;
        CBCPlayer player = combatContext.players().getPlayer(playerEntity);

        // Check if damage was caused by lava
        if (e.getCause() == EntityDamageEvent.DamageCause.LAVA) {

            if (player.isImmune()) {
                e.setCancelled(true);
                return;
            }

            combatContext.playerDeath(player, DeathCause.LAVA);

        }

    }

}
