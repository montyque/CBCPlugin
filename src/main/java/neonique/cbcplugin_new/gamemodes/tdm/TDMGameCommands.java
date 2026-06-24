package neonique.cbcplugin_new.gamemodes.tdm;

import neonique.cbcplugin_new.gamemodes._base.BaseTeamGameCommands;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import java.util.List;

public class TDMGameCommands extends BaseTeamGameCommands {

    private final TDMGame game;

    public TDMGameCommands(TDMGame game) {
        super(game);
        this.game = game;
    }

    @Override
    public void run(Player user, String[] args, int perms) {
        // Go through each base game command
        super.run(user, args, perms);
        // Add gamemode specific commands
        if ("settimer".equals(args[0])) {
            settimer(user, args, perms);
        }
    }

    @Override
    public List<String> tabComplete(Player user, String[] args, int perms) {
        // Get default tab completions
        List<String> tabCompletions = super.tabComplete(user, args, perms);

        // Add other tab completions
        int level = args.length;
        if (level == 1) {
            if (perms >= 1) {
                tabCompletions.add("settimer");
            }
        }

        return tabCompletions;
    }

    public void settimer (Player user, String[] args, int perms) {

        if (checkIfPerms(user, perms, 1)) return;

        if (!game.isTimerEnabled()) {
            sendColorMessage(user, "The timer is not enabled right now!", NamedTextColor.YELLOW);
            return;
        }

        if (args.length < 2) {
            sendColorMessage(user, "You must include an integer to set the time to! (Must be in seconds)", NamedTextColor.YELLOW);
            return;
        }

        try {
            Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            sendColorMessage(user, "'" + args[1] + "' is not an integer! You must include an integer to set the time to! (Must be in seconds)", NamedTextColor.YELLOW);
            return;
        }

        int newTime =  Integer.parseInt(args[1]);

        if (newTime < 1) {
            sendColorMessage(user, "The time must be at least 1 second!", NamedTextColor.YELLOW);
            return;
        }

        if (newTime > game.getMaxTimer()) {
            sendColorMessage(user, "The time must be lower than " + game.getMaxTimer() + " seconds!", NamedTextColor.YELLOW);
            return;
        }

        game.setTimer(newTime);
        broadcastAction(user, "used /game settimer to set remaining game time to " + game.timerToText());
    }
}