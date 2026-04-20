package neonique.cbcplugin_new.gamemodes.koth.tasks;

import neonique.cbcplugin_new.gamemodes.koth.KOTHGame;
import neonique.cbcplugin_new.gamemodes.koth.KOTHHill;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.scheduler.BukkitRunnable;

public class KOTHHillParticlesTask extends BukkitRunnable {

    private final KOTHGame game;
    private final KOTHHill hill;

    public KOTHHillParticlesTask(KOTHGame game, KOTHHill hill) {

        this.game = game;
        this.hill = hill;

    }

    @Override
    public void run() {

        if (game.isGameOver()) {
            this.cancel();
            return;
        }

        NamedTextColor color = NamedTextColor.WHITE;
        if (game.getPointControlTeam() != null) {
            color = game.getPointControlTeam().getColor();
        }

        this.hill.playParticles(color);

    }
}
