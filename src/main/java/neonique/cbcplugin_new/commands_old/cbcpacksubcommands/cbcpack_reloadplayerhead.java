package neonique.cbcplugin_new.commands_old.cbcpacksubcommands;

import neonique.cbcplugin_new.CBCPlugin;
import neonique.cbcplugin_new.commands_old.CBCPackCommand;
import neonique.cbcplugin_new.commands_old._SubCommand;
import neonique.cbcplugin_new.resourcepack.ResourcePackManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class cbcpack_reloadplayerhead extends _SubCommand {

    public static void run(CBCPackCommand command, Player user, String[] args, int perms) {

        int level = args.length;

        if (level < 2) {
            user.sendMessage(Component.text("You must include a player name!").color(NamedTextColor.YELLOW));
            return;
        }

        // Get Player with username with UUID
        Player playerTargeted = Bukkit.getPlayer(args[1]);

        // Check if player exists
        if (playerTargeted == null) {
            user.sendMessage(Component.text("Player not on server right now!").color(NamedTextColor.YELLOW));
            return;
        }

        // Get player head component
        ResourcePackManager rpm = command.getResourcePackManager();

        // Update player head component
        user.sendMessage(Component.text("Attempting to reload player head...").color(NamedTextColor.GREEN));
        new BukkitRunnable() {
            @Override
            public void run() {
                rpm.addPlayerHead(playerTargeted.getUniqueId(), playerTargeted.getName(), CBCPlugin.getPlugin());
            }
        }.runTaskAsynchronously(CBCPlugin.getPlugin());
    }

}
