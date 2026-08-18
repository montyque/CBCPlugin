package neonique.cbcplugin_new.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;

@SuppressWarnings("UnstableApiUsage")
public interface SubCommand {

    LiteralArgumentBuilder<CommandSourceStack> build ();

}
