package neonique.cbcplugin_new.weapons;

import neonique.cbcplugin_new.CBCPlugin;
import neonique.cbcplugin_new.weapons.presets.CreeperPreset;
import neonique.cbcplugin_new.playerclasses.CBCPlayer;
import neonique.cbcplugin_new.weapons.projectiles.CBCCreeper;
import neonique.cbcplugin_new.weapons.projectiles.Projectile;
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

import static neonique.cbcplugin_new.resourcepack.ResourcePackManager.noShadowText;

public class CreeperCannon implements CrossbowWeapon {

    private final CBCPlayer owner;
    private final WeaponReloader weaponReloader;
    private final CreeperPreset weaponOptions;

    public CreeperCannon(CBCPlayer player, CreeperPreset creeperPreset) {
        owner = player;
        weaponReloader = new WeaponReloader();

        weaponOptions = creeperPreset;
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
        Component itemTitle = Component.text("Creeper Cannon").color(TextColor.color(91, 183, 34))
                .decoration(TextDecoration.ITALIC, TextDecoration.State.FALSE);
        itemMeta.displayName(itemTitle);
        itemMeta.addEnchant(Enchantment.QUICK_CHARGE, 10, true);

        PersistentDataContainer itemTags = itemMeta.getPersistentDataContainer();
        itemTags.set(new NamespacedKey(CBCPlugin.getPlugin(), "cbc_weapon_id"), PersistentDataType.INTEGER, weaponId);

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
    public Projectile fireProjectile(Arrow arrowFired) {

        arrowFired.setDamage(0);
        Vector arrowVelocity = arrowFired.getVelocity();
        Location creeperSpawnLocation = arrowFired.getLocation();
        World world = arrowFired.getWorld();
        Creeper creeperFired = (Creeper) world.spawnEntity(new Location(world, 0, 100, 0), EntityType.CREEPER,
                CreatureSpawnEvent.SpawnReason.CUSTOM,
                creeper -> {
                    creeper.setVelocity(arrowVelocity.multiply(weaponOptions.getLaunchVelocityModifier()));
                    creeper.setInvulnerable(true);
                }
        );

        creeperFired.setPowered(true);
        creeperFired.setExplosionRadius(weaponOptions.getCreeperExplosionRadius());
        creeperFired.teleport(creeperSpawnLocation);
        arrowFired.remove();

        // Change name of creeper depending on team name
        if (owner.getTeam() != null) {
            creeperFired.customName(Component.text(owner.getTeam().getTeamName() + "Creeper"));
        }

        return new CBCCreeper(owner, creeperFired);

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

        int charNum = Math.round(weaponReloader.getReloadPercentage() * 60.0f) + 57344;
        Component xpBarComponent = Component.text(
                String.valueOf((char) charNum)).style(Style.style().font(Key.key("cbc_customfonts", "xpreloadbars"))
        );

        xpBarComponent = noShadowText(xpBarComponent);
        return xpBarComponent;

    }

}
