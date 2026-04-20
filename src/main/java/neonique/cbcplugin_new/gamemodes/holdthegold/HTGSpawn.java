package neonique.cbcplugin_new.gamemodes.holdthegold;

import neonique.cbcplugin_new.gamemodes._base.Game;
import neonique.cbcplugin_new.managers.GameManager;
import neonique.cbcplugin_new.managers.PlayerSession;
import neonique.cbcplugin_new.playerclasses.CBCPlayer;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.Objects;

public class HTGSpawn extends Location {

    private final PlayerSession<HTGPlayer> playerSession;

    private Location goldLocation;
    private final int allyRadius;
    private final int enemyRadius;
    private final double goldRadius;
    private final boolean ignoreY;
    private double goldDistanceMinusGoldRadius;

    public HTGSpawn(World world, Vector vector, PlayerSession<HTGPlayer> playerSession, int allyRadius, int enemyRadius, int goldRadius, boolean ignoreY) {
        super(world, vector.getX(), vector.getY(), vector.getZ());
        this.playerSession = playerSession;
        this.allyRadius = allyRadius;
        this.enemyRadius = enemyRadius;
        this.goldRadius = Math.pow(goldRadius, 2);
        this.ignoreY = ignoreY;
    }

    public void goldLocation (Location loc) {
        this.goldLocation = loc;
    }

    public void setGoldDistanceMinusGoldRadius () {
        goldDistanceMinusGoldRadius = Math.abs(distanceToSpawn(this.goldLocation) - goldRadius);
    }

    public double getGoldDistanceMinusGoldRadius () {
        return goldDistanceMinusGoldRadius;
    }

    public boolean isEnemyNearbySpawn (HTGPlayer ownPlayer)  {
        return isEnemyNearby(this, ownPlayer);
    }

    public boolean isEnemyNearby (Location loc, HTGPlayer ownPlayer)  {
        return playerSession.getPlayers().stream()
                .filter(CBCPlayer::isOnline)
                .filter(CBCPlayer::isAlive)
                .filter(p -> !p.isAlly(ownPlayer))
                .anyMatch(p -> distance(loc, p.getPlayer().getLocation()) <= enemyRadius);
    }

    public boolean isOutOfCombatAllyNearSpawn (HTGPlayer ownPlayer) {
        return playerSession.getPlayers().stream()
                .filter(CBCPlayer::isOnline)
                .filter(CBCPlayer::isAlive)
                .filter(p -> p.isAlly(ownPlayer) && p != ownPlayer)
                .filter(p -> distance(p.getPlayer().getLocation()) <= allyRadius)
                .anyMatch(p -> !isEnemyNearby(p.getPlayer().getLocation(), p));

    }

    private double distance (Location l1, Location l2) {
        if (ignoreY) {
            double xDistanceFromCentre = Math.abs(l1.getX() - l2.getX());
            double zDistanceFromCentre = Math.abs(l1.getZ() - l2.getZ());
            return Math.sqrt(Math.pow(xDistanceFromCentre, 2) + Math.pow(zDistanceFromCentre, 2));
        } else {
            return l1.distance(l2);
        }
    }

    private double distanceToSpawn (Location location) {
        return distance(this, location);
    }

}
