package neonique.cbcplugin_new.mechanics;

import neonique.cbcplugin_new.managers.GameManager;
import neonique.cbcplugin_new.core.CBCPlayer;
import neonique.cbcplugin_new.managers.PlayerRegistry;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.Collection;
import java.util.Objects;

// Used for setting spawnpoints in free for alls
public class FFASpawnpoint {

    private final Location location;

    public FFASpawnpoint (Location location) {
        this.location = location.clone();
    }

    public FFASpawnpoint(GameManager gameManager, Vector coordinates) {
        this.location = new Location(gameManager.getWorld(), coordinates.getX(), coordinates.getY(), coordinates.getZ());
    }


    public double findDistanceOfNearestPlayer(PlayerRegistry registry, Double maxRange, CBCPlayer self) {

        // Get nearby players within the range
        return location.getNearbyEntitiesByType(Player.class, maxRange).stream()
                .map(registry::getPlayer)
                .filter(Objects::nonNull)
                .filter(CBCPlayer::isAlive)
                .filter(p -> !p.isAlly(self))
                .map(p -> p.getPlayer().getLocation().distanceSquared(location))
                .max(Double::compareTo)
                .orElse(Math.pow(maxRange, 2));

    }

    public Location location () {
        return location.clone();
    }

}
