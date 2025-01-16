package neonique.cbcplugin_new.tasks.gamemodetasks.showdown;

import neonique.cbcplugin_new.gamemodes.showdown.ShowdownGame;
import neonique.cbcplugin_new.managers.GameManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class ShowdownNextRoundTimer extends BukkitRunnable {

    private final GameManager gameManager;
    private final ShowdownGame showdownGame;

    private int countdownTimer;

    public ShowdownNextRoundTimer(GameManager gameManager, ShowdownGame showdownGame, int countdownTimer) {

        this.gameManager = gameManager;
        this.showdownGame = showdownGame;
        this.countdownTimer = countdownTimer;

    }

    @Override
    public void run() {

        // Check if game is over, and if so cancel
        if (showdownGame.isGameOver()) {
            this.cancel();
            return;
        }

        World world = gameManager.getWorld();

        // Decrement timer
        countdownTimer--;

        if (countdownTimer >= 1 && countdownTimer <= 5) {
            // Play click sound
            for (Player player : world.getPlayers()) {
                player.playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 100, 1);
            }
            // Send message
            world.sendMessage(
                    Component.text("Next round starting in ").color(NamedTextColor.GREEN)
                            .append(Component.text(countdownTimer).color(NamedTextColor.YELLOW).decorate(TextDecoration.BOLD))
            );
        }
        else if (countdownTimer == 0) {
            // Start next round
            this.cancel();
            showdownGame.setupRound();
        }
    }
}
