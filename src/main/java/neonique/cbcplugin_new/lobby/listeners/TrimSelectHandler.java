package neonique.cbcplugin_new.lobby.listeners;

import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import neonique.cbcplugin_new.services.ArmorTrimService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.*;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ArmorMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.trim.ArmorTrim;

import static neonique.cbcplugin_new.util.StringUtil.firstLetterUpper;

public class TrimSelectHandler implements Listener {

    private final ArmorTrimService trimManager;

    public TrimSelectHandler (ArmorTrimService trimManager) {
        this.trimManager = trimManager;
    }

    @EventHandler
    public void armorStandClickEvent(PlayerInteractAtEntityEvent e) {

        Entity entityHit = e.getRightClicked();

        // Entity hit must be armor stand, damaging entity must be player
        if (entityHit.getType() != EntityType.ARMOR_STAND) return;

        // Get player and armor stand hit
        ArmorStand standHit = (ArmorStand) entityHit;
        Player player = e.getPlayer();

        // Check if stand hit has a certain tag
        if (!standHit.getScoreboardTags().contains("ARMORY_STAND")) return;
        // Cancel event
        e.setCancelled(true);

        // Get chestplate on armor stand
        ItemStack standChestplate = standHit.getEquipment().getChestplate();
        if (standChestplate == null) return;

        ItemMeta meta = standChestplate.getItemMeta();
        ArmorMeta armorMeta = (ArmorMeta) meta;

        // Check if chestplate has a trim
        ArmorTrim armorTrim = armorMeta.getTrim();
        if (armorTrim == null) return;

        // Check if trim has ID
        NamespacedKey key = RegistryAccess.registryAccess().getRegistry(RegistryKey.TRIM_PATTERN).getKey(armorTrim.getPattern());
        if (key == null) return;
        String trimName = key.getKey();

        // Set player's armor trim pattern
        trimManager.setTrim(player, armorTrim.getPattern());

        // Display to player using sound
        Location armorStandLoc = standHit.getLocation().add(0, 1.1, 0);
        player.playSound(armorStandLoc, Sound.BLOCK_ENCHANTMENT_TABLE_USE, 2, 1);

        // Display to player using particles
        player.spawnParticle(Particle.GLOW, armorStandLoc, 20, 0.2, 0.4, 0.2, 0.2);

        // Put message in chat
        player.sendMessage(Component.text("Set in-game armor trim pattern to ").color(NamedTextColor.YELLOW)
                .append(Component.text(firstLetterUpper(trimName)).color(NamedTextColor.AQUA)));

    }

}
