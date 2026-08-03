package neonique.cbcplugin_new.gamemodes.kmation;

import neonique.cbcplugin_new.core.CBCPlayer;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class KMationSpawn extends Location {

    private final KMationGame game;

    private final boolean ignoreY;
    private final int enemyRadius;
    private final double enemyTargetDistance;
    private double nearestEnemyDistanceMinusTarget;
    private final Location mapCenter;
    private double mapYAverage;

    public KMationSpawn(KMationGame game, World world, Vector vector, int enemyRadius, int enemyTargetDistance, boolean ignoreY,
                        Location mapCenter, double mapYAverage) {
        super(world, vector.getX(), vector.getY(), vector.getZ());
        this.game = game;
        this.enemyRadius = enemyRadius;
        this.enemyTargetDistance = Math.pow(enemyTargetDistance, 2);
        this.mapCenter = mapCenter;
        this.ignoreY = ignoreY;
    }

    public void setNearestEnemyDistanceMinusTarget () {

        double nearestEnemyDistance = 1000000;
        // Go through all enemy alive players and check the distance between them and this spawn point
        for (CBCPlayer player : game.players()) {
            if (!player.isAlive()) continue;
            if (calculateDistance(player.getPlayer().getLocation()) < nearestEnemyDistance) {
                nearestEnemyDistance = calculateDistance(player.getPlayer().getLocation());
            }
        }

        // Just in case, if nearest enemy distance is still equal to 1000000, set nearestEnemyDistance to the distance
        // from the spawn to the centre of the map
        if (nearestEnemyDistance == 1000000) {
            nearestEnemyDistance = calculateDistance(mapCenter);
        }

        nearestEnemyDistanceMinusTarget = Math.abs(nearestEnemyDistance - enemyTargetDistance);
    }

    public double getNearestEnemyDistanceMinusTarget () {
        return nearestEnemyDistanceMinusTarget;
    }

    public boolean isEnemyNearbySpawn(CBCPlayer ownPlayer) {
        for (Player player : getNearbyPlayers(enemyRadius)) {
            if (!game.hasPlayer(player)) continue;
            CBCPlayer cbcplayer = game.getPlayer(player);
            if (!cbcplayer.isAlive()) continue;
            if (ownPlayer.isAlly(cbcplayer)) continue;
            return true;
        }
        return false;
    }

    private double calculateDistance(Location location) {
        if (ignoreY) {
            double xDistanceFromCentre = Math.abs(this.getX() - location.getX());
            double zDistanceFromCentre = Math.abs(this.getZ() - location.getZ());
            return Math.pow(xDistanceFromCentre, 2) + Math.pow(zDistanceFromCentre, 2);
        } else {
            return location.distanceSquared(this);
        }
    }

    public boolean isEnemyTargetDistance (CBCPlayer player) {

        Location locCheck = this.clone();
        if (ignoreY) {
            locCheck.setY(mapYAverage);
        }

        Collection<Player> enemiesNearby = getEnemiesNearby(locCheck, player, enemyTargetDistance + 10);
        double sqrd = (enemyTargetDistance - 5) * (enemyTargetDistance - 5);
        for (Player plr : enemiesNearby) {
            if (distanceSquared(plr.getLocation()) < sqrd) {
                return false;
            }
        }

        return !enemiesNearby.isEmpty();
    }

    public Collection<Player> getEnemiesNearby (Location loc, CBCPlayer ownPlayer, double radius) {

        Set<Player> nearbyPlayerList = new HashSet<>();

        for (Player playerEntity : loc.getNearbyEntitiesByType(Player.class, radius)) {
            if (game.hasPlayer(playerEntity)) {
                CBCPlayer player = game.getPlayer(playerEntity);
                if (player.isAlive()) {
                    if (!player.isAlly(ownPlayer)) {
                        nearbyPlayerList.add(playerEntity);
                    }
                }
            }
        }
        return nearbyPlayerList;
    }
}
