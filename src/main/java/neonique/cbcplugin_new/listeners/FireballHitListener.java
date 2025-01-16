package neonique.cbcplugin_new.listeners;

import neonique.cbcplugin_new.CBCPlugin;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.ProjectileHitEvent;

public class FireballHitListener implements Listener {

    @EventHandler
    public void onFireballHit(EntityDamageByEntityEvent e) {

        if (!(e.getDamager() instanceof Player)) return;
        if (!(e.getEntity() instanceof Fireball)) return;

        new FireballSlowTask((Fireball) e.getEntity()).runTaskLater(CBCPlugin.getPlugin(), 3L);
    }
}
