package neonique.cbcplugin_new.listeners.combat;

import com.destroystokyo.paper.event.player.PlayerJumpEvent;
import neonique.cbcplugin_new.managers.GameManager;
import neonique.cbcplugin_new.managers.CombatManager;
import neonique.cbcplugin_new.playerclasses.CBCPlayer;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class PlayerJumpListener implements Listener {

    private final GameManager gameManager;
    private final CombatManager combatManager;

    public PlayerJumpListener(GameManager gameManager, CombatManager combatManager) {
        this.gameManager = gameManager;
        this.combatManager = combatManager;
    }

    @EventHandler
    public void playerJump(PlayerJumpEvent e) {

        if (!combatManager.isJumpPadsEnabled()) return;

        Player player = e.getPlayer();
        // Verify that player is in game
        if (!gameManager.hasPlayer(player)) {
            return;
        }

        CBCPlayer cbcPlayer = gameManager.getPlayer(player);

        if (combatManager.getJumpPadTask().getPlayersOnJumpPads().contains(cbcPlayer)) {
            player.playSound(player.getLocation(), Sound.ENTITY_ILLUSIONER_PREPARE_BLINDNESS, 200, 2);

        }
    }
}
