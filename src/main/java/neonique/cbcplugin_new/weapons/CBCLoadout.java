package neonique.cbcplugin_new.weapons;

import neonique.cbcplugin_new.weapons.presets.CreeperCannonSettings;
import neonique.cbcplugin_new.weapons.presets.FlameZonerSettings;
import neonique.cbcplugin_new.weapons.presets.XbowSettings;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ArmorMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.trim.ArmorTrim;
import org.bukkit.inventory.meta.trim.TrimMaterial;
import org.bukkit.inventory.meta.trim.TrimPattern;

import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class CBCLoadout {

    public static Map<Integer, Supplier<InventorySlot>> DEFAULT_WEAPONS = Map.of(
            0, () -> new WeaponSlot(new CreeperCannon(CreeperCannonSettings.DEFAULT)),
            1, () -> new WeaponSlot(new FlameZoner(FlameZonerSettings.DEFAULT)),
            2, () -> new WeaponSlot(new XBow(XbowSettings.DEFAULT))
    );

    private final Map<Integer, Supplier<InventorySlot>> hotbarSlots;
    private final Supplier<TrimPattern> trimPattern;
    private final Supplier<TrimMaterial> trimMaterial;
    private final Supplier<ItemStack> helmet;

    public CBCLoadout (Map<Integer, Supplier<InventorySlot>> hotbarSlots,
                       Supplier<TrimPattern> trimPattern,
                       Supplier<TrimMaterial> trimMaterial,
                       Supplier<ItemStack> helmet) {
        this.hotbarSlots = hotbarSlots;
        this.trimPattern = trimPattern;
        this.trimMaterial = trimMaterial;
        this.helmet = helmet;
    }

    public Map<Integer, InventorySlot> getSlots () {
        return hotbarSlots.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> e.getValue().get()
                ));
    }

    public ItemStack getChestplate () {

        ItemStack chestplate = new ItemStack(Material.NETHERITE_CHESTPLATE);
        ItemMeta chestplateMeta = chestplate.getItemMeta();
        ArmorMeta armorChestplateMeta = (ArmorMeta) chestplateMeta;
        chestplateMeta.addEnchant(Enchantment.BINDING_CURSE, 1, true);

        TrimMaterial material = trimMaterial.get();
        TrimPattern pattern = trimPattern.get();
        ArmorTrim armorTrim = new ArmorTrim(material, pattern);
        armorChestplateMeta.setTrim(armorTrim);

        chestplateMeta.setUnbreakable(true);
        chestplate.setItemMeta(chestplateMeta);
        return chestplate;

    }

    public ItemStack getHelmet () {
        return helmet.get();
    }

}
