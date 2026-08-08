package neonique.cbcplugin_new.commands;

import neonique.cbcplugin_new.commands.practicesubcommands.*;
import neonique.cbcplugin_new.managers.GameManager;
import neonique.cbcplugin_new.practice.PracticeManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class PracticeCommand extends _BaseCommand {

    public GameManager gameManager;
    public PracticeManager practiceManager;

    public PracticeCommand (GameManager gameManager, PracticeManager practiceManager) {
        this.gameManager = gameManager;
        this.practiceManager = practiceManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {

        int level = args.length;

        if (!(sender instanceof Player)) {
            return true;
        }

        // Get user and their permissions
        Player user = (Player) sender;
        int perms = getPerms(user);

        String subcommand = args[0].toLowerCase();
        if (gameManager.isPracticeActive()) {
            // Subcommands that can be used when practice is active
            switch (subcommand) {
                case "close":
                    if (checkIfPerms(user, perms, 1)) return true;
                    practice_close.run(this, user, args, perms);
                    break;
                case "setweaponpreset":
                    practice_setweaponpreset.run(this, user, args, perms);
                    break;
                case "leave":
                    practice_leave.run(this, user, args, perms);
                    break;
                case "changemap":
                    practice_changemap.run(this, user, args, perms);
                    break;
            }
        }

        else {
            // Subcommands that should only be used when practice is not active
            if ("start".equals(subcommand)) {
                if (checkIfPerms(user, perms, 1)) return true;
                practice_start.run(this, user, args, perms);
            }
        }

        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {

        if (!(sender instanceof Player)) {
            return null;
        }

        // Get user and their permissions
        Player user = (Player) sender;
        int perms = getPerms(user);

        List<String> tabCompleters = new ArrayList<>();
        int level = args.length;
        if (level == 1) {
            if (gameManager.isPracticeActive()) {
                // Add sub commands to list
                tabCompleters.add("leave");
                if (perms >= 1) {
                    tabCompleters.add("close");
                    tabCompleters.add("setweaponpreset");
                    tabCompleters.add("changemap");
                }
            } else if (perms >= 1) {
                // Only show start
                tabCompleters.add("start");
            }
        } else if (level >= 2) {
            String subcommand = args[0].toLowerCase();
            // Show arguments for sub commands
            switch (subcommand) {
                case "close":
                    if (perms < 1) break;
                    tabCompleters = practice_close.getTabCompletions(this, args, perms);
                    break;
                case "setweaponpreset":
                    if (perms < 1) break;
                    tabCompleters = practice_setweaponpreset.getTabCompletions(this, args, perms);
                    break;
                case "leave":
                    tabCompleters = practice_leave.getTabCompletions(this, args, perms);
                    break;
                case "changemap":
                    tabCompleters = practice_changemap.getTabCompletions(this, args, perms);
                    break;
                case "start":
                    if (perms < 1) break;
                    tabCompleters = practice_start.getTabCompletions(this, args, perms);
                    break;
            }
        }
        return tabCompleters;

    }

}
