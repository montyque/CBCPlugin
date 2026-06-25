package neonique.cbcplugin_new.gamemodes.holdthegold;

import neonique.cbcplugin_new.managers.GameBossBarManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static neonique.cbcplugin_new.resourcepack.ResourcePackManager.smallRaisedText;
import static neonique.cbcplugin_new.resourcepack.ResourcePackManager.smallText;

public class HTGBossbarManager extends GameBossBarManager {

    private final HTGGame game;

    public HTGBossbarManager(HTGGame game) {

        // Create rows
        super(4);

        this.game = game;

    }

    @Override
    public void update () {

        // Update player display
        setServerText(1, createPlayerDisplayComponent());

        // Get score component
        Component currentScore = smallText("").color(NamedTextColor.WHITE);
        List<HTGTeam> teams = new ArrayList<>(game.getTeams());
        int i = 0; // Number used to track the amount of teams added to the component - this is used for the dashes between numbers
        for (HTGTeam team : teams) {
            currentScore = currentScore.append(smallText(String.valueOf(team.getScore())).color(team.textColor()));
            i++;
            if (i != teams.size()) {
                currentScore = currentScore.append(smallText("-").color(NamedTextColor.WHITE));
            }
        }
        setServerText(2, currentScore);

        if (game.getWinner() == null) {
            // Get gold holder
            HTGPlayer goldHolder = game.getGoldHolder();
            Component goldComponent;

            if (goldHolder == null) {
                goldComponent = smallRaisedText("GOLD VACANT").color(NamedTextColor.GOLD);
            }
            else {
                goldComponent = smallRaisedText("GOLD HELD BY ").color(NamedTextColor.GOLD).append(
                        smallRaisedText(goldHolder.getName()).color(goldHolder.getTeam().textColor())
                );
            }

            setServerText(3, goldComponent);
        }
        else {
            HTGTeam winner = game.getWinner();
            setServerText(3, smallRaisedText(winner.name() + " WINS!").color(winner.textColor()));
        }

        updateClientBars();

    }

    public Component createPlayerDisplayComponent () {

        HTGPlayer goldHolder = game.getGoldHolder();

        // Separator
        String playerSeparator = "\uF823";
        String teamSeparator = "\uF829";

        // Teams
        Collection<HTGTeam> teams = game.getTeams();

        // Update player display
        int i = 0;
        Component playerDisplayComponent = Component.text("");
        for (HTGTeam team : teams) {

            i++;

            // Create component for each player
            int j = 0;

            Component teamComponent = Component.text("");
            Collection<HTGPlayer> players = team.getPlayers();

            for (HTGPlayer player : players) {

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
                if (game.getGameLength() == 0) {
                    playerComponent = getPlayerHeadDisplay(player, healthDec, player.isOnline(), team.textColor());
                }
                else {
                    playerComponent = getPlayerHeadDisplay(player, healthDec, player.isAlive(), team.textColor());
                }

                // Check if player has gold
                if (goldHolder != null && goldHolder == player) {
                    playerComponent = playerComponent.append(Component.text("\uF808\uF801\uE462\uF821").color(NamedTextColor.WHITE));
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
