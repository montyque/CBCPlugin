package neonique.cbcplugin_new.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import neonique.cbcplugin_new.commands.practice.PracticeStartCommand;
import neonique.cbcplugin_new.practice.PracticeManager;
import org.bukkit.command.CommandSender;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.incendo.cloud.Command;
import org.incendo.cloud.CommandManager;

import java.util.List;

public class PracticeCommand implements CommandComponent<CommandSender> {

    private final PracticeManager practiceManager;

    // Subcommands
    private final List<CommandComponent<CommandSender>> subcommands;

    public PracticeCommand (PracticeManager practiceManager) {
        this.practiceManager = practiceManager;
        this.subcommands = List.of(
                new PracticeStartCommand(practiceManager)
        );
    }

    @Override
    public void register(CommandManager<CommandSender> manager, Command.Builder<CommandSender> base) {
        subcommands.forEach(c -> c.register(manager, base
                .literal("practice")));
    }

}

