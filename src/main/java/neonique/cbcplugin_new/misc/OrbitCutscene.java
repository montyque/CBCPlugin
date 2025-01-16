package neonique.cbcplugin_new.misc;

import neonique.cbcplugin_new.enums.CutsceneType;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.Collection;

public class OrbitCutscene extends Cutscene {

    private Location focalPosition;
    private Location startingPosition;

    private double cameraOrbitRadius;
    private double focusOrbitRadius;
    private double ticksPerRevolution;

    public OrbitCutscene(World world, Location startingPosition, int length,
                         Collection<Player> players, Location focalPosition, double cameraOrbitRadius,
                         double focusOrbitRadius, double ticksPerRevolution) {
        super(world, startingPosition, length, players);

        this.focalPosition = focalPosition;
        this.cameraOrbitRadius = cameraOrbitRadius;
        this.focusOrbitRadius = focusOrbitRadius;
        this.ticksPerRevolution = ticksPerRevolution;
        this.startingPosition = startingPosition;
    }

    @Override
    public void tick(boolean init) {

        // Retrieve camera entity
        Entity entity = getSpectateEntity();

        // If entity no longer exists for some reason
        if (entity == null) {
            endCutscene();
            return;
        }

        double tickProg = ((double) getTickNum() % ticksPerRevolution) / ticksPerRevolution;
        double angleRad = tickProg * Math.PI * 2;

        // Set location of camera
        setCameraLocation(angleRad);

        if (!init) {
            incrementTick();
        }
    }

    public void setCameraLocation (double angleRad) {

        double _2PI = Math.PI * 2;

        double x = Math.cos(angleRad) * cameraOrbitRadius;
        double z = Math.sin(angleRad) * cameraOrbitRadius;

        // Coordinates of center
        double centerX = focalPosition.getX();
        double centerZ = focalPosition.getZ();

        // New coordinates
        double newPositionX = x + centerX;
        double newPositionZ = z + centerZ;

        // New location
        Location newLocation = new Location(getWorld(), newPositionX, startingPosition.getY(), newPositionZ);
        //double yaw = Math.atan2(newPositionZ - centerZ, newPositionX - centerX) + 45;
        //double pitch =  Math.asin((newLocation.getY()-focalPosition.getY()) / newLocation.distance(focalPosition));

        //yaw = (float) Math.toDegrees((yaw + _2PI) % _2PI);
        //pitch = (float) Math.toDegrees((pitch + _2PI) % _2PI);

        //newLocation.setYaw((float) yaw);
        //newLocation.setPitch((float) pitch);

        Vector playerLookDirection = focalPosition.clone().subtract(newLocation).toVector();//make a vector going from the player's location to the center point
        newLocation.setDirection(playerLookDirection.normalize());

        // Retrieve camera entity
        Entity entity = getSpectateEntity();
        entity.teleport(newLocation);
    }
}
