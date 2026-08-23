package neonique.cbcplugin_new.combat.weapons;

import neonique.cbcplugin_new.core.CBCPlayer;
import neonique.cbcplugin_new.combat.projectiles.Projectile;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Arrow;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.CrossbowMeta;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.function.Consumer;

public interface CrossbowWeapon {

    NamespacedKey LOADED_KEY = new NamespacedKey("cbc", "cbc_loaded");

    void fireWeapon (Plugin plugin, CBCPlayer player, Arrow arrowFired, Consumer<Projectile> projectileRegistry);

    void editItem (ItemStack item);

    Projectile fireProjectile (Plugin plugin, CBCPlayer player, Arrow arrowFired);

    WeaponReloader getWeaponReloader();

    Component getXPBarComponent();

    default ItemStack getWeaponItem () {

        ItemStack weaponItem = new ItemStack(Material.CROSSBOW);

        weaponItem.editMeta(CrossbowMeta.class, meta ->
                meta.addEnchant(Enchantment.QUICK_CHARGE, 10, true));

        if (getWeaponReloader().isLoaded()) {

            ItemStack projectile = new ItemStack(Material.ARROW);
            weaponItem.editMeta(CrossbowMeta.class, meta ->
                    meta.addChargedProjectile(projectile));
            weaponItem.editPersistentDataContainer(pdc ->
                    pdc.set(LOADED_KEY, PersistentDataType.BOOLEAN, true));

        } else {

            float reloadPercentage = getWeaponReloader().getReloadPercentage();
            weaponItem.editMeta(Damageable.class, meta -> {
                meta.setMaxDamage(1000);
                meta.setDamage(Math.round((1.0f - reloadPercentage) * 1000.0f));
            });
            weaponItem.editPersistentDataContainer(pdc ->
                    pdc.set(LOADED_KEY, PersistentDataType.BOOLEAN, false));

        }

        editItem(weaponItem);

        return weaponItem;

    }

}
