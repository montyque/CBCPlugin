package neonique.cbcplugin_new.commands.practice;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import neonique.cbcplugin_new.commands.SubCommand;
import neonique.cbcplugin_new.commands.arguments.CBCMapDataArgumentType;
import neonique.cbcplugin_new.mapconfig.CBCMapData;
import neonique.cbcplugin_new.practice.PracticeManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.List;

@SuppressWarnings("UnstableApiUsage")
public class PracticeStartCommand implements SubCommand {

    private final PracticeManager practiceManager;
    private final CBCMapDataArgumentType mapArgument;

    public PracticeStartCommand(PracticeManager practiceManager) {
        this.practiceManager = practiceManager;
        this.mapArgument = new CBCMapDataArgumentType(practiceManager::getPracticeMaps);
    }

    @Override
    public LiteralArgumentBuilder<CommandSourceStack> build() {
        return Commands.literal("start")
                .requires(s -> s.getSender().hasPermission("cbcplugin.host"))
                .requires(s -> !practiceManager.instanceActive())
                .then(
                    Commands.argument("mapId", mapArgument)
                            .executes(this::startPracticeInstance)
                );
    }

    private int startPracticeInstance (CommandContext<CommandSourceStack> ctx) {

        CBCMapData practiceMap = ctx.getArgument("mapId", CBCMapData.class);
        practiceManager.newInstance(practiceMap);
        ctx.getSource().getSender().sendMessage(Component.text("started practice arena").color(NamedTextColor.GREEN));

        return Command.SINGLE_SUCCESS;

    }

}
