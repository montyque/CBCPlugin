package neonique.cbcplugin_new.combat.listeners;

import org.bukkit.Material;
import org.bukkit.entity.Entity;import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.function.Predicate;

public class BlockInteractListener implements Listener {

    private final Predicate<Entity> playerChecker;

    public BlockInteractListener (Predicate<Entity> playerChecker) {
        this.playerChecker = playerChecker;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent e) {

        if (!playerChecker.test(e.getPlayer())) return;
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
