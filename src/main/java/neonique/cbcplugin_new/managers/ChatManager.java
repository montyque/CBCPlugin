package neonique.cbcplugin_new.managers;

import io.papermc.paper.event.player.AsyncChatEvent;
import neonique.cbcplugin_new.CBCPlugin;
import neonique.cbcplugin_new.enums.ChatType;
import neonique.cbcplugin_new.gamemodes._base.CBCTeam;
import neonique.cbcplugin_new.gamemodes._base.Game;
import neonique.cbcplugin_new.playerclasses.CBCPlayer;
import neonique.cbcplugin_new.resourcepack.ResourcePackManager;
import neonique.cbcplugin_new.util.TextUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static neonique.cbcplugin_new.resourcepack.ResourcePackManager.normalText;
import static neonique.cbcplugin_new.resourcepack.ResourcePackManager.smallText;

public class ChatManager implements Listener {

    private final GameManager gameManager;

    private final HashMap<UUID, ChatType> chatTypes;

    public ChatManager (GameManager gameManager) {
        this.gameManager = gameManager;
        this.chatTypes = new HashMap<>();

        CBCPlugin.getPlugin().getServer().getPluginManager().registerEvents(this, CBCPlugin.getPlugin());
    }

    @EventHandler
    public void onPlayerChatMessage (AsyncChatEvent e) {

        Player player = e.getPlayer();
        UUID playerUUID = player.getUniqueId();

        Game game = gameManager.getCurrentGame();

        // Check if game is over already
        if (game == null) return;
        if (game.isGameOver()) return;

        // Check if game is a team game
        if (!game.getGamemode().isTeamGamemode()) return;

        // Check if player is in game
        if (!gameManager.hasPlayer(player)) return;

        // Check if player in team chat
        if (chatTypes.get(playerUUID) != ChatType.TEAM) return;

        CBCPlayer cbcPlayer = gameManager.getPlayer(player);

        // Check if player has a team
        if (cbcPlayer.getTeam() == null) return;
        CBCTeam team = cbcPlayer.getTeam();
        NamedTextColor color = cbcPlayer.getTeam().getColor();

        // Get current message
        Component message = e.message();

        Component newMsg = getChatComponentWithTeam(player, message, team);

        // Only send current messages to teammates
        for (CBCPlayer teammate : team.getOnlinePlayers()) {
            Player teammateEntity = teammate.getPlayer();
            teammateEntity.sendMessage(newMsg);
        }

        // Cancel chat message
        e.setCancelled(true);

    }

    public Component getTeamComponent (NamedTextColor color) {
        return Component.text("[TEAM] ").color(color);
    }

    public Component getChatComponentWithTeam (Player player, Component message, CBCTeam team) {

        // Add team component to the message
        Component msg = getTeamComponent(team.getColor());

        msg = msg.append(Component.text("<").color(NamedTextColor.WHITE)).append(
                Component.text(team.getPrefix() + " ").color(team.getColor()).decorate(TextDecoration.BOLD)
        ).append(
                player.displayName().color(team.getColor())
        ).append(
                Component.text("> ").color(NamedTextColor.WHITE)
        ).append(message.color(NamedTextColor.WHITE));

        return msg;

    }

    public void setChatType (UUID playerUUID, ChatType chatType) {
        chatTypes.put(playerUUID, chatType);
    }

    public void clearChatManager () {
        // This clears the chat manager, putting everyone back into all chat
        chatTypes.clear();
    }

    public ChatType getChatType (UUID playerUUID) {
        return chatTypes.getOrDefault(playerUUID, ChatType.ALL);
    }
}
