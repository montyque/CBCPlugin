package neonique.cbcplugin_new.gamemodes.throwdown;

import neonique.cbcplugin_new.managers.GameBossBarManager;
import neonique.cbcplugin_new.playerclasses.CBCPlayer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;

import java.util.Collection;

import static neonique.cbcplugin_new.resourcepack.ResourcePackManager.smallText;

public class ThrowdownBossbarManager extends GameBossBarManager {

    private final ThrowdownGame game;

    public ThrowdownBossbarManager(ThrowdownGame game) {

        // Create rows
        super(3);

        this.game = game;

    }

    @Override
    public void update () {

        setServerText(1, createPlayerDisplayComponent());

        // Add round information
        Component roundInfo = smallText("ROUND " + game.getRoundNumber()).color(NamedTextColor.AQUA);

        if (game.getRoundWinner() != null) {
            // Round has ended, show round winner
            ThrowdownPlayer roundWinner = game.getRoundWinner();
            roundInfo = roundInfo.append(smallText(" \uE000 " + roundWinner.getName() + " WIN").color(NamedTextColor.GREEN));
        }
        else {
            if (game.isSuddenDeath()) {
                roundInfo = roundInfo.append(smallText(" \uE000 SUDDEN DEATH").color(NamedTextColor.RED));
            } else {
                if (game.getSuddenDeathTimer() < 10) {
                    roundInfo = roundInfo.append(smallText(" \uE000 SUDDEN DEATH IN " + game.getSuddenDeathTimer()).color(NamedTextColor.GOLD));
                }
                else if (game.getSuddenDeathTimer() < 30) {
                    roundInfo = roundInfo.append(smallText(" \uE000 SUDDEN DEATH IN " + game.getSuddenDeathTimer()).color(NamedTextColor.YELLOW));
                }
                else {
                    roundInfo = roundInfo.append(smallText(" \uE000 SUDDEN DEATH IN " + game.getSuddenDeathTimer()).color(NamedTextColor.GREEN));
                }
            }
        }

        setServerText(2, roundInfo);

        updateClientBars();

    }

    public Component createPlayerDisplayComponent () {

        // Separator
        String playerSeparator = "\uF823";

        // Teams
        Collection<ThrowdownPlayer> players = game.getThrowdownPlayers();

        // Update player display
        int i = 0;
        Component playerDisplayComponent = Component.text("");

        for (CBCPlayer player : players) {

            i++;

            // Get player health
            double healthDec = 0;
            if (player.isOnline() && player.isAlive()) {
                Player playerEntity = player.getPlayer();
                AttributeInstance maxHealth = playerEntity.getAttribute(Attribute.GENERIC_MAX_HEALTH);
                if (maxHealth == null) {
                    healthDec = playerEntity.getHealth() / 20d;
                }
                else {
                    healthDec = playerEntity.getHealth() / maxHealth.getValue();
                }
            }

            // Create component
            Component playerComponent;

            if (game.isRoundStartCountdown()) {
                playerComponent = getPlayerHeadDisplay(player, 1.0, player.isOnline(), null);
            }
            else {
                playerComponent = getPlayerHeadDisplay(player, healthDec, player.isAlive(), null);
            }

            playerDisplayComponent = playerDisplayComponent.append(playerComponent);
            // Add a space in between each player
            if (i != players.size()) {
                playerDisplayComponent = playerDisplayComponent.append(Component.text(playerSeparator).color(NamedTextColor.WHITE));
            }
        }

        return playerDisplayComponent;
    }

}
