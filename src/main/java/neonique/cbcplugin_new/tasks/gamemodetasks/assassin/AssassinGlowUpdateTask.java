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
        for (Player player : this.game.getWorld().getPlayers()) {

            CBCPlayer playerObj = this.game.getPlayer(player);
            if (playerObj == null) continue;

            AssassinPlayer assassinPlayerObj = (AssassinPlayer) playerObj;
            AssassinPlayer glowingPlayer = assassinPlayerObj.getCurrentTarget();

            if (glowingPlayer == null) {
                game.getGlowManager().updateGlowingPlayer(player, null);
                continue;
            }

            if (glowingPlayer.isAlive()) {
                game.getGlowManager().updateGlowingPlayer(player, glowingPlayer.getPlayer());
            }
            else {
                game.getGlowManager().updateGlowingPlayer(player, null);
            }
        }
    }

}
