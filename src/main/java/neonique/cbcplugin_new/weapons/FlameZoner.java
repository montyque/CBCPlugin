package neonique.cbcplugin_new.weapons;

import neonique.cbcplugin_new.CBCPlugin;
import neonique.cbcplugin_new.core.CBCPlayer;
import neonique.cbcplugin_new.weapons.presets.FlameZonerSettings;
import neonique.cbcplugin_new.weapons.projectiles.FlameArrow;
import neonique.cbcplugin_new.weapons.projectiles.Projectile;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.*;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Arrow;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.CrossbowMeta;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.function.Consumer;

import static neonique.cbcplugin_new.resourcepack.ResourcePackManager.noShadowText;

public class FlameZoner implements CrossbowWeapon {

    private final WeaponReloader weaponReloader;
    private final FlameZonerSettings settings;

    public FlameZoner(FlameZonerSettings settings) {

        this.settings = settings;

        weaponReloader = new WeaponReloader();
        weaponReloader.setReloadTime(settings.reloadTicks());

    }

    @Override
    public void fireWeapon (CBCPlayer player, Arrow arrowFired, Consumer<Projectile> projectileRegistry) {
        Projectile projectile = fireProjectile(player, arrowFired);
        projectileRegistry.accept(projectile);
        weaponReloader.startReload();
    }

    @Override
    public ItemStack getWeaponItem() {

        // Create crossbow weapon
        ItemStack weaponItem = new ItemStack(Material.CROSSBOW);
        CrossbowMeta itemMeta = (CrossbowMeta) weaponItem.getItemMeta();
        Component itemTitle = Component.text("Flame Zoner").color(TextColor.color(232, 98, 58))
                .decoration(TextDecoration.ITALIC, TextDecoration.State.FALSE);
        itemMeta.displayName(itemTitle);
        itemMeta.addEnchant(Enchantment.QUICK_CHARGE, 10, true);

        PersistentDataContainer itemTags = itemMeta.getPersistentDataContainer();

        if (weaponReloader.isLoaded()) {
            // Loads crossbow so the player is able to fire it
            ItemStack arrow = new ItemStack(Material.ARROW);
            itemMeta.addChargedProjectile(arrow);
            itemTags.set(new NamespacedKey(CBCPlugin.getPlugin(), "cbc_loaded"), PersistentDataType.INTEGER, 1);
            itemMeta.setCustomModelData(5);
            weaponItem.setItemMeta(itemMeta);
        }
        else {
            // Changes the damage bar on the weapon depending on how much it has loaded
            float reloadPercentage = weaponReloader.getReloadPercentage();
            itemTags.set(new NamespacedKey(CBCPlugin.getPlugin(), "cbc_loaded"), PersistentDataType.INTEGER, 0);

            // Changes the sprite of the weapon depending on how much it has loaded
            if (reloadPercentage > 0.7) {
                itemMeta.setCustomModelData(8);
            }
            else if (reloadPercentage > 0.4) {
                itemMeta.setCustomModelData(7);
            }
            else if (reloadPercentage > 0.1) {
                itemMeta.setCustomModelData(6);
            }
            else {
                itemMeta.setCustomModelData(5);
            }

            weaponItem.setItemMeta(itemMeta);

            Damageable damageableMeta = (Damageable) weaponItem.getItemMeta();
            damageableMeta.setDamage(Math.round((1.0f - reloadPercentage) * 465.0f));
            weaponItem.setItemMeta(damageableMeta);
        }

        return weaponItem;

    }

    @Override
    public Projectile fireProjectile (CBCPlayer player, Arrow arrowFired) {

        arrowFired.setPickupStatus(AbstractArrow.PickupStatus.DISALLOWED);
        arrowFired.setInvulnerable(true);
        arrowFired.setDamage(1);
        arrowFired.setPierceLevel(20);
        arrowFired.setGlowing(true);

        return new FlameArrow(player, arrowFired, settings.zoneRadius(), settings.zoneLifeTicks());

    }

    @Override
    public WeaponReloader getWeaponReloader() {
        return weaponReloader;
    }

    @Override
    public Component getXPBarComponent() {

        int charNum = (int) Math.ceil(weaponReloader.getReloadPercentage() * 60.0) + 57600;
        Component xpBarComponent = Component.text(
                String.valueOf((char) charNum)).style(Style.style().font(Key.key("cbc_customfonts", "xpreloadbars"))
        );

        xpBarComponent = noShadowText(xpBarComponent);
        return xpBarComponent;

    }



}
