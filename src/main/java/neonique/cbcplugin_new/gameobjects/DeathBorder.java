package neonique.cbcplugin_new.gameobjects;

import neonique.cbcplugin_new.CBCPlugin;
import neonique.cbcplugin_new.enums.DeathBorderShape;
import neonique.cbcplugin_new.enums.DeathCause;
import neonique.cbcplugin_new.gamemodes._base.Game;
import neonique.cbcplugin_new.managers.CombatManager;
import neonique.cbcplugin_new.playerclasses.CBCPlayer;
import neonique.cbcplugin_new.tasks.gametasks.DeathBorderDamageTask;
import neonique.cbcplugin_new.tasks.gametasks.DeathBorderShrinkTask;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;

import java.time.Duration;
import java.util.HashSet;
import java.util.Set;

public class DeathBorder {

    private final Game game;

    // Defualt variables
    private final Location center;
    private final DeathBorderShape shape;
    private final int startRadius;
    private final int finalRadius;
    private final int highestY; // Highest Y value of the border particle display
    private final int lowestY; // Lowest Y value of the border particle display
    private final int shrinkRate;
    private final int warnDistance;

    // Current border
    private boolean active = false;
    private double currentRadius;

    // Border tasks
    private DeathBorderShrinkTask borderShrinkTask;
    private DeathBorderDamageTask borderDamageTask;

    public DeathBorder (Game game, Location center, DeathBorderShape shape, int startRadius, int finalRadius, int highestY, int lowestY, int shrinkRate) {

        this.game = game;

        this.center = center;
        this.shape = shape;
        this.startRadius = startRadius;
        this.finalRadius = finalRadius;
        this.highestY = highestY;
        this.lowestY = lowestY;
        this.shrinkRate = shrinkRate;

        this.warnDistance = 5;

    }

    public void activateBorder () {

        active = true;

        // Reset radius to start radius
        this.currentRadius = startRadius;

        borderShrinkTask = new DeathBorderShrinkTask(this);
        borderShrinkTask.runTaskTimer(CBCPlugin.getPlugin(), 0, (long) shrinkRate * 3L);
        borderDamageTask = new DeathBorderDamageTask(this);
        borderDamageTask.runTaskTimer(CBCPlugin.getPlugin(), 0, 10);

    }

    public void deactivateBorder () {

        active = false;

        if (borderShrinkTask != null) {
            if (borderShrinkTask.isCancelled()) {
                borderShrinkTask.cancel();
            }
            borderShrinkTask = null;
        }

        if (borderDamageTask != null) {
            if (borderDamageTask.isCancelled()) {
                borderDamageTask.cancel();
            }
            borderDamageTask = null;
        }

    }

    public void shrinkBorder () {

        if (!active) return;

        // Make sure border does not shrink completely
        if (currentRadius - 1.5 < finalRadius) {
            currentRadius = finalRadius;
        }
        else {
            currentRadius -= 1.5;
        }

        playParticles();

    }

    public void checkIfPlayerOutsideBorder (CBCPlayer player) {

        Location playerLocation = player.getPlayer().getLocation();
        double xDistanceFromCentre = Math.abs(center.getX() - playerLocation.getX());
        double zDistanceFromCentre = Math.abs(center.getZ() - playerLocation.getZ());

        if (shape == DeathBorderShape.SQUARE) {
            // Damage player if either of these coordinates is outside the border
            if (xDistanceFromCentre > currentRadius ||
                    zDistanceFromCentre > currentRadius) {
                applyDamageToPlayer(player);
                return;
            }

            // Warn player if either of these coordinates is 3 blocks to the border
            if (xDistanceFromCentre > currentRadius - warnDistance ||
                    zDistanceFromCentre > currentRadius - warnDistance) {
                warnPlayer(player, false);
            }
        }
        else if (shape == DeathBorderShape.CIRCLE) {
            // Damage player if their distance from the centre is larger than the border
            double distance = Math.sqrt(Math.pow(xDistanceFromCentre, 2) + Math.pow(zDistanceFromCentre, 2));
            if (distance > currentRadius) {
                applyDamageToPlayer(player);
                return;
            }

            // Warn player if their distance from the centre is larger than the border
            if (distance > currentRadius - warnDistance) {
                warnPlayer(player, false);
            }
        }
    }

    private void applyDamageToPlayer (CBCPlayer player) {

        if (!player.isOnline()) return;
        if (player.isImmune()) return;

        CombatManager combatManager = game.getGameManager().combatManager;

        if (player.getPlayer().getHealth() <= 1) {
            // Kill player
            if (player.getLastPlayerHitBy() == null) {
                combatManager.playerDeath(player, null, DeathCause.DEATH_BORDER, false);
            } else {
                combatManager.playerDeath(player, player.getLastPlayerHitBy(), DeathCause.DEATH_BORDER, false);
            }
        } else {
            player.getPlayer().damage(1);
            warnPlayer(player, true);
        }
    }

    public void warnPlayer(CBCPlayer player, boolean isOutsideBorder) {

        if (!player.isOnline()) return;
        if (player.isImmune()) return;

        Title.Times times = Title.Times.times(
                Duration.ofMillis(0), Duration.ofMillis(1000), Duration.ofMillis(250)
        );

        Component title = Component.text("");
        Component subtitle = Component.text("You are ").color(NamedTextColor.YELLOW).append(
                Component.text("close to the border").color(NamedTextColor.GOLD)
        );

        if (isOutsideBorder) {
            // Player is taking damage from border
            title = Component.text("");
            subtitle = Component.text("You are ").color(NamedTextColor.YELLOW).append(
                    Component.text("taking damage outside the border").color(NamedTextColor.RED)
            );
        }

        // Display title
        player.getPlayer().showTitle(Title.title(title, subtitle, times));
    }

    public void playParticles () {

        World world = center.getWorld();
        double centerY = center.getY();
        Particle.DustOptions dustOptions = new Particle.DustOptions(org.bukkit.Color.fromRGB(255, 0, 0), 12);

        if (shape == DeathBorderShape.SQUARE) {

            int horizontalParticleEstimate = (int) Math.round(currentRadius / 10 + 4);
            double horizontalDistanceBetweenParticles = currentRadius / (double) horizontalParticleEstimate;

            for (double yCoord = highestY; yCoord >= lowestY; yCoord -= 3) {
                double offset = yCoord - centerY;
                for (double xzCoord = -currentRadius; xzCoord <= currentRadius; xzCoord += horizontalDistanceBetweenParticles) {

                    world.spawnParticle(Particle.DUST,
                            center.clone().add(xzCoord, offset, currentRadius),
                            1, 0, 0, 0, 1, dustOptions, true);

                    world.spawnParticle(Particle.DUST,
                            center.clone().add(xzCoord, offset, -currentRadius),
                            1, 0, 0, 0, 1, dustOptions, true);

                    world.spawnParticle(Particle.DUST,
                            center.clone().add(-currentRadius, offset, xzCoord),
                            1, 0, 0, 0, 1, dustOptions, true);

                    world.spawnParticle(Particle.DUST,
                            center.clone().add(currentRadius, offset, xzCoord),
                            1, 0, 0, 0, 1, dustOptions, true);

                    CBCPlugin.getPlugin().getLogger().info("DEATH PARTICLE MADE");

                }
            }
        }
        else if (shape == DeathBorderShape.CIRCLE) {

            int horizontalParticleEstimate = (int) Math.round((currentRadius / 10) * 6 + 8);

            Set<double[]> xAndZPoints = new HashSet<>();
            for (int i = 0; i < horizontalParticleEstimate; i++) {
                double angle = ((double) i / (double) horizontalParticleEstimate) * Math.PI * 2;
                double[] xZArray = new double[2];
                xZArray[0] = Math.cos(angle) * currentRadius;
                xZArray[1] = Math.sin(angle) * currentRadius;
                xAndZPoints.add(xZArray);
            }

            for (double yCoord = highestY; yCoord >= lowestY; yCoord -= 3) {
                double offset = yCoord - centerY;
                for (double[] xzArray : xAndZPoints) {
                    world.spawnParticle(Particle.DUST,
                            center.clone().add(xzArray[0], offset, xzArray[1]),
                            1, 0, 0, 0, 1, dustOptions, true);
                }
            }

            CBCPlugin.getPlugin().getLogger().info("DEATH PARTICLE MADE");
        }
    }

    public boolean isActive() {
        return active;
    }

    public boolean isGameOver() {
        return game.isGameOver();
    }

    public double getCurrentRadius() {
        return currentRadius;
    }

    public Game getGame () {
        return game;
    }

}
