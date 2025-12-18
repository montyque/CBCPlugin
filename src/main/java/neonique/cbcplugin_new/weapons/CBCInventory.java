package neonique.cbcplugin_new.weapons;

import neonique.cbcplugin_new.CBCPlugin;
import neonique.cbcplugin_new.playerclasses.CBCPlayer;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class CBCInventory {

    public final static NamespacedKey slotKey = new NamespacedKey(CBCPlugin.getPlugin(), "cbc_item_slot");

    private final CBCPlayer player;
    private final WeaponFactory weaponFactory;
    private final EquipmentFactory equipmentFactory;
    private final Map<Integer, InventorySlot> permanentSlots;

    private ItemStack helmetOverride = null;

    public CBCInventory (CBCPlayer player, WeaponFactory weaponFactory, EquipmentFactory equipmentFactory) {

        this.player = player;
        this.equipmentFactory = equipmentFactory;
        this.weaponFactory = weaponFactory;
        permanentSlots = new HashMap<>();
        setWeapons();

    }

    public void setWeapons () {
        int slotNum = 0;
        for (CrossbowWeapon weapon : weaponFactory.getPlayerBaseWeapons(player)) {
            permanentSlots.put(slotNum, new WeaponSlot(weapon));
            slotNum++;
        }
    }

    public void setItem (int slot, ItemStack item) {
        permanentSlots.put(slot, new StaticItemSlot(item));
        if (player.isAlive()) loadSlots();
    }

    public void removeItem (int slot) {
        permanentSlots.remove(slot);
        if (player.isAlive()) {
            Player playerEntity = player.getPlayer();
            playerEntity.getInventory().setItem(slot, null);
            playerEntity.updateInventory();
            loadSlots();
        }
    }

    public void loadEquipment () {

        Player playerEntity = player.getPlayer();
        PlayerInventory inventory = playerEntity.getInventory();

        // Set chestplate and helmet
        inventory.setChestplate(equipmentFactory.getChestplate(player));
        inventory.setHelmet(helmetOverride != null ? helmetOverride : equipmentFactory.getHelmet(player));
        if (player.isAlive()) loadSlots();

        playerEntity.updateInventory();

    }

    public void loadSlots () {
        for (Integer slot : permanentSlots.keySet()) {
            setPlayerSlotItem(slot);
        }
    }

    public void setPlayerSlotItem (int slot) {

        if (!player.isOnline()) return;

        Player playerEntity = player.getPlayer();
        ItemStack slotItem = permanentSlots.get(slot).getItem();

        // Add PDC information for the slot of this weapon
        ItemMeta meta = slotItem.getItemMeta();
        meta.getPersistentDataContainer().set(slotKey, PersistentDataType.INTEGER, slot);
        slotItem.setItemMeta(meta);

        playerEntity.getInventory().setItem(slot, slotItem);
        playerEntity.updateInventory();

    }

    public void updateWeaponReloads () {
        forEachWeapon(w -> w.getWeaponReloader().updateReload());
    }

    public void setReloadsBySecond (double seconds) {
        forEachWeapon(w -> w.getWeaponReloader().setReloadBySecond(seconds));
        if (player.isAlive()) loadSlots();
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
        loadEquipment();
    }

}
