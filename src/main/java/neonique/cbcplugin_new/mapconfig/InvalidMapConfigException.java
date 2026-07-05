package neonique.cbcplugin_new.mapconfig;

import neonique.cbcplugin_new.gamemodes.CBCGamemode;

public class InvalidMapConfigException extends RuntimeException {

    public InvalidMapConfigException (String mapId, String reason) {
        super("Error occurred while parsing map file '" + mapId + "': " + reason);
    }

    public InvalidMapConfigException (String mapId, Throwable cause) {
        super("Error occurred while parsing map file '" + mapId + "'", cause);
    }

    public InvalidMapConfigException (CBCGamemode gamemode, String mapId, Throwable cause) {
        super("Error occurred while parsing map file '" + mapId + "' for gamemode " + gamemode.name(), cause);
    }

}
