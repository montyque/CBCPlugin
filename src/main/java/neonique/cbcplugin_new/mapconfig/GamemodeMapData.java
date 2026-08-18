package neonique.cbcplugin_new.mapconfig;

import org.bukkit.Material;

public interface GamemodeMapData extends MapData {

    CBCMapData mapData();

    default String id () {
        return mapData().id();
    }

    default String name () {
        return mapData().name();
    }

    default Material blockSymbol () {
        return mapData().blockSymbol();
    }


}
