package neonique.cbcplugin_new.tasks.gamemodetasks;

import neonique.cbcplugin_new.gamemodes._base.CBCTeam;
import neonique.cbcplugin_new.gamemodes._base.CBCMap;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Firework;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.scheduler.BukkitRunnable;

public class VictoryFireworkTask extends BukkitRunnable {

    private int tick; // Increments each time a firework is fired

    private CBCTeam winningTeam;
    private CBCMap map;

    public VictoryFireworkTask(CBCTeam winningTeam, CBCMap map) {

        this.winningTeam = winningTeam;
        this.map = map;

    }

    @Override
    public void run() {

        // Calculate position of firework
        Location mapCentre = map.getMapCentre();
        Location fireworkSpawn = mapCentre;

        // If the fireworks are going around in a circle
        if (tick < 8) {
            double angleRad = ((double) tick / (double) 8) * Math.PI * 2;

            double fireworkX = Math.cos(angleRad) * map.getFireworkSpawnRadius();
            double fireworkZ = Math.sin(angleRad) * map.getFireworkSpawnRadius();

            fireworkSpawn = mapCentre.clone().add(fireworkX, map.getFireworkSpawnHeight(), fireworkZ);
        }

        // Summon a firework
        Firework firework = (Firework) map.getWorld().spawnEntity(fireworkSpawn.clone().add(0, 2, 0),
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

        if (winningTeam != null) {
            // Team exists, so make the firework that color
            return builder.withColor(Color.fromRGB(winningTeam.getColor().value()));
        }
        else {
            // Team does not exist, so make the firework the four CBC colours
            return builder.withColor(Color.fromRGB(NamedTextColor.RED.value()))
                    .withColor(Color.fromRGB(NamedTextColor.BLUE.value()))
                    .withColor(Color.fromRGB(NamedTextColor.GREEN.value()))
                    .withColor(Color.fromRGB(NamedTextColor.YELLOW.value()));
        }

    }
}
