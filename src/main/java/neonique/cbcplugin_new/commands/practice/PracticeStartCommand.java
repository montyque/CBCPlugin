package neonique.cbcplugin_new.commands.practice;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.exceptions.WrapperCommandSyntaxException;
import dev.jorel.commandapi.executors.CommandArguments;
import dev.jorel.commandapi.executors.CommandExecutor;
import neonique.cbcplugin_new.CBCPlugin;
import neonique.cbcplugin_new.commands.Subcommand;
import neonique.cbcplugin_new.commands.arguments.MapDataArgumentParser;
import neonique.cbcplugin_new.mapconfig.CBCMapData;
import neonique.cbcplugin_new.practice.PracticeManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;

public class PracticeStartCommand implements Subcommand {

    public PracticeManager practiceManager;

    public PracticeStartCommand (PracticeManager practiceManager) {
        this.practiceManager = practiceManager;
    }

    @Override
    public CommandAPICommand get() {
        return new CommandAPICommand("start")
                .withPermission(CommandPermission.OP)
                .withRequirement(s -> !practiceManager.instanceActive())
                .withArguments(
                        MapDataArgumentParser.argument("mapId", practiceManager::getPracticeMaps)
                )
                .executes(this::run);
    }

    public void run (CommandSender sender, CommandArguments arguments) throws WrapperCommandSyntaxException {

        CBCMapData map = (CBCMapData) arguments.get("mapId");
        practiceManager.newInstance(map);

        sender.sendMessage(Component.text("Opened the practice arena.").color(NamedTextColor.YELLOW));

    }
}
