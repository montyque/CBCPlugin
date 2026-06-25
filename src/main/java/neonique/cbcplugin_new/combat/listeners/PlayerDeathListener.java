package neonique.cbcplugin_new.combat.listeners;

import neonique.cbcplugin_new.combat.DeathCause;
import neonique.cbcplugin_new.managers.GameManager;
import neonique.cbcplugin_new.combat.CombatManager;
import neonique.cbcplugin_new.core.CBCPlayer;
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
            combatManager.playerDeath(player, player.getLastPlayerHitBy(), DeathCause.NATURAL, false);
        }
        else {
            combatManager.playerDeath(player, null, DeathCause.NATURAL, false);
        }

        // Cancel the player's death event
        e.setCancelled(true);

    }
}
