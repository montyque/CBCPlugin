package neonique.cbcplugin_new.tasks.gamemodetasks.assassin;

import neonique.cbcplugin_new.gamemodes.assassin.AssassinGame;
import neonique.cbcplugin_new.gamemodes.assassin.AssassinPlayer;
import neonique.cbcplugin_new.playerclasses.CBCPlayer;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class AssassinGlowUpdateTask extends BukkitRunnable {

    public AssassinGame game;

    public AssassinGlowUpdateTask(AssassinGame game) {
        this.game = game;
    }

    @Override
    public void run() {

        if (this.game.isGameOver()) return;

        // Get all players in the world
        for (AssassinPlayer player : game.getPlayers()) {

            AssassinPlayer glowingPlayer = player.getCurrentTarget();

            if (glowingPlayer == null) {
                game.getGlowManager().updateGlowingPlayer(player.getPlayer(), null);
            } else if (glowingPlayer.isAlive()) {
                game.getGlowManager().updateGlowingPlayer(player.getPlayer(), glowingPlayer.getPlayer());
            } else {
                game.getGlowManager().updateGlowingPlayer(player.getPlayer(), null);
            }

        }

    }

}
