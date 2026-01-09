package neonique.cbcplugin_new.gamemodes.ctf;

import neonique.cbcplugin_new.gamemodes._base.BaseTeamGameCommands;
import neonique.cbcplugin_new.gamemodes._base.CBCTeam;
import neonique.cbcplugin_new.managers.GameManager;
import neonique.cbcplugin_new.managers.CombatManager;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

import java.util.List;

public class CTFGameCommands extends BaseTeamGameCommands {

    private final CTFGame game;

    public CTFGameCommands(GameManager gm, CombatManager wm, CTFGame game) {
        super(gm, wm, game);
        this.game = game;
    }

    @Override
    public void run(Player user, String[] args, int perms) {
        // Go through each base game command
        super.run(user, args, perms);
        // Add gamemode specific commands
        if ("setflags".equals(args[0])) {
            setFlags(user, args, perms);
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
                // Add all sub commands
                tabCompletions.add("setflags");
            }
        }
        else if (level >= 2) {
            String subcommand = args[0];
            if (perms >= 1) {
                // Set flags sub command - second argument
                if (subcommand.equals("setflags")) {
                    if (level == 2) {
                        tabCompletions = getTeamIds();
                    }
                }
            }


        }

        return tabCompletions;
    }

    public void setFlags (Player user, String[] args, int perms) {

        if (checkIfPerms(user, perms, 1)) return;

        if (args.length < 2) {
            sendColorMessage(user, "You must include a team to change the flags of!", NamedTextColor.YELLOW);
            return;
        }

        if (args.length < 3) {
            sendColorMessage(user, "You must include the amount of flags you are setting this team to! (Must be integer, e.g. 2)", NamedTextColor.YELLOW);
            return;
        }

        try {
            Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            sendColorMessage(user, "'" + args[2] + "' is not an integer! You must include an integer to set the time to! (Must be in seconds)", NamedTextColor.YELLOW);
            return;
        }

        // Find team and check if this team exists
        CBCTeam<?> team = findTeam(user, args[1]);
        if (team == null) return;

        int newFlagAmount = Integer.parseInt(args[2]);

        if (newFlagAmount < 0) {
            sendColorMessage(user, "You cannot set a team's flags to a negative number!", NamedTextColor.YELLOW);
            return;
        }

        CTFTeam ctfTeam = game.getTypedTeam(team);
        ctfTeam.setFlagsLeft(newFlagAmount);

        sendColorMessage(user, "Set flags left amount for " + ctfTeam.getTeamName() + " Team to " + newFlagAmount + "!", NamedTextColor.GREEN);
        broadcastAction(user, "set flags left amount for " + ctfTeam.getTeamName() + " Team to " + newFlagAmount);
    }
}
