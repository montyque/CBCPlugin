package neonique.cbcplugin_new.commands;

import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Villager;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.UUID;

public class LaunchParticleTask extends BukkitRunnable {

    UUID entityUUID;

    int tick = 0;

    public LaunchParticleTask (Entity entity) {
        this.entityUUID = entity.getUniqueId();
    }
    @Override
    public void run() {

        Entity entity = Bukkit.getServer().getEntity(entityUUID);

        if (entity == null) {
            this.cancel();
            return;
        }

        tick++;

        Particle.DustOptions dustOptions = new Particle.DustOptions(org.bukkit.Color.fromRGB(255, 127, 0), 4);

        entity.getWorld().spawnParticle(Particle.DUST,
                entity.getLocation(),
                1, 0, 0, 0, 1, dustOptions, true);

        if (entity.isOnGround() || tick > 200) {
            this.cancel();
            entity.remove();
        }
    }
}
