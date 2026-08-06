package neonique.cbcplugin_new.commands.practicesubcommands;

import neonique.cbcplugin_new.commands.PracticeCommand;
import neonique.cbcplugin_new.managers.GameState;
import neonique.cbcplugin_new.mapconfig.CBCMap;
import neonique.cbcplugin_new.managers.GameManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class practice_changemap {

    public static void run(PracticeCommand command, Player user, String[] args, int perms) {

        if (args.length < 2) {
            user.sendMessage(Component.text("You must include a map to enable practice on!").color(NamedTextColor.YELLOW));
            return;
        }

        GameManager gameManager = command.gameManager;

        if (gameManager.getGameState() == GameState.ACTIVE) {
            user.sendMessage(Component.text("There is already an active game!").color(NamedTextColor.YELLOW));
            return;
        }

        if (!gameManager.isPracticeActive()) {
            user.sendMessage(Component.text("The Practice Arena is not active! Use /practice start <map> to open the practice arena.").color(NamedTextColor.YELLOW));
            return;
        }

        // Find map
        String map_id = args[1].toLowerCase();

        // Attempt to find map
        CBCMap mapChoice = gameManager.getPracticeMap(map_id);
        if (mapChoice == null) {
            user.sendMessage(Component.text("No map with that ID has been found!").color(NamedTextColor.YELLOW));
            return;
        }

        // Check if map choice is same as current map
        if (gameManager.practiceManager.getMap().id().equals(mapChoice.id())) {
            user.sendMessage(Component.text("That map is already the Practice Arena map!").color(NamedTextColor.YELLOW));
            return;
        }

        // Change practice arena map
        gameManager.practiceManager.changeMap(mapChoice);

    }

    public static List<String> getTabCompletions (PracticeCommand command, String[] args, int perms) {

        int level = args.length;
        GameManager gameManager = command.gameManager;
        List<String> tabCompletions = new ArrayList<>();

        if (level == 2) {
            if (perms >= 1) {
                // Show all practice maps
                for (CBCMap map : gameManager.getPracticeMaps()) {
                    tabCompletions.add(map.id());
                }

                // Remove the map currently set
                if (gameManager.practiceManager.getMap() != null) {
                    tabCompletions.remove(gameManager.practiceManager.getMap().id());
                }
            }
        }

        return tabCompletions;
    }

}
