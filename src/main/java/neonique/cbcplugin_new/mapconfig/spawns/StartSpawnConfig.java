package neonique.cbcplugin_new.mapconfig.spawns;

import neonique.cbcplugin_new.core.TeamColor;
import neonique.cbcplugin_new.util.ConfigUtil;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.util.Vector;

import java.util.List;

public sealed interface StartSpawnConfig {

    MapStartSpawn getSpawn (World world);

    enum StartSpawnType {

        BOX () {
            @Override
            public StartSpawnConfig fromConfig (ConfigurationSection parent, Vector vec) {
                ConfigurationSection boxSize = ConfigUtil.requireConfigurationSection(parent, "box_size");
                int x = ConfigUtil.requireInt(boxSize, "x");
                int y = ConfigUtil.requireInt(boxSize, "y");
                int z = ConfigUtil.requireInt(boxSize, "z");
                return new BoxSpawnConfig(vec, x, y, z);
            }
        },

        FROZEN () {
            @Override
            public StartSpawnConfig fromConfig (ConfigurationSection parent, Vector vec) {
                return new FrozenSpawnConfig(vec);
            }
        };

        public abstract StartSpawnConfig fromConfig (ConfigurationSection parent,
                                                     Vector vec);

    }

}

record BoxSpawnConfig (Vector vec,
                       int boxSizeX,
                       int boxSizeY,
                       int boxSizeZ) implements StartSpawnConfig {

    @Override
    public MapStartSpawn getSpawn(World world) {
        return new BoxSpawn(world, vec, boxSizeX, boxSizeY, boxSizeZ);
    }

}

record FrozenSpawnConfig (Vector vec) implements StartSpawnConfig {
    @Override
    public MapStartSpawn getSpawn(World world) {
        return new FrozenSpawn(world, vec);
    }
}
