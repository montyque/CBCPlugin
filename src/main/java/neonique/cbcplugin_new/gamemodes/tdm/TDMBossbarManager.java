package neonique.cbcplugin_new.gamemodes.tdm;

import neonique.cbcplugin_new.managers.GameBossBarManager;
import neonique.cbcplugin_new.playerclasses.CBCPlayer;
import neonique.cbcplugin_new.util.StringUtil;
import neonique.cbcplugin_new.util.TextUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;

import java.util.*;

import static neonique.cbcplugin_new.resourcepack.ResourcePackManager.smallRaisedText;
import static neonique.cbcplugin_new.resourcepack.ResourcePackManager.smallText;

public class TDMBossbarManager extends GameBossBarManager {

    private final TDMGame game;

    public TDMBossbarManager (TDMGame game) {
        super(4);
        this.game = game;
    }

    @Override
    public void update () {

        setServerText(1, createPlayerDisplayComponent());

        // Get score component
        Component finalScore = smallText("").color(NamedTextColor.WHITE);
        List<TDMTeam> teamsByScore = new ArrayList<>(game.getTeams());
        int i = 0; // Number used to track the amount of teams added to the component - this is used for the dashes between numbers
        for (TDMTeam team : teamsByScore) {
            finalScore = finalScore.append(smallText(String.valueOf(team.getKills())).color(team.textColor()));
            i++;
            if (i != teamsByScore.size()) {
                finalScore = finalScore.append(smallText("-").color(NamedTextColor.WHITE));
            }
        }

        Component gameInfo = finalScore.append(smallText(" \uE000 ").color(NamedTextColor.WHITE));

        // Show timer
        if (game.getWinner() != null) {
            TDMTeam winningTeam = game.getWinner();
            gameInfo = gameInfo.append(smallText(winningTeam.name() + " WINS!").color(winningTeam.textColor()));
        }
        else {
            if (game.isOvertime()) {
                gameInfo = gameInfo.append(smallText("OVERTIME: FIRST TO " + game.getOvertimeKillsToWin() + " WINS").color(NamedTextColor.RED));
            }
            else {
                String timerText = TextUtil.timerToText(game.getTimer());
                if (game.getTimer() < 60) {
                    gameInfo = gameInfo.append(smallText(timerText).color(NamedTextColor.RED));
                }
                else if (game.getTimer() < 180) {
                    gameInfo = gameInfo.append(smallText(timerText).color(NamedTextColor.YELLOW));
                }
                else {
                    gameInfo = gameInfo.append(smallText(timerText).color(NamedTextColor.AQUA));
                }
            }
        }

        setServerText(2, gameInfo);

        // Go through each team and each player in every team
        for (TDMTeam team : teamsByScore) {

            Component placeComponent;
            if (team.isTied()) {
                placeComponent = smallRaisedText("TIED FOR ").color(NamedTextColor.WHITE)
                        .append(smallRaisedText(StringUtil.getPlacementString(team.getPlacement())).color(team.textColor()));
            }
            else {
                placeComponent = smallRaisedText("CURRENTLY IN ").color(NamedTextColor.WHITE)
                        .append(smallRaisedText(StringUtil.getPlacementString(team.getPlacement())).color(team.textColor()));
            }

            for (CBCPlayer player : team.getPlayers()) {
                if (player.isOnline()) {
                    Player playerEntity = player.getPlayer();
                    setClientText(playerEntity, 3, placeComponent);
                }
            }
        }

        updateClientBars();

    }

    public Component createPlayerDisplayComponent () {
        // Separator
        String playerSeparator = "\uF823";
        String teamSeparator = "\uF829";

        // Teams
        Collection<TDMTeam> teams = game.getTeams();

        // Update player display
        int i = 0;
        Component playerDisplayComponent = Component.text("");
        for (TDMTeam team : teams) {

            i++;

            // Create component for each player
            int j = 0;

            Component teamComponent = Component.text("");

            Collection<TDMPlayer> players = team.getPlayers();

            for (TDMPlayer player : players) {

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
                playerComponent = getPlayerHeadDisplay(player, healthDec, player.isAlive(), team.textColor());

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
