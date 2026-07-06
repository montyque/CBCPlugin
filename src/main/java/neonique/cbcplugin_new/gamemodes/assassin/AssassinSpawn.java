package neonique.cbcplugin_new.gamemodes.assassin;

import neonique.cbcplugin_new.core.CBCPlayer;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.util.Vector;

public class AssassinSpawn extends Location {

    private final AssassinGame game;

    private final boolean ignoreY;
    private final double enemyRadius;

    public AssassinSpawn (AssassinGame game, World world, Vector vector, double enemyRadius, boolean ignoreY) {

        super(world, vector.getX(), vector.getY(), vector.getZ());
        this.game = game;
        this.enemyRadius = enemyRadius;
        this.ignoreY = ignoreY;

    }

    public boolean isEnemyNearbySpawn(CBCPlayer ownPlayer)  {
        return game.getPlayers().stream()
                .filter(CBCPlayer::isOnline)
                .filter(CBCPlayer::isAlive)
                .filter(p -> !p.isAlly(ownPlayer))
                .anyMatch(p -> calculateDistance(p.getPlayer().getLocation()) <= enemyRadius);

    }

    public double calculateDistance(Location location) {
        if (ignoreY) {
            double xDistanceFromCentre = Math.abs(this.getX() - location.getX());
            double zDistanceFromCentre = Math.abs(this.getZ() - location.getZ());
            return Math.pow(xDistanceFromCentre, 2) + Math.pow(zDistanceFromCentre, 2);
        } else {
            return location.distanceSquared(this);
        }
    }
}
