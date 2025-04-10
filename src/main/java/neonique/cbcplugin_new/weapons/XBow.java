package neonique.cbcplugin_new.weapons;

import neonique.cbcplugin_new.CBCPlugin;
import neonique.cbcplugin_new.managers.CBCScoreboardManager;
import neonique.cbcplugin_new.playerclasses.CBCPlayer;
import neonique.cbcplugin_new.weapons.presets.XbowPreset;
import neonique.cbcplugin_new.weapons.projectiles.Projectile;
import neonique.cbcplugin_new.weapons.projectiles.XbowArrow;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Arrow;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.CrossbowMeta;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import static neonique.cbcplugin_new.resourcepack.ResourcePackManager.noShadowText;

public class XBow implements CrossbowWeapon {

    private final CBCPlayer owner;
    private final WeaponReloader weaponReloader;
    private final XbowPreset weaponOptions;

    public XBow(CBCPlayer player, XbowPreset weaponPreset) {

        owner = player;
        weaponReloader = new WeaponReloader();

        weaponOptions = weaponPreset;
        weaponReloader.setReloadTime(weaponOptions.getReloadTicks());

    }

    @Override
    public void fireWeapon(Arrow arrowFired) {

        Projectile projectile = fireProjectile(arrowFired);
        owner.getGameManager().getCombatManager().getProjectileManager().addProjectile(projectile);
        weaponReloader.startReload();

    }

    @Override
    public ItemStack getWeaponItem(int weaponId) {

        // Create crossbow weapon
        ItemStack weaponItem = new ItemStack(Material.CROSSBOW);
        CrossbowMeta itemMeta = (CrossbowMeta) weaponItem.getItemMeta();
        Component itemTitle = Component.text("X-Bow").color(TextColor.color(124, 226, 226))
                .decoration(TextDecoration.ITALIC, TextDecoration.State.FALSE);
        itemMeta.displayName(itemTitle);
        itemMeta.addEnchant(Enchantment.QUICK_CHARGE, 10, true);

        PersistentDataContainer itemTags = itemMeta.getPersistentDataContainer();
        itemTags.set(new NamespacedKey(CBCPlugin.getPlugin(), "cbc_weapon_id"), PersistentDataType.INTEGER, weaponId);

        if (weaponReloader.isLoaded()) {
            // Loads crossbow so the player is able to fire it
            ItemStack arrow = new ItemStack(Material.ARROW);
            itemMeta.addChargedProjectile(arrow);
            itemTags.set(new NamespacedKey(CBCPlugin.getPlugin(), "cbc_loaded"), PersistentDataType.INTEGER, 1);
            itemMeta.setCustomModelData(9);
            weaponItem.setItemMeta(itemMeta);
        }
        else {
            // Changes the damage bar on the weapon depending on how much it has loaded
            float reloadPercentage = weaponReloader.getReloadPercentage();
            itemTags.set(new NamespacedKey(CBCPlugin.getPlugin(), "cbc_loaded"), PersistentDataType.INTEGER, 0);

            // Changes the sprite of the weapon depending on how much it has loaded
            if (reloadPercentage > 0.7) {
                itemMeta.setCustomModelData(12);
            }
            else if (reloadPercentage > 0.4) {
                itemMeta.setCustomModelData(11);
            }
            else if (reloadPercentage > 0.1) {
                itemMeta.setCustomModelData(10);
            }
            else {
                itemMeta.setCustomModelData(9);
            }

            weaponItem.setItemMeta(itemMeta);

            Damageable damageableMeta = (Damageable) weaponItem.getItemMeta();
            damageableMeta.setDamage(Math.round((1.0f - reloadPercentage) * 465.0f));
            weaponItem.setItemMeta(damageableMeta);

        }

        return weaponItem;

    }

    @Override
    public Projectile fireProjectile(Arrow arrowFired) {

        arrowFired.setPickupStatus(AbstractArrow.PickupStatus.DISALLOWED);
        arrowFired.setInvulnerable(true);
        arrowFired.setDamage(1);
        arrowFired.setPierceLevel(127);
        arrowFired.setGlowing(true);
        arrowFired.setVelocity(arrowFired.getVelocity().multiply(weaponOptions.getArrowVelocityModifier()));
        CBCScoreboardManager.getInstance().addTeamEntry(arrowFired.getUniqueId().toString(), "xbowArrows");

        return new XbowArrow(owner, arrowFired);

    }

    @Override
    public CBCPlayer getPlayer() {
        return owner;
    }

    @Override
    public WeaponReloader getWeaponReloader() {
        return weaponReloader;
    }

    @Override
    public Component getXPBarComponent() {

        int charNum = (int) Math.ceil(weaponReloader.getReloadPercentage() * 60.0) + 57856;
        Component xpBarComponent = Component.text(
                String.valueOf((char) charNum)).style(Style.style().font(Key.key("cbc_customfonts", "xpreloadbars"))
        );

        xpBarComponent = noShadowText(xpBarComponent);
        return xpBarComponent;

    }

}
