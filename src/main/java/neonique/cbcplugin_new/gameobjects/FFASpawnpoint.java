package neonique.cbcplugin_new.gameobjects;

import neonique.cbcplugin_new.managers.GameManager;
import neonique.cbcplugin_new.playerclasses.CBCPlayer;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.Collection;

// Used for setting spawnpoints in free forr alls
public class FFASpawnpoint extends Location {

    GameManager gameManager;

    // Spawnpoint variables
    private Double nearestPlayerRange;

    public FFASpawnpoint(GameManager gameManager, Vector coordinates) {
        super(gameManager.getWorld(), coordinates.getX(), coordinates.getY(), coordinates.getZ());
        this.gameManager = gameManager;
    }


    public double getNearestPlayerRange() {
        return nearestPlayerRange;
    }

    public void findDistanceOfNearestPlayer(Double maxRange, CBCPlayer ownPlayer) {

        // Get nearby players within the range
        nearestPlayerRange = Math.pow(maxRange, 2);
        Collection<Player> nearbyPlayers = this.getNearbyEntitiesByType(Player.class, maxRange);

        // Go through each nearby player and set nearestPlayerRange to
        for (Player player : nearbyPlayers) {

            // Check if player is in game
            if (!gameManager.hasPlayer(player)) {
                continue;
            }

            // Check if player is not an ally
            if (gameManager.getPlayer(player).isAlly(ownPlayer)) {
                continue;
            }

            // Checj if player is alive
            if (!gameManager.getPlayer(player).isAlive()) {
                continue;
            }

            // Get the distance between this spawn and the player
            double distance = this.distanceSquared(player.getLocation());
            if (distance < nearestPlayerRange) {
                nearestPlayerRange = distance;
            }
        }
    }
}
