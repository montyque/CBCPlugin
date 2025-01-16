package neonique.cbcplugin_new.commands;

import neonique.cbcplugin_new.CBCPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

public abstract class _BaseCommand implements TabExecutor {

    // Command information
    private String command = "";
    private int minimumPerms = 0;

    // Create hashmap for all subcommands (key is subcommand, value is _SubCommand object to run)
    private final HashMap<String, _SubCommand> registeredSubcommands = new HashMap<>();

    public boolean checkIfPerms (Player user, int userPerms, int permsNeeded) {
        if (userPerms < permsNeeded) {
            user.sendMessage(Component.text("You do not have permission to run this command.").color(NamedTextColor.RED));
            return true;
        } else {
            return false;
        }
    }

    public int getPerms (Player user) {

        if (CBCPlugin.isPlayerOperator(user.getUniqueId())) {
            return 2;
        }

        // Set perms number depending on the player's tags and permissions
        if (CBCPlugin.isPlayerAdmin(user.getUniqueId())) {
            return 1;
        }

        // Return default parameters
        return 0;
    }

    public static ArrayList<String> getAllPlayerNames () {
        ArrayList<String> playerNames = new ArrayList<>();
        Player[] players = new Player[Bukkit.getServer().getOnlinePlayers().size()];
        Bukkit.getServer().getOnlinePlayers().toArray(players);
        for (Player player : players) {
            playerNames.add(player.getName());
        }
        return playerNames;
    }

    public static ArrayList<String> getPlayerNames (Collection<Player> players) {
        ArrayList<String> playerNames = new ArrayList<>();
        for (Player player : players) {
            playerNames.add(player.getName());
        }
        return playerNames;
    }

    public static Player getPlayer (String playerName) {
        return Bukkit.getServer().getPlayer(playerName);
    }

    // This is used if you want to get a player and send an error message if the player does not exist
    public static Player getPlayerOrError (String playerName, Player user) {
        Player player = Bukkit.getServer().getPlayer(playerName);
        if (player == null) {
            user.sendMessage(
                    Component.text("Player ").color(NamedTextColor.YELLOW)
                            .append(Component.text(playerName).color(NamedTextColor.GREEN))
                            .append(Component.text(" was not found!").color(NamedTextColor.YELLOW))
            );
            return null;
        } else {
            return player;
        }
    }
}
