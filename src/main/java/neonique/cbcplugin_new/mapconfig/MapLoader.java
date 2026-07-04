package neonique.cbcplugin_new.mapconfig;

import neonique.cbcplugin_new.CBCPlugin;
import neonique.cbcplugin_new.managers.GameManager;
import neonique.cbcplugin_new.mapmechanics.MapMechanicsManager;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.nio.file.Path;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MapLoader {

    private MapMechanicLoader mechanicsLoader;
    private GameManager gameManager;
    private Logger logger;

    public MapLoader (GameManager gameManager, MapMechanicLoader loader, Logger logger) {

    }

    public Map<String, CBCMap> loadMapsFromDirectory (File mapDirectory) {

        File[] dirFiles = mapDirectory.listFiles(file -> {
            String name = file.getName().toLowerCase();
            return file.isFile() && (name.endsWith(".yaml") || name.endsWith(".yml"));
        });

        if (dirFiles == null)
            throw new IllegalArgumentException("Given file is not a directory, or an I/O error has occurred.");


        List<File> yamlFiles = Arrays.asList(dirFiles);
        return loadMaps(yamlFiles);

    }

    public Map<String, CBCMap> loadMaps (Collection<File> mapFiles) {

        Map<String, CBCMap> maps = new HashMap<>();

        for (File mapFile : mapFiles) {
            try {
                CBCMap map = loadMap(mapFile);
                maps.put(map.getId(), map);
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

}
