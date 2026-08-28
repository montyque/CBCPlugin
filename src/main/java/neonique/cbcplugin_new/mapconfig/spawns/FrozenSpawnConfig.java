package neonique.cbcplugin_new.mapconfig.spawns;

import org.bukkit.World;
import org.bukkit.util.Vector; /**
 * Configuration object for FrozenSpawn.
 * @param vec The x, y and z coordinates of the spawn.
 */
public record FrozenSpawnConfig (Vector vec) implements StartSpawnConfig {
    @Override
    public MapStartSpawn getSpawn(World world) {
        return new FrozenSpawn(world, vec);
    }
}
