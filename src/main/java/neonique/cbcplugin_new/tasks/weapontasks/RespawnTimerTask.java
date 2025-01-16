package neonique.cbcplugin_new.tasks.weapontasks;

import neonique.cbcplugin_new.managers.GameManager;
import neonique.cbcplugin_new.managers.CombatManager;
import neonique.cbcplugin_new.playerclasses.CBCPlayer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.scheduler.BukkitRunnable;

import java.time.Duration;

public class RespawnTimerTask extends BukkitRunnable {

    private final GameManager gameManager;
    private final CombatManager combatManager;
    private final CBCPlayer player;
    private Integer respawnSeconds;

    public RespawnTimerTask(GameManager gm, CombatManager wm, CBCPlayer player, Integer time) {
        gameManager = gm;
        combatManager = wm;
        this.player = player;
        respawnSeconds = time;
    }

    @Override
    public void run() {
        // Check if player is still online
        if (!player.isOnline()) {
            this.cancel();
            return;
        }
        // Check if player is still considered to be respawning
        if (!player.isRespawning()) {
            this.cancel();
            return;
        }

        // Decrement respawn timer
        respawnSeconds--;

        // Create title
        // Show respawned title
        Component titleComponent = Component.text("YOU DIED!").color(NamedTextColor.RED)
                .decorate(TextDecoration.BOLD);
        Title diedTitle = Title.title(titleComponent, Component.text("Respawning in " + respawnSeconds)
                .color(NamedTextColor.YELLOW), Title.Times.times(Duration.ofMillis(0), Duration.ofMillis(1500), Duration.ofMillis(250)));
        player.getPlayer().showTitle(diedTitle);

        // Check if respawn timer is zero
        if (respawnSeconds == 0) {
            // Respawn the player
            combatManager.playerRespawn(player);
            this.cancel();
        }
    }
}
