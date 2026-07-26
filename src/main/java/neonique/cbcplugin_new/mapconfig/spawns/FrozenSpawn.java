package neonique.cbcplugin_new.mapconfig.spawns;

import neonique.cbcplugin_new.util.VectorUtil;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.util.Vector;

public class FrozenSpawn implements MapStartSpawn {

    private final Location location;

    public FrozenSpawn (World world, Vector vector) {
        this.location = VectorUtil.vecToLocation(vector, world);
    }

    public Location location () {
        return location.clone();
    }

}
