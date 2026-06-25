package neonique.cbcplugin_new.core;

import neonique.cbcplugin_new.lobby.LobbyPlayer;
import neonique.cbcplugin_new.managers.GameManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.Sound;

import java.time.Duration;
import java.util.Collection;


public abstract class FFAGame<P extends CBCPlayer, M extends CBCMap> extends Game<P, M> {

    private P winningPlayer;

    public FFAGame(GameManager gameManager) {
        super(gameManager);
    }

    public void createPlayers (Collection<LobbyPlayer> players) {
        for (LobbyPlayer onlinePlayer : players) {
            createAndAddPlayer(onlinePlayer.getPlayer());
        }
    }

    public void playerWonGame (P player) {

        final GameManager gameManager = getGameManager();

        winningPlayer = player;

        // Set all alive players to immune
        getCombatManager().setAllPlayersImmune(true);
        getCombatManager().setVoidKill(false);

        // Display title of game win
        Component titleToDisplay = Component.text("GAME OVER")
                .decorate(TextDecoration.BOLD).color(NamedTextColor.GREEN);

        Component subtitleToDisplay = Component.text(player.getName()).color(NamedTextColor.GREEN).append(
                Component.text(" has won the game!").color(NamedTextColor.WHITE)).decorate(TextDecoration.BOLD);

        gameManager.sendGlobalTitle(Title.title(titleToDisplay, subtitleToDisplay,
                Title.Times.times(Duration.ofMillis(0), Duration.ofMillis(3000), Duration.ofMillis(500))));

        // Send message of game win
        gameManager.sendGlobalMessage(
                Component.newline()
                        .append(Component.text("GAME WIN > ").decorate(TextDecoration.BOLD).color(NamedTextColor.WHITE))
                        .append(Component.text(player.getName()).decorate(TextDecoration.BOLD).color(NamedTextColor.GREEN))
                        .append(Component.text(" has won the game!").color(NamedTextColor.WHITE))
                        .append(Component.newline())
        );

        // Play sound to all players
        gameManager.playGlobalSound(Sound.UI_TOAST_CHALLENGE_COMPLETE, 200, 1);

        // Set all alive players to immune
        for (P plr : getPlayers()) {
            if (plr.isAlive()) {
                plr.setImmune(true);
            }
        }

        // Play fireworks
        playVictoryFireworks(null);
        updateServerSidebar();
        updateBossbarManager();

    }

    public P getWinner () {
        return winningPlayer;
    }

}
