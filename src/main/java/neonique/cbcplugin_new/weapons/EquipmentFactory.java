package neonique.cbcplugin_new.weapons;

import neonique.cbcplugin_new.core.CBCTeam;
import neonique.cbcplugin_new.services.ArmorTrimService;
import neonique.cbcplugin_new.core.CBCPlayer;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ArmorMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.trim.ArmorTrim;
import org.bukkit.inventory.meta.trim.TrimMaterial;
import org.bukkit.inventory.meta.trim.TrimPattern;

public class EquipmentFactory {

    public ArmorTrimService trimService;

    public EquipmentFactory (ArmorTrimService trimService) {
        this.trimService = trimService;
    }

    public ItemStack getChestplate (CBCPlayer player) {

        // Create netherite chestplate
        ItemStack chestplate = new ItemStack(Material.NETHERITE_CHESTPLATE);
        ItemMeta chestplateMeta = chestplate.getItemMeta();
        ArmorMeta armorChestplateMeta = (ArmorMeta) chestplateMeta;
        chestplateMeta.addEnchant(Enchantment.BINDING_CURSE, 1, true);

        // Set the trim of the chestplate
        CBCTeam<?> team = player.getTeam();
        if (team != null) {
            TrimMaterial material = team.getTrimMaterial();
            TrimPattern pattern = trimService.getPlayerTrim(player.getOfflinePlayer().getUniqueId());
            ArmorTrim armorTrim = new ArmorTrim(material, pattern);
            armorChestplateMeta.setTrim(armorTrim);
        }

        chestplateMeta.setUnbreakable(true);
        chestplate.setItemMeta(chestplateMeta);
        return chestplate;

    }

    public ItemStack getHelmet (CBCPlayer player) {

        CBCTeam<?> team = player.getTeam();
        if (team != null) {
            return team.getGlassHead();
        }
        return null;

    }

}
