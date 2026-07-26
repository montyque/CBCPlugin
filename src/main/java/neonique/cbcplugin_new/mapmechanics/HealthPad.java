package neonique.cbcplugin_new.mapmechanics;

import neonique.cbcplugin_new.core.CBCPlayer;
import neonique.cbcplugin_new.managers.PlayerSession;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.AreaEffectCloud;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import java.util.*;

public class HealthPad {

    private final Location location;
    private final int resetTimer;
    private final int healing;

    private boolean enabled = false;

    // Hologram for heal pad display
    private UUID hologramUUID = null;
    private int healPadTimer = 0;

    public HealthPad (Location loc, int resetTimer, int healing) {
        this.location = loc;
        this.resetTimer = resetTimer;
        this.healing = healing;
    }

    public boolean isOnline () {
        return healPadTimer == 0;
    }

    public void playParticles () {
        location.getWorld().spawnParticle(Particle.HAPPY_VILLAGER, location.clone().add(0, 1, 0),
                3, 1d, 1d, 1d, 1);
    }

    public void playerCheck (PlayerSession<?> registry) {
         getNearestOnPad(registry).ifPresent(this::healPadPressed);
    }

    private Optional<? extends CBCPlayer> getNearestOnPad (PlayerSession<? extends CBCPlayer> registry) {
        return location.getNearbyEntitiesByType(Player.class, 3).stream()
                .filter(this::isOnPad)
                .sorted(Comparator.comparingDouble(p -> p.getLocation().distanceSquared(location)))
                .map(registry::getPlayer)
                .filter(Objects::nonNull)
                .filter(CBCPlayer::isAlive)
                .findFirst();
    }

    private boolean isOnPad (Entity e) {
        Location playerLocation = e.getLocation();
        Block blockBelowPlayer = playerLocation.subtract(0, 1, 0).getBlock();
        return blockBelowPlayer.getType() == Material.EMERALD_BLOCK;
    }

    public void startTimer () {
        healPadTimer = resetTimer;
        updateTimerDisplay();
    }

    public void setOnline () {
        healPadTimer = 0;
        setBlocksAround(Material.EMERALD_BLOCK);
    }

    public void setOffline () {
        setBlocksAround(Material.GREEN_TERRACOTTA);
        if (enabled) {
            startTimer();
        }
    }

    public void setBlocksAround (Material mat) {
        for (double q = -1; q < 2; q++) {
            for (double p = -1; p < 2; p++) {
                location.clone().add(q, 0, p).getBlock().setType(mat);
            }
        }
    }

    public void updateTimerDisplay () {
        AreaEffectCloud hologram = getHologram();
        if (hologram == null) return;
        if (healPadTimer > 0) {
            Component hologramComponent = Component.text("⌚ " + ((healPadTimer + 19) / 20) + "s").color(NamedTextColor.GREEN);
            hologram.customName(hologramComponent);
            hologram.setCustomNameVisible(true);
        } else {
            hologram.setCustomNameVisible(false);
            setOnline();
        }
    }

    public void decrementTimer () {
        healPadTimer--;
        updateTimerDisplay();
    }

    public boolean isEnabled () {
        return enabled;
    }

    public void disable () {
        if (!enabled) return;

        // Remove the hologram
        AreaEffectCloud hologram = getHologram();
        if (hologram != null) {
            hologram.remove();
            hologramUUID = null;
        }

        enabled = false;
        setOffline();
    }

    public void enable (boolean immediatelyOnline) {

        if (enabled) {
            return;
        }

        AreaEffectCloud hologram = getHologram();
        if (hologram == null) {

            // Delete any nearby holograms
            Collection<AreaEffectCloud> nearbyHolograms = location.clone().add(0, 1, 0)
                    .getNearbyEntitiesByType(AreaEffectCloud.class, 0.1);

            for (AreaEffectCloud h : nearbyHolograms) {
                if (!h.isDead()) {
                    h.remove();
                }
            }

            // Spawn a new hologram
            hologram = (AreaEffectCloud) location.getWorld().spawnEntity(location.clone().add(0, 1, 0),
                    EntityType.AREA_EFFECT_CLOUD);
            hologram.clearCustomEffects();
            hologram.setRadius(0);
            hologram.setDuration(30000000);
            hologramUUID = hologram.getUniqueId();

        }

        enabled = true;
        if (immediatelyOnline) {
            setOnline();
        } else {
            startTimer();
        }

    }

    public void healPadPressed (CBCPlayer player) {
        if (!player.isOnline()) return;
        setOffline();
        player.addHealing(healing);
        player.getPlayer().playSound(player.getPlayer().getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 100, 2);
    }

    private AreaEffectCloud getHologram () {
        return hologramUUID != null ? (AreaEffectCloud) location.getWorld().getEntity(hologramUUID) : null;
    }

}
