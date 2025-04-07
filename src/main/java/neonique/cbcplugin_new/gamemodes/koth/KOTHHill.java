package neonique.cbcplugin_new.gamemodes.koth;

import neonique.cbcplugin_new.managers.GameManager;
import neonique.cbcplugin_new.playerclasses.CBCPlayer;

import neonique.cbcplugin_new.util.CosSineTable;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;

public class KOTHHill {

    private final GameManager gameManager;

    private final Location zoneCenter; // Center of hill
    private final HillShape zoneShape; // Geometric shape of hill bounding box
    private final float zoneRadius;
    private final float zoneHeight; // Height that hill should extend above the center y

    // Calculations for particles
    private final double perimeter;
    private final double area;

    // Get CosSineTable to calculate angles
    private static final CosSineTable cosSineTable;

    static {
        cosSineTable = new CosSineTable(360);
    }

    public KOTHHill (GameManager gameManager, Location zoneCenter, HillShape zoneShape,
                     float zoneRadius, float zoneHeight) {

        this.gameManager = gameManager;

        this.zoneShape = zoneShape;
        this.zoneCenter = zoneCenter;
        this.zoneRadius = zoneRadius;
        this.zoneHeight = zoneHeight;

        // Calculate perimeter and area for particles
        if (zoneShape == HillShape.CIRCLE) {
            perimeter = zoneRadius * 2 * Math.PI;
            area = zoneRadius * zoneRadius * Math.PI;
        }
        else {
            perimeter = zoneRadius * 8;
            area = zoneRadius * zoneRadius * 4;
        }

    }

    public boolean inZone (Location playerLoc) {

        // Get player's Y level and check if within zone vertical boundaries
        double playerY = playerLoc.getY() + 0.1;

        // Check if player's Y is within the upwards extension max (from checkpoint to X blocks above the checkpoint)
        double yDistBetweenPlayerAndCheckpoint = playerY - zoneCenter.getY();
        if (yDistBetweenPlayerAndCheckpoint < -1 || yDistBetweenPlayerAndCheckpoint > zoneHeight) {
            return false;
        }

        // Check if player within horizontal boundaries
        if (zoneShape == HillShape.CIRCLE) {
            // Check if within boundaries of circle
            Location playerLocAtCheckpointY = playerLoc.clone();
            playerLocAtCheckpointY.setY(zoneCenter.getY());
            return playerLocAtCheckpointY.distance(zoneCenter) <= zoneRadius;
        }
        else if (zoneShape == HillShape.SQUARE) {
            // Check if within boundaries of square
            double xDistanceFromCentre = Math.abs(zoneCenter.getX() - playerLoc.getX());
            double zDistanceFromCentre = Math.abs(zoneCenter.getZ() - playerLoc.getZ());
            return (xDistanceFromCentre < zoneRadius && zDistanceFromCentre < zoneRadius);
        }

        return false;

    }

    public Set<CBCPlayer> getPlayersInHill() {

        // Get all players within the detection zone of the hill
        Set<CBCPlayer> playersInHill = new HashSet<>();

        for (CBCPlayer player : gameManager.getAlivePlayers()) {
            Location playerLocation = player.getPlayer().getLocation();
            if (inZone(playerLocation)) {
                playersInHill.add(player);
            }
        }

        return playersInHill;

    }

    public boolean isPlayerInHill (CBCPlayer player) {

        // If player is not alive then they are not in the hill
        if (!player.isAlive()) return false;

        Location playerLocation = player.getPlayer().getLocation();
        return (inZone(playerLocation));

    }

    public void playParticles (NamedTextColor teamColor) {

        World world = getWorld();

        // Play the particles around the edge
        double particlesPerMeter = 0.5;

        // Calculate amount of particles to play around edge
        int particleAmount = (int) Math.round(perimeter * particlesPerMeter);

        Color color = Color.fromRGB(teamColor.red(), teamColor.green(), teamColor.blue());
        Particle.DustOptions dustOptions = new Particle.DustOptions(color, 2);

        for (int i = 0; i < particleAmount; i++) {

            // Calculate offset of X and Z
            double particleOffsetX;
            double particleOffsetZ;

            // Random object to randomise angles
            Random rand = new Random();

            if (zoneShape == HillShape.CIRCLE) {
                // Choose a random angle of circle
                int randAngle = rand.nextInt(360);
                // Calculate particle x and particle z
                particleOffsetX = cosSineTable.getSin(randAngle) * zoneRadius;
                particleOffsetZ = cosSineTable.getCos(randAngle) * zoneRadius;
            }
            else {

                // Get the offset on the side that was chosen
                double randSideParticle = (rand.nextDouble() * zoneRadius * 2) - zoneRadius;
                int randSide = rand.nextInt(4);
                if (randSide == 0) {
                    particleOffsetX = zoneRadius;
                    particleOffsetZ = randSideParticle;
                }
                else if (randSide == 1) {
                    particleOffsetX = -zoneRadius;
                    particleOffsetZ = randSideParticle;
                }
                else if (randSide == 2) {
                    particleOffsetX = randSideParticle;
                    particleOffsetZ = zoneRadius;
                }
                else {
                    particleOffsetX = randSideParticle;
                    particleOffsetZ = -zoneRadius;
                }

            }

            // Calculate particle position
            double particleX = particleOffsetX + zoneCenter.getX();
            double particleY = zoneCenter.getY() + 0.25;
            double particleZ = particleOffsetZ + zoneCenter.getZ();

            // Spawn particle with upwards velocity
            world.spawnParticle(Particle.DUST, particleX, particleY, particleZ,
                    0, 0F, 0.5F, 0F, 1, dustOptions, true);

        }
    }

    public void particlesOnCapture (NamedTextColor teamColor) {

        World world = getWorld();

        // Play the particles around the edge
        double particlesPerMeter = 3;

        // Calculate amount of particles to play around edge
        int particleAmount = (int) Math.round(perimeter * particlesPerMeter);

        Color color = Color.fromRGB(teamColor.red(), teamColor.green(), teamColor.blue());
        Particle.DustOptions dustOptions = new Particle.DustOptions(color, 2);

        for (int i = 0; i < particleAmount; i++) {

            // Calculate offset of X and Z
            double particleOffsetX;
            double particleOffsetZ;

            // Random object to randomise angles
            Random rand = new Random();

            if (zoneShape == HillShape.CIRCLE) {
                // Choose a random angle of circle
                int randAngle = rand.nextInt(360);
                // Calculate particle x and particle z
                particleOffsetX = cosSineTable.getSin(randAngle) * zoneRadius;
                particleOffsetZ = cosSineTable.getCos(randAngle) * zoneRadius;
            }
            else {

                // Get the offset on the side that was chosen
                double randSideParticle = (rand.nextDouble() * zoneRadius * 2) - zoneRadius;
                int randSide = rand.nextInt(4);
                if (randSide == 0) {
                    particleOffsetX = zoneRadius;
                    particleOffsetZ = randSideParticle;
                }
                else if (randSide == 1) {
                    particleOffsetX = -zoneRadius;
                    particleOffsetZ = randSideParticle;
                }
                else if (randSide == 2) {
                    particleOffsetX = randSideParticle;
                    particleOffsetZ = zoneRadius;
                }
                else {
                    particleOffsetX = randSideParticle;
                    particleOffsetZ = -zoneRadius;
                }

            }

            // Calculate particle position
            double particleX = particleOffsetX + zoneCenter.getX();
            double particleY = zoneCenter.getY() + 0.25;
            double particleZ = particleOffsetZ + zoneCenter.getZ();

            // Spawn particle with upwards velocity
            world.spawnParticle(Particle.DUST, particleX, particleY, particleZ,
                    0, 0F, 10F, 0F, 1, dustOptions, true);

        }
    }

    public World getWorld() {
        return this.zoneCenter.getWorld();
    }

    public Location getCenter() {
        return this.zoneCenter;
    }
}
