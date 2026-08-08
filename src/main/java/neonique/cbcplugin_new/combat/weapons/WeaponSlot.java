package neonique.cbcplugin_new.combat.weapons;

import org.bukkit.inventory.ItemStack;

public class WeaponSlot implements InventorySlot {

    private final CrossbowWeapon weapon;

    public WeaponSlot(CrossbowWeapon weapon) {
        this.weapon = weapon;
    }

    public CrossbowWeapon getWeapon() {
        return weapon;
    }

    @Override
    public ItemStack getItem() {
        return weapon.getWeaponItem();
    }

}
