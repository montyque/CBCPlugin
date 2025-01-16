package neonique.cbcplugin_new.gamemodes.assassin;

import neonique.cbcplugin_new.managers.GameManager;
import neonique.cbcplugin_new.playerclasses.CBCPlayer;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public class AssassinSpawn extends Location {

    private final GameManager gameManager;

    private final boolean ignoreY;
    private final int enemyRadius;

    public AssassinSpawn(World world, Vector vector, GameManager gameManager, int enemyRadius, boolean ignoreY) {
        super(world, vector.getX(), vector.getY(), vector.getZ());
        this.gameManager = gameManager;
        this.enemyRadius = enemyRadius;
        this.ignoreY = ignoreY;
    }

    public boolean isEnemyNearbySpawn(CBCPlayer ownPlayer)  {

        for (Player player : getNearbyPlayers(enemyRadius)) {
            if (!this.gameManager.hasPlayer(player)) continue;
            CBCPlayer cbcplayer = this.gameManager.getPlayer(player);
            if (!cbcplayer.isAlive()) continue;
            if (ownPlayer.isAlly(cbcplayer)) continue;
            return true;
        }
        return false;
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
