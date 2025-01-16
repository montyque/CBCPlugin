package neonique.cbcplugin_new.enums;

import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.HashSet;
import java.util.Set;

public enum CBCGamemode {

    CTF ("Capture The Flag", 1, true, 1,
            TextColor.color(86, 197, 209)),

    SHOWDOWN ("Showdown", 2, true, 2,
            TextColor.color(255, 132, 66)),

    TDM ("Team Deathmatch", 3, true, 3,
            TextColor.color(227, 66, 255)),

    HOLDTHEGOLD ("Hold The Gold", 4, true, 4,
            TextColor.color(255, 239, 66)),

    FLAGRUSH ("Flag Rush", 5, true, 5,
            TextColor.color(75, 66, 255)),

    THROWDOWN ("Throwdown", 6, false, 1,
            TextColor.color(255, 66, 107)),

    KMATION ("Killimination", 7, false, 2,
            TextColor.color(66, 255, 72)),

    RENDEZVOUS ("Rendezvous", 8, true, 6,
            TextColor.color(66, 255, 185)),

    ASSASSIN ("Assassin", 9, false, 3,
            TextColor.color(255, 191, 0)),

    CBCTAG ("Crossbow Tag", 10, true, 7,
            TextColor.color(179, 255, 66)),

    KOTH ("King Of The Hill", 11, true, 8,
            TextColor.color(135, 66, 255));

    private final String gamemodeName;
    private final int gamemodeNum;
    private final boolean teamGamemode;
    private final int gamemodeIdInCategory;
    private final TextColor color;

    CBCGamemode(String gamemodeName, int gamemodeNum, boolean teamGamemode, int gamemodeIdInCategory, TextColor color) {
        this.gamemodeName = gamemodeName;
        this.gamemodeNum = gamemodeNum;
        this.teamGamemode = teamGamemode;
        this.gamemodeIdInCategory = gamemodeIdInCategory;
        this.color = color;
    }

    public static Set<String> getGamemodeIds () {
        Set<String> gamemodeIds = new HashSet<>();
        for (CBCGamemode gamemode : CBCGamemode.values()) {
            gamemodeIds.add(gamemode.toString().toUpperCase());
        }
        return gamemodeIds;
    }

    public int gamemodeNum() {
        return gamemodeNum;
    }

    public ItemStack getGamemodeIconItem () {

        ItemStack item = new ItemStack(Material.LIGHT_GRAY_DYE);
        ItemMeta itemMeta = item.getItemMeta();
        itemMeta.setCustomModelData(gamemodeNum);
        item.setItemMeta(itemMeta);
        return item;

    }

    public TextColor getColor () {
        return color;
    }
    public int getGamemodeIdInCategory() {return gamemodeIdInCategory;}

    public boolean isTeamGamemode() {return teamGamemode;}

    public String getGamemodeName() {
        return gamemodeName;
    }

    public String getUnicodeIcon (NamedTextColor color) {

        if (isTeamGamemode()) {
            final int startingUnicode = 57600;
            int gamemodeUnicode = startingUnicode + ((getGamemodeIdInCategory() - 1) * 16);

            if (color == NamedTextColor.BLUE) {
                gamemodeUnicode += 1;
            } else if (color == NamedTextColor.GREEN) {
                gamemodeUnicode += 2;
            } else if (color == NamedTextColor.YELLOW) {
                gamemodeUnicode += 3;
            } else if (color == NamedTextColor.AQUA) {
                gamemodeUnicode += 4;
            } else if (color == NamedTextColor.GOLD) {
                gamemodeUnicode += 5;
            } else if (color == NamedTextColor.LIGHT_PURPLE) {
                gamemodeUnicode += 6;
            } else if (color == NamedTextColor.DARK_PURPLE) {
                gamemodeUnicode += 7;
            } else if (color != NamedTextColor.RED) {
                gamemodeUnicode += 9;
            }

            return String.valueOf((char) (gamemodeUnicode));
        }
        else {
            int startingUnicode = 57840;
            return String.valueOf((char) (startingUnicode + getGamemodeIdInCategory() - 1));
        }

    }
}
