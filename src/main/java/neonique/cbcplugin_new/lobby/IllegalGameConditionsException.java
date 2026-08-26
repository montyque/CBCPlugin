package neonique.cbcplugin_new.lobby;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public class IllegalGameConditionsException extends RuntimeException {

    public IllegalGameConditionsException (String message) {
        super("Illegal conditions for starting game: " + message);
    }

    public Component getComponentMessage () {
        return Component.text(getMessage()).color(NamedTextColor.YELLOW);
    }

}
