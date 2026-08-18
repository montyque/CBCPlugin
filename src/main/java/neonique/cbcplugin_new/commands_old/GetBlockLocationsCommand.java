package neonique.cbcplugin_new.commands_old;

import neonique.cbcplugin_new.CBCPlugin;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class GetBlockLocationsCommand implements TabExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        int level = args.length;
        if (!(sender instanceof Player)) {
            return true;
        }

        Player user = (Player) sender;

        if (level < 7) {
            user.sendMessage(Component.text("Not enough arguments! Usage of command: /getblocklocations <block> <from> <to> [limit]").color(NamedTextColor.YELLOW));
            return true;
        }

        String blockString = args[0];
        Material blockToFind = Material.getMaterial(blockString.toUpperCase());
        if (blockToFind == null) {
            user.sendMessage(Component.text("Block \"" + blockString + "\" not found!").color(NamedTextColor.YELLOW));
            return true;
        }



        Location fromVector;
        try {
            fromVector = new Location(
                    user.getWorld(),
                    Math.min(Double.parseDouble(args[1]), Double.parseDouble(args[4])),
                    Math.min(Double.parseDouble(args[2]), Double.parseDouble(args[5])),
                    Math.min(Double.parseDouble(args[3]), Double.parseDouble(args[6]))
            );
        } catch (Exception e) {
            user.sendMessage(Component.text("Could not parse the 'from' block coordinates!").color(NamedTextColor.YELLOW));
            return true;
        }

        Location toVector;
        try {
            toVector = new Location(
                    user.getWorld(),
                    Math.max(Double.parseDouble(args[1]), Double.parseDouble(args[4])),
                    Math.max(Double.parseDouble(args[2]), Double.parseDouble(args[5])),
                    Math.max(Double.parseDouble(args[3]), Double.parseDouble(args[6]))
            );
        } catch (Exception e) {
            user.sendMessage(Component.text("Could not parse the 'to' block coordinates!").color(NamedTextColor.YELLOW));
            return true;
        }

        int limit = 1000000;
        try {
            limit = Integer.parseInt(args[7]);
        } catch (Exception ignored) {}

        Set<Location> blocksFoundCoords = new HashSet<>();
        for (double x = fromVector.getX(); x <= toVector.getX(); x++) {
            for (double y = fromVector.getY(); y <= toVector.getY(); y++) {
                for (double z = fromVector.getZ(); z <= toVector.getZ(); z++) {
                    Location lll = new Location(user.getWorld(), x, y, z);
                    if (lll.getBlock().getType() == blockToFind) {
                        blocksFoundCoords.add(lll);
                    }
                    if (blocksFoundCoords.size() >= limit) break;
                }
                if (blocksFoundCoords.size() >= limit) break;
            }
            if (blocksFoundCoords.size() >= limit) break;
        }

        for (Location location : blocksFoundCoords) {
            CBCPlugin.getPlugin().getLogger().info(
                    "- \"" + location.getBlockX() + " " + location.getBlockY() + " " + location.getBlockZ() + "\""
            );
        }

        user.sendMessage(Component.text("Found " + blocksFoundCoords.size() + " and printed them to console.").color(NamedTextColor.GREEN));
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        return null;
    }
}
