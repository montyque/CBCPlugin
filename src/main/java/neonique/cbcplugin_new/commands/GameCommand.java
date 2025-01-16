package neonique.cbcplugin_new.commands;

import neonique.cbcplugin_new.commands.gamesubcommands.game_end;
import neonique.cbcplugin_new.commands.gamesubcommands.game_lastgamestats;
import neonique.cbcplugin_new.enums.GameState;
import neonique.cbcplugin_new.managers.GameManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class GameCommand extends _BaseCommand {

    public GameManager gameManager;

    public GameCommand(GameManager gameManager) {
        this.gameManager = gameManager;
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

        if (level == 0) {
            user.sendMessage(Component.text("You must include a subcommand!").color(NamedTextColor.YELLOW));
            return true;
        }

        String subcommand = args[0].toLowerCase();
        if (gameManager.getGameState() == GameState.ACTIVE) {
            // ************************************************************
            // END COMMAND - Ends the current game
            if (checkIfPerms(user, perms, 1)) return true;
            if (subcommand.equals("end")) {
                game_end.run(this, user, args, perms);
                return true;
            }
            // ************************************************************
            // ANY OTHER COMMANDS - Run current game commands
            if (gameManager.getGameCommands() != null) {
                gameManager.getGameCommands().run(user, args, perms);
            }
        } else {
            // ************************************************************
            // LASTGAMESTATS COMMAND - Displays the stats of the last game to the player
            if (subcommand.equals("lastgamestats")) {
                game_lastgamestats.run(this, user, args, perms);
            }
        }

        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {

        int level = args.length;

        if (!(sender instanceof Player)) {
            return new ArrayList<>();
        }

        // Get user and their permissions
        Player user = (Player) sender;
        int perms = getPerms(user);

        List<String> tabCompletions = new ArrayList<>();

        if (gameManager.getGameState() == GameState.ACTIVE) {
            // ************************************************************
            // Get tab completions from game
            if (gameManager.getGameCommands() != null) {
                tabCompletions = gameManager.getGameCommands().tabComplete(user, args, perms);
            }
            // Add end command onto tab completions
            if (perms >= 1) {
                if (level == 1) {
                    tabCompletions.add("end");
                }
            }

        } else {
            // ************************************************************
            // LASTGAMESTATS COMMAND - Displays the stats of the last game to the player
            tabCompletions.add("lastgamestats");
        }

        return tabCompletions;
    }
}
