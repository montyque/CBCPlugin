package neonique.cbcplugin_new.commands.practicesubcommands;

import neonique.cbcplugin_new.commands.PracticeCommand;
import neonique.cbcplugin_new.managers.GameState;
import neonique.cbcplugin_new.gamemodes._base.CBCMap;
import neonique.cbcplugin_new.managers.GameManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class practice_start {

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

        if (gameManager.isPracticeActive()) {
            user.sendMessage(Component.text("The Practice Arena is already active! Use /practice close to close the arena.").color(NamedTextColor.YELLOW));
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

        // Start practice arena
        gameManager.practiceManager.enable(mapChoice);

    }

    public static List<String> getTabCompletions (PracticeCommand command, String[] args, int perms) {

        int level = args.length;
        GameManager gameManager = command.gameManager;
        List<String> tabCompletions = new ArrayList<>();

        if (level == 2) {
            if (perms >= 1) {
                // Show all practice maps
                for (CBCMap map : gameManager.getPracticeMaps()) {
                    tabCompletions.add(map.getMapId());
                }
            }
        }

        return tabCompletions;
    }
}
