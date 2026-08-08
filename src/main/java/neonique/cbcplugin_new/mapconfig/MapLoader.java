package neonique.cbcplugin_new.mapconfig;

import neonique.cbcplugin_new.combat.display.DeathMessageLoader;
import neonique.cbcplugin_new.gamemodes.CBCGamemode;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MapLoader {

    private final MapMechanicLoader mechanicsLoader;
    private final DeathMessageLoader deathMessageLoader;
    private final Logger logger;

    public MapLoader (MapMechanicLoader loader, DeathMessageLoader deathMessageLoader, Logger logger) {
        this.mechanicsLoader = loader;
        this.deathMessageLoader = deathMessageLoader;
        this.logger = logger;
    }

    public void loadAllIntoRepository (File mainDir, MapRepository repo, Collection<CBCGamemode> gamemodes)
            throws FileNotFoundException {

        if (!mainDir.exists())
            throw new FileNotFoundException("Main directory '%s' could not be found".formatted(mainDir.getPath()));

        // Find maps directory
        File mapFolder = new File(mainDir, "maps");
        File gamemodesFolder = new File(mainDir, "gamemodes");

        // Load all base maps
        Map<String, CBCMapData> maps = loadMapsFromDirectory(mapFolder);

        // Load all gamemodes
        Map<CBCGamemode, Map<String, GamemodeMapData>> gamemodeMaps = new HashMap<>();
        for (CBCGamemode gamemode : gamemodes) {
            File gamemodeMapsFolder = new File(gamemodesFolder, "maps");
            gamemodeMaps.put(gamemode, loadGamemodeMapsFromDirectory(gamemode, maps, gamemodeMapsFolder));
        }

        // Load maps into repo
        repo.addMaps(maps.values());
        logger.info("Successfully loaded " + maps.size() + " base maps");

        // Load gamemode maps into repo
        for (CBCGamemode gamemode : gamemodeMaps.keySet()) {
            repo.addGamemodeMaps(gamemode, gamemodeMaps.get(gamemode).values());
            logger.info("Successfully loaded " + maps.size() + " " + gamemode + " maps");
        }

    }

    public Map<String, CBCMapData> loadMapsFromDirectory (File mapDirectory) throws FileNotFoundException {
        if (!mapDirectory.exists())
            throw new FileNotFoundException("Map directory '%s' could not be found".formatted(mapDirectory.getPath()));
        return loadMaps(getYamlFiles(mapDirectory));
    }

    public Map<String, CBCMapData> loadMaps (Collection<File> mapFiles) {

        Map<String, CBCMapData> maps = new HashMap<>();

        for (File mapFile : mapFiles) {
            try {
                CBCMapData map = loadMap(mapFile);
                maps.put(map.id(), map);
            } catch (InvalidMapConfigException | FileNotFoundException e) {
                logger.log(Level.WARNING, e.getMessage(), e.getCause());
            }
        }

        return maps;

    }

    public CBCMapData loadMap (File mapFile) throws FileNotFoundException {

        if (!mapFile.exists())
            throw new FileNotFoundException("Map file '%s' could not be found".formatted(mapFile.getPath()));

        String mapId = mapFile.getName();
        try {
            YamlConfiguration ymlMapConfig = YamlConfiguration.loadConfiguration(mapFile);
            if (ymlMapConfig.getKeys(false).isEmpty()) {
                throw new IllegalArgumentException("Config is empty or could not be parsed into YAMLConfiguration");
            }
            return CBCMapData.fromConfig(ymlMapConfig, mechanicsLoader, deathMessageLoader);
        } catch (Exception e) {
            throw new InvalidMapConfigException(mapId, e);
        }
    }

    public Map<String, GamemodeMapData> loadGamemodeMapsFromDirectory (CBCGamemode gamemode,
                                                                Map<String, CBCMapData> maps,
                                                                File mapDirectory) {

        try {
            List<File> files = getYamlFiles(mapDirectory);
            return loadGamemodeMaps(gamemode, maps, getYamlFiles(mapDirectory));
        } catch (FileNotFoundException e) {
            logger.log(Level.WARNING, e.getMessage(), e.getCause());
            return Map.of();
        }

    }

    private List<File> getYamlFiles(File mapDirectory) throws FileNotFoundException {

        if (!mapDirectory.exists())
            throw new FileNotFoundException("Directory '%s' could not be found".formatted(mapDirectory.getPath()));

        File[] dirFiles = mapDirectory.listFiles(file -> {
            String name = file.getName().toLowerCase();
            return file.isFile() && (name.endsWith(".yaml") || name.endsWith(".yml"));
        });

        if (dirFiles == null)
            throw new IllegalArgumentException("Given file is not a directory, or an I/O error has occurred.");

        return Arrays.asList(dirFiles);

    }

    public Map<String, GamemodeMapData> loadGamemodeMaps (CBCGamemode gamemode,
                                                   Map<String, CBCMapData> maps,
                                                   Collection<File> mapFiles) {

        Map<String, GamemodeMapData> gamemodeMapDataList = new HashMap<>();

        for (File mapFile : mapFiles) {
            try {
                GamemodeMapData mapData = loadGamemodeMapData(gamemode, maps, mapFile);
                gamemodeMapDataList.put(mapData.mapData().id(), mapData);
            } catch (InvalidMapConfigException | FileNotFoundException e) {
                logger.log(Level.WARNING, e.getMessage(), e.getCause());
            }
        }

        return gamemodeMapDataList;

    }

    public GamemodeMapData loadGamemodeMapData (CBCGamemode gamemode,
                                                Map<String, CBCMapData> maps,
                                                File file) throws FileNotFoundException {

        if (!file.exists())
            throw new FileNotFoundException("Gamemode map file '%s' could not be found".formatted(file.getPath()));

        String mapId = file.getName();
        try {

            // Parse YAML file
            YamlConfiguration ymlMapConfig = YamlConfiguration.loadConfiguration(file);
            if (ymlMapConfig.getKeys(false).isEmpty()) {
                throw new IllegalArgumentException("Config is empty or could not be parsed into YAMLConfiguration");
            }

            // Retrieve base map
            CBCMapData baseMap = maps.get(mapId);
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
