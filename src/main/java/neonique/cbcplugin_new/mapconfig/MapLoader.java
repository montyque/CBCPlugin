package neonique.cbcplugin_new.mapconfig;

import neonique.cbcplugin_new.gamemodes.CBCGamemode;
import neonique.cbcplugin_new.managers.GameManager;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MapLoader {

    private final MapMechanicLoader mechanicsLoader;
    private final GameManager gameManager;
    private final Logger logger;

    public MapLoader (GameManager gameManager, MapMechanicLoader loader, Logger logger) {
        this.gameManager = gameManager;
        this.mechanicsLoader = loader;
        this.logger = logger;
    }

    public Map<String, CBCMap> loadMapsFromDirectory (File mapDirectory) {
        return loadMaps(getYamlFiles(mapDirectory));
    }

    public Map<String, CBCMap> loadMaps (Collection<File> mapFiles) {

        Map<String, CBCMap> maps = new HashMap<>();

        for (File mapFile : mapFiles) {
            try {
                CBCMap map = loadMap(mapFile);
                maps.put(map.id(), map);
            } catch (InvalidMapConfigException e) {
                logger.log(Level.WARNING, e.getMessage(), e.getCause());
            }
        }

        return maps;

    }

    public CBCMap loadMap (File mapFile) {
        String mapId = mapFile.getName();
        try {
            YamlConfiguration ymlMapConfig = YamlConfiguration.loadConfiguration(mapFile);
            if (ymlMapConfig.getKeys(false).isEmpty()) {
                throw new IllegalArgumentException("Config is empty or could not be parsed into YAMLConfiguration");
            }
            return new CBCMap(gameManager.getWorld(), ymlMapConfig, mechanicsLoader);
        } catch (Exception e) {
            throw new InvalidMapConfigException(mapId, e);
        }
    }

    public Map<String, GamemodeMapData> loadGamemodeMapsFromDirectory (CBCGamemode gamemode,
                                                                Map<String, CBCMap> maps,
                                                                File mapDirectory) {
        return loadGamemodeMaps(gamemode, maps, getYamlFiles(mapDirectory));
    }

    private List<File> getYamlFiles(File mapDirectory) {
        File[] dirFiles = mapDirectory.listFiles(file -> {
            String name = file.getName().toLowerCase();
            return file.isFile() && (name.endsWith(".yaml") || name.endsWith(".yml"));
        });

        if (dirFiles == null)
            throw new IllegalArgumentException("Given file is not a directory, or an I/O error has occurred.");

        return Arrays.asList(dirFiles);

    }

    public Map<String, GamemodeMapData> loadGamemodeMaps (CBCGamemode gamemode,
                                                   Map<String, CBCMap> maps,
                                                   Collection<File> mapFiles) {

        Map<String, GamemodeMapData> gamemodeMapDataList = new HashMap<>();

        for (File mapFile : mapFiles) {
            try {
                GamemodeMapData mapData = loadGamemodeMapData(gamemode, maps, mapFile);
                gamemodeMapDataList.put(mapData.mapData().id(), mapData);
            } catch (InvalidMapConfigException e) {
                logger.log(Level.WARNING, e.getMessage(), e.getCause());
            }
        }

        return gamemodeMapDataList;

    }

    public GamemodeMapData loadGamemodeMapData (CBCGamemode gamemode,
                                                Map<String, CBCMap> maps,
                                                File file) {

        String mapId = file.getName();
        try {

            // Parse YAML file
            YamlConfiguration ymlMapConfig = YamlConfiguration.loadConfiguration(file);
            if (ymlMapConfig.getKeys(false).isEmpty()) {
                throw new IllegalArgumentException("Config is empty or could not be parsed into YAMLConfiguration");
            }

            // Retrieve base map
            CBCMap baseMap = maps.get(mapId);
            if (baseMap == null) {
                throw new IllegalArgumentException("No base map exists with map id '" + mapId + "'");
            }

            // Parse YAML config into gamemode map data
            return gamemode.mapDataFromConfig(baseMap, ymlMapConfig);

        } catch (Exception e) {
            throw new InvalidMapConfigException(gamemode, mapId, e);
        }

    }

}
