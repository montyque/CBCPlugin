package neonique.cbcplugin_new.core;

public class InvalidSettingValueException extends RuntimeException {
    public InvalidSettingValueException (String setting, String value, Throwable cause) {
        super("Invalid value '" + value + "' for " + setting + ":\n" + cause.getMessage());
    }
}
