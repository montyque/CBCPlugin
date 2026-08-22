package neonique.cbcplugin_new.commands.practice;

import io.papermc.paper.command.brigadier.CommandSourceStack;
import neonique.cbcplugin_new.commands.CommandComponent;
import neonique.cbcplugin_new.mapconfig.CBCMapData;
import neonique.cbcplugin_new.practice.PracticeManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.incendo.cloud.Command;
import org.incendo.cloud.CommandManager;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.execution.CommandExecutionHandler;

import static neonique.cbcplugin_new.commands.parsers.CBCMapDataParser.mapDataParser;

public class PracticeStartCommand implements CommandComponent<CommandSender>, CommandExecutionHandler<CommandSender> {

    private final PracticeManager practiceManager;

    public PracticeStartCommand(PracticeManager practiceManager) {
        this.practiceManager = practiceManager;
    }

    @Override
    public void register(CommandManager<CommandSender> manager, Command.Builder<CommandSender> base) {
        manager.command(
                base.literal("start")
                        .permission("cbcplugin.host")
                        .required("mapId", mapDataParser(practiceManager::getPracticeMaps, CBCMapData.class))
                        .handler(this)
        );
    }

    @Override
    public void execute (@NonNull CommandContext<CommandSender> ctx) {
        CBCMapData practiceMap = ctx.get("mapId");
        practiceManager.newInstance(practiceMap);
        ctx.sender().sendMessage(Component.text("started practice arena").color(NamedTextColor.GREEN));
    }
}
