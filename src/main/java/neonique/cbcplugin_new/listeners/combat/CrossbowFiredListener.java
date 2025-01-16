package neonique.cbcplugin_new.listeners.combat;

import neonique.cbcplugin_new.CBCPlugin;
import neonique.cbcplugin_new.enums.WeaponType;
import neonique.cbcplugin_new.managers.GameManager;
import neonique.cbcplugin_new.managers.CombatManager;
import neonique.cbcplugin_new.playerclasses.CBCPlayer;
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

        // Check if the weapon fired was a crossbow
        ItemStack itemFired = e.getBow();
        assert itemFired != null;
        if (itemFired.getType() != Material.CROSSBOW) {
            return;
        }

        // Check if the entity who fired it was a piglin
        Entity entityFired = e.getEntity();
        if (entityFired instanceof Piglin) {
            piglinFired(e);
            return;
        }

        // Check if the entity who fired it was a player
        if (!(entityFired instanceof Player)) {
            return;
        }

        Player playerFired = (Player) entityFired;
        // Check if player is in players list
        if (!(this.gameManager.hasPlayer(playerFired))) {
            return;
        }

        CBCPlayer cbcPlayerFired = this.gameManager.getPlayer(playerFired);

        // Check what weapon player fired from
        ItemMeta itemFiredMeta = itemFired.getItemMeta();
        PersistentDataContainer itemFiredTags = itemFiredMeta.getPersistentDataContainer();
        String crossbowTypeString = itemFiredTags.get(new NamespacedKey(CBCPlugin.getPlugin(), "cbc_crossbow"), PersistentDataType.STRING);
        WeaponType crossbowType;

        if (Objects.equals(crossbowTypeString, "CREEPER")) {crossbowType = WeaponType.CREEPER;}
        else if (Objects.equals(crossbowTypeString, "FLAME")) {crossbowType = WeaponType.FLAME;}
        else if (Objects.equals(crossbowTypeString, "XBOW")) {crossbowType = WeaponType.XBOW;}
        else {return;}

        // Fire the weapon
        combatManager.weaponFired(cbcPlayerFired, e, crossbowType);

        // Play fired crossbow sound
        playerFired.playSound(playerFired.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, 10, 2);

    }

    public void piglinFired (EntityShootBowEvent e) {

        ItemStack itemFired = e.getBow();
        assert itemFired != null;

        // Check what weapon player fired from
        ItemMeta itemFiredMeta = itemFired.getItemMeta();
        Component displayName = itemFiredMeta.displayName();

        if (!(displayName instanceof TextComponent)) {
            return;
        }

        TextComponent text = (TextComponent) displayName;

        if (text.content().equals("X-Bow")) {
            combatManager.fireXBowPiglin(e);
        }


    }
}
