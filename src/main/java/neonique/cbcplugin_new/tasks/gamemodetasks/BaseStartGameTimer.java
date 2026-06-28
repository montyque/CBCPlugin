package neonique.cbcplugin_new.tasks.gamemodetasks;

import neonique.cbcplugin_new.core.CBCTeam;
import neonique.cbcplugin_new.core.Game;
import neonique.cbcplugin_new.cbcevents.CBCEventManager;
import neonique.cbcplugin_new.core.TeamColor;
import neonique.cbcplugin_new.managers.GameManager;
import neonique.cbcplugin_new.util.TextUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public abstract class BaseStartGameTimer extends BukkitRunnable {

    private final GameManager gameManager;
    private final Game<?, ?> game;

    private int countdownTimer;
    private int eventCountdownTimer = 6;
    private boolean firstSound = true;

    public BaseStartGameTimer (GameManager gameManager, Game<?, ?> game, int countdownTimer) {
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

        if (firstSound) {
            gameManager.playGlobalSound(Sound.ENTITY_PLAYER_LEVELUP, 100, 1);
            firstSound = false;
        }

        // Decrement timer
        decrementTimer();
    }

    public void decrementTimer() {

        // Check if event is happening
        if (gameManager.isEventGame() && eventCountdownTimer > 0 && countdownTimer > 10) {

            World world = gameManager.getWorld();
            CBCEventManager eventManager = gameManager.getEventManager();

            eventCountdownTimer--;
            if (eventCountdownTimer > 0) {
                Title title = eventManager.getPregameTitle();
                // Iterate through each player in the world
                for (Player player : world.getPlayers()) {
                    player.showTitle(title);
                }
                return;
            }
            else {
                gameManager.playGlobalSound(Sound.BLOCK_NOTE_BLOCK_PLING, 100, 1);
            }
        }

        // If countdown timer is larger than 0, decrement timer and show title
        countdownTimer--;
        if (countdownTimer > 0) {

            World world = gameManager.getWorld();

            // Iterate through each player in the world
            for (Player player : world.getPlayers()) {

                player.showTitle(getDefaultTitle(player));

            }

            // Play sound if countdown timer is equal to or smaller than 5
            if (countdownTimer <= 5) {
                gameManager.playGlobalSound(Sound.BLOCK_NOTE_BLOCK_PLING, 100, 1);
            }
        }
        // If countdown timer is 0, start the game
        else if (countdownTimer == 0) {
            timerFinished();
            this.cancel();
        }

    }

    public void timerFinished () {

        World world = gameManager.getWorld();
        // Iterate through each player in the world
        for (Player player : world.getPlayers()) {
            // Clear titles and play sound
            player.clearTitle();
        }

        gameManager.playGlobalSound(Sound.BLOCK_NOTE_BLOCK_PLING, 100, 2);

        // Start the game
        startGame();

    }

    public abstract void startGame ();

    public Title getDefaultTitle (Player player) {

        CBCTeam<?> team = null;
        if (gameManager.hasPlayer(player)) {
            if (gameManager.getPlayer(player).team() != null) {
                team = gameManager.getPlayer(player).team();
            }
        }

        Component titleComponent;
        Component subtitleComponent;

        TextColor textColor = team != null ? team.textColor() : game.getGamemodeColor();
        TeamColor teamColor = team != null ? team.teamColor() : null;
        String gamemodeIcon = game.getGamemode().getIcon(teamColor);

        titleComponent = Component.text(gamemodeIcon).color(NamedTextColor.WHITE).decoration(TextDecoration.BOLD, TextDecoration.State.FALSE)
                .append(
                        Component.text(" " + game.getGamemode().getGamemodeName() + " ").color(textColor).decoration(TextDecoration.BOLD, TextDecoration.State.TRUE)
                ).append(
                        Component.text(gamemodeIcon).color(NamedTextColor.WHITE).decoration(TextDecoration.BOLD, TextDecoration.State.FALSE)
                );
        subtitleComponent = Component.text(game.getMap().getMapName()).decorate(TextDecoration.BOLD).color(NamedTextColor.WHITE);

        if (countdownTimer <= 5) {
            subtitleComponent = Component.text("Starting in ").decorate(TextDecoration.BOLD).color(NamedTextColor.WHITE)
                    .append(Component.text(countdownTimer).decorate(TextDecoration.BOLD).color(textColor));
        }

        // Show title
        return Title.title(
                titleComponent,
                subtitleComponent,
                TextUtil.titleTimes(0, 3000, 500)
        );
    }

    public GameManager getGameManager() {
        return gameManager;
    }

    public int getCountdownTimer() {
        return countdownTimer;
    }

    public Game<?, ?> getGame() {
        return game;
    }
}
