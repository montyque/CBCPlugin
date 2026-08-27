package neonique.cbcplugin_new.commands;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.executors.CommandArguments;
import neonique.cbcplugin_new.commands.arguments.EnumArgumentParser;
import neonique.cbcplugin_new.commands.arguments.MapDataArgumentParser;
import neonique.cbcplugin_new.commands.arguments.PlayerArgumentParser;
import neonique.cbcplugin_new.core.CBCGamemode;
import neonique.cbcplugin_new.core.TeamColor;
import neonique.cbcplugin_new.lobby.Lobby;
import neonique.cbcplugin_new.mapconfig.MapRepository;
import org.bukkit.command.CommandSender;

public class LobbyCommand implements Subcommand {

    private final Lobby lobby;
    private final MapRepository repository;

    public LobbyCommand (Lobby lobby) {
        this.lobby = lobby;
        this.repository = lobby.mapRepository();
    }

    @Override
    public CommandAPICommand get() {

        return new CommandAPICommand("lobby")
                .withRequirement(s -> lobby.isActive())

                // Game subcommand
                .withSubcommand(new CommandAPICommand("game")
                        .withSubcommand(new CommandAPICommand("select")
                                .withArguments(
                                        EnumArgumentParser.argument("gamemode", CBCGamemode.class, repository::allGamemodes),
                                        MapDataArgumentParser.argument("mapId",
                                                a -> repository.allGamemodeMaps((CBCGamemode) a.get("gamemode")))
                                )
                                .executes(this::gameSelectExecutor)
                        )
                        .withSubcommand(new CommandAPICommand("start")
                                .executes(this::gameStartExecutor)
                        )
                        .withSubcommand(new CommandAPICommand("settings")
                                // TODO: Add settings modifiers
                        )
                )

                // Team subcommand
                .withSubcommand(new CommandAPICommand("team")
                        .withSubcommand(new CommandAPICommand("randomize")
                                // TODO: add randomizer
                        )
                        .withSubcommand(new CommandAPICommand("join")
                                .withArguments(
                                        PlayerArgumentParser.argument("playerName", lobby::players),
                                        EnumArgumentParser.argument("teamColor", TeamColor.class)
                                )
                                .executes(this::teamJoinExecutor)
                        )
                        .withSubcommand(new CommandAPICommand("clear")
                                .executes(this::teamClearExecutor))
                );
    }

    private void gameStartExecutor (CommandSender sender, CommandArguments arguments) {

    }

    private void gameSelectExecutor (CommandSender sender, CommandArguments arguments) {

    }

    private void teamJoinExecutor (CommandSender sender, CommandArguments arguments) {

    }

    private void teamClearExecutor (CommandSender sender, CommandArguments arguments) {

    }

}
