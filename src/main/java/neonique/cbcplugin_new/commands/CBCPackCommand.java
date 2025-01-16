package neonique.cbcplugin_new.commands;

import neonique.cbcplugin_new.commands.cbcpacksubcommands.cbcpack_reloadplayerhead;
import neonique.cbcplugin_new.resourcepack.ResourcePackManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class CBCPackCommand extends _BaseCommand {

    private final ResourcePackManager resourcePackManager;

    public CBCPackCommand (ResourcePackManager resourcePackManager) {
        this.resourcePackManager = resourcePackManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String s, @NotNull String[] args) {

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

        // Check if they have CBC operator permissions
        if (checkIfPerms(user, perms, 2)) return true;

        if (subcommand.equals("reloadplayerhead")) {
            cbcpack_reloadplayerhead.run(this, user, args, perms);
        }

        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String s, @NotNull String[] args) {

        int level = args.length;

        if (!(sender instanceof Player)) {
            return new ArrayList<>();
        }

        // Get user and their permissions
        Player user = (Player) sender;
        int perms = getPerms(user);

        if (perms < 2) return new ArrayList<>();

        List<String> tabCompletions = new ArrayList<>();

        if (level == 1) {
            tabCompletions.add("reloadplayerhead");
        }
        else if (level >= 2) {
            if (args[0].equals("reloadplayerhead")) {
                tabCompletions = _SubCommand.getAllPlayerNames();
            }
        }

        return tabCompletions;
    }

    public ResourcePackManager getResourcePackManager() {
        return resourcePackManager;
    }
}
