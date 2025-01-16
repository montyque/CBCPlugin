package neonique.cbcplugin_new.gamemodes.tdm;

import neonique.cbcplugin_new.managers.GameManager;
import neonique.cbcplugin_new.playerclasses.CBCPlayer;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.Objects;

public class TDMSpawn extends Location {

    private final GameManager gameManager;

    private final int allyRadius;
    private final int enemyRadius;
    private final double enemyTargetDistance;
    private double nearestEnemyDistanceMinusTarget;
    private final Location mapCenter;
    private final boolean ignoreY;

    public TDMSpawn(World world, Vector vector, GameManager gameManager, int allyRadius, int enemyRadius,
                    int enemyTargetDistance, Location mapCenter, boolean ignoreY) {
        super(world, vector.getX(), vector.getY(), vector.getZ());
        this.gameManager = gameManager;
        this.allyRadius = allyRadius;
        this.enemyRadius = enemyRadius;
        this.enemyTargetDistance = Math.pow(enemyTargetDistance, 2);
        this.mapCenter = mapCenter;
        this.ignoreY = ignoreY;
    }

    public void setNearestEnemyDistanceMinusTarget (CBCPlayer ownPlayer) {

        double nearestEnemyDistance = 1000000;
        // Go through all enemy alive players and check the distance between them and this spawn point
        for (CBCPlayer player : gameManager.getAlivePlayers()) {

            if (!player.isOnline()) continue;
            if (ownPlayer.isAlly(player)) continue;
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
            if (!this.gameManager.hasPlayer(player)) continue;
            CBCPlayer cbcplayer = this.gameManager.getPlayer(player);
            if (!cbcplayer.isAlive()) continue;
            if (ownPlayer.isAlly(cbcplayer)) continue;
            return true;
        }
        return false;
    }

    public boolean isOutOfCombatAllyNearSpawn(CBCPlayer ownPlayer) {
        for (Player player : getNearbyPlayers(allyRadius)) {
            if (!this.gameManager.hasPlayer(player)) continue;
            CBCPlayer cbcplayer = this.gameManager.getPlayer(player);
            if (!cbcplayer.isAlive()) continue;
            if (!cbcplayer.isAlly(ownPlayer)) continue;

            boolean allyOutOfCombat = true;
            for (Player playerNearbyAlly : Objects.requireNonNull(player.getPlayer()).getLocation().getNearbyPlayers(enemyRadius)) {

                if (!this.gameManager.hasPlayer(playerNearbyAlly)) continue;
                CBCPlayer cbcplayerNearbyAlly = this.gameManager.getPlayer(playerNearbyAlly);
                if (!cbcplayerNearbyAlly.isAlive()) continue;
                if (cbcplayerNearbyAlly.isAlly(ownPlayer)) continue;

                allyOutOfCombat = false;
                break;
            }

            if (allyOutOfCombat) {
                return true;
            }
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
}