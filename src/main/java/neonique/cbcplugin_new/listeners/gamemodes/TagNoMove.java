package neonique.cbcplugin_new.listeners.gamemodes;

import neonique.cbcplugin_new.gamemodes.crossbowtag.TagGame;
import neonique.cbcplugin_new.gamemodes.crossbowtag.TagPlayer;
import neonique.cbcplugin_new.gamemodes.crossbowtag.TagTeam;
import neonique.cbcplugin_new.managers.GameManager;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

public class TagNoMove implements Listener {

    private final GameManager gameManager;
    private final TagGame game;

    private boolean evadersMove = false;

    public TagNoMove (GameManager gameManager, TagGame game) {
        this.gameManager = gameManager;
        this.game = game;
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent e) {

        if (gameManager.getCurrentGame() == null) {
            PlayerMoveEvent.getHandlerList().unregister(this);
            return;
        }


        Player playerEntity = e.getPlayer();
        TagPlayer player = game.getPlayer(playerEntity);
        if (player == null) {
            return;
        }
        TagTeam team = game.getPlayerTeam(player);

        if (team != game.getTaggers() && evadersMove) return;

        // Prevent player from moving by teleporting them back to their original location
        Location from = e.getFrom().clone();
        Location to = e.getTo().clone();
        if (from.getX() == to.getX() && from.getZ() == to.getZ()) return;
        e.setTo(from);

    }

    public void setEvadersMove (boolean b) {
        evadersMove = b;
    }
}
