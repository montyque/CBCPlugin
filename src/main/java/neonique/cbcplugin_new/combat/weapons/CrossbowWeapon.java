package neonique.cbcplugin_new.combat.weapons;

import neonique.cbcplugin_new.core.CBCPlayer;
import neonique.cbcplugin_new.combat.projectiles.Projectile;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Arrow;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.util.function.Consumer;

public interface CrossbowWeapon {

    void fireWeapon (Plugin plugin, CBCPlayer player, Arrow arrowFired, Consumer<Projectile> projectileRegistry);

    ItemStack getWeaponItem ();

    Projectile fireProjectile (Plugin plugin, CBCPlayer player, Arrow arrowFired);

    WeaponReloader getWeaponReloader();

    Component getXPBarComponent();

}
