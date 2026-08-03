package neonique.cbcplugin_new.gamemodes.ctf.tasks;

import neonique.cbcplugin_new.gamemodes.ctf.CTFGame;
import neonique.cbcplugin_new.gamemodes.ctf.CTFPlayer;
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

        if (game.isGameOver()) {
            cancel();
            return;
        }

        for (CTFPlayer player : game.players()) {

            // Update glow manager if player is online
            if (!player.isOnline()) continue;

            Set<Player> glowingPlayers = player.getGlowingPlayers();
            game.getGlowManager().updateGlowingList(player.getPlayer(), glowingPlayers);

        }

    }
}
