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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CBCReloadCommand extends _BaseCommand {

    GameManager gameManager;

    public CBCReloadCommand(GameManager gameManager) {
        this.gameManager = gameManager;
    }


    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {

        if (!(sender instanceof Player user)) {
            return true;
        }

        if (!CBCPlugin.isPlayerOperator(user.getUniqueId())) {
            user.sendMessage(Component.text("You do not have permission to run this command."));
            return true;
        }

        if (args.length < 1) {
            user.sendMessage(Component.text("You must specify what to reload!").color(NamedTextColor.YELLOW));
            return true;
        }

        String reloadType = args[0].toLowerCase();

        if (reloadType.equals("maps")) {
            user.sendMessage(Component.text("Reloading maps...").color(NamedTextColor.GREEN));
            gameManager.loadMaps();
            user.sendMessage(Component.text("Maps loaded!").color(NamedTextColor.GREEN));
        }
        else if (reloadType.equals("weaponpresets")) {
            user.sendMessage(Component.text("Reloading weapon presets...").color(NamedTextColor.GREEN));
            CBCPlugin.getPlugin().getWeaponPresetService().loadWeaponPresets();
            user.sendMessage(Component.text("Weapon presets loaded!").color(NamedTextColor.GREEN));
        }
        else if (reloadType.equals("deathmessages")) {
            user.sendMessage(Component.text("Reloading death messages...").color(NamedTextColor.GREEN));

            boolean success = gameManager.combatManager.getDeathMessageManager().loadDeathMessages();
            if (success) {
                user.sendMessage(Component.text("Loaded death messages!").color(NamedTextColor.GREEN));
            } else {
                user.sendMessage(Component.text("Error occurred while loading death messages!").color(NamedTextColor.RED));
            }
        }
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
                                                @NotNull String alias, @NotNull String[] args) {

        int level = args.length;

        if (!(sender instanceof Player user)) {
            return new ArrayList<>();
        }

        if (!CBCPlugin.isPlayerOperator(user.getUniqueId())) {
            return new ArrayList<>();
        }

        if (level == 1) {
            // Add sub commands to list
            return new ArrayList<>(Arrays.asList("maps", "weaponpresets", "deathmessages"));
        }

        return new ArrayList<>();
    }
}
