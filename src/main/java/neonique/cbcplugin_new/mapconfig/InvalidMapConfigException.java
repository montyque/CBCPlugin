package neonique.cbcplugin_new.mapconfig;

public class InvalidMapConfigException extends RuntimeException {

    public InvalidMapConfigException (String mapId, String reason) {
        super("Error occured while parsing map file '" + mapId + "': " + reason);
    }

    public InvalidMapConfigException (String mapId, Throwable cause) {
        super("Error occured while parsing map file '" + mapId + "'", cause);
    }

}
