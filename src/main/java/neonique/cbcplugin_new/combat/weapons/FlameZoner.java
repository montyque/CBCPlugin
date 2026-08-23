package neonique.cbcplugin_new.combat.weapons;

import neonique.cbcplugin_new.CBCPlugin;
import neonique.cbcplugin_new.core.CBCPlayer;
import neonique.cbcplugin_new.combat.weapons.presets.FlameZonerSettings;
import neonique.cbcplugin_new.combat.projectiles.FlameArrow;
import neonique.cbcplugin_new.combat.projectiles.Projectile;
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
import org.bukkit.plugin.Plugin;

import java.util.function.Consumer;

import static neonique.cbcplugin_new.resourcepack.ResourcePackManager.noShadowText;

public class FlameZoner implements CrossbowWeapon {

    public final static NamespacedKey MODEL = new NamespacedKey("cbc", "flame_zoner");

    private final WeaponReloader weaponReloader;
    private final FlameZonerSettings settings;

    public FlameZoner(FlameZonerSettings settings) {

        this.settings = settings;

        weaponReloader = new WeaponReloader();
        weaponReloader.setReloadTime(settings.reloadTicks());

    }

    @Override
    public void fireWeapon (Plugin plugin, CBCPlayer player, Arrow arrowFired, Consumer<Projectile> projectileRegistry) {
        Projectile projectile = fireProjectile(plugin, player, arrowFired);
        projectileRegistry.accept(projectile);
        weaponReloader.startReload();
    }

    @Override
    public void editItem (ItemStack item) {

        item.editMeta(m -> {
            m.displayName(
                    Component.text("Flame Zoner").color(TextColor.color(232, 98, 58))
                            .decoration(TextDecoration.ITALIC, TextDecoration.State.FALSE)
            );
            m.setItemModel(MODEL);
        });

    }

    @Override
    public Projectile fireProjectile (Plugin plugin, CBCPlayer player, Arrow arrowFired) {

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
