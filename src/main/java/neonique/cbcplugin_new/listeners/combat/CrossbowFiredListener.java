package neonique.cbcplugin_new.listeners.combat;

import neonique.cbcplugin_new.CBCPlugin;
import neonique.cbcplugin_new.enums.WeaponType;
import neonique.cbcplugin_new.managers.GameManager;
import neonique.cbcplugin_new.managers.CombatManager;
import neonique.cbcplugin_new.playerclasses.CBCPlayer;
import neonique.cbcplugin_new.weapons.CrossbowWeapon;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityShootBowEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.Objects;

public class CrossbowFiredListener implements Listener {

    private final GameManager gameManager;
    private final CombatManager combatManager;

    public CrossbowFiredListener(GameManager gameManager, CombatManager combatManager) {
        this.gameManager = gameManager;
        this.combatManager = combatManager;
    }

    @EventHandler
    public void onBowFired(EntityShootBowEvent e) {

        ItemStack itemFired = e.getBow();
        Entity projectileFired = e.getProjectile();

        assert itemFired != null;

        if (itemFired.getType() != Material.CROSSBOW) {
            return;
        }

        if (!(projectileFired instanceof Arrow arrowFired)) {
            return;
        }

        Entity entityFired = e.getEntity();
        if (!(entityFired instanceof Player playerFired)) {
            return;
        }

        if (!(this.gameManager.hasPlayer(playerFired))) {
            return;
        }

        CBCPlayer cbcPlayerFired = this.gameManager.getPlayer(playerFired);

        // Check what weapon player fired from
        ItemMeta itemFiredMeta = itemFired.getItemMeta();
        PersistentDataContainer itemFiredTags = itemFiredMeta.getPersistentDataContainer();
        Integer playerWeaponId = itemFiredTags.get(
                new NamespacedKey(CBCPlugin.getPlugin(), "cbc_weapon_id"),
                PersistentDataType.INTEGER
        );

        CrossbowWeapon weaponFired = cbcPlayerFired.getWeaponFromId(playerWeaponId);
        weaponFired.fireWeapon(arrowFired);

        // Play fired crossbow sound
        playerFired.playSound(playerFired.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 10, 2);

    }

}
