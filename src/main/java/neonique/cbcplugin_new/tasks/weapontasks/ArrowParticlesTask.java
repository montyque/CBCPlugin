package neonique.cbcplugin_new.tasks.weapontasks;

import neonique.cbcplugin_new.CBCPlugin;
import neonique.cbcplugin_new.managers.GameManager;
import neonique.cbcplugin_new.managers.CombatManager;
import neonique.cbcplugin_new.playerclasses.CBCPlayer;
import neonique.cbcplugin_new.util.CosSineTable;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.awt.geom.Point2D;
import java.util.*;

public class ArrowParticlesTask extends BukkitRunnable {

    private final GameManager gameManager;
    private final CombatManager combatManager;

    private final List<Point2D> circlePositions = new ArrayList<>();
    private Integer currentCirclePosition = 0;
    private double flameRadius;
    private double offset;

    private static final CosSineTable cosSineTable;

    static {
        cosSineTable = CosSineTable.getTable();
    }

    public ArrowParticlesTask(GameManager gameManager, CombatManager combatManager) {
        this.gameManager = gameManager;
        this.combatManager = combatManager;

        // Calculating coordinate positions
        flameRadius = combatManager.getFlameRadius();
        offset = flameRadius / 2;
        for (double j = 0; j < Math.PI*2; j += Math.PI/8) {

            double x = Math.cos(j) * flameRadius;
            double y = Math.sin(j) * flameRadius;

            circlePositions.add(new Point2D.Double(x, y));
        }
    }

    public void updateFlameRadius () {
        // Calculating coordinate positions
        flameRadius = combatManager.getFlameRadius();
        offset = flameRadius / 2;
        for (double j = 0; j < Math.PI*2; j += Math.PI/8) {
            double x = Math.cos(j) * flameRadius;
            double y = Math.sin(j) * flameRadius;
            circlePositions.add(new Point2D.Double(x, y));
        }
    }

    @Override
    public void run() {

        // How far away players need to be in order to render the particles
        final long PARTICLE_RENDER_RADIUS = 48;

        // Calculate positions of flame zone arrows
        currentCirclePosition++;
        if (currentCirclePosition == circlePositions.size()) {
            currentCirclePosition = 0;
        }

        Set<Vector> offsets = new HashSet<>();
        Random rand = new Random();

        long flameRadiusRound = Math.round(flameRadius);
        long flameRadiusRoundHalf = Math.round(flameRadius / 2);

        if (flameRadiusRoundHalf == 0) {
            flameRadiusRoundHalf = 1;
        }

        for (int i = 0; i <= flameRadiusRoundHalf; i++) {
            int randAngle1 = rand.nextInt(360);
            double radius = cosSineTable.getSine(randAngle1) * flameRadius;
            double y = cosSineTable.getCos(randAngle1) * flameRadius;
            for (int a = 0; a <= flameRadiusRound; a++) {
                int randAngle2 = rand.nextInt(360);
                double z = cosSineTable.getSine(randAngle2) * radius;
                double x = cosSineTable.getCos(randAngle2) * radius;
                offsets.add(new Vector(x, y, z));
            }
        }

        // Play particles on the arrows
        for (UUID arrowUUID : this.combatManager.flameZoneArrowSet) {

            Entity entity = CBCPlugin.getPlugin().getServer().getEntity(arrowUUID);
            if (entity == null) continue;
            if (!(entity instanceof Arrow)) continue;
            Arrow arrow = (Arrow) entity;

            if (arrow.isDead()) {
                continue;
            }

            // Find every player within 32 blocks and check if we should show flame or soul flame particles to them
            Location arrowLocation = arrow.getLocation();

            World world = arrowLocation.getWorld();

            List<Player> playerFlames = new ArrayList<>();
            List<Player> playerSoulFlames = new ArrayList<>();

            for (Player p : arrowLocation.getNearbyPlayers(PARTICLE_RENDER_RADIUS)) {
                if (gameManager.hasPlayer(p)) {
                    CBCPlayer cbcplayer = gameManager.getPlayer(p);
                    if (cbcplayer.isEntityAlly(arrow)) {
                        playerSoulFlames.add(p);
                    } else {
                        playerFlames.add(p);
                    }
                } else {
                    playerFlames.add(p);
                }
            }

            double arrowX = arrowLocation.getX();
            double arrowY = arrowLocation.getY();
            double arrowZ = arrowLocation.getZ();

            // Get current circle position
            double circleParticleX = circlePositions.get(currentCirclePosition).getX() + arrowX;
            double circleParticleZ = circlePositions.get(currentCirclePosition).getY() + arrowZ;

            for (Player p : playerFlames) {
                p.spawnParticle(Particle.FLAME, circleParticleX, arrowY, circleParticleZ, 1, 0F, 0F, 0F, 0.001);
                p.spawnParticle(Particle.FLAME, arrowX, arrowY, arrowZ, 4, 0.5F, 0.5F, 0.5F, 0.001);
            }
            for (Player p : playerSoulFlames) {
                p.spawnParticle(Particle.SOUL_FIRE_FLAME, circleParticleX, arrowY, circleParticleZ, 1, 0F, 0F, 0F, 0.001);
                p.spawnParticle(Particle.SOUL_FIRE_FLAME, arrowX, arrowY, arrowZ, 4, 0.5F, 0.5F, 0.5F, 0.001);
            } 

            for (Vector vector : offsets) {
                for (Player p : playerFlames) {
                    p.spawnParticle(Particle.FLAME, arrowX + vector.getX(), arrowY + vector.getY(), arrowZ + vector.getZ(), 1,
                            0F, 0F, 0F, 0.001);
                }

                for (Player p : playerSoulFlames) {
                    p.spawnParticle(Particle.SOUL_FIRE_FLAME, arrowX + vector.getX(), arrowY + vector.getY(), arrowZ + vector.getZ(), 1,
                            0F, 0F, 0F, 0.001);
                }
            }
        }
    }
}
