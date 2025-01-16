package neonique.cbcplugin_new.listeners.combat;

import neonique.cbcplugin_new.enums.DeathCauses;
import neonique.cbcplugin_new.managers.GameManager;
import neonique.cbcplugin_new.managers.CombatManager;
import neonique.cbcplugin_new.playerclasses.CBCPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

public class PlayerDeathListener implements Listener {

    private final GameManager gameManager;
    private final CombatManager combatManager;

    public PlayerDeathListener(GameManager gameManager, CombatManager combatManager) {
        this.gameManager = gameManager;
        this.combatManager = combatManager;
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent e) {

        // Check if player died due to creeper
        Player playerEntity = e.getEntity();

        // Check if the player is in game
        if (!gameManager.hasPlayer(playerEntity)) {
            return;
        }

        // Get player
        CBCPlayer player = gameManager.getPlayer(playerEntity);

        // If player is not alive, cancel event
        if (!player.isAlive()) {
            e.setCancelled(true);
            return;
        }

        // Kill player because they are still alive
        if (player.getLastPlayerHitBy() != null) {
            combatManager.playerDeath(player, player.getLastPlayerHitBy(), DeathCauses.NATURAL, false);
        }
        else {
            combatManager.playerDeath(player, null, DeathCauses.NATURAL, false);
        }

        // Cancel the player's death event
        e.setCancelled(true);

    }
}
