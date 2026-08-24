package neonique.cbcplugin_new.commands_old.lobbysubcommands;

import neonique.cbcplugin_new.commands_old.LobbyCommand;
import neonique.cbcplugin_new.managers.GameState;
import neonique.cbcplugin_new.lobby_old.Lobby;
import neonique.cbcplugin_new.managers.GameManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

public class lobby_setgamemode {

    public static void run(LobbyCommand lobbyCommand, Player user, String[] args, int perms) {

        Lobby lobby = lobbyCommand.lobby;
        GameManager gameManager = lobbyCommand.gameManager;

        // Make sure game state is in lobby and that the gamemode/map has not been finalized
        if (gameManager.getGameState() != GameState.LOBBY) {
            user.sendMessage(Component.text("You can only set the gamemode/map when the lobby is active!").color(NamedTextColor.YELLOW));
            return;
        }

        if (lobby.isGameStarting()) {
            user.sendMessage(Component.text("The game is already starting! To set a new gamemode, " +
                    "you need to cancel the game start.").color(NamedTextColor.YELLOW));
            return;
        }

        lobby.openGamemodeMenu(user);
    }
}
