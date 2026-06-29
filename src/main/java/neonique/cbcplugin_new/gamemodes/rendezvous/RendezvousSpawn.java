package neonique.cbcplugin_new.gamemodes.rendezvous;

import neonique.cbcplugin_new.managers.GameManager;
import neonique.cbcplugin_new.core.CBCPlayer;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.Collection;

public class RendezvousSpawn extends Location {

    private final RendezvousGame game;

    public RendezvousSpawn(RendezvousGame game, World world, Vector vector) {
        super(world, vector.getX(), vector.getY(), vector.getZ());
        this.game = game;
    }

    public boolean isEnemyNearby (CBCPlayer ownPlayer, double radius) {

        Collection<Player> nearbyPlayerList = getNearbyEntitiesByType(Player.class, radius);

        for (Player playerEntity : nearbyPlayerList) {
            if (game.hasPlayer(playerEntity)) {
                CBCPlayer player = game.getPlayer(playerEntity);
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
