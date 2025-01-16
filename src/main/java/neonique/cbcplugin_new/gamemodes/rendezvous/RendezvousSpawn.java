package neonique.cbcplugin_new.gamemodes.rendezvous;

import neonique.cbcplugin_new.CBCPlugin;
import neonique.cbcplugin_new.managers.GameManager;
import neonique.cbcplugin_new.playerclasses.CBCPlayer;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.Collection;
import java.util.List;

public class RendezvousSpawn extends Location {

    private final GameManager gameManager;

    public RendezvousSpawn(World world, Vector vector, GameManager gameManager) {
        super(world, vector.getX(), vector.getY(), vector.getZ());
        this.gameManager = gameManager;
    }

    public boolean isEnemyNearby (CBCPlayer ownPlayer, double radius) {

        Collection<Player> nearbyPlayerList = getNearbyEntitiesByType(Player.class, radius);

        for (Player playerEntity : nearbyPlayerList) {
            if (gameManager.hasPlayer(playerEntity)) {
                CBCPlayer player = gameManager.getPlayer(playerEntity);
                if (player.isAlive()) {
                    if (!player.isAlly(ownPlayer)) {
                        return true;
                    }
                }
            }
        }

        return false;

    }
}
