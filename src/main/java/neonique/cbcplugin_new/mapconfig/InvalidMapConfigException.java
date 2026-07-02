package neonique.cbcplugin_new.mapconfig;

public class InvalidMapConfigException extends RuntimeException {

    public InvalidMapConfigException (String mapName, String reason) {
        super("Error parsing map '" + mapName + "': " + reason);
    }

    public InvalidMapConfigException (String mapName, Throwable cause) {
        super("Error parsing map '" + mapName + "'", cause);
    }

}
