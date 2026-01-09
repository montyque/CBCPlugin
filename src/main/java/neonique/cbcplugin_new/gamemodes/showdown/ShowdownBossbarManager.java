package neonique.cbcplugin_new.gamemodes.showdown;

import neonique.cbcplugin_new.managers.GameBossBarManager;
import neonique.cbcplugin_new.playerclasses.CBCPlayer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;

import java.util.Collection;

import static neonique.cbcplugin_new.resourcepack.ResourcePackManager.smallText;

public class ShowdownBossbarManager extends GameBossBarManager {

    private final ShowdownGame game;

    public ShowdownBossbarManager(ShowdownGame game) {

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
            ShowdownTeam roundWinner = game.getRoundWinner();
            roundInfo = roundInfo.append(smallText(" \uE000 " + roundWinner.getTeamName() + " WIN").color(roundWinner.getColor()));
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
        String teamSeparator = "\uF829";

        // Teams
        Collection<ShowdownTeam> teams = game.getTeams();

        // Update player display
        int i = 0;
        Component playerDisplayComponent = Component.text("");
        for (ShowdownTeam team : teams) {

            i++;

            // Create component for each player
            int j = 0;

            Component teamComponent = Component.text("");

            Collection<ShowdownPlayer> players = team.getPlayers();

            for (ShowdownPlayer player : players) {

                j++;

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
                    playerComponent = getPlayerHeadDisplay(player, healthDec, player.isOnline(), team.getColor());
                }
                else {
                    playerComponent = getPlayerHeadDisplay(player, healthDec, player.isAlive(), team.getColor());
                }

                teamComponent = teamComponent.append(playerComponent);
                // Add a space in between each player
                if (j != players.size()) {
                    teamComponent = teamComponent.append(Component.text(playerSeparator).color(NamedTextColor.WHITE));
                }
            }

            playerDisplayComponent = playerDisplayComponent.append(teamComponent);
            // Add a space in between each team
            if (i != teams.size()) {
                playerDisplayComponent = playerDisplayComponent.append(Component.text(teamSeparator).color(NamedTextColor.WHITE));
            }
        }
        return playerDisplayComponent;
    }

}
