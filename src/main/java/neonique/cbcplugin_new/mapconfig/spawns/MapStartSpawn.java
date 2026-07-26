package neonique.cbcplugin_new.mapconfig.spawns;

public interface MapStartSpawn extends MapSpawn {

    default void onSetup () {}

    default void reset () {}

    default boolean canMove () {
        return false;
    }

}
