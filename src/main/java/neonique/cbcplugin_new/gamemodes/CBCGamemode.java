package neonique.cbcplugin_new.gamemodes;

import neonique.cbcplugin_new.core.Game;
import neonique.cbcplugin_new.core.TeamColor;
import neonique.cbcplugin_new.gamemodes.assassin.AssassinGame;
import neonique.cbcplugin_new.gamemodes.crossbowtag.TagGame;
import neonique.cbcplugin_new.gamemodes.ctf.CTFGame;
import neonique.cbcplugin_new.gamemodes.ctf.CTFMapData;
import neonique.cbcplugin_new.gamemodes.ctf.CTFSettings;
import neonique.cbcplugin_new.gamemodes.holdthegold.HTGGame;
import neonique.cbcplugin_new.gamemodes.kmation.KMationGame;
import neonique.cbcplugin_new.gamemodes.koth.KOTHGame;
import neonique.cbcplugin_new.gamemodes.rendezvous.RendezvousGame;
import neonique.cbcplugin_new.gamemodes.showdown.ShowdownGame;
import neonique.cbcplugin_new.gamemodes.tdm.TDMGame;
import neonique.cbcplugin_new.gamemodes.throwdown.ThrowdownGame;
import neonique.cbcplugin_new.managers.GameManager;
import neonique.cbcplugin_new.mapconfig.CBCMap;
import neonique.cbcplugin_new.mapconfig.GamemodeMapData;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Material;
import org.bukkit.configuration.Configuration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.HashSet;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

public enum CBCGamemode {

    CTF ("Capture The Flag",
            1,
            true,
            1,
            TextColor.color(86, 197, 209),
            CTFGame::new,
            CTFMapData::new,
            CTFSettings::new
    ),

    SHOWDOWN ("Showdown",
            2,
            true,
            2,
            TextColor.color(255, 132, 66),
            ShowdownGame::new,
            null,
            null
    ),

    TDM ("Team Deathmatch",
            3,
            true,
            3,
            TextColor.color(227, 66, 255),
            TDMGame::new,
            null,
            null
    ),

    HOLDTHEGOLD ("Hold The Gold",
            4,
            true,
            4,
            TextColor.color(255, 239, 66),
            HTGGame::new,
            null,
            null
    ),

    THROWDOWN ("Throwdown",
            6,
            false,
            1,
            TextColor.color(255, 66, 107),
            ThrowdownGame::new,
            null,
            null
    ),

    KMATION ("Killimination",
            7,
            false,
            2,
            TextColor.color(66, 255, 72),
            KMationGame::new,
            null,
            null
    ),

    RENDEZVOUS ("Rendezvous",
            8,
            true,
            6,
            TextColor.color(66, 255, 185),
            RendezvousGame::new,
            null,
            null
    ),

    ASSASSIN ("Assassin",
            9,
            false,
            3,
            TextColor.color(255, 191, 0),
            AssassinGame::new,
            null,
            null
    ),

    CBCTAG ("Crossbow Tag",
            10,
            true,
            7,
            TextColor.color(179, 255, 66),
            TagGame::new,
            null,
            null
    ),

    KOTH ("King Of The Hill",
            11,
            true,
            8,
            TextColor.color(135, 66, 255),
            KOTHGame::new,
            null,
            null
    );




    private final String gamemodeName;
    private final int gamemodeNum;
    private final boolean teamGamemode;
    private final int gamemodeIdInCategory;
    private final TextColor color;
    private final Function<GameManager, Game<?>> gameFactory;
    private final BiFunction<CBCMap, Configuration, GamemodeMapData> gamemodeMapDataFactory;
    private final Supplier<GameSettings> defaultGameSettings;

    CBCGamemode (String gamemodeName,
                 int gamemodeNum,
                 boolean teamGamemode,
                 int gamemodeIdInCategory,
                 TextColor color,
                 Function<GameManager, Game<?>> gameFactory,
                 BiFunction<CBCMap, Configuration, GamemodeMapData> gamemodeMapDataFactory,
                 Supplier<GameSettings> defaultGameSettings) {

        this.gamemodeName = gamemodeName;
        this.gamemodeNum = gamemodeNum;
        this.teamGamemode = teamGamemode;
        this.gamemodeIdInCategory = gamemodeIdInCategory;
        this.color = color;
        this.gameFactory = gameFactory;
        this.gamemodeMapDataFactory = gamemodeMapDataFactory;
        this.defaultGameSettings = defaultGameSettings;

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

    public int getGamemodeIdInCategory() {
        return gamemodeIdInCategory;
    }

    public boolean isTeamGamemode() {
        return teamGamemode;
    }

    public String getGamemodeName() {
        return gamemodeName;
    }

    public String getIcon () {
        return getIcon((TeamColor) null);
    }

    public String getIcon (TeamColor color) {
        if (isTeamGamemode()) {
            return String.valueOf((char) (0xE100 + (gamemodeIdInCategory - 1) * 16 + (color != null ? color.num() : 9)));
        } else {
            return String.valueOf((char) (0xE1F0 + gamemodeIdInCategory - 1));
        }
    }

    public String getIcon (NamedTextColor color) {

        if (isTeamGamemode()) {
            final int startingUnicode = 0xE100;
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
            int startingUnicode = 0xE1F0;
            return String.valueOf((char) (startingUnicode + getGamemodeIdInCategory() - 1));
        }

    }

    public Game<?> newGameInstance (GameManager gameManager) {
        return gameFactory.apply(gameManager);
    }

    public GamemodeMapData mapDataFromConfig (CBCMap map, Configuration gamemodeConfig) {
        return gamemodeMapDataFactory.apply(map, gamemodeConfig);
    }

    public GameSettings defaultGameSettings () {
        return defaultGameSettings.get();
    }


}
