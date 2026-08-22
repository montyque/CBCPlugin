package neonique.cbcplugin_new.commands;

import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.exceptions.WrapperCommandSyntaxException;
import dev.jorel.commandapi.executors.CommandArguments;
import neonique.cbcplugin_new.commands.arguments.MapDataArgumentParser;
import neonique.cbcplugin_new.mapconfig.CBCMapData;
import neonique.cbcplugin_new.practice.PracticeManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class PracticeCommand implements Subcommand {

    private final PracticeManager practiceManager;

    public PracticeCommand (PracticeManager practiceManager) {
        this.practiceManager = practiceManager;
    }

    @Override
    public CommandAPICommand get() {
        return new CommandAPICommand("practice")

                .withSubcommand(new CommandAPICommand("start")
                        .withPermission(CommandPermission.OP)
                        .withRequirement(s -> !practiceManager.instanceActive())
                        .withArguments(
                                MapDataArgumentParser.argument("mapId", practiceManager::getPracticeMaps)
                        )
                        .executes(this::startExecutor))

                .withSubcommand(new CommandAPICommand("stop")
                        .withPermission(CommandPermission.OP)
                        .withRequirement(s -> practiceManager.instanceActive())
                        .executes(this::stopExecutor))

                .withSubcommand(new CommandAPICommand("join")
                        .withRequirement(s -> practiceManager.instanceActive())
                        .withRequirement(s -> !practiceManager.instanceHasPlayer((Player) s))
                        .executesPlayer(this::joinExecutor))

                .withSubcommand(new CommandAPICommand("leave")
                        .withRequirement(s -> practiceManager.instanceActive())
                        .withRequirement(s -> practiceManager.instanceHasPlayer((Player) s))
                        .executesPlayer(this::leaveExecutor));

    }

    private void startExecutor (CommandSender sender, CommandArguments arguments) throws WrapperCommandSyntaxException {

        CBCMapData map = (CBCMapData) arguments.get("mapId");
        if (map == null) {
            throw CommandAPI.failWithString("The given map does not exist.");
        }

        practiceManager.newInstance(map);
        sender.sendMessage(Component.text("You have opened the practice arena on " + map.name() + ".").color(NamedTextColor.GREEN));

    }

    private void stopExecutor (CommandSender sender, CommandArguments arguments) {

        practiceManager.endInstance();
        sender.sendMessage(Component.text("You have closed the practice arena.").color(NamedTextColor.YELLOW));

    }

    private void joinExecutor (CommandSender sender, CommandArguments arguments) {
        Player player = (Player) sender;
        practiceManager.playerJoin(player);
    }

    private void leaveExecutor (CommandSender sender, CommandArguments arguments) {
        Player player = (Player) sender;
        practiceManager.playerLeave(player);
    }

}
