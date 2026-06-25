package neonique.cbcplugin_new.core;

import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.trim.TrimMaterial;

public enum TeamColor {

    RED (0, NamedTextColor.RED, TrimMaterial.REDSTONE, Material.RED_STAINED_GLASS),
    BLUE (0, NamedTextColor.BLUE, TrimMaterial.LAPIS, Material.BLUE_STAINED_GLASS),
    GREEN (0, NamedTextColor.GREEN, TrimMaterial.EMERALD, Material.LIME_STAINED_GLASS),
    YELLOW (0, NamedTextColor.YELLOW, TrimMaterial.GOLD, Material.YELLOW_STAINED_GLASS),
    CYAN (0, NamedTextColor.AQUA, TrimMaterial.DIAMOND, Material.CYAN_STAINED_GLASS),
    ORANGE (0, NamedTextColor.GOLD, TrimMaterial.COPPER, Material.ORANGE_STAINED_GLASS),
    MAGENTA (0, NamedTextColor.LIGHT_PURPLE, TrimMaterial.IRON, Material.MAGENTA_STAINED_GLASS),
    PURPLE (0, NamedTextColor.DARK_PURPLE, TrimMaterial.AMETHYST, Material.PURPLE_STAINED_GLASS);

    private final int num;
    private final NamedTextColor color;
    private final TrimMaterial trimMat;
    private final Material glassMat;

    TeamColor (int num, NamedTextColor color, TrimMaterial trimMat, Material glassMat) {
        this.num = num;
        this.color = color;
        this.trimMat = trimMat;
        this.glassMat = glassMat;
    }

    public int num () {
        return num;
    }

    public NamedTextColor color () {
        return color;
    }

    public TrimMaterial trimMat () {
        return trimMat;
    }

    public ItemStack getGlassHeadItem () {
        ItemStack item = new ItemStack(glassMat);
        ItemMeta itemMeta = item.getItemMeta();
        itemMeta.addEnchant(Enchantment.BINDING_CURSE, 1, false);
        item.setItemMeta(itemMeta);
        return item;
    }

    public ItemStack getLetterItem () {
        ItemStack item = new ItemStack(Material.WHITE_DYE);
        ItemMeta itemMeta = item.getItemMeta();
        itemMeta.setCustomModelData(num + 1);
        item.setItemMeta(itemMeta);
        return item;
    }


}
