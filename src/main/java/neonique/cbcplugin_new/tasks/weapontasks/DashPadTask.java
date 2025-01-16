package neonique.cbcplugin_new.tasks.weapontasks;

import neonique.cbcplugin_new.gameobjects.DashPad;
import neonique.cbcplugin_new.managers.GameManager;
import neonique.cbcplugin_new.managers.CombatManager;
import neonique.cbcplugin_new.playerclasses.CBCPlayer;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashSet;
import java.util.Set;

public class DashPadTask extends BukkitRunnable {

    GameManager gameManager;
    CombatManager combatManager;

    public DashPadTask (GameManager gameManager, CombatManager combatManager) {
        this.gameManager = gameManager;
        this.combatManager = combatManager;
    }

    @Override
    public void run() {

        if (!combatManager.isDashPadsEnabled()) return;

        // Iterate through each jump pad
        for (DashPad dashPad : combatManager.getDashPadList()) {

            dashPad.refreshCooldowns();

            Set<CBCPlayer> playersOnDashPad = new HashSet<>();
            for (Location block : dashPad.getBlocks()) {
                for (Player playerEntity : block.getNearbyPlayers(1)) {
                    // Check if each player is in game
                    if (!gameManager.hasPlayer(playerEntity)) continue;
                    CBCPlayer player = gameManager.getPlayer(playerEntity);
                    // Check if player is alive
                    if (!player.isAlive()) continue;
                    for (int x = -1; x <= 1; x++) {
                        for (int z = -1; z <= 1; z++) {
                            if (playerEntity.getLocation().subtract(x, 1, z).getBlock().getType() == Material.ORANGE_GLAZED_TERRACOTTA) {
                                playersOnDashPad.add(player);
                                break;
                            }
                        }
                    }
                }
            }

            for (CBCPlayer player : playersOnDashPad) {
                dashPad.playerPressed(player);
            }
        }
    }
}
