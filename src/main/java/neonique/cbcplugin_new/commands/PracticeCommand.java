package neonique.cbcplugin_new.commands;

import dev.jorel.commandapi.CommandAPICommand;
import neonique.cbcplugin_new.commands.practice.PracticeStartCommand;
import neonique.cbcplugin_new.practice.PracticeManager;

import java.util.List;

public class PracticeCommand implements Subcommand {

    private final PracticeManager practiceManager;

    public PracticeCommand (PracticeManager practiceManager) {
        this.practiceManager = practiceManager;
    }

    @Override
    public CommandAPICommand get() {
        return new CommandAPICommand("practice")
                .withSubcommand(new PracticeStartCommand(practiceManager).get());
    }

}
