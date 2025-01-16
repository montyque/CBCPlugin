package neonique.cbcplugin_new.tasks.gamemodetasks.holdthegold;

import neonique.cbcplugin_new.gamemodes.holdthegold.HTGGame;
import neonique.cbcplugin_new.gamemodes.holdthegold.HTGPlayer;
import neonique.cbcplugin_new.gamemodes.holdthegold.HTGTeam;
import neonique.cbcplugin_new.playerclasses.CBCPlayer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Objects;

public class HTGPlayerTrackingTask extends BukkitRunnable {

    private final HTGGame game;

    public HTGPlayerTrackingTask (HTGGame game) {
        this.game = game;
    }

    @Override
    public void run() {

        if (this.game.isGameOver()) {
            this.cancel();
            return;
        }

        for (CBCPlayer player : game.getPlayers().values()) {

            HTGPlayer htgPlayer = (HTGPlayer) player;

            // Update flag holder laser
            // htgPlayer.updateFlagHolderLaser();

            // Show action bar
            if (!player.isOnline()) continue;

            int distanceFromGold = Math.round((float) game.getGoldLocation().distance(player.getPlayer().getLocation()));

            /*if (game.isGoldHeld()) {
                if (Objects.equals(game.getGoldHolder().getPlayerId(), htgPlayer.getPlayerId())) {
                    // Show that player is holding gold
                    htgPlayer.getPlayer().sendActionBar(
                            Component.text("⬛ You're holding the gold! ⬛").color(NamedTextColor.YELLOW).decorate(TextDecoration.BOLD)
                    );
                } else {
                    HTGTeam goldHolderTeam = (HTGTeam) game.getGoldHolder().getTeam();
                    htgPlayer.getPlayer().sendActionBar(
                            Component.text("⬛ ").color(NamedTextColor.YELLOW).decorate(TextDecoration.BOLD).append(
                                    Component.text(game.getGoldHolder().getName() + ": ")
                                            .append(Component.text(distanceFromGold + "m ").decorate(TextDecoration.BOLD))
                                            .append(Component.text("away"))
                            ).color(goldHolderTeam.getColor())
                    );
                }
            } else {
                htgPlayer.getPlayer().sendActionBar(
                        Component.text("⬛ Gold: " + distanceFromGold + "m away").color(NamedTextColor.YELLOW).decorate(TextDecoration.BOLD)
                );
            }*/
        }
    }
}
