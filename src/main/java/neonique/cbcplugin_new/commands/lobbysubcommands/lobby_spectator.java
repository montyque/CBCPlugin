package neonique.cbcplugin_new.commands.lobbysubcommands;

import neonique.cbcplugin_new.commands.LobbyCommand;
import neonique.cbcplugin_new.commands._SubCommand;
import neonique.cbcplugin_new.lobby.Lobby;
import neonique.cbcplugin_new.lobby.LobbyPlayer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class lobby_spectator extends _SubCommand {

    public static void run(LobbyCommand lobbyCommand, Player user, String[] args, int perms) {

        if (args.length < 2) {
            user.sendMessage(Component.text("You must include a sub-sub command!").color(NamedTextColor.YELLOW));
            return;
        }

        Lobby lobby = lobbyCommand.lobby;

        String subsubcommand = args[1].toLowerCase();
        if (subsubcommand.equals("toggle")) {
            // Check if player is in lobby
            LobbyPlayer player = lobby.getLobbyPlayer(user);
            if (player == null) {
                user.sendMessage(Component.text("You are not registered in the lobby!").color(NamedTextColor.YELLOW));
                return;
            }
            lobby.playerToggleSpectator(player);
        }
        else if (subsubcommand.equals("toggleplayer")) {

            Player player = getPlayerOrError(args[2], user);
            if (player == null) return;

            // Check if player is in lobby
            LobbyPlayer lbPlayer = lobby.getLobbyPlayer(player);
            if (lbPlayer == null) {
                user.sendMessage(Component.text("The player is not registered in the lobby!").color(NamedTextColor.YELLOW));
                return;
            }

            lobby.playerToggleSpectator(lbPlayer);

        }
    }

    public static List<String> getTabCompletions (LobbyCommand lobbyCommand, String[] args, int perms) {

        int level = args.length;
        Lobby lobby = lobbyCommand.lobby;
        List<String> tabCompletions = new ArrayList<>();

        if (level == 2) {
            // Show all subcommands
            //tabCompletions = Arrays.asList("toggle");
            tabCompletions.add("toggle");

            // Administrative commands
            if (perms >= 1) {
                tabCompletions.add("toggleplayer");
            }
        }

        else if (level >= 3) {

            String subcommand = args[1];
            if (subcommand.equals("toggleplayer")) {
                tabCompletions = getAllPlayerNames();
            }
        }

        return tabCompletions;

    }
}
