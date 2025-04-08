package neonique.cbcplugin_new.weapons.projectiles;

import neonique.cbcplugin_new.playerclasses.CBCPlayer;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.*;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.Collection;

public class CBCCreeper extends PlayerProjectile {

    public CBCCreeper(CBCPlayer playerFired, Creeper creeper) {
        super(playerFired, creeper);
    }

    /**
     * Returns the Creeper entity associated with this projectile.
     * If the Creeper does not exist,
     * @return the Creeper entity or null if entity does not exist.
     */
    public Creeper getCreeper() {

        Entity projectileEntity = getProjectileEntity();
        if (projectileEntity == null) {
            markForRemoval();
            return null;
        }

        Creeper creeper = (Creeper) getProjectileEntity();

        if (creeper.isDead()) {
            markForRemoval();
            return null;
        }

        return creeper;

    }

    public void update() {
        checkExplosion();
    }

    /**
     * Prompts the creeper to explode and deal damage to surrounding players.
     * @param directHit If the explosion was the result of the projectile entering an enemy player's hitbox.
     */
    public void explode(boolean directHit) {

        Creeper creeper = getCreeper();
        if (creeper == null) return;

        creeper.getWorld().spawnParticle(Particle.EXPLOSION_EMITTER, creeper.getLocation(), 1);
        creeper.getWorld().spawnParticle(Particle.DUST, creeper.getLocation(), 30, 1.5, 1.5,
                1.5, 1, getParticleOptions(), false);

        // Creeper is going to explode
        creeper.explode();

        // If it was a direct hit, play particles
        if (directHit) {
            creeper.getWorld().spawnParticle(Particle.ENCHANTED_HIT, creeper.getLocation(), 150, 0, 1, 0, 1);
        }
    }

    /**
     * Checks if the creeper fits the intended requirements to explode.
     * A creeper projectile will explode if one of the following conditions are met:
     * <ul>
     *     <li>The creeper is on the ground</li>
     *     <li>The creeper has a solid block within 0.5 blocks of it after 2 ticks of being fired (i.e. it has hit a wall)</li>
     *     <li>The creeper is within 1.0 blocks of an enemy player</li>
     *     <li>The creeper is in contact with water or lava</li>
     * </ul>
     * This method is intended to run every tick.
     */
    public void checkExplosion () {

        Creeper creeper = getCreeper();
        if (creeper == null) return;

        creeper.getWorld().spawnParticle(Particle.DUST, creeper.getLocation(), 1, 0.5, 0.5,
                0.5, 1, getParticleOptions(), false);

        if (creeper.isOnGround()) {
            explode(false);
            return;
        }

        if (isHittingWall()) {
            explode(false);
            return;
        }

        if (isHittingEnemy()) {
            explode(true);
            return;
        }

        if (creeper.getLocation().getBlock().isLiquid()) {
            explode(false);
        }

    }

    private boolean isHittingWall () {

        Creeper creeper = getCreeper();
        Location creeperLocation = creeper.getLocation();

        double creeperX = creeperLocation.getX();
        double creeperY = creeperLocation.getY();
        double creeperZ = creeperLocation.getZ();
        World w = creeperLocation.getWorld();

        for (int xBlock = 0; xBlock < 2; xBlock++) {
            for (int zBlock = 0; zBlock < 2; zBlock++) {
                Material blockMaterial = new Location(w,
                        creeperX - 0.5 + xBlock,
                        creeperY,
                        creeperZ - 0.5 + zBlock
                ).getBlock().getType();
                if (blockMaterial.isSolid()) {
                    return true;
                }
            }
        }

        return false;

    }

    private boolean isHittingEnemy () {

        Creeper creeper = getCreeper();
        Location creeperLocation = creeper.getLocation();

        // Get a list of players nearby
        Collection<Player> playersNearby = creeperLocation.getNearbyEntitiesByType(Player.class, 0.8);

        if (playersNearby.isEmpty()) {
            return false;
        }

        for (Player player : playersNearby) {
            if (getSource().isPlayerEntityAliveEnemy(player)) {
                return true;
            }
        }

        return false;

    }

    private Particle.DustOptions getParticleOptions () {

        Color particleColor = Color.fromRGB(255, 255, 255);
        if (getSource().getTeam() != null) {
            NamedTextColor teamColor = getSource().getTeam().getColor();
            particleColor = Color.fromRGB(teamColor.red(), teamColor.green(), teamColor.blue());
        }

        // Display particles
        return new Particle.DustOptions(particleColor, 1.0F);
    }

}
