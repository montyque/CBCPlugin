package neonique.cbcplugin_new.combat.weapons;

import neonique.cbcplugin_new.CBCPlugin;
import neonique.cbcplugin_new.combat.weapons.presets.CreeperCannonSettings;
import neonique.cbcplugin_new.core.CBCPlayer;
import neonique.cbcplugin_new.combat.projectiles.CBCCreeper;
import neonique.cbcplugin_new.combat.projectiles.Projectile;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.*;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.EntityType;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.CrossbowMeta;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.util.Vector;

import java.util.function.Consumer;

import static neonique.cbcplugin_new.resourcepack.ResourcePackManager.noShadowText;

public class CreeperCannon implements CrossbowWeapon {

    public final static NamespacedKey horKbKey = new NamespacedKey(CBCPlugin.getPlugin(), "hor_kb");
    public final static NamespacedKey verKbKey = new NamespacedKey(CBCPlugin.getPlugin(), "ver_kb");
    public final static NamespacedKey allyDamageRatioKey = new NamespacedKey(CBCPlugin.getPlugin(), "ally_dmg_ratio");

    private final WeaponReloader weaponReloader;
    private final CreeperCannonSettings settings;

    public CreeperCannon (CreeperCannonSettings settings) {

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
        Component itemTitle = Component.text("Creeper Cannon").color(TextColor.color(91, 183, 34))
                .decoration(TextDecoration.ITALIC, TextDecoration.State.FALSE);
        itemMeta.displayName(itemTitle);
        itemMeta.addEnchant(Enchantment.QUICK_CHARGE, 10, true);

        PersistentDataContainer itemTags = itemMeta.getPersistentDataContainer();

        if (weaponReloader.isLoaded()) {

            // Loads crossbow so the player is able to fire it
            ItemStack ccProjectile = new ItemStack(Material.ARROW);
            itemMeta.addChargedProjectile(ccProjectile);
            itemTags.set(new NamespacedKey(CBCPlugin.getPlugin(), "cbc_loaded"), PersistentDataType.INTEGER, 1);
            itemMeta.setCustomModelData(1);
            weaponItem.setItemMeta(itemMeta);

        }
        else {

            // Changes the damage bar on the weapon depending on how much it has loaded
            float reloadPercentage = weaponReloader.getReloadPercentage();
            itemTags.set(new NamespacedKey(CBCPlugin.getPlugin(), "cbc_loaded"), PersistentDataType.INTEGER, 0);

            // Changes the sprite of the weapon depending on how much it has loaded
            if (reloadPercentage > 0.7) {
                itemMeta.setCustomModelData(4);
            }
            else if (reloadPercentage > 0.4) {
                itemMeta.setCustomModelData(3);
            }
            else if (reloadPercentage > 0.1) {
                itemMeta.setCustomModelData(2);
            }
            else {
                itemMeta.setCustomModelData(1);
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

        arrowFired.setDamage(0);
        Vector arrowVelocity = arrowFired.getVelocity();
        Location creeperSpawnLocation = arrowFired.getLocation();
        World world = arrowFired.getWorld();
        Creeper creeperFired = (Creeper) world.spawnEntity(new Location(world, 0, 100, 0), EntityType.CREEPER,
                CreatureSpawnEvent.SpawnReason.CUSTOM,
                creeper -> {
                    creeper.setVelocity(arrowVelocity.multiply(settings.launchVelocityModifier()));
                    creeper.setInvulnerable(true);
                }
        );

        creeperFired.setPowered(true);
        creeperFired.setExplosionRadius(settings.explosionRadius());
        creeperFired.teleport(creeperSpawnLocation);
        arrowFired.remove();

        // Change name of creeper depending on team name
        if (player.team() != null) {
            creeperFired.customName(Component.text(player.team().name() + "Creeper"));
        }

        // Add data to creeper used when creeper does damage
        PersistentDataContainer data = creeperFired.getPersistentDataContainer();
        data.set(horKbKey, PersistentDataType.DOUBLE, settings.horizontalKnockbackCoefficient());
        data.set(verKbKey, PersistentDataType.DOUBLE, settings.verticalKnockbackCoefficient());
        data.set(allyDamageRatioKey, PersistentDataType.DOUBLE, settings.allyDamageModifier());

        return new CBCCreeper(player, creeperFired);

    }

    @Override
    public WeaponReloader getWeaponReloader() {
        return weaponReloader;
    }

    @Override
    public Component getXPBarComponent() {

        int charNum = Math.round(weaponReloader.getReloadPercentage() * 60.0f) + 57344;
        Component xpBarComponent = Component.text(
                String.valueOf((char) charNum)).style(Style.style().font(Key.key("cbc_customfonts", "xpreloadbars"))
        );

        xpBarComponent = noShadowText(xpBarComponent);
        return xpBarComponent;

    }

}
