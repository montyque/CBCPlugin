package neonique.cbcplugin_new.commands;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collection;

public class _SubCommand {

    // Get subcommand information
    private _BaseCommand baseCommand;
    private int permsRequired;
    private String subcommandString;

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
