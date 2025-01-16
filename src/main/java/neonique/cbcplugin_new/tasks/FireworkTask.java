package neonique.cbcplugin_new.tasks;

import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Firework;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.scheduler.BukkitRunnable;

public class FireworkTask extends BukkitRunnable {

    private int tick; // Increments each time a firework is fired

    private final NamedTextColor winningColor;
    private final Location fireworkSpawn;

    public FireworkTask(NamedTextColor winningColor, Location fireworkSpawn) {

        this.winningColor = winningColor;
        this.fireworkSpawn = fireworkSpawn;

    }

    @Override
    public void run() {

        // If the fireworks are going around in a circle
        Location spawn = fireworkSpawn.clone();
        if (tick < 8) {
            double angleRad = ((double) tick / (double) 8) * Math.PI * 2;

            double fireworkX = Math.cos(angleRad) * 7.0;
            double fireworkZ = Math.sin(angleRad) * 7.0;
            spawn.add(fireworkX, 0, fireworkZ);
        }

        // Summon a firework
        Firework firework = (Firework) spawn.getWorld().spawnEntity(spawn,
                EntityType.FIREWORK_ROCKET, CreatureSpawnEvent.SpawnReason.COMMAND);
        FireworkMeta fireworkMeta = firework.getFireworkMeta();

        FireworkEffect.Builder fireworkBuilder = addColorToEffect(FireworkEffect.builder());
        FireworkEffect effect;
        // If the fireworks are going around in a circle, make them small
        if (tick < 8) {
            effect = fireworkBuilder.with(FireworkEffect.Type.BALL).build();
        }
        // If all 8 fireworks have already been shot, fire a big one in the middle
        else {
            effect = fireworkBuilder.with(FireworkEffect.Type.BALL_LARGE).flicker(true).trail(true).build();
            fireworkMeta.addEffect(FireworkEffect.builder().withColor(Color.WHITE).with(FireworkEffect.Type.BALL).build());
        }

        fireworkMeta.addEffect(effect);
        fireworkMeta.setPower(2);
        firework.setFireworkMeta(fireworkMeta);

        if (tick == 8) {
            this.cancel();
        }

        // Increment tick by 1
        tick++;
    }

    private FireworkEffect.Builder addColorToEffect (FireworkEffect.Builder builder) {
        // Team exists, so make the firework that color
        return builder.withColor(Color.fromRGB(winningColor.value()));
    }
}
