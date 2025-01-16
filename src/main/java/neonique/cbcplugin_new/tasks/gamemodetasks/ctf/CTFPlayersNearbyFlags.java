package neonique.cbcplugin_new.tasks.gamemodetasks.ctf;

import neonique.cbcplugin_new.gamemodes.ctf.CTFGame;
import neonique.cbcplugin_new.gamemodes.ctf.CTFPlayer;
import neonique.cbcplugin_new.gamemodes.ctf.CTFTeam;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Objects;

public class CTFPlayersNearbyFlags extends BukkitRunnable {

    private CTFGame game;

    public CTFPlayersNearbyFlags (CTFGame game) {
        this.game = game;
    }

    @Override
    public void run() {

        if (this.game.isGameOver()) return;
        if (!this.game.canCaptureOrTake()) return;

        // Go through all teams and their flag locations
        for (CTFTeam team : game.getTeams()) {

            Location flagLocation = team.getFlagLocation().clone().add(0, 1, 0);

            for (Player player : flagLocation.getNearbyEntitiesByType(Player.class, 2)) {

                if (game.getPlayer(player) != null) {

                    CTFPlayer playerObj = (CTFPlayer) game.getPlayer(player);
                    // Check if the player is alive
                    if (!playerObj.isAlive()) continue;

                    // Check if the player is an enemy player
                    if (!Objects.equals(playerObj.getTeam().getTeamName(), team.getTeamName())) {

                        // Check if player is not holding a flag
                        if (playerObj.getFlagHeld() != null) continue;

                        // Check if team has a flag at their base
                        if (!team.isFlagAtBase()) continue;

                        // Let the player pick up the flag
                        playerObj.playerPickupFlag(team);
                        team.flagPickedUp(playerObj);
                    }
                    // The player is an allied player
                    else {

                        // Check if player is holding a flag
                        if (playerObj.getFlagHeld() == null) continue;

                        // Capture the flag
                        CTFTeam teamHeldFlag = playerObj.getFlagHeld();
                        playerObj.playerCaptureFlag();
                        teamHeldFlag.flagCaptured();
                    }
                }

            }
        }
    }
}
