package neonique.cbcplugin_new.tasks.gamemodetasks.ctf;

import neonique.cbcplugin_new.gamemodes.ctf.CTFGame;
import neonique.cbcplugin_new.gamemodes.ctf.CTFPlayer;
import neonique.cbcplugin_new.playerclasses.CBCPlayer;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Set;

public class CTFGlowManagerTask extends BukkitRunnable {

    public CTFGame game;

    public CTFGlowManagerTask(CTFGame game) {
        this.game = game;
    }

    @Override
    public void run() {

        if (this.game.isGameOver()) return;

        // Get all players in the world
        for (Player player : this.game.getWorld().getPlayers()) {

            CBCPlayer playerObj = this.game.getPlayer(player);
            if (playerObj == null) continue;

            CTFPlayer ctfPlayerObj = (CTFPlayer) playerObj;

            Set<Player> glowingPlayers = ctfPlayerObj.getGlowingPlayers();
            game.getGlowManager().updateGlowingList(player, glowingPlayers);
        }
    }
}
