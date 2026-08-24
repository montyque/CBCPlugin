package neonique.cbcplugin_new.lobby_old.tasks;

import neonique.cbcplugin_new.lobby_old.Lobby;
import neonique.cbcplugin_new.managers.GameManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.time.Duration;
import java.util.HashMap;

public class GameCountdownTask extends BukkitRunnable {

    Lobby lobby;
    GameManager gameManager;
    int countdownTimer;

    HashMap<Integer, TextColor> colors = new HashMap<>();

    public GameCountdownTask(GameManager gameManager, Lobby lobby, int countdownTimer) {
        this.gameManager = gameManager;
        this.lobby = lobby;
        this.countdownTimer = countdownTimer;

        colors.put(1, TextColor.color(255, 81, 81));
        colors.put(2, TextColor.color(255, 95, 72));
        colors.put(3, TextColor.color(255, 110, 63));
        colors.put(4, TextColor.color(255, 125, 53));
        colors.put(5, TextColor.color(255, 139, 44));
        colors.put(6, TextColor.color(255, 159, 41));
        colors.put(7, TextColor.color(253, 182, 46));
        colors.put(8, TextColor.color(252, 207, 51));
        colors.put(9, TextColor.color(250, 230, 56));
        colors.put(10, TextColor.color(248, 255, 62));
    }

    @Override
    public void run() {

        // Check if game is still starting
        if (!this.lobby.isGameStarting()) {
            this.cancel();
            return;
        }

        // Decrement timer
        countdownTimer--;

        if (countdownTimer <= 10 && countdownTimer >= 1) {
            // Display countdown to everyone
            Title title = Title.title(
                    Component.text(countdownTimer).decorate(TextDecoration.BOLD).color(colors.get(countdownTimer)),
                    Component.space(),
                    Title.Times.times(Duration.ofMillis(0), Duration.ofMillis(3000), Duration.ofMillis(500))
            );
            gameManager.getWorld().showTitle(title);
            gameManager.getWorld().sendMessage(
                    Component.text("Game starting in ").color(colors.get(countdownTimer))
                            .append(Component.text(countdownTimer).decorate(TextDecoration.BOLD).color(colors.get(countdownTimer)))
                            .append(Component.text(" seconds!").decorate(TextDecoration.BOLD).color(colors.get(countdownTimer)))
            );
            for (Player player : gameManager.getWorld().getPlayers()) {
                player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 100, 1);
            }
        } else if (countdownTimer == 0) {
            // Start game
            this.cancel();
            lobby.startGame();
        }
    }
}
