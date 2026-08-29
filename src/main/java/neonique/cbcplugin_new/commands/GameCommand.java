package neonique.cbcplugin_new.commands;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.executors.CommandArguments;
import neonique.cbcplugin_new.session.Session;
import neonique.cbcplugin_new.session.SessionState;
import org.bukkit.command.CommandSender;

public class GameCommand implements Subcommand {

    private final Session session;

    public GameCommand (Session session) {
        this.session = session;
    }

    @Override
    public CommandAPICommand get() {

        return new CommandAPICommand("game")
                .withRequirement(_ -> session.state() == SessionState.IN_GAME)

                // Game subcommand
                .withSubcommand(new CommandAPICommand("stop")
                        .withPermission("cbcplugin.host")
                        .executes(this::stopExecutor)
                );
    }

    private void stopExecutor (CommandSender sender, CommandArguments arguments) {
        session.stopGame();
    }

}
