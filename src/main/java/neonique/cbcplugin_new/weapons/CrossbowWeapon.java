package neonique.cbcplugin_new.weapons;

import neonique.cbcplugin_new.combat.ProjectileManager;
import neonique.cbcplugin_new.core.CBCPlayer;
import neonique.cbcplugin_new.weapons.projectiles.Projectile;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Arrow;
import org.bukkit.inventory.ItemStack;

import java.util.function.Consumer;

public interface CrossbowWeapon {

    void fireWeapon (CBCPlayer player, Arrow arrowFired, Consumer<Projectile> projectileRegistry);

    ItemStack getWeaponItem ();

    Projectile fireProjectile (CBCPlayer player, Arrow arrowFired);

    WeaponReloader getWeaponReloader();

    Component getXPBarComponent();

}
