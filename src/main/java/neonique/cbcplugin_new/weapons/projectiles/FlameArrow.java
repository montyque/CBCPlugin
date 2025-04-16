package neonique.cbcplugin_new.weapons.projectiles;

import neonique.cbcplugin_new.managers.GameManager;
import neonique.cbcplugin_new.playerclasses.CBCPlayer;
import neonique.cbcplugin_new.util.CosSineTable;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.*;

public class FlameArrow extends PlayerProjectile {

    private final double flameRadius;
    private int ticksAlive = 0;

    private boolean despawning = false;
    private int ticksLeft;

    private static final CosSineTable cosSineTable16;

    static {
        cosSineTable16 = new CosSineTable(16);
    }

    public FlameArrow(CBCPlayer playerFired, Arrow arrow, double flameRadius, int life) {
        super(playerFired, arrow);
        this.flameRadius = flameRadius;
        this.ticksLeft = life;
    }

    @Override
    public void update() {

        Arrow arrow = getArrow();
        if (arrow == null) return;

        GameManager gameManager = getSource().getGameManager();

        Location arrowLocation = arrow.getLocation();
        Collection<Player> playersNearby = arrowLocation.getNearbyPlayers(flameRadius);

        for (Player playerNearby : playersNearby) {
            if (arrowLocation.distanceSquared(playerNearby.getLocation()) < (flameRadius * flameRadius)) {

                CBCPlayer playerInZone = gameManager.getPlayer(playerNearby);
                if (playerInZone == null) continue;
                if (!playerInZone.isAlive()) continue;
                if (playerInZone.isImmune()) continue;
                if (playerInZone.isAlly(getSource())) continue;

                playerInZone.getFlameDamager().checkNewFlameArrow(this);

            }
        }

        ticksAlive++;
        playParticles();

        if (despawning) {
            ticksLeft--;
            if (ticksLeft <= 0) {
                markForRemoval();
            }
        } else {
            if (arrow.isInBlock() || arrow.isInWaterOrBubbleColumn() || arrow.isInLava()) {
                despawning = true;
            }
        }
    }

    public Arrow getArrow() {

        Entity projectileEntity = getProjectileEntity();
        if (projectileEntity == null) {
            markForRemoval();
            return null;
        }

        Arrow arrow = (Arrow) getProjectileEntity();

        if (arrow.isDead()) {
            markForRemoval();
            return null;
        }

        return arrow;

    }

    public void playParticles () {

        // Find every player within 64 blocks and check if we should show flame or soul flame particles to them
        assert getProjectileEntity() != null;
        Location arrowLocation = getProjectileEntity().getLocation();
        Set<Player> playerFlames = new HashSet<>();
        Set<Player> playerSoulFlames = new HashSet<>();

        for (Player p : arrowLocation.getNearbyPlayers(64)) {
            if (getSource().isPlayerEntityAlly(p)) {
                playerSoulFlames.add(p);
            } else {
                playerFlames.add(p);
            }
        }

        double circleParticleX = cosSineTable16.getCos(ticksAlive % 16);
        double circleParticleZ = cosSineTable16.getSin(ticksAlive % 16);
        Vector circleParticleVector = new Vector(circleParticleX, 0, circleParticleZ);
        spawnFireParticle(arrowLocation, playerFlames, playerSoulFlames, circleParticleVector);

        int randomParticleAmount = Math.max(1, (int) Math.round(flameRadius * (2.0 / 3.0)));
        for (int i = 0; i < randomParticleAmount; i++) {
            spawnFireParticle(arrowLocation, playerFlames, playerSoulFlames, getRandomParticleVector());
        }

        for (Player p : playerFlames) {
            p.spawnParticle(Particle.FLAME, arrowLocation, 1, 0.5F, 0.5F, 0.5F, 0.001, null, true);
        }

        for (Player p : playerSoulFlames) {
            p.spawnParticle(Particle.SOUL_FIRE_FLAME, arrowLocation, 1, 0.5F, 0.5F, 0.5F, 0.001, null, true);
        }

    }

    public Vector getRandomParticleVector () {

        double x = new Random().nextGaussian();
        double y = new Random().nextGaussian();
        double z = new Random().nextGaussian();

        // Handling edge case where x, y, z = 0
        if (x == 0 && y == 0 && z == 0) {
            x = 1;
        }

        double normalizationFactor = 1 / Math.sqrt(x*x + y*y + z*z);
        return new Vector(x, y, z).multiply(normalizationFactor);

    }

    public void spawnFireParticle (Location arrowLocation, Set<Player> nonAllies, Set<Player> allies, Vector vector) {

        double particleX = arrowLocation.getX() + vector.getX() * flameRadius;
        double particleY = arrowLocation.getY() + vector.getY() * flameRadius;
        double particleZ = arrowLocation.getZ() + vector.getZ() * flameRadius;

        for (Player player : nonAllies) {
            player.spawnParticle(Particle.FLAME, particleX, particleY, particleZ, 1,
                    0F, 0F, 0F, 0.001, null, true);
        }

        for (Player player : allies) {
            player.spawnParticle(Particle.SOUL_FIRE_FLAME, particleX, particleY, particleZ, 1,
                    0F, 0F, 0F, 0.001, null, true);
        }

    }

}
