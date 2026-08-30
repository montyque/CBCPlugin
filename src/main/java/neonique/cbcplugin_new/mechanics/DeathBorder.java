package neonique.cbcplugin_new.mechanics;

import neonique.cbcplugin_new.combat.DeathCause;
import neonique.cbcplugin_new.combat.events.CBCPlayerDeathEvent;
import neonique.cbcplugin_new.core.PlayerStore;
import neonique.cbcplugin_new.core.CBCPlayer;
import neonique.cbcplugin_new.util.ConfigUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.time.Duration;
import java.util.HashSet;
import java.util.Set;

public class DeathBorder {

    private final Plugin plugin;
    private final PlayerStore players;

    // Defualt variables
    private final Location center;
    private final DeathBorderShape shape;
    private final double startRadius;
    private final double finalRadius;
    private final double highestY; // Highest Y value of the border particle display
    private final double lowestY; // Lowest Y value of the border particle display
    private final int shrinkRate;
    private final double warnDistance;

    // Current border
    private boolean active = false;
    private double currentRadius;

    // Border tasks
    private BukkitRunnable borderShrinkTask;
    private BukkitRunnable borderDamageTask;

    public DeathBorder (Plugin plugin, PlayerStore players, Location center, DeathBorderOptions options) {

        this.plugin = plugin;
        this.players = players;
        this.center = center;
        this.shape = options.shape;
        this.startRadius = options.maxRadius;
        this.finalRadius = options.minRadius;
        this.highestY = options.topY;
        this.lowestY = options.bottomY;
        this.shrinkRate = options.ticksToShrink;
        this.warnDistance = 5;

    }

    public void activateBorder () {

        active = true;

        // Reset radius to start radius
        currentRadius = startRadius;

        startShrinkTask();
        startDamageTask();

    }

    private void startShrinkTask () {
        borderShrinkTask = new BukkitRunnable() {
            @Override
            public void run() {
                shrinkBorder();
            }
        };
        borderShrinkTask.runTaskTimer(plugin, 0, shrinkRate * 3L);
    }

    private void startDamageTask () {
        borderDamageTask = new BukkitRunnable() {
            @Override
            public void run() {
                for (CBCPlayer player : players.players()) {
                    if (player.isOnline() && player.isAlive()) {
                        checkIfPlayerOutsideBorder(player);
                    }
                }
            }
        };
        borderDamageTask.runTaskTimer(plugin, 0, 10);
    }

    public void deactivateBorder () {

        active = false;

        if (borderShrinkTask != null && !borderShrinkTask.isCancelled()) {
            borderShrinkTask.cancel();
        }

        if (borderDamageTask != null && !borderDamageTask.isCancelled()) {
            borderDamageTask.cancel();
        }

    }

    public void shrinkBorder () {

        if (!active) return;

        // Make sure border does not shrink completely
        if (currentRadius - 1.5 < finalRadius) {
            currentRadius = finalRadius;
        } else {
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

        if (player.getPlayer().getHealth() <= 1) {
            plugin.getServer().getPluginManager().callEvent(new CBCPlayerDeathEvent(
                    player, player.getLastPlayerHitBy(), DeathCause.DEATH_BORDER, false
            ));
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
        player.showTitle(Title.title(title, subtitle, times));
    }

    public void playParticles () {

        World world = center.getWorld();
        double centerY = center.getY();
        Particle.DustOptions dustOptions = new Particle.DustOptions(org.bukkit.Color.fromRGB(255, 0, 0), 4.0f);

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
        }
    }

    public boolean isActive() {
        return active;
    }

    public double getCurrentRadius() {
        return currentRadius;
    }

    public enum DeathBorderShape {
        SQUARE, CIRCLE
    }

    public record DeathBorderOptions (DeathBorderShape shape,
                                      double maxRadius,
                                      double minRadius,
                                      int ticksToShrink,
                                      double topY,
                                      double bottomY) {

        public static DeathBorderOptions fromConfig (ConfigurationSection section) {
            return new DeathBorderOptions(
                    ConfigUtil.requireEnum(section, "border_shape", DeathBorderShape.class),
                    ConfigUtil.requireDouble(section, "max_radius"),
                    ConfigUtil.requireDouble(section, "min_radius"),
                    ConfigUtil.requireInt(section, "ticks_to_shrink"),
                    ConfigUtil.requireDouble(section, "top_y"),
                    ConfigUtil.requireDouble(section, "bottom_y")
            );
        }

    }

}
