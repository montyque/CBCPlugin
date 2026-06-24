package neonique.cbcplugin_new.commands.gamesubcommands;

import neonique.cbcplugin_new.commands.GameCommand;
import neonique.cbcplugin_new.managers.GameState;
import neonique.cbcplugin_new.gamemodes._base.Game;
import neonique.cbcplugin_new.managers.GameManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

public class game_end {

    public static void run(GameCommand gameCommand, Player user, String[] args, int perms) {

        GameManager gameManager = gameCommand.gameManager;

        // Check if game is active
        if (gameManager.getGameState() != GameState.ACTIVE) {
            user.sendMessage(Component.text("You cannot end the game as there is no game in progress!").color(NamedTextColor.YELLOW));
            return;
        }

        // Check if there is a game
        Game currentGame = gameManager.getCurrentGame();
        if (currentGame == null) {
            user.sendMessage(Component.text("You cannot end the game as there is no game in progress!").color(NamedTextColor.YELLOW));
            return;
        }

        // End the game
        gameManager.endGame();
    }
}
