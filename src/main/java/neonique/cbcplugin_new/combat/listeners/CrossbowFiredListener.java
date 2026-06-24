package neonique.cbcplugin_new.combat.listeners;

import neonique.cbcplugin_new.managers.PlayerRegistry;
import neonique.cbcplugin_new.combat.ProjectileManager;
import neonique.cbcplugin_new.playerclasses.CBCPlayer;
import neonique.cbcplugin_new.weapons.CBCInventory;
import neonique.cbcplugin_new.weapons.InventorySlot;
import neonique.cbcplugin_new.weapons.WeaponSlot;
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

import java.util.Optional;

public class CrossbowFiredListener implements Listener {

    private final PlayerRegistry playerRegistry;
    private final ProjectileManager projectileManager;

    public CrossbowFiredListener(PlayerRegistry playerRegistry, ProjectileManager projectileManager) {
        this.playerRegistry = playerRegistry;
        this.projectileManager = projectileManager;
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

        CBCPlayer playerSource = playerRegistry.getPlayerByUUID(entityFired.getUniqueId());
        if (playerSource == null) return;

        Optional<Integer> itemSlotId = getItemSlotId(itemFired);
        if (itemSlotId.isEmpty()) return;

        InventorySlot slotFrom = playerSource.getInventory().getSlot(itemSlotId.get());

        if (slotFrom instanceof WeaponSlot weaponSlot) {
            weaponSlot.getWeapon().fireWeapon(arrowFired, projectileManager);
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
