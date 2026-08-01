package neonique.cbcplugin_new.combat.tasks;

import neonique.cbcplugin_new.combat.CombatManager;
import neonique.cbcplugin_new.managers.PlayerRegistry;
import neonique.cbcplugin_new.core.CBCPlayer;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Collection;
import java.util.function.Consumer;
import java.util.function.Supplier;


public class RespawnTimerTask extends BukkitRunnable {

    private final Supplier<Collection<? extends CBCPlayer>> players;
    private final Consumer<CBCPlayer> respawner;

    public RespawnTimerTask (Supplier<Collection<? extends CBCPlayer>> players, Consumer<CBCPlayer> respawner) {
        this.players = players;
        this.respawner = respawner;
    }

    @Override
    public void run () {

        for (CBCPlayer player : players.get()) {

            if (!player.isOnline()) continue;
            if (player.isAlive()) continue;

            if (player.getRespawnTicks() <= 0) continue;

            // Decrement player's respawn timer, and respawn if their timer reaches 0
            player.respawnTick();
            if (player.getRespawnTicks() == 0) {
                respawner.accept(player);
            } else {
                // Redisplay respawn timer on screen
                if (player.getRespawnTicks() % 20 == 0) {
                    player.getPlayer().showTitle(player.getRespawnTitle());
                }
            }

        }

    }
}
