package neonique.cbcplugin_new.tasks.gamemodetasks.ctf;

import neonique.cbcplugin_new.gamemodes.ctf.CTFGame;
import neonique.cbcplugin_new.gamemodes.ctf.CTFPlayer;
import neonique.cbcplugin_new.gamemodes.ctf.CTFTeam;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Objects;

public class CTFPlayersNearbyFlags extends BukkitRunnable {

    private final CTFGame game;

    public CTFPlayersNearbyFlags (CTFGame game) {
        this.game = game;
    }

    @Override
    public void run() {

        if (this.game.isGameOver()) {
            this.cancel();
            return;
        }

        if (!this.game.canCaptureOrTake()) return;

        for (CTFTeam team : game.getTeams()) {

            Location flagLocation = team.getFlagLocation();

            // Find all players within a radius of 2
            for (Player playerEntity : flagLocation.getNearbyEntitiesByType(Player.class, 2)) {

                CTFPlayer player = game.getPlayer(playerEntity);
                if (player == null) continue;

                if (!player.isAlive()) continue;

                // Make sure player isn't one block or more below the flag
                if (playerEntity.getLocation().getY() - flagLocation.getY() < -1) continue;

                // Check if the player is picking up an enemy flag or capturing a flag at their own base
                if (player.getTeam() != team) {

                    // Check if this team has a flag that can be picked up
                    if (!team.isFlagAtBase()) continue;

                    // Check if player is already holding a flag
                    if (player.getFlagHeld() != null) continue;

                    // Player picks up the enemy flag
                    player.playerPickupFlag(team);
                    team.flagPickedUp(player);

                } else {

                    // Check if player is holding a flag
                    if (player.getFlagHeld() == null) continue;

                    // Capture the flag
                    CTFTeam teamHeldFlag = player.getFlagHeld();
                    player.playerCaptureFlag();
                    teamHeldFlag.flagCaptured();

                }
            }
        }
    }

}
