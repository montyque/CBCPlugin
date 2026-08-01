package neonique.cbcplugin_new.combat.listeners;

import neonique.cbcplugin_new.combat.DeathCause;
import neonique.cbcplugin_new.core.PlayerStore;
import neonique.cbcplugin_new.managers.GameManager;
import neonique.cbcplugin_new.combat.CombatManager;
import neonique.cbcplugin_new.core.CBCPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

public class PlayerDeathListener implements Listener {

    private final PlayerStore players;

    public PlayerDeathListener(PlayerStore players) {
        this.players = players;
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent e) {

        // Check if player died due to creeper
        Player playerEntity = e.getEntity();

        // Check if the player is in game
        if (!players.hasPlayer(playerEntity)) {
            return;
        }

        // Get player
        CBCPlayer player = players.getPlayer(playerEntity);

        // If player is not alive, cancel event
        if (!player.isAlive()) {
            e.setCancelled(true);
            return;
        }

        // Kill player because they are still alive
        combatManager.playerDeath(player, DeathCause.NATURAL);

        // Cancel the player's death event
        e.setCancelled(true);

    }
}
