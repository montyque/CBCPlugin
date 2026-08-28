package neonique.cbcplugin_new.mapconfig.spawns;

import org.bukkit.World;
import org.bukkit.util.Vector;

/**
 * Configuration object that holds information for a map's starting spawn.
 * Its main purpose is to provide a place to store spawn information without storing a Bukkit-bound object, such
 * as a location.
 */
public interface StartSpawnConfig {

    /**
     * Returns a MapStartSpawn from this spawn config.
     * @param world The world in which this spawn is placed.
     * @return The spawn.
     */
    MapStartSpawn getSpawn (World world);

    Vector vec ();

}

/**
 * Configuration object for BoxSpawn.
 * @param vec The x, y and z coordinates of the spawn. Note that these coordinates should denote where players
 *            get teleported, i.e. one block above the bottom block of the box.
 * @param boxSize The x, y, and z size of the box.
 */
record BoxSpawnConfig (Vector vec, Vector boxSize) implements StartSpawnConfig {

    @Override
    public MapStartSpawn getSpawn(World world) {
        return new BoxSpawn(world, vec, (int) boxSize.getX(), (int) boxSize.getY(), (int) boxSize.getZ());
    }

}

/**
 * Configuration object for FrozenSpawn.
 * @param vec The x, y and z coordinates of the spawn.
 */
record FrozenSpawnConfig (Vector vec) implements StartSpawnConfig {
    @Override
    public MapStartSpawn getSpawn(World world) {
        return new FrozenSpawn(world, vec);
    }
}
