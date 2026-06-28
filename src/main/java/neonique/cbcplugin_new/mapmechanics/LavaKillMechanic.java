package neonique.cbcplugin_new.mapmechanics;

import neonique.cbcplugin_new.CBCPlugin;
import neonique.cbcplugin_new.combat.CombatManager;
import neonique.cbcplugin_new.combat.DeathCause;
import neonique.cbcplugin_new.core.CBCPlayer;
import neonique.cbcplugin_new.managers.PlayerRegistry;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByBlockEvent;
import org.bukkit.event.entity.EntityDamageEvent;

public class LavaKillMechanic implements MapMechanic, Listener {

    private PlayerRegistry registry;
    private CombatManager combatManager;

    @Override
    public void activate (PlayerRegistry registry, CombatManager combatManager) {
        this.registry = registry;
        this.combatManager = combatManager;
        CBCPlugin.getPlugin().registerListener(this);
    }

    @Override
    public void deactivate() {
        CBCPlugin.getPlugin().unregisterListener(this);
    }

    @EventHandler
    public void lavaKillCheck (EntityDamageByBlockEvent e) {

        // Check if the entity damaged is a player
        Entity entity = e.getEntity();
        if (!(entity instanceof Player playerEntity)) {
            return;
        }

        if (!registry.hasPlayer(playerEntity)) return;
        CBCPlayer player = registry.getPlayer(playerEntity);

        // Check if damage was caused by lava
        if (e.getCause() == EntityDamageEvent.DamageCause.LAVA) {

            if (player.isImmune()) {
                e.setCancelled(true);
                return;
            }

            combatManager.playerDeath(player, DeathCause.LAVA);

        }

    }

}
