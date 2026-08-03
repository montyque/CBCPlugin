package neonique.cbcplugin_new.gamemodes.assassin.tasks;

import neonique.cbcplugin_new.gamemodes.assassin.AssassinGame;
import neonique.cbcplugin_new.gamemodes.assassin.AssassinPlayer;
import org.bukkit.scheduler.BukkitRunnable;

public class AssassinTargetChangeTimer extends BukkitRunnable {

    private final AssassinGame game;

    public AssassinTargetChangeTimer (AssassinGame game) {
        this.game = game;
    }

    @Override
    public void run() {

        // Cancel task if game is already over
        if (game.isGameOver()) {
            this.cancel();
            return;
        }

        // Do not run task if game has already decided a winner
        if (game.getWinner() != null) {
            return;
        }

        // Decrement player's target change timer
        for (AssassinPlayer player : game.players()) {
            player.decrementTargetChangeTimer();
        }

        // Update bossbar
        game.updateBossbarManager();

    }
}
