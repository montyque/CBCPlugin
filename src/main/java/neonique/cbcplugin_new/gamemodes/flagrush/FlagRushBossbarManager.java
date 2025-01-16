package neonique.cbcplugin_new.gamemodes.flagrush;

import neonique.cbcplugin_new.gamemodes._base.CBCTeam;
import neonique.cbcplugin_new.gamemodes.ctf.CTFPlayer;
import neonique.cbcplugin_new.gamemodes.ctf.CTFTeam;
import neonique.cbcplugin_new.managers.GameBossBarManager;
import neonique.cbcplugin_new.playerclasses.CBCPlayer;
import neonique.cbcplugin_new.util.StringUtil;
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

public class FlagRushBossbarManager extends GameBossBarManager {

    private final FlagRushGame game;

    public FlagRushBossbarManager (FlagRushGame game) {

        // Create rows
        super(4);
        this.game = game;

    }

    @Override
    public void update () {

        setServerText(1, createPlayerDisplayComponent());

        // Add timer information
        // Get score component
        Component currentScore = smallText("").color(NamedTextColor.WHITE);
        List<FlagRushTeam> teamList = new ArrayList<>(game.getFlagRushTeams());
        int i = 0; // Number used to track the amount of teams added to the component - this is used for the dashes between numbers
        for (FlagRushTeam team : teamList) {
            currentScore = currentScore.append(smallText(String.valueOf(team.getScore())).color(team.getColor()));
            i++;
            if (i != teamList.size()) {
                currentScore = currentScore.append(smallText("-").color(NamedTextColor.WHITE));
            }
        }

        Component gameInfo = currentScore.append(smallText(" \uE000 ").color(NamedTextColor.WHITE));

        // Show timer
        if (game.getWinner() != null) {
            CBCTeam winningTeam = game.getWinner();
            gameInfo = gameInfo.append(smallText(winningTeam.getTeamName() + " WINS!").color(winningTeam.getColor()));
        }
        else {
            if (game.isOvertime()) {
                gameInfo = gameInfo.append(smallText("OVERTIME: FIRST TO " + game.getOvertimeTarget() + " WINS").color(NamedTextColor.RED));
            }
            else {
                if (game.getTimer() < 60) {
                    gameInfo = gameInfo.append(smallText(game.timerToText()).color(NamedTextColor.RED));
                }
                else if (game.getTimer() < 180) {
                    gameInfo = gameInfo.append(smallText(game.timerToText()).color(NamedTextColor.YELLOW));
                }
                else {
                    gameInfo = gameInfo.append(smallText(game.timerToText()).color(NamedTextColor.AQUA));
                }
            }
        }

        setServerText(2, gameInfo);

        // Go through each team and each player in every team
        for (FlagRushTeam team : teamList) {

            // Show the player their current placement
            Component placeComponent;
            if (team.isTied()) {
                placeComponent = smallRaisedText("TIED FOR ").color(NamedTextColor.WHITE)
                        .append(smallRaisedText(StringUtil.getPlacementString(team.getPlacement())).color(team.getColor()));
            }
            else {
                placeComponent = smallRaisedText("CURRENTLY IN ").color(NamedTextColor.WHITE)
                        .append(smallRaisedText(StringUtil.getPlacementString(team.getPlacement())).color(team.getColor()));
            }

            // Show the player their current flag status
            Component flagComponent = Component.text("");
            if (!team.isFlagAtBase()) {
                CTFPlayer flagHolder = team.getFlagHolder();
                if (flagHolder != null) {
                    CBCTeam flagHolderTeam = flagHolder.getTeam();
                    if (flagHolderTeam != null) {
                        if (team.getFlagsLeft() == 1) {
                            flagComponent = smallRaisedText(" - !! ").color(NamedTextColor.RED).append(
                                            smallRaisedText("FLAG STOLEN BY ").color(team.getColor()))
                                    .append(smallRaisedText(team.getFlagHolder().getName())).color(flagHolderTeam.getColor());
                        }
                    }
                }
            } else {
                flagComponent = smallRaisedText(" - FLAG SAFE AT BASE").color(team.getColor());
            }

            for (CBCPlayer player : team.getPlayers()) {
                if (player.isOnline()) {
                    Player playerEntity = player.getPlayer();
                    setClientText(playerEntity, 3, placeComponent.append(flagComponent));
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
        Collection<CTFTeam> teams = game.getTeams();

        // Update player display
        int i = 0;
        Component playerDisplayComponent = Component.text("");
        for (CTFTeam team : teams) {

            i++;

            // Create component for each player
            int j = 0;

            Component teamComponent = Component.text("");

            Collection<CBCPlayer> players = team.getPlayers();

            for (CBCPlayer player : players) {

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
