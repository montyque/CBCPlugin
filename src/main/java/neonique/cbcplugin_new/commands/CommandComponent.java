package neonique.cbcplugin_new.commands;

import org.incendo.cloud.Command;
import org.incendo.cloud.CommandManager;

public interface CommandComponent<C> {

    void register (CommandManager<C> manager, Command.Builder<C> base);

}
