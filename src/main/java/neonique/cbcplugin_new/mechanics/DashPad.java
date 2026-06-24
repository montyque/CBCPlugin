package neonique.cbcplugin_new.mechanics;

import neonique.cbcplugin_new.CBCPlugin;
import neonique.cbcplugin_new.managers.GameManager;
import neonique.cbcplugin_new.combat.CombatManager;
import neonique.cbcplugin_new.playerclasses.CBCPlayer;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

public class DashPad {

    private final boolean enabled;

    private final GameManager gameManager;
    private final CombatManager combatManager;

    private final Set<Location> blocks;
    private final Vector velocity;

    private final HashMap<CBCPlayer, Integer> cooldown;

    public DashPad (GameManager gameManager, CombatManager combatManager, Vector startBlock, Vector endBlock, Vector velocity) {

        enabled = true;

        this.gameManager = gameManager;
        this.combatManager = combatManager;

        this.velocity = velocity;

        // Create locations
        blocks = new HashSet<>();
        double lowerX = Math.min(startBlock.getX(), endBlock.getX());
        double higherX = Math.max(startBlock.getX(), endBlock.getX());
        double lowerY = Math.min(startBlock.getY(), endBlock.getY());
        double higherY = Math.max(startBlock.getY(), endBlock.getY());
        double lowerZ = Math.min(startBlock.getZ(), endBlock.getZ());
        double higherZ = Math.max(startBlock.getZ(), endBlock.getZ());

        for (double x = lowerX; x <= higherX; x++) {
            for (double y = lowerY; y <= higherY; y++) {
                for (double z = lowerZ; z <= higherZ; z++) {
                    blocks.add(new Location(gameManager.getWorld(), x, y, z));
                }
            }
        }

        cooldown = new HashMap<>();

        System.out.println("Created new dash pad");
    }

    public void playerPressed (CBCPlayer player) {

        if (!player.isOnline()) return;
        if (cooldown.containsKey(player)) return;

        // Launch player
        player.getPlayer().setVelocity(velocity);
        player.getPlayer().playSound(player.getPlayer().getLocation(), Sound.ITEM_TRIDENT_RIPTIDE_3, 100F, 1.5F);
        cooldown.put(player, 10);

        // Particle effect
        new BukkitRunnable() {

            int tick = 0;
            @Override
            public void run() {

                if (!combatManager.isDashPadsEnabled()) {
                    this.cancel();
                    return;
                }

                if (!player.isOnline()) {
                    this.cancel();
                    return;
                }

                Player playerEntity = player.getPlayer();
                if (playerEntity == null) {
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

    public Set<Location> getBlocks() {
        return blocks;
    }

    public void refreshCooldowns() {
        for (CBCPlayer player : new HashSet<>(cooldown.keySet())) {
            if (cooldown.getOrDefault(player, 20) == 1) {
                cooldown.remove(player);
            } else {
                cooldown.put(player, cooldown.get(player) - 1);
            }
        }
    }
}
