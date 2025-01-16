package neonique.cbcplugin_new.tasks.gamemodetasks.tdm;

import neonique.cbcplugin_new.gamemodes._base.CBCTeam;
import neonique.cbcplugin_new.gamemodes.tdm.MapRushTDMGame;
import neonique.cbcplugin_new.managers.GameManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.time.Duration;

public class MapRushTDMStartMapTimer extends TDMStartGameTimer {

    private MapRushTDMGame game;

    public MapRushTDMStartMapTimer(GameManager gameManager, MapRushTDMGame tdmGame, int countdownTimer, boolean isFinalMap) {
        super(gameManager, tdmGame, countdownTimer);
        game = tdmGame;
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

        subtitleComponent = Component.text("MAP RUSH").decorate(TextDecoration.BOLD).color(NamedTextColor.WHITE);

        if (getCountdownTimer() <= 5) {
            subtitleComponent = Component.text(game.getMap().getMapName() + " - Starting in ")
                    .decorate(TextDecoration.BOLD).color(NamedTextColor.WHITE)
                    .append(Component.text(getCountdownTimer()).decorate(TextDecoration.BOLD).color(textColor));
            if (game.isFinalMap()) {
                titleComponent = Component.text("FINAL MAP").decorate(TextDecoration.BOLD).color(textColor);
            }
            else {
                titleComponent = Component.text("MAP " + game.getMapNumber() + "/"
                                + game.getMapsMaximum())
                        .decorate(TextDecoration.BOLD).color(textColor);
            }
        }

        // Show title
        return Title.title(
                titleComponent,
                subtitleComponent,
                Title.Times.times(Duration.ofMillis(0), Duration.ofMillis(3000), Duration.ofMillis(500))
        );
    }

    @Override
    public void startGame () {
        if (game.getMapNumber() == 1) {
            game.startGame();
        }
        else {
            game.startMap();
        }
    }
}
