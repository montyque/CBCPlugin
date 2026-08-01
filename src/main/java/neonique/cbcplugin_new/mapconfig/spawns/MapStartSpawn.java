package neonique.cbcplugin_new.mapconfig.spawns;

public interface MapStartSpawn extends MapSpawn {

    /**
     * Called before players are teleported to this spawn.
     * This is usually called at the start of a game or round.
     */
    default void onSetup () {}

    /**
     * Resets all the logic in onSetup().
     */
    default void reset () {}

    /**
     * @return Whether players are allowed to move or not when teleported to the spawn.
     */
    default boolean canMove () {
        return false;
    }

}
