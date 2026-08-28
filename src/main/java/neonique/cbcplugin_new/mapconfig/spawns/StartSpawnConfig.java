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

