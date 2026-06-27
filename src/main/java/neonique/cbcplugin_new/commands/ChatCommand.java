package neonique.cbcplugin_new.commands;

import neonique.cbcplugin_new.managers.ChatType;
import neonique.cbcplugin_new.core.Game;
import neonique.cbcplugin_new.managers.ChatManager;
import neonique.cbcplugin_new.managers.GameManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ChatCommand extends _BaseCommand {

    private final GameManager gameManager;
    private final ChatManager chatManager;

    public ChatCommand (GameManager gameManager, ChatManager chatManager) {
        this.gameManager = gameManager;
        this.chatManager = chatManager;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {

        int level = args.length;

        if (!(sender instanceof Player user)) {
            return true;
        }

        // Get user
        UUID userUUID = user.getUniqueId();

        // Check what chat user is in
        ChatType chatType = chatManager.getChatType(userUUID);

        // If user does not provide argument, say what chat player is
        if (level == 0) {
            user.sendMessage(Component.text("You are currently in " + chatType + " chat. To switch chat, use /chat <type>.").color(NamedTextColor.YELLOW));
            return true;
        }

        // Get new chat
        String newChatTypeString = args[0];
        ChatType newChatType;
        try {
            newChatType = ChatType.valueOf(newChatTypeString.toUpperCase());
        } catch (IllegalArgumentException e) {
            user.sendMessage(Component.text("Chat type " + newChatTypeString.toLowerCase() + " is not a valid chat type!").color(NamedTextColor.YELLOW));
            return true;
        }

        // Check if player is already in this chat
        if (chatType == newChatType) {
            user.sendMessage(Component.text("You are already in " + newChatType + " chat!").color(NamedTextColor.YELLOW));
            return true;
        }

        // Check if player is trying to change to team, and check if they can do so
        if (newChatType == ChatType.TEAM) {

            Game<?, ?> game = gameManager.getCurrentGame();

            // Check if game is over already
            if (game == null) {
                user.sendMessage(Component.text("You are not on a team right now!").color(NamedTextColor.YELLOW));
                return true;
            }

            if (game.isGameOver()) {
                user.sendMessage(Component.text("You are not on a team right now!").color(NamedTextColor.YELLOW));
                return true;
            }

            // Check if game is a team game
            if (!game.getGamemode().isTeamGamemode()) {
                user.sendMessage(Component.text("You are not on a team right now!").color(NamedTextColor.YELLOW));
                return true;
            }

            // Check if player is in game
            if (!game.hasPlayer(user)) {
                user.sendMessage(Component.text("You are not on a team right now!").color(NamedTextColor.YELLOW));
                return true;
            }

            // Check if player has a team
            if (game.getPlayer(user).team() == null) {
                user.sendMessage(Component.text("You are not on a team right now!").color(NamedTextColor.YELLOW));
                return true;
            }
        }

        chatManager.setChatType(userUUID, newChatType);
        user.sendMessage(Component.text("You are now in " + newChatType + " chat.").color(NamedTextColor.GREEN));
        return true;

    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {

        List<String> tabCompletions = new ArrayList<>();
        if (args.length == 1) {
            for (ChatType chatType : ChatType.values()) {
                tabCompletions.add(chatType.toString().toLowerCase());
            }
        }

        return tabCompletions;

    }
}
