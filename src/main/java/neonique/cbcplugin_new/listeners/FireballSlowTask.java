package neonique.cbcplugin_new.listeners;

import org.bukkit.entity.Fireball;
import org.bukkit.scheduler.BukkitRunnable;

public class FireballSlowTask extends BukkitRunnable {

    private final Fireball fireball;
    static float fireballVelocity = 1;

    public FireballSlowTask (Fireball fireball) {
        this.fireball = fireball;
    }

    public void run() {
        if (fireball.isDead()) return;
        fireball.setVelocity(fireball.getVelocity().multiply(fireballVelocity));
    }

    public static void setFireballVelocity(float i) {
        fireballVelocity = i;
    }
}
