package neonique.cbcplugin_new.commands;

import neonique.cbcplugin_new.enums.ChatType;
import neonique.cbcplugin_new.gamemodes._base.Game;
import neonique.cbcplugin_new.gamemodes.rendezvous.RendezvousPlayer;
import neonique.cbcplugin_new.playerclasses.CBCPlayer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class AlphaOrderCommand extends _BaseCommand {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {

        int level = args.length;

        if (!(sender instanceof Player)) {
            return true;
        }

        // Get user
        Player user = (Player) sender;

        if (level == 0) {
            user.sendMessage(Component.text("You must add multiple arguments to order!").color(NamedTextColor.YELLOW));
            return true;
        }

        // Create two lists - sortedArgsList will be the sorted one, while argsList is in the original order
        List<String> sortedArgsList = Arrays.asList(args);
        List<String> originalArgsList = new ArrayList<>(sortedArgsList);

        // Sort alphabetically
        sortedArgsList.sort(Comparator.comparing(String::toLowerCase));

        StringBuilder numbers = new StringBuilder();

        // Go through each character in number order list
        for (String str : originalArgsList) {

            // Get index of string in alphabetical list
            int index = sortedArgsList.indexOf(str);
            if (index < 0) continue;

            // Get the number
            int numberInAlphabeticalList = index + 1;
            numbers.append(numberInAlphabeticalList);

        }

        // Say number string
        user.sendMessage(Component.text("Number order generated: ").color(NamedTextColor.GREEN).append(
                Component.text(numbers.toString()).color(NamedTextColor.AQUA)
        ));

        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        return _SubCommand.getAllPlayerNames();
    }

}
