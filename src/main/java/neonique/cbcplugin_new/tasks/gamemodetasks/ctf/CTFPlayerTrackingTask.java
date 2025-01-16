package neonique.cbcplugin_new.tasks.gamemodetasks.ctf;

import neonique.cbcplugin_new.gamemodes.ctf.CTFGame;
import neonique.cbcplugin_new.gamemodes.ctf.CTFPlayer;
import neonique.cbcplugin_new.gamemodes.ctf.CTFTeam;
import neonique.cbcplugin_new.playerclasses.CBCPlayer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class CTFPlayerTrackingTask extends BukkitRunnable {

    private final CTFGame game;

    public CTFPlayerTrackingTask(CTFGame game) {
        this.game = game;
    }

    @Override
    public void run() {

        if (this.game.isGameOver()) {
            this.cancel();
            return;
        }

        // Go through each team
        for (CTFTeam team : game.getTeams()) {

            for (CBCPlayer player : team.getOnlinePlayers()) {
                CTFPlayer ctfPlayer = (CTFPlayer) player;

                if (team.getFlagHolder() != null) {
                    CTFPlayer flagHolder = team.getFlagHolder();
                    Player flagHolderPlr = flagHolder.getPlayer();

                    // Add laser tracking
                    try {
                        ctfPlayer.updateFlagHolderLaser();
                    } catch (ReflectiveOperationException ignored) {}


                    if (ctfPlayer.getFlagHeld() == null) {
                        CTFTeam flagHolderTeam = (CTFTeam) flagHolder.getTeam();
                        int distanceFromFlagHolder = Math.round((float) flagHolderPlr.getLocation().distance(player.getPlayer().getLocation()));
                    }
                }

                if (ctfPlayer.getFlagHeld() != null) {

                    // Add laser tracking
                    try {
                        ctfPlayer.updateToBaseLaser();
                    } catch (ReflectiveOperationException ignored) {}


                    CTFTeam flagHeld = ctfPlayer.getFlagHeld();
                    int distanceFromBase = Math.round((float) player.getPlayer().getLocation().distance(team.getFlagLocation()));

                    /*ctfPlayer.getPlayer().sendActionBar(
                            Component.text("⚑ YOU HAVE THE " + flagHeld.getTeamName() + " FLAG! ").color(flagHeld.getColor())
                                    .append(
                                            Component.text("RUN BACK TO BASE! (" + distanceFromBase + "m)").color(team.getColor())
                                    ).decorate(TextDecoration.BOLD)
                    );*/
                }
            }

        }
    }
}
