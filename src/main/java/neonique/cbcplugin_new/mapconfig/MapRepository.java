package neonique.cbcplugin_new.mapconfig;

import neonique.cbcplugin_new.core.CBCGamemode;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class MapRepository {

    private final Map<String, CBCMapData> maps = new HashMap<>();
    private final Map<CBCGamemode, Map<String, GamemodeMapData>> gamemodeMaps = new HashMap<>();

    public Map<String, CBCMapData> allMaps () {
        return Collections.unmodifiableMap(maps);
    }

    public Map<String, GamemodeMapData> allGamemodeMaps (CBCGamemode gamemode) {
        return Collections.unmodifiableMap(gamemodeMaps.getOrDefault(gamemode, Map.of()));
    }

    public CBCMapData getMap (String mapId) {
        return maps.get(mapId);
    }

    public void clearAll () {
        maps.clear();
        gamemodeMaps.clear();
    }

    public void addMap (CBCMapData mapData) {
        maps.put(mapData.id(), mapData);
    }

    public void addMaps (Collection<CBCMapData> m) {
        m.forEach(i -> maps.put(i.id(), i));
    }

    public void addGamemodeMap (CBCGamemode gamemode, GamemodeMapData mapData) {
        gamemodeMaps.computeIfAbsent(gamemode, k -> new HashMap<>())
                .put(mapData.mapData().id(), mapData);
    }

    public void addGamemodeMaps (CBCGamemode gamemode, Collection<GamemodeMapData> m) {
        var gamemodeList = gamemodeMaps.computeIfAbsent(gamemode, k -> new HashMap<>());
        m.forEach(i -> gamemodeList.put(i.mapData().id(), i));
    }

    public Collection<CBCGamemode> allGamemodes () {
        return gamemodeMaps.keySet();
    }

}
