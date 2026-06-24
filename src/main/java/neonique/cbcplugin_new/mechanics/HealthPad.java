package neonique.cbcplugin_new.mechanics;

import neonique.cbcplugin_new.CBCPlugin;
import neonique.cbcplugin_new.managers.GameManager;
import neonique.cbcplugin_new.combat.CombatManager;
import neonique.cbcplugin_new.playerclasses.CBCPlayer;
import neonique.cbcplugin_new.combat.tasks.HealPadTimerTask;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.AreaEffectCloud;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.util.Vector;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class HealthPad extends Location  {

    GameManager gameManager;
    CombatManager combatManager;

    private boolean enabled = false;

    // Hologram for heal pad display
    private UUID hologramUUID = null;
    private UUID itemUUID = null;

    // Healing pad stats
    private int healPadTimer = 0;

    // Blocks around heal pad
    private final Set<Block> blockSet = new HashSet<>();

    public HealthPad(GameManager gameManager, CombatManager combatManager, Vector coordinates) {
        super(gameManager.getWorld(), coordinates.getX(), coordinates.getY(), coordinates.getZ());

        this.gameManager = gameManager;
        this.combatManager = combatManager;

        // Get the blocks around the location and add them to the blockSet
        for (double q = -1; q < 2; q++) {
            for (double p = -1; p < 2; p++) {
                blockSet.add(this.clone().add(q, 0, p).getBlock());
            }
        }
    }

    public boolean isOnline () {
        return healPadTimer == 0;
    }

    public void startTimer () {
        healPadTimer = this.combatManager.getHealPadTimer() + 1; // Reset healpad timer
        new HealPadTimerTask(this).runTaskTimer(CBCPlugin.getPlugin(), 0, 20);
    }

    public Entity getItem () {
        if (itemUUID != null) {
            return getWorld().getEntity(itemUUID);
        }
        else {
            return null;
        }
    }

    public void setOnline () {

        /* Item itemEntity = (Item) getWorld().spawnEntity(this.clone().add(0, 2, 0),
                EntityType.DROPPED_ITEM, CreatureSpawnEvent.SpawnReason.COMMAND);

        itemEntity.setVelocity(new Vector(0, 0, 0));
        itemEntity.setItemStack(new ItemStack(Material.EXPERIENCE_BOTTLE));
        itemEntity.setInvulnerable(true);
        itemEntity.setGravity(false);
        itemEntity.setCanPlayerPickup(false);
        itemEntity.setCanMobPickup(false);

        itemUUID = itemEntity.getUniqueId();

        if (weaponManager.getHealthPadItemTeam() != null) {
            weaponManager.getHealthPadItemTeam().addEntry(itemEntity.getUniqueId().toString());
        } */

        healPadTimer = 0;
        for (Block block : blockSet) {
            block.setType(Material.EMERALD_BLOCK);
        }
    }

    public void setOffline () {

        /*
        if (itemUUID != null) {
            Entity item = getWorld().getEntity(itemUUID);
            if (item != null) {
                item.remove();
            }
            itemUUID = null;
        }
        */

        // Disabling heal pad visually
        for (Block block : blockSet) {
            block.setType(Material.GREEN_TERRACOTTA);
        }
        // Check if enabled
        if (enabled) {
            startTimer();
        }
    }

    public void decrementTimer (HealPadTimerTask task) {
        healPadTimer--;

        AreaEffectCloud hologram = getHologram();



        if (healPadTimer > 0) {
            if (hologram != null) {
                // Change hologram title
                Component hologramComponent = Component.text("⌚ " + healPadTimer + "s").color(NamedTextColor.GREEN);
                hologram.customName(hologramComponent);
                hologram.setCustomNameVisible(true);
            }
        } else {
            task.cancel();
            if (hologram != null) {
                hologram.setCustomNameVisible(false);
            }
            setOnline();
        }
    }

    public boolean isEnabled () {
        return enabled;
    }

    public void disable () {

        if (!enabled) {
            return;
        }

        // Remove hologram
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

        // Create hologram
        AreaEffectCloud hologram = getHologram();
        if (hologram == null) {
            // Delete any nearby holograms
            Collection<AreaEffectCloud> nearbyHolograms = this.clone().add(0, 1, 0).getNearbyEntitiesByType(AreaEffectCloud.class, 0.1);
            for (AreaEffectCloud h : nearbyHolograms) {
                if (!h.isDead()) {
                    h.remove();
                }
            }

            hologram = (AreaEffectCloud) gameManager.getWorld().spawnEntity(this.clone().add(0, 1, 0), EntityType.AREA_EFFECT_CLOUD);
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
        // Healing player over time
        player.addHealing(6);
        player.getPlayer().playSound(player.getPlayer().getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 100, 2);

    }

    private AreaEffectCloud getHologram () {
        if (hologramUUID != null) {
            return (AreaEffectCloud) CBCPlugin.getPlugin().getServer().getEntity(hologramUUID);
        } else {
            return null;
        }
    }
}
