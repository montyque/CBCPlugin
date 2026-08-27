package neonique.cbcplugin_new.commands;

import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.exceptions.WrapperCommandSyntaxException;
import dev.jorel.commandapi.executors.CommandArguments;
import neonique.cbcplugin_new.commands.arguments.EnumArgumentParser;
import neonique.cbcplugin_new.commands.arguments.MapDataArgumentParser;
import neonique.cbcplugin_new.commands.arguments.PlayerArgumentParser;
import neonique.cbcplugin_new.core.CBCGamemode;
import neonique.cbcplugin_new.core.TeamColor;
import neonique.cbcplugin_new.lobby.IllegalGameConditionsException;
import neonique.cbcplugin_new.lobby.Lobby;
import neonique.cbcplugin_new.lobby.LobbyPlayer;
import neonique.cbcplugin_new.mapconfig.GamemodeMapData;
import neonique.cbcplugin_new.mapconfig.MapRepository;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
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
                                        PlayerArgumentParser.argument("player", lobby::players),
                                        EnumArgumentParser.argument("teamColor", TeamColor.class)
                                )
                                .executes(this::teamJoinExecutor)
                        )
                        .withSubcommand(new CommandAPICommand("clear")
                                .executes(this::teamClearExecutor))
                );
    }

    /**
     * Begins the game countdown, or sends an error message if the conditions are not met.
     */
    private void gameStartExecutor (CommandSender sender, CommandArguments arguments) {

        // If the game could not be started due to insufficient conditions, send message to the command user
        try {
            lobby.startGameCountdown();
            sender.sendMessage(Component.text("Started the game's countdown!").color(NamedTextColor.YELLOW));
        } catch (IllegalGameConditionsException e) {
            sender.sendMessage(e.getComponentMessage());
        }

    }

    /**
     * Selects the gamemode and map selected.
     */
    private void gameSelectExecutor (CommandSender sender, CommandArguments arguments) {

        // Sel
        CBCGamemode gamemode = (CBCGamemode) arguments.get("gamemode");
        GamemodeMapData mapData = (GamemodeMapData) arguments.get("mapId");
        lobby.gameSelector().setGameSelection(gamemode, mapData);

    }

    /**
     * Adds a player to a team.
     */
    private void teamJoinExecutor (CommandSender sender, CommandArguments arguments) throws WrapperCommandSyntaxException {

        LobbyPlayer player = (LobbyPlayer) arguments.get("player");
        if (player == null) throw CommandAPI.failWithString("The given player does not exist.");

        TeamColor color = (TeamColor) arguments.get("TeamColor");
        if (color == null) throw CommandAPI.failWithString("The given team color does not exist.");

        // Join team with given color
        lobby.playerJoinTeam(player, color, true);
        sender.sendMessage(Component.text()
                .append(Component.text("Added " + player.name() + " to team ").color(NamedTextColor.GREEN))
                .append(Component.text(color.name()).color(color.color()))
                .append(Component.text("!").color(NamedTextColor.GREEN))
                .build()
        );

    }

    /**
     * Removes all players from teams.
     */
    private void teamClearExecutor (CommandSender sender, CommandArguments arguments) {

        lobby.clearAllTeams();
        sender.sendMessage(Component.text("Cleared all teams!").color(NamedTextColor.GREEN));

    }

}
