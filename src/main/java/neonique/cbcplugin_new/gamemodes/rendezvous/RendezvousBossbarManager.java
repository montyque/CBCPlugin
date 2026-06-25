package neonique.cbcplugin_new.gamemodes.rendezvous;

import neonique.cbcplugin_new.managers.GameBossBarManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static neonique.cbcplugin_new.resourcepack.ResourcePackManager.smallText;
import static neonique.cbcplugin_new.util.TextUtil.blankComponent;
import static neonique.cbcplugin_new.util.TextUtil.timerToText;

public class RendezvousBossbarManager extends GameBossBarManager {

    private final RendezvousGame game;

    public RendezvousBossbarManager(RendezvousGame game) {

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
        List<RendezvousTeam> teams = new ArrayList<>(game.getTeams());
        int i = 0; // Number used to track the amount of teams added to the component - this is used for the dashes between numbers
        for (RendezvousTeam team : teams) {
            currentScore = currentScore.append(smallText(String.valueOf(team.getScore())).color(team.textColor()));
            i++;
            if (i != teams.size()) {
                currentScore = currentScore.append(smallText("-").color(NamedTextColor.WHITE));
            }
        }

        Component swapComponent = blankComponent();
        if (game.getWinner() == null) {

            if (game.getSwapType() == RendezvousSwapSystem.TIMER) {

                int flagsRemovedTimer = game.getSwapTimer();

                // Show timer
                String timerText = timerToText(flagsRemovedTimer);
                NamedTextColor textColor = NamedTextColor.AQUA;

                if (flagsRemovedTimer < 10) {
                    textColor = NamedTextColor.RED;
                } else if (flagsRemovedTimer < 30) {
                    textColor = NamedTextColor.GOLD;
                } else if (flagsRemovedTimer < 60) {
                    textColor = NamedTextColor.YELLOW;
                }

                swapComponent = smallText(" \uE000 " + timerText).color(textColor).append(
                        smallText(" UNTIL RUNNER SWAP").color(NamedTextColor.WHITE)
                );
            }

            setServerText(2, currentScore.append(swapComponent));
        }
        else {
            setServerText(3, blankComponent());
        }

        updateClientBars();

    }

    public Component createPlayerDisplayComponent () {

        // Separator
        String playerSeparator = "\uF823";
        String teamSeparator = "\uF829";

        // Teams
        Collection<RendezvousTeam> teams = game.getTeams();

        // Update player display
        int i = 0;
        Component playerDisplayComponent = Component.text("");
        for (RendezvousTeam team : teams) {

            i++;

            // Create component for each player
            int j = 0;

            Component teamComponent = Component.text("");

            Collection<RendezvousPlayer> players = team.getPlayers();

            for (RendezvousPlayer player : players) {

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
                if (player.isPlayerRunner()) {
                    // Add gold icon
                    playerComponent = playerComponent.append(Component.text("\uF808\uF802\uE470\uF801").color(team.textColor()));
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