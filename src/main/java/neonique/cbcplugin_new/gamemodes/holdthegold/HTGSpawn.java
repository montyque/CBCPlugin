package neonique.cbcplugin_new.gamemodes.holdthegold;

import neonique.cbcplugin_new.managers.GameManager;
import neonique.cbcplugin_new.playerclasses.CBCPlayer;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.Objects;

public class HTGSpawn extends Location {

    private final GameManager gameManager;

    private Location goldLocation;
    private final int allyRadius;
    private final int enemyRadius;
    private final double goldRadius;
    private final boolean ignoreY;
    private double goldDistanceMinusGoldRadius;

    public HTGSpawn(World world, Vector vector, GameManager gameManager, int allyRadius, int enemyRadius, int goldRadius, boolean ignoreY) {
        super(world, vector.getX(), vector.getY(), vector.getZ());
        this.gameManager = gameManager;
        this.allyRadius = allyRadius;
        this.enemyRadius = enemyRadius;
        this.goldRadius = Math.pow(goldRadius, 2);
        this.ignoreY = ignoreY;
    }

    public void goldLocation (Location loc) {
        this.goldLocation = loc;
    }

    public void setGoldDistanceMinusGoldRadius () {
        goldDistanceMinusGoldRadius = Math.abs(calculateDistance(this.goldLocation) - goldRadius);
    }

    public double getGoldDistanceMinusGoldRadius () {
        return goldDistanceMinusGoldRadius;
    }

    public boolean isEnemyNearbySpawn(HTGPlayer ownPlayer) {
        for (Player player : getNearbyPlayers(enemyRadius)) {
            if (!this.gameManager.hasPlayer(player)) continue;
            CBCPlayer cbcplayer = this.gameManager.getPlayer(player);
            if (!cbcplayer.isAlive()) continue;
            if (cbcplayer.isAlly(ownPlayer)) continue;
            return true;
        }
        return false;
    }

    public boolean isOutOfCombatAllyNearSpawn(HTGPlayer ownPlayer) {
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
