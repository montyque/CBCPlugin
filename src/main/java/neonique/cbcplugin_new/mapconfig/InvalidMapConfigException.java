package neonique.cbcplugin_new.mapconfig;

public class InvalidMapConfigException extends Exception {
    public InvalidMapConfigException () {
        super();
    }
    public InvalidMapConfigException (String message) {
        super(message);
    }
    public InvalidMapConfigException (String message, Throwable e) {
        super(message, e);
    }
}
