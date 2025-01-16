package neonique.cbcplugin_new.listeners.gamemodes;

import neonique.cbcplugin_new.gamemodes._base.CBCTeam;
import neonique.cbcplugin_new.gamemodes.crossbowtag.TagGame;
import neonique.cbcplugin_new.gamemodes.crossbowtag.TagPlayer;
import neonique.cbcplugin_new.managers.GameManager;
import neonique.cbcplugin_new.playerclasses.CBCPlayer;
import org.bukkit.Location;
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

        // Check that the player is in the game
        if (!gameManager.hasPlayer(e.getPlayer())) return;

        CBCPlayer cbcPlayer = gameManager.getPlayer(e.getPlayer());
        CBCTeam playerTeam = cbcPlayer.getTeam();

        // Check that player's moving is allowed
        if (cbcPlayer instanceof TagPlayer) {
            TagPlayer tagPlayer = (TagPlayer) cbcPlayer;
            if (tagPlayer.isCanMove()) {
                return;
            }
        }

        if (playerTeam != game.getTaggers() && evadersMove) return;

        Location from = e.getFrom().clone();
        Location to = e.getTo().clone();

        if (from.getX() == to.getX() && from.getZ() == to.getZ()) return;

        e.setTo(from);

    }

    public void setEvadersMove (boolean b) {
        evadersMove = b;
    }
}
