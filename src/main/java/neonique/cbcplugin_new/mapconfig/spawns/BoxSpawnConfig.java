package neonique.cbcplugin_new.mapconfig.spawns;

import org.bukkit.World;
import org.bukkit.util.Vector; /**
 * Configuration object for BoxSpawn.
 * @param vec The x, y and z coordinates of the spawn. Note that these coordinates should denote where players
 *            get teleported, i.e. one block above the bottom block of the box.
 * @param boxSize The x, y, and z size of the box.
 */
public record BoxSpawnConfig (Vector vec, Vector boxSize) implements StartSpawnConfig {

    @Override
    public MapStartSpawn getSpawn(World world) {
        return new BoxSpawn(world, vec, (int) boxSize.getX(), (int) boxSize.getY(), (int) boxSize.getZ());
    }

}
