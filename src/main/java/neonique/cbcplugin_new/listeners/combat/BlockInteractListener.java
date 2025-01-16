package neonique.cbcplugin_new.listeners.combat;

import neonique.cbcplugin_new.managers.GameManager;
import neonique.cbcplugin_new.managers.CombatManager;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

public class BlockInteractListener implements Listener {

    private final GameManager gameManager;
    private final CombatManager combatManager;

    public BlockInteractListener(GameManager gameManager, CombatManager combatManager) {
        this.gameManager = gameManager;
        this.combatManager = combatManager;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent e) {

        if (combatManager.isCanTrapdoorsOpen()) return;
        Player player = e.getPlayer();
        if (!gameManager.hasPlayer(player)) return;
        if (e.getAction() == Action.RIGHT_CLICK_BLOCK) {
            if (e.getClickedBlock() != null) {
                Material type = e.getClickedBlock().getType();
                if (
                        type == Material.ACACIA_TRAPDOOR ||
                                type == Material.BIRCH_TRAPDOOR ||
                                type == Material.CRIMSON_TRAPDOOR ||
                                type == Material.OAK_TRAPDOOR ||
                                type == Material.JUNGLE_TRAPDOOR ||
                                type == Material.DARK_OAK_TRAPDOOR ||
                                type == Material.SPRUCE_TRAPDOOR ||
                                type == Material.WARPED_TRAPDOOR
                ) {
                    e.setCancelled(true);
                }
            }
        }
    }

}
