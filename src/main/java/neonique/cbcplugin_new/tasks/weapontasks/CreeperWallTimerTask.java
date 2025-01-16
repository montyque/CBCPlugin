package neonique.cbcplugin_new.tasks.weapontasks;
import neonique.cbcplugin_new.CBCPlugin;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Entity;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.UUID;

public class CreeperWallTimerTask extends BukkitRunnable {

    UUID creeperUUID;

    public CreeperWallTimerTask (UUID uuid) {
        this.creeperUUID = uuid;
    }

    @Override
    public void run() {

        Entity e = CBCPlugin.getPlugin().getServer().getEntity(creeperUUID);

        if (!(e instanceof Creeper)) {
            return;
        }

        Creeper creeper = (Creeper) e;

        if (!creeper.isDead()) {
            creeper.addScoreboardTag("canWallExplode");
        }
    }
}
