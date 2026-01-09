package neonique.cbcplugin_new.listeners.gamemodes;

import neonique.cbcplugin_new.CBCPlugin;
import neonique.cbcplugin_new.gamemodes.crossbowtag.TagGame;
import neonique.cbcplugin_new.gamemodes.crossbowtag.TagPlayer;
import neonique.cbcplugin_new.gamemodes.ctf.CTFPlayer;
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
        if (e.getCause() != PlayerTeleportEvent.TeleportCause.SPECTATE) return;

        Player playerEntity = e.getPlayer();
        TagPlayer player = game.getPlayer(playerEntity);
        if (player == null) {
            return;
        }

        if (player.isTagger()) {
            playerEntity.sendMessage(Component.text("You cannot use spectator to teleport to players!").color(NamedTextColor.YELLOW));
            e.setCancelled(true);
        }

    }
}
