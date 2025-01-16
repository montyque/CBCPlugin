package neonique.cbcplugin_new.commands;

import neonique.cbcplugin_new.CBCPlugin;
import neonique.cbcplugin_new.managers.GameManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ReloadCBCCommand extends _BaseCommand {

    GameManager gameManager;

    public ReloadCBCCommand(GameManager gameManager) {
        this.gameManager = gameManager;
    }


    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {

        if (!(sender instanceof Player)) {
            return true;
        }

        // Get user and their permissions
        Player user = (Player) sender;
        int perms = getPerms(user);

        if (!CBCPlugin.isPlayerOperator(user.getUniqueId())) {
            user.sendMessage(Component.text("You do not have permission to run this command."));
            return true;
        }

        if (args.length < 1) {
            user.sendMessage(Component.text("You must specify what to reload!").color(NamedTextColor.YELLOW));
            return true;
        }

        String reloadtype = args[0].toLowerCase();

        if (reloadtype.equals("maps")) {
            user.sendMessage(Component.text("Reloading maps...").color(NamedTextColor.GREEN));
            gameManager.loadMaps();
            user.sendMessage(Component.text("Maps loaded!").color(NamedTextColor.GREEN));
        }
        else if (reloadtype.equals("weaponpresets")) {
            user.sendMessage(Component.text("Reloading weapon presets...").color(NamedTextColor.GREEN));
            gameManager.combatManager.loadWeaponPresets();
            user.sendMessage(Component.text("Weapon presets loaded!").color(NamedTextColor.GREEN));
        }
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
        return null;
    }
}
