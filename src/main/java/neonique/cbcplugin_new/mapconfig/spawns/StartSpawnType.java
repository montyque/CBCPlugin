package neonique.cbcplugin_new.mapconfig.spawns;

import neonique.cbcplugin_new.util.ConfigUtil;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.util.Vector;

public enum StartSpawnType {

    /**
     * Corresponds to BoxSpawnConfig and BoxSpawn.
     * The configuration requires a section "box_size" with 3 integers, "x", "y", and "z", denoting the size of the box.
     */
    BOX () {
        @Override
        public StartSpawnConfig fromConfig (ConfigurationSection parent, Vector vec) {
            Vector boxSize = ConfigUtil.requireVector(parent, "box_size");
            return new BoxSpawnConfig(vec, boxSize);
        }
    },

    /**
     * Corresponds to FrozenSpawnConfig and FrozenSpawn.
     */
    FROZEN () {
        @Override
        public StartSpawnConfig fromConfig (ConfigurationSection parent, Vector vec) {
            return new FrozenSpawnConfig(vec);
        }
    };

    /**
     * Returns a StartSpawnConfig to store spawn information.
     * @param parent The parent configuration.
     * @param vec The spawn's position in a vector.
     * @return Returns a spawn config, which can be used to create a spawn.
     */
    public abstract StartSpawnConfig fromConfig (ConfigurationSection parent,
                                                 Vector vec);

}
