package neonique.cbcplugin_new.gamemodes.rendezvous;

import neonique.cbcplugin_new.managers.CBCScoreboardManager;
import neonique.cbcplugin_new.managers.GameManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.util.Transformation;
import org.bukkit.util.Vector;
import org.joml.AxisAngle4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class RendezvousCheckpoint extends Location {

    private final GameManager gameManager;
    private RendezvousGame game;

    // Checkpoint area information
    private final double checkpointRadius;
    private double upwardsExtensionMax;

    // Particle information
    private static List<Point2D> circlePositions = new ArrayList<>();
    private static List<Point2D> circlePositions2 = new ArrayList<>();
    Integer currentCirclePosition = 0;

    // Glowing snowball marker information
    private final HashMap<RendezvousTeam, UUID> glowingMarkers;
    private final HashMap<RendezvousTeam, UUID> progressMarkers;

    public RendezvousCheckpoint (World world, Vector vector, GameManager gameManager, double checkpointRadius) {

        super(world, vector.getX(), vector.getY(), vector.getZ());
        this.gameManager = gameManager;

        this.checkpointRadius = checkpointRadius;

        setUpwardsExtensionMax();

        glowingMarkers = new HashMap<>();
        progressMarkers = new HashMap<>();
    }

    public static void recalculateCirclePositions () {

        // Calculating coordinate positions
        circlePositions.clear();
        circlePositions2.clear();
        for (double j = 0; j < Math.PI/2; j += Math.PI/16) {
            double x = Math.cos(j);
            double y = Math.sin(j);
            circlePositions.add(new Point2D.Double(x, y));

            double x1 = Math.cos(j + Math.PI/2);
            double y1 = Math.sin(j + Math.PI/2);
            circlePositions2.add(new Point2D.Double(x1, y1));
        }

    }

    public void setGame (RendezvousGame game) {
        this.game = game;
    }

    public void setUpwardsExtensionMax () {
        upwardsExtensionMax = 15;
        Location location = this.clone();
        for (int i = 0; i < (int) Math.round(upwardsExtensionMax); i++) {
            location.add(0, 1, 0);
            if (location.getBlock().isSolid()) {
                upwardsExtensionMax = i;
                break;
            }
        }
    }

    public void createGlowingMarker(RendezvousTeam team) {

        // Create block display
        Entity markerEntity = getWorld().spawnEntity(this.clone().add(0, -1.05 - (glowingMarkers.size()), 0),
                EntityType.BLOCK_DISPLAY, CreatureSpawnEvent.SpawnReason.COMMAND,
                entity -> entity.setInvulnerable(true));

        BlockDisplay blockDisplay = (BlockDisplay) markerEntity;
        blockDisplay.setBlock(Bukkit.createBlockData(Material.DIAMOND_BLOCK));
        blockDisplay.setGlowColorOverride(Color.fromRGB(team.getColor().value()));
        blockDisplay.setGlowing(true);
        blockDisplay.setTransformation(
                new Transformation(new Vector3f(-0.5f, 0f, -0.5f), new AxisAngle4f(0, 0, 0, 0),
                        new Vector3f(1f, 1f, 1f), new AxisAngle4f(0, 0, 0, 0))
        );

        // Add to glowing markers
        glowingMarkers.put(team, markerEntity.getUniqueId());

        // Create hologram
        AreaEffectCloud hologram = (AreaEffectCloud) gameManager.getWorld().spawnEntity(this.clone().add(0, 4 +
                (progressMarkers.size()) * 0.5, 0), EntityType.AREA_EFFECT_CLOUD);
        hologram.clearCustomEffects();
        hologram.setRadius(0);
        hologram.setDuration(30000000);

        progressMarkers.put(team, hologram.getUniqueId());

        // Change hologram title
        hologram.setCustomNameVisible(true);
        setHologramTitle(team);
    }

    public void setHologramTitle (RendezvousTeam team) {

        UUID uuid = progressMarkers.get(team);
        if (uuid == null) return;

        Entity entity = Bukkit.getEntity(uuid);
        if (entity == null) return;

        // Change hologram title

        float percentileProgress = 1f - (((float) team.getTargetProgress()) / (float) team.getProgressMax());
        final int lengthOfColor = (int) (percentileProgress * 10f);

        float timeUntilCapture = ((float) team.getTargetProgress()) / 10f;

        final StringBuilder coloredTitle = new StringBuilder();
        final StringBuilder whiteTitle = new StringBuilder();

        for (int i = 0; i < 10; i++) {
            if (i < lengthOfColor) {
                coloredTitle.append("⬛");
            }
            else {
                whiteTitle.append("⬛");
            }
        }

        // Create component
        Component hologramName = Component.text(team.getPrefix() + " ").color(team.getColor()).decorate(TextDecoration.BOLD);

        hologramName = hologramName.append(Component.text(coloredTitle.toString())
                .color(team.getColor()).decoration(TextDecoration.BOLD, TextDecoration.State.FALSE));

        hologramName = hologramName.append(Component.text(whiteTitle.toString())
                .color(NamedTextColor.GRAY).decoration(TextDecoration.BOLD, TextDecoration.State.FALSE));

        hologramName = hologramName.append(Component.text(" " + String.format(java.util.Locale.US,"%.1f", timeUntilCapture) + "s")
                .color(team.getColor()).decoration(TextDecoration.BOLD, TextDecoration.State.FALSE));

        entity.customName(hologramName);
    }

    public void deleteEntity(UUID uuid) {
        Entity entity = Bukkit.getEntity(uuid);
        if (entity == null) return;
        entity.remove();
    }

    public void removeGlowingMarker(RendezvousTeam team) {
        if (!glowingMarkers.containsKey(team)) return;
        UUID snowballUUID = glowingMarkers.get(team);
        if (snowballUUID != null) {
            deleteEntity(snowballUUID);
        }
        glowingMarkers.remove(team);
    }

    public void removeHologram(RendezvousTeam team) {
        if (!progressMarkers.containsKey(team)) return;
        UUID uuid = progressMarkers.get(team);
        if (uuid != null) {
            deleteEntity(uuid);
        }
        progressMarkers.remove(team);
    }

    public boolean checkIfPlayerInCheckpoint (Player player) {

        // Get player location and Y level
        Location loc = player.getLocation();
        double playerY = loc.getY() + 0.1;

        // Check if player's Y is within the upwards extension max (from checkpoint to X blocks above the checkpoint)
        double yDistBetweenPlayerAndCheckpoint = playerY - this.getY();
        if (yDistBetweenPlayerAndCheckpoint >= -1 && yDistBetweenPlayerAndCheckpoint <= upwardsExtensionMax) {

            // Get the location of the player if their Y level was at the checkpoint's Y level
            Location playerLocAtCheckpointY = loc.clone();
            playerLocAtCheckpointY.setY(this.getY());

            // Check if distance is within checkpoint radius
            return playerLocAtCheckpointY.distance(this) <= checkpointRadius;
        }
        return false;
    }

    public void playParticles (NamedTextColor teamColor, boolean playWhiteParticles) {

        // Get current circle position
        double circleParticleX = circlePositions.get(currentCirclePosition).getX() * checkpointRadius + this.getX();
        double circleParticleZ = circlePositions.get(currentCirclePosition).getY() * checkpointRadius + this.getZ();

        double circleParticleX1 = -circlePositions.get(currentCirclePosition).getX() * checkpointRadius + this.getX();
        double circleParticleZ1 = -circlePositions.get(currentCirclePosition).getY() * checkpointRadius + this.getZ();

        double circleParticleX2 = circlePositions2.get(currentCirclePosition).getX() * checkpointRadius + this.getX();
        double circleParticleZ2 = circlePositions2.get(currentCirclePosition).getY() * checkpointRadius + this.getZ();

        double circleParticleX3 = -circlePositions2.get(currentCirclePosition).getX() * checkpointRadius + this.getX();
        double circleParticleZ3 = -circlePositions2.get(currentCirclePosition).getY() * checkpointRadius + this.getZ();

        if (playWhiteParticles) {
            this.getWorld().spawnParticle(Particle.FIREWORK, circleParticleX, this.getY(), circleParticleZ,
                    0, 0F, 0.5F, 0F, 1);
            this.getWorld().spawnParticle(Particle.FIREWORK, circleParticleX1, this.getY(), circleParticleZ1,
                    0, 0F, 0.5F, 0F, 1);
            this.getWorld().spawnParticle(Particle.FIREWORK, circleParticleX2, this.getY(), circleParticleZ2,
                    0, 0F, 0.5F, 0F, 1);
            this.getWorld().spawnParticle(Particle.FIREWORK, circleParticleX3, this.getY(), circleParticleZ3,
                    0, 0F, 0.5F, 0F, 1);
        }

        Color color = Color.fromRGB(teamColor.red(), teamColor.green(), teamColor.blue());
        Particle.DustOptions dustOptions = new Particle.DustOptions(color, 2);
        Particle.DustOptions bigParticleDustOptions = new Particle.DustOptions(color, 4);

        this.getWorld().spawnParticle(Particle.DUST, circleParticleX, this.getY() + 0.5, circleParticleZ,
                1, 0F, 0F, 0F, 1, dustOptions, true);
        this.getWorld().spawnParticle(Particle.DUST, circleParticleX1, this.getY() + 0.5, circleParticleZ1,
                1, 0F, 0F, 0F, 1, dustOptions, true);
        this.getWorld().spawnParticle(Particle.DUST, circleParticleX2, this.getY() + 0.5, circleParticleZ2,
                1, 0F, 0F, 0F, 1, dustOptions, true);
        this.getWorld().spawnParticle(Particle.DUST, circleParticleX3, this.getY() + 0.5, circleParticleZ3,
                1, 0F, 0F, 0F, 1, dustOptions, true);

        this.getWorld().spawnParticle(Particle.DUST, getX(), this.getY() + 30, getZ(),
                4, 0.5F, 10F, 0.5F, 1, bigParticleDustOptions, true);

        currentCirclePosition++;
        if (currentCirclePosition == circlePositions.size()) {
            currentCirclePosition = 0;
        }

    }

    public boolean isCheckpointTaken () {
        return (glowingMarkers.size() > 0);
    }

    public void playSoundOnCheckpointClear () {

        gameManager.playSound(this.clone().add(0, 1.5, 0), Sound.BLOCK_BEACON_ACTIVATE, 4, 2);

    }
}
