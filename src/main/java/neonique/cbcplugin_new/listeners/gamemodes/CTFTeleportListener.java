package neonique.cbcplugin_new.listeners.gamemodes;

import neonique.cbcplugin_new.gamemodes.ctf.CTFGame;
import neonique.cbcplugin_new.gamemodes.ctf.CTFPlayer;
import neonique.cbcplugin_new.playerclasses.CBCPlayer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerTeleportEvent;

public class CTFTeleportListener implements Listener {

    private final CTFGame game;

    public CTFTeleportListener(CTFGame game) {
        this.game = game;
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

        // Check if player is a CTF player
        if (!(player instanceof CTFPlayer)) {
            return;
        }

        CTFPlayer ctfPlayer = (CTFPlayer) player;

        // Check if teleport reason is spectate
        if (e.getCause() == PlayerTeleportEvent.TeleportCause.SPECTATE) {

            if (ctfPlayer.isEliminated()) return;

            playerEntity.sendMessage(Component.text("You cannot use spectator mode to teleport to players!").color(NamedTextColor.YELLOW));
            e.setCancelled(true);
        }
    }
}
