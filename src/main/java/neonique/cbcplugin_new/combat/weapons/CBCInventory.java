package neonique.cbcplugin_new.combat.weapons;

import neonique.cbcplugin_new.CBCPlugin;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class CBCInventory {

    public final static NamespacedKey slotKey = new NamespacedKey(CBCPlugin.getPlugin(), "cbc_item_slot");

    private final CBCLoadout loadout;
    private final Runnable inventoryUpdateListener;

    private Map<Integer, InventorySlot> permanentSlots;
    private ItemStack helmetOverride = null;

    public CBCInventory (CBCLoadout loadout, Runnable inventoryUpdateListener) {
        this.loadout = loadout;
        this.inventoryUpdateListener = inventoryUpdateListener;
        setWeapons();
    }

    public void setWeapons () {
        permanentSlots = new HashMap<>();
        permanentSlots.putAll(loadout.getSlots());
    }

    public void setItem (int slot, ItemStack item) {
        permanentSlots.put(slot, new StaticItemSlot(item));
    }

    public void removeItem (int slot) {
        permanentSlots.remove(slot);
    }

    public void loadEquipment (PlayerInventory inventory) {
        inventory.setChestplate(loadout.getChestplate());
        inventory.setHelmet(helmetOverride != null ? helmetOverride : loadout.getHelmet());
        loadSlots(inventory);
    }

    public void loadSlots (PlayerInventory inventory) {
        for (Integer slot : permanentSlots.keySet()) {
            setPlayerSlotItem(inventory, slot);
        }
    }

    public void setPlayerSlotItem (PlayerInventory inventory, int slot) {

        ItemStack slotItem = permanentSlots.get(slot).getItem();

        // Add PDC information for the slot of this weapon
        ItemMeta meta = slotItem.getItemMeta();
        meta.getPersistentDataContainer().set(slotKey, PersistentDataType.INTEGER, slot);
        slotItem.setItemMeta(meta);
        inventory.setItem(slot, slotItem);

    }

    public void updateWeaponReloads () {
        forEachWeapon(w -> w.getWeaponReloader().updateReload());
    }

    public void setReloadsBySecond (double seconds) {
        forEachWeapon(w -> w.getWeaponReloader().setReloadBySecond(seconds));
    }

    public InventorySlot getSlot (int slot) {
        return permanentSlots.get(slot);
    }

    public void forEachWeapon (Consumer<CrossbowWeapon> action) {
        permanentSlots.values().forEach(slot -> {
            if (slot instanceof WeaponSlot weaponSlot) {
                action.accept(weaponSlot.getWeapon());
            }
        });
    }

    public void setHelmetOverride (ItemStack override) {
        helmetOverride = override;
    }

}
