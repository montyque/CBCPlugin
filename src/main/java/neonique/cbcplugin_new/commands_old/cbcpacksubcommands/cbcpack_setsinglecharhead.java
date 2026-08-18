package neonique.cbcplugin_new.commands_old.cbcpacksubcommands;

import neonique.cbcplugin_new.commands_old.CBCPackCommand;
import neonique.cbcplugin_new.commands_old._SubCommand;
import neonique.cbcplugin_new.resourcepack.ResourcePackManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

public class cbcpack_setsinglecharhead  extends _SubCommand {

    public static void run(CBCPackCommand command, Player user, String[] args, int perms) {

        int level = args.length;

        if (level < 2) {
            user.sendMessage(Component.text("You must state 'true' or 'false'!").color(NamedTextColor.YELLOW));
            return;
        }

        // Get player head component
        ResourcePackManager rpm = command.getResourcePackManager();

        // Get Player with username with UUID
        if (args[1].equalsIgnoreCase("true")) {
            rpm.setSingleCharHead(true);
            user.sendMessage(Component.text("Player head displays will now use single unicode characters.").color(NamedTextColor.GREEN));
        } else if (args[1].equalsIgnoreCase("false")) {
            rpm.setSingleCharHead(false);
            user.sendMessage(Component.text("Player head displays will now use multiple unicode characters if needed.").color(NamedTextColor.GREEN));
        } else {
            user.sendMessage(Component.text("You must state 'true' or 'false'!").color(NamedTextColor.YELLOW));
        }
    }
}
