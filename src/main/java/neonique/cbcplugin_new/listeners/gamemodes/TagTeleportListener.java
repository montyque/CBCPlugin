package neonique.cbcplugin_new.listeners.gamemodes;

import neonique.cbcplugin_new.CBCPlugin;
import neonique.cbcplugin_new.gamemodes.crossbowtag.TagGame;
import neonique.cbcplugin_new.gamemodes.crossbowtag.TagPlayer;
import neonique.cbcplugin_new.playerclasses.CBCPlayer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerTeleportEvent;

public class TagTeleportListener implements Listener {

    private final TagGame game;

    public TagTeleportListener (TagGame tagGame) {
        this.game = tagGame;
    }

    @EventHandler
    public void onPlayerTeleport (PlayerTeleportEvent e) {

        if (game.isGameOver()) return;

        // Get player
        Player playerEntity = e.getPlayer();

        // Check if player is in game
        if (game.getPlayer(playerEntity) == null) {
            return;
        }

        CBCPlayer player = game.getPlayer(playerEntity);

        // Check if player is a tag player
        if (!(player instanceof TagPlayer)) {
            return;
        }

        TagPlayer tagPlayer = (TagPlayer) player;

        // Check if teleport reason is spectate
        if (e.getCause() == PlayerTeleportEvent.TeleportCause.SPECTATE) {
            if (tagPlayer.isTagger()) {
                playerEntity.sendMessage(Component.text("You cannot use spectator to teleport to players!").color(NamedTextColor.YELLOW));
                e.setCancelled(true);
            }
        }
    }
}
