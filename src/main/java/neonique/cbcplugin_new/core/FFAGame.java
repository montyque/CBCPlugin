package neonique.cbcplugin_new.core;

import neonique.cbcplugin_new.gamemodes.FFAGameContext;
import neonique.cbcplugin_new.gamemodes.GameContext;
import neonique.cbcplugin_new.mapmechanics.VoidMechanic;
import neonique.cbcplugin_new.scoreboard.CBCScoreboardManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.World;
import org.bukkit.plugin.Plugin;

import java.time.Duration;
import java.util.Collection;

public abstract class FFAGame<P extends CBCPlayer> extends Game<P> {

    private P winningPlayer;

    public FFAGame (Plugin plugin, CBCScoreboardManager scoreboardManager, World world) {
        super(plugin, scoreboardManager, world);
    }


    public void setupGame (GameContext context) {
        setupGame((FFAGameContext) context);
    }

    public abstract void setupGame (FFAGameContext context);

    public void createPlayers (Collection<PlayerLike> players) {
        players.stream()
                .filter(PlayerLike::isOnline)
                .map(PlayerLike::getPlayer)
                .forEach(this::createAndAddPlayer);
    }

    public void playerWonGame (P player) {

        winningPlayer = player;

        // Display title of game win
        Component titleToDisplay = Component.text("GAME OVER")
                .decorate(TextDecoration.BOLD).color(NamedTextColor.GREEN);

        Component subtitleToDisplay = Component.text(player.name()).color(NamedTextColor.GREEN).append(
                Component.text(" has won the game!").color(NamedTextColor.WHITE)).decorate(TextDecoration.BOLD);

        showTitle(Title.title(titleToDisplay, subtitleToDisplay,
                Title.Times.times(Duration.ofMillis(0), Duration.ofMillis(3000), Duration.ofMillis(500))));

        // Send message of game win
        sendMessage(
                Component.newline()
                        .append(Component.text("GAME WIN > ").decorate(TextDecoration.BOLD).color(NamedTextColor.WHITE))
                        .append(Component.text(player.name()).decorate(TextDecoration.BOLD).color(NamedTextColor.GREEN))
                        .append(Component.text(" has won the game!").color(NamedTextColor.WHITE))
                        .append(Component.newline())
        );

        // Set all alive players to immune
        for (P plr : this.players()) {
            plr.setPermanentlyImmune(true);
        }
        combatSession().mapMechanicsManager().getMechanicsOfType(VoidMechanic.class).forEach(v -> v.setKillOnVoid(false));

        // Play fireworks
        playVictoryFireworks(null);

    }

    public P getWinner () {
        return winningPlayer;
    }

}
