package neonique.cbcplugin_new.combat.weapons;

import neonique.cbcplugin_new.CBCPlugin;
import neonique.cbcplugin_new.combat.weapons.presets.CreeperCannonSettings;
import neonique.cbcplugin_new.core.CBCPlayer;
import neonique.cbcplugin_new.combat.projectiles.CBCCreeper;
import neonique.cbcplugin_new.combat.projectiles.Projectile;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.ShadowColor;
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
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;

import java.util.function.Consumer;

import static neonique.cbcplugin_new.resourcepack.ResourcePackManager.noShadowText;

public class CreeperCannon implements CrossbowWeapon {

    public final static NamespacedKey MODEL = new NamespacedKey("cbc", "creeper_cannon");

    public final static NamespacedKey horKbKey = new NamespacedKey("cbcplugin", "hor_kb");
    public final static NamespacedKey verKbKey = new NamespacedKey("cbcplugin", "ver_kb");
    public final static NamespacedKey allyDamageRatioKey = new NamespacedKey("cbcplugin", "ally_dmg_ratio");

    private final WeaponReloader weaponReloader;
    private final CreeperCannonSettings settings;

    public CreeperCannon (CreeperCannonSettings settings) {

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
                    Component.text("Creeper Cannon").color(TextColor.color(91, 183, 34))
                            .decoration(TextDecoration.ITALIC, TextDecoration.State.FALSE)
            );
            m.setItemModel(MODEL);
        });

    }

    @Override
    public Projectile fireProjectile (Plugin plugin, CBCPlayer player, Arrow arrowFired) {

        arrowFired.setDamage(0);
        Vector arrowVelocity = arrowFired.getVelocity();
        Location creeperSpawnLocation = arrowFired.getLocation();
        Vector creeperVelocity = arrowVelocity.multiply(settings.launchVelocityModifier());
        World world = arrowFired.getWorld();

        Creeper creeper = world.createEntity(creeperSpawnLocation, Creeper.class);
        creeper.setInvulnerable(true);
        creeper.setPowered(true);
        creeper.setVelocity(creeperVelocity);

        world.addEntity(creeper);

        arrowFired.remove();

        // Change name of creeper depending on team name
        if (player.team() != null) {
            creeper.customName(Component.text(player.team().name() + "Creeper"));
        }

        // Add data to creeper used when creeper does damage
        PersistentDataContainer data = creeper.getPersistentDataContainer();
        data.set(horKbKey, PersistentDataType.DOUBLE, settings.horizontalKnockbackCoefficient());
        data.set(verKbKey, PersistentDataType.DOUBLE, settings.verticalKnockbackCoefficient());
        data.set(allyDamageRatioKey, PersistentDataType.DOUBLE, settings.allyDamageModifier());

        return new CBCCreeper(player, creeper);

    }

    @Override
    public WeaponReloader getWeaponReloader() {
        return weaponReloader;
    }

    @Override
    public Component getXPBarComponent() {

        int charNum = Math.round(weaponReloader.getReloadPercentage() * 60.0f) + 57344;
        return Component.text(
                        String.valueOf((char) charNum)).style(Style.style().font(Key.key("cbc_customfonts", "xpreloadbars")))
                .shadowColor(ShadowColor.shadowColor(0));

    }

}
