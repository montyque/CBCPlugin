package neonique.cbcplugin_new.core;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public interface TeamLike {

    String id ();
    String name ();
    TeamColor teamColor ();

    default NamedTextColor textColor () {
        return teamColor().color();
    }

    default ItemStack getIconItem () {
        ItemStack item = teamColor().getLetterItem();
        ItemMeta meta = item.getItemMeta();
        Component itemTitle = Component.text(name()).color(textColor())
                .decorate(TextDecoration.BOLD)
                .decoration(TextDecoration.ITALIC, TextDecoration.State.FALSE);
        meta.displayName(itemTitle);
        item.setItemMeta(meta);
        return item;
    }

    default ItemStack getGlassHead() {
        ItemStack item = teamColor().getGlassHeadItem();
        ItemMeta itemMeta = item.getItemMeta();
        Component itemTitle = Component.text(name() + " Glass Head").color(textColor())
                .decorate(TextDecoration.BOLD)
                .decoration(TextDecoration.ITALIC, TextDecoration.State.FALSE);
        itemMeta.displayName(itemTitle);
        itemMeta.addEnchant(Enchantment.BINDING_CURSE, 1, false);
        item.setItemMeta(itemMeta);
        return item;
    }

}
