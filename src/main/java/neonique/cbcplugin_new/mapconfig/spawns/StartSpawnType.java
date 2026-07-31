package neonique.cbcplugin_new.mapconfig.spawns;

import neonique.cbcplugin_new.util.ConfigUtil;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.util.Vector;

public enum StartSpawnType {

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
