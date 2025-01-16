package neonique.cbcplugin_new.tasks;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.scheduler.BukkitRunnable;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

public class CircularParticlesTask extends BukkitRunnable {

    List<Point2D> circlePositions = new ArrayList<>();
    List<Point2D> circlePositions2 = new ArrayList<>();
    Integer currentCirclePosition = 0;
    double radius;
    double offset;
    Location location;

    public CircularParticlesTask(Location location) {

        this.location = location;

        radius = 2.5;

        // Calculating coordinate positions
        for (double j = 0; j < Math.PI/2; j += Math.PI/16) {
            double x = Math.cos(j) * radius;
            double y = Math.sin(j) * radius;
            circlePositions.add(new Point2D.Double(x, y));

            double x1 = Math.cos(j + Math.PI/2) * radius;
            double y1 = Math.sin(j + Math.PI/2) * radius;
            circlePositions2.add(new Point2D.Double(x1, y1));
        }
    }

    @Override
    public void run() {

        // Get current circle position
        double circleParticleX = circlePositions.get(currentCirclePosition).getX() + location.getX();
        double circleParticleZ = circlePositions.get(currentCirclePosition).getY() + location.getZ();

        double circleParticleX1 = -circlePositions.get(currentCirclePosition).getX() + location.getX();
        double circleParticleZ1 = -circlePositions.get(currentCirclePosition).getY() + location.getZ();

        double circleParticleX2 = circlePositions2.get(currentCirclePosition).getX() + location.getX();
        double circleParticleZ2 = circlePositions2.get(currentCirclePosition).getY() + location.getZ();

        double circleParticleX3 = -circlePositions2.get(currentCirclePosition).getX() + location.getX();
        double circleParticleZ3 = -circlePositions2.get(currentCirclePosition).getY() + location.getZ();

        location.getWorld().spawnParticle(Particle.FIREWORK, circleParticleX, location.getY(), circleParticleZ,
                0, 0F, 0.5F, 0F, 1);
        location.getWorld().spawnParticle(Particle.FIREWORK, circleParticleX1, location.getY(), circleParticleZ1,
                0, 0F, 0.5F, 0F, 1);
        location.getWorld().spawnParticle(Particle.FIREWORK, circleParticleX2, location.getY(), circleParticleZ2,
                0, 0F, 0.5F, 0F, 1);
        location.getWorld().spawnParticle(Particle.FIREWORK, circleParticleX3, location.getY(), circleParticleZ3,
                0, 0F, 0.5F, 0F, 1);

        Particle.DustOptions dustOptions = new Particle.DustOptions(Color.BLUE, 1);

        location.getWorld().spawnParticle(Particle.DUST, circleParticleX, location.getY() + 1, circleParticleZ,
                1, 0F, 0F, 0F, 1, dustOptions);
        location.getWorld().spawnParticle(Particle.DUST, circleParticleX1, location.getY() + 1, circleParticleZ1,
                1, 0F, 0F, 0F, 1, dustOptions);
        location.getWorld().spawnParticle(Particle.DUST, circleParticleX2, location.getY() + 1, circleParticleZ2,
                1, 0F, 0F, 0F, 1, dustOptions);
        location.getWorld().spawnParticle(Particle.DUST, circleParticleX3, location.getY() + 1, circleParticleZ3,
                1, 0F, 0F, 0F, 1, dustOptions);

        currentCirclePosition++;
        if (currentCirclePosition == circlePositions.size()) {
            currentCirclePosition = 0;
        }
    }
}
