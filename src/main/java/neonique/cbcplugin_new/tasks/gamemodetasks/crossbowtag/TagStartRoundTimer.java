package neonique.cbcplugin_new.tasks.gamemodetasks.crossbowtag;

import neonique.cbcplugin_new.gamemodes._base.CBCTeam;
import neonique.cbcplugin_new.gamemodes.crossbowtag.TagGame;
import neonique.cbcplugin_new.gamemodes.crossbowtag.TagTeam;
import neonique.cbcplugin_new.gamemodes.showdown.ShowdownGame;
import neonique.cbcplugin_new.managers.GameManager;
import neonique.cbcplugin_new.tasks.gamemodetasks.BaseStartGameTimer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.time.Duration;

public class TagStartRoundTimer extends BaseStartGameTimer {

    private final TagGame game;
    private final boolean gameStart;

    public TagStartRoundTimer(GameManager gameManager, TagGame game, int countdownTimer, boolean gameStart) {
        super(gameManager, game, countdownTimer);
        this.game = game;
        this.gameStart = gameStart;
    }

    @Override
    public Title getDefaultTitle (Player player) {

        GameManager gameManager = getGameManager();

        CBCTeam team = null;
        if (gameManager.hasPlayer(player)) {
            if (gameManager.getPlayer(player).getTeam() != null) {
                team = gameManager.getPlayer(player).getTeam();
            }
        }

        Component titleComponent;
        Component subtitleComponent;

        TextColor textColor = game.getGamemodeColor();
        NamedTextColor teamColor = NamedTextColor.WHITE;
        if (team != null) {
            textColor = team.getColor();
            teamColor = team.getColor();
        }

        String gamemodeUnicode = game.getGamemode().getUnicodeIcon(teamColor);

        titleComponent = Component.text(gamemodeUnicode).color(NamedTextColor.WHITE).decoration(TextDecoration.BOLD, TextDecoration.State.FALSE)
                .append(
                        Component.text(" " + game.getGamemode().getGamemodeName() + " ").color(textColor).decoration(TextDecoration.BOLD, TextDecoration.State.TRUE)
                ).append(
                        Component.text(gamemodeUnicode).color(NamedTextColor.WHITE).decoration(TextDecoration.BOLD, TextDecoration.State.FALSE)
                );

        subtitleComponent = Component.text(game.getMap().getMapName()).decorate(TextDecoration.BOLD).color(NamedTextColor.WHITE);

        if (getCountdownTimer() == 10 && gameStart) {
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 100, 1);
        }

        if (getCountdownTimer() <= 10) {
            titleComponent = Component.text("ROUND " + game.getRoundNumber()).decorate(TextDecoration.BOLD).color(textColor);
            if (getCountdownTimer() <= 5) {
                subtitleComponent = Component.text("Evaders released in ").decorate(TextDecoration.BOLD).color(NamedTextColor.WHITE)
                        .append(Component.text(getCountdownTimer()).decorate(TextDecoration.BOLD).color(textColor));
            }
            else {
                TagTeam taggers = game.getTaggers();
                if (taggers != null) {
                    if (taggers == team) {
                        subtitleComponent = Component.text("YOU ARE A TAGGER").decorate(TextDecoration.BOLD).color(taggers.getColor());
                    }
                    else {
                        subtitleComponent = Component.text("Taggers: ").decorate(TextDecoration.BOLD).color(NamedTextColor.WHITE)
                                .append(Component.text(taggers.getTeamName()).decorate(TextDecoration.BOLD).color(taggers.getColor()));
                    }
                }
            }

        }

        // Show title
        return Title.title(
                titleComponent,
                subtitleComponent,
                Title.Times.times(Duration.ofMillis(0), Duration.ofMillis(3000), Duration.ofMillis(500))
        );

    }

    public void startGame () {
        game.releaseEvaders();
    }
}