package neonique.cbcplugin_new.commands_old;

import neonique.cbcplugin_new.CBCPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

public class LaunchCommand implements CommandExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        int level = args.length;
        if (!(sender instanceof Player)) {
            return true;
        }

        Player user = (Player) sender;

        if (!user.getScoreboardTags().contains("Admin")) {
            user.sendMessage(Component.text("You do not have permission to run this command.").color(NamedTextColor.RED));
            return true;
        }

        if (level < 3) {
            user.sendMessage(Component.text("Not enough arguments! Usage of command: /launch <xvel> <yvel> <zvel> [test]").color(NamedTextColor.YELLOW));
            return true;
        }

        Vector velVector;
        try {
            velVector = new Vector(
                    Double.parseDouble(args[0]),
                    Double.parseDouble(args[1]),
                    Double.parseDouble(args[2])
            );
        } catch (Exception e) {
            user.sendMessage(Component.text("Could not parse the vector numbers!").color(NamedTextColor.YELLOW));
            return true;
        }

        if (level == 4) {
            // Launch testing villager
            ArmorStand armorStand = user.getWorld().spawn(user.getLocation(), ArmorStand.class);
            armorStand.setVelocity(velVector);
            new LaunchParticleTask(armorStand).runTaskTimer(CBCPlugin.getPlugin(), 0, 1);
        }
        else {
            // Launch player
            user.setVelocity(velVector);
        }
        user.sendMessage(Component.text("Woooooosh!").color(NamedTextColor.GREEN));
        return true;
    }

}
