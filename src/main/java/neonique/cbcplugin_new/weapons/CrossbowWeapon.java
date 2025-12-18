package neonique.cbcplugin_new.weapons;

import neonique.cbcplugin_new.playerclasses.CBCPlayer;
import neonique.cbcplugin_new.weapons.presets.WeaponPreset;
import neonique.cbcplugin_new.weapons.projectiles.Projectile;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Arrow;
import org.bukkit.inventory.ItemStack;

public interface CrossbowWeapon {

    void fireWeapon (Arrow arrowFired);

    ItemStack getWeaponItem ();

    Projectile fireProjectile (Arrow arrowFired);

    CBCPlayer getPlayer();

    WeaponReloader getWeaponReloader();

    Component getXPBarComponent();

}
