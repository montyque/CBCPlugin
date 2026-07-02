package neonique.cbcplugin_new.mapmechanics;

public class InvalidMapMechanicConfigException extends RuntimeException {
    public InvalidMapMechanicConfigException (String type) {
        super("Failure parsing map mechanic config: no map mechanic config of type '" + type + "' exists");
    }
    public InvalidMapMechanicConfigException (String type, Throwable cause) {
        super("Failure parsing config of map mechanic type '" + type + "'", cause);
    }

}
