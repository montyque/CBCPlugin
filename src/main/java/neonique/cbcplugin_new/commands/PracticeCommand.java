package neonique.cbcplugin_new.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import neonique.cbcplugin_new.commands.practice.PracticeStartCommand;
import neonique.cbcplugin_new.practice.PracticeManager;

@SuppressWarnings("UnstableApiUsage")
public class PracticeCommand implements SubCommand {

    private final PracticeManager practiceManager;

    // Subcommands
    private final PracticeStartCommand start;

    public PracticeCommand (PracticeManager practiceManager) {
        this.practiceManager = practiceManager;
        this.start = new PracticeStartCommand(practiceManager);
    }

    @Override
    public LiteralArgumentBuilder<CommandSourceStack> build() {
        return Commands.literal("practice")
                .then(start.build());
    }

}

