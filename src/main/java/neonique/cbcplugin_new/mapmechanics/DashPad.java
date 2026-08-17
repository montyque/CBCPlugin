package neonique.cbcplugin_new.mapmechanics;

import neonique.cbcplugin_new.CBCPlugin;
import neonique.cbcplugin_new.core.CBCPlayer;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.stream.Collectors;
/*
public class DashPad {

    private final Set<Location> checkLocations;
    private final Vector velocity;

    private final Map<CBCPlayer, Integer> cooldown;

    public DashPad (Location startBlock, Location endBlock, Vector velocity) {

        this.velocity = velocity;
        this.checkLocations = new HashSet<>();
        double lowerX = Math.min(startBlock.getX(), endBlock.getX());
        double higherX = Math.max(startBlock.getX(), endBlock.getX());
        double lowerY = Math.min(startBlock.getY(), endBlock.getY());
        double higherY = Math.max(startBlock.getY(), endBlock.getY());
        double lowerZ = Math.min(startBlock.getZ(), endBlock.getZ());
        double higherZ = Math.max(startBlock.getZ(), endBlock.getZ());

        World w = startBlock.getWorld();
        for (double x = lowerX; x <= higherX; x++) {
            for (double y = lowerY; y <= higherY; y++) {
                for (double z = lowerZ; z <= higherZ; z++) {
                    checkLocations.add(new Location(w, x, y, z));
                }
            }
        }

        cooldown = new HashMap<>();

    }

    public DashPad (GameManager gameManager, CombatManager combatManager, Vector startBlock, Vector endBlock, Vector velocity) {
        this(
                new Location(gameManager.getWorld(), startBlock.getX(), startBlock.getY(), startBlock.getZ()),
                new Location(gameManager.getWorld(), endBlock.getX(), endBlock.getY(), endBlock.getZ()),
                velocity
        );
    }

    public Collection<CBCPlayer> getPlayersOnPad (PlayerRegistry registry) {
        return checkLocations.stream()
                .flatMap(l -> l.getNearbyPlayers(1).stream())
                .distinct()
                .filter(registry::hasPlayer)
                .map(registry::getPlayer)
                .filter(CBCPlayer::isAlive)
                .filter(p -> cooldown.getOrDefault(p, 0) == 0)
                .collect(Collectors.toUnmodifiableSet());
    }

    public void launchPlayer (CBCPlayer player) {

        // Launch player with the given velocity
        Player entity = player.getPlayer();
        entity.setVelocity(velocity);
        entity.playSound(player.getPlayer().getLocation(), Sound.ITEM_TRIDENT_RIPTIDE_3, 100F, 1.5F);
        cooldown.put(player, 10);

        // Give the player a particle trail
        new BukkitRunnable() {

            private int tick = 0;
            @Override
            public void run() {

                if (!player.isOnline()) {
                    this.cancel();
                    return;
                }

                if (!player.isAlive()) {
                    this.cancel();
                    return;
                }

                tick++;

                Particle.DustOptions dustOptionsOrange = new Particle.DustOptions(org.bukkit.Color.fromRGB(255, 127, 0), 2);
                Particle.DustOptions dustOptionsYellow = new Particle.DustOptions(org.bukkit.Color.fromRGB(255, 255, 0), 1);

                player.getPlayer().getWorld().spawnParticle(Particle.DUST,
                        player.getPlayer().getLocation(),
                        1, 0, 0, 0, 1, dustOptionsOrange, true);

                player.getPlayer().getWorld().spawnParticle(Particle.DUST,
                        player.getPlayer().getLocation(),
                        3, 0, 0.2, 0.5, 3, dustOptionsYellow, true);

                if (tick > 40) {
                    this.cancel();
                }
            }
        }.runTaskTimer(CBCPlugin.getPlugin(), 1, 1);

    }

    public void updateCooldowns () {
        cooldown.replaceAll((p, c) -> Math.max(c - 1, 0));
    }

}*/
