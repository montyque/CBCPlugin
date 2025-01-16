package neonique.cbcplugin_new.commands.gamesubcommands;

import neonique.cbcplugin_new.commands.GameCommand;
import neonique.cbcplugin_new.gamemodes._base.PostGameStats;
import neonique.cbcplugin_new.managers.GameManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

public class game_lastgamestats {

    public static void run(GameCommand gameCommand, Player user, String[] args, int perms) {

        GameManager gameManager = gameCommand.gameManager;

        // Check if post game stats exist
        PostGameStats gameStats = gameManager.getPostGameStats();
        if (gameStats == null) {
            user.sendMessage(Component.text("Statistics for the last game are unavailable.").color(NamedTextColor.YELLOW));
            return;
        }

        // Open inventory menu
        Inventory lastGameStats = gameStats.createInventoryGui(user);
        user.openInventory(lastGameStats);
    }
}
