package neonique.cbcplugin_new.weapons;

import org.bukkit.inventory.ItemStack;

public class StaticItemSlot implements InventorySlot {

    private final ItemStack item;

    public StaticItemSlot (ItemStack item) {
        this.item = item;
    }

    @Override
    public ItemStack getItem() {
        return item;
    }

}
