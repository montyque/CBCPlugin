package neonique.cbcplugin_new.gamemodes.throwdown.tasks;

import neonique.cbcplugin_new.gamemodes.throwdown.ThrowdownGame;
import neonique.cbcplugin_new.managers.GameManager;
import neonique.cbcplugin_new.core.tasks.BaseStartGameTimer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.entity.Player;

import java.time.Duration;

public class ThrowdownStartRoundTimer extends BaseStartGameTimer {

    private final ThrowdownGame game;
    private final boolean gameStart;

    public ThrowdownStartRoundTimer(GameManager gameManager, ThrowdownGame game, int countdownTimer, boolean gameStart) {
        super(game, countdownTimer);
        this.game = game;
        this.gameStart = gameStart;
    }

    @Override
    public Title getDefaultTitle (Player player) {

        Component titleComponent;
        Component subtitleComponent;

        TextColor textColor = game.getGamemodeColor();
        String gamemodeUnicode = game.getGamemode().getIcon();

        titleComponent = Component.text(gamemodeUnicode).color(NamedTextColor.WHITE).decoration(TextDecoration.BOLD, TextDecoration.State.FALSE)
                .append(
                        Component.text(" " + game.getGamemode().getGamemodeName() + " ").color(textColor).decoration(TextDecoration.BOLD, TextDecoration.State.TRUE)
                ).append(
                        Component.text(gamemodeUnicode).color(NamedTextColor.WHITE).decoration(TextDecoration.BOLD, TextDecoration.State.FALSE)
                );

        subtitleComponent = Component.text(game.getMap().getName()).decorate(TextDecoration.BOLD).color(NamedTextColor.WHITE);

        if (!gameStart) {
            titleComponent = Component.text("ROUND " + game.getRoundNumber()).decorate(TextDecoration.BOLD).color(textColor);
        }

        if (getCountdownTimer() <= 5) {
            subtitleComponent = Component.text("Starting in ").decorate(TextDecoration.BOLD).color(NamedTextColor.WHITE)
                    .append(Component.text(getCountdownTimer()).decorate(TextDecoration.BOLD).color(textColor));
        }

        // Show title
        return Title.title(
                titleComponent,
                subtitleComponent,
                Title.Times.times(Duration.ofMillis(0), Duration.ofMillis(3000), Duration.ofMillis(500))
        );

    }

    public void startGame () {
        game.startRound();
    }
}