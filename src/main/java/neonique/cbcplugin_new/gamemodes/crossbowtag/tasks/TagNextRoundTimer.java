package neonique.cbcplugin_new.gamemodes.crossbowtag.tasks;

import neonique.cbcplugin_new.gamemodes.crossbowtag.TagGame;
import neonique.cbcplugin_new.managers.GameManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class TagNextRoundTimer extends BukkitRunnable {

    private final GameManager gameManager;
    private final TagGame game;

    private int countdownTimer;

    public TagNextRoundTimer(GameManager gameManager, TagGame game, int countdownTimer) {

        this.gameManager = gameManager;
        this.game = game;
        this.countdownTimer = countdownTimer;

    }

    @Override
    public void run() {

        // Check if game is over, and if so cancel
        if (game.isGameOver()) {
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
            game.setupRound();
        }
    }
}
