package neonique.cbcplugin_new.mapconfig.spawns;

import org.bukkit.World;
import org.bukkit.util.Vector;

public sealed interface StartSpawnConfig {

    MapStartSpawn getSpawn (World world);

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
