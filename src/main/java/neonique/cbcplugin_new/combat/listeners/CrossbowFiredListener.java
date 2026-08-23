package neonique.cbcplugin_new.combat.listeners;

import neonique.cbcplugin_new.core.PlayerStore;
import neonique.cbcplugin_new.combat.ProjectileManager;
import neonique.cbcplugin_new.core.CBCPlayer;
import neonique.cbcplugin_new.combat.weapons.CBCInventory;
import neonique.cbcplugin_new.combat.weapons.InventorySlot;
import neonique.cbcplugin_new.combat.weapons.WeaponSlot;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.Optional;
import java.util.function.Function;

public class CrossbowFiredListener implements Listener {

    private final Plugin plugin;
    private final Function<Entity, CBCPlayer> playerGetter;
    private final ProjectileManager projectileManager;

    public CrossbowFiredListener (Plugin plugin, ProjectileManager projectileManager, PlayerStore players) {
        this.plugin = plugin;
        this.projectileManager = projectileManager;
        this.playerGetter = players::getPlayer;
    }

    @EventHandler
    public void onBowFired(EntityShootBowEvent e) {

        ItemStack itemFired = e.getBow();
        Entity projectileFired = e.getProjectile();

        if (itemFired == null) return;
        if (itemFired.getType() == Material.CROSSBOW && projectileFired instanceof Arrow arrowFired) {
            crossbowFired(e.getEntity(), itemFired, arrowFired);
        }

    }

    public void crossbowFired (Entity entityFired, ItemStack itemFired, Arrow arrowFired) {

        CBCPlayer playerSource = playerGetter.apply(entityFired);
        if (playerSource == null) return;

        Optional<Integer> itemSlotId = getItemSlotId(itemFired);
        if (itemSlotId.isEmpty()) return;

        InventorySlot slotFrom = playerSource.getInventory().getSlot(itemSlotId.get());

        if (slotFrom instanceof WeaponSlot weaponSlot) {
            weaponSlot.getWeapon().fireWeapon(plugin, playerSource, arrowFired, projectileManager::addProjectile);
            if (entityFired instanceof Player playerEntity) {
                playerEntity.playSound(playerEntity.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 10, 2);
            }
        }

    }

    public Optional<Integer> getItemSlotId (ItemStack item) {
        ItemMeta itemFiredMeta = item.getItemMeta();
        PersistentDataContainer itemFiredTags = itemFiredMeta.getPersistentDataContainer();
        Integer itemSlotId = itemFiredTags.get(CBCInventory.slotKey, PersistentDataType.INTEGER);
        return Optional.ofNullable(itemSlotId);
    }

}
