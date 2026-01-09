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
        if (e.getCause() != PlayerTeleportEvent.TeleportCause.SPECTATE) return;

        Player playerEntity = e.getPlayer();
        CTFPlayer player = game.getPlayer(playerEntity);
        if (player == null) {
            return;
        }

        if (player.isEliminated()) return;

        // If player tries to teleport to a player while in spectator mode, remove them from team
        playerEntity.sendMessage(Component.text("You cannot use spectator mode to teleport to players!").color(NamedTextColor.YELLOW));
        e.setCancelled(true);

    }
}
