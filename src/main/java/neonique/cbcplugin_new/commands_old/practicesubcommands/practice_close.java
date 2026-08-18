package neonique.cbcplugin_new.commands_old.practicesubcommands;

import neonique.cbcplugin_new.commands_old.PracticeCommand;
import neonique.cbcplugin_new.managers.GameManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class practice_close {
    public static void run(PracticeCommand command, Player user, String[] args, int perms) {

        GameManager gameManager = command.gameManager;

        if (!gameManager.isPracticeActive()) {
            user.sendMessage(Component.text("The Practice Arena is not active currently!").color(NamedTextColor.YELLOW));
            return;
        }

        gameManager.practiceManager.disable();
    }

    public static List<String> getTabCompletions (PracticeCommand command, String[] args, int perms) {
        return new ArrayList<>();
    }
}
