package neonique.cbcplugin_new.gamemodes.showdown;

import neonique.cbcplugin_new.gamemodes._base.*;
import neonique.cbcplugin_new.managers.GameManager;
import neonique.cbcplugin_new.managers.CombatManager;
import neonique.cbcplugin_new.misc.ClientSidebar;
import neonique.cbcplugin_new.util.TextUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.List;

import static neonique.cbcplugin_new.resourcepack.ResourcePackManager.smallText;
import static neonique.cbcplugin_new.util.TextUtil.*;

public class ShowdownSidebarManager extends GameSidebarManager {

    private final ShowdownGame game;

    private List<Component> displayToEveryone = new ArrayList<>();

    private final HashMap<CBCTeam, Integer> teamOrderOnSidebar = new HashMap<>();

    private SidebarStatCycle<ShowdownPlayer> statCycle;
    private List<Component> spectatorLeaderboardComponents = new ArrayList<>();

    public ShowdownSidebarManager (GameManager gameManager, CombatManager combatManager, ShowdownGame game) {
        super(gameManager, "sdSidebar");
        String before = "CBC: ";

        if (gameManager.isEventGame()) {
            before = gameManager.getEventManager().getEventNameShorthand() + " " + gameManager.getEventManager().getGameNameShorthand() + ": ";
        }

        setSidebarTitle(
                smallText(before).color(NamedTextColor.AQUA).append(
                        smallText("SHOWDOWN").color(NamedTextColor.YELLOW)
                )
        );

        this.game = game;

        // Start stat change timer
        if (game.getGameManager().isEventGame()) {
            createSpectatorStats();
        }
    }

    public void createSpectatorStats () {

        // The expression below adds extra space if there are more than 5 flags
        int playerNameMaxLength = 101 + Math.max(0, 6 * (game.getRoundsToWin() - 6));
        statCycle = new SidebarStatCycle<>(game, this, playerNameMaxLength);
        statCycle.addCycleState(new PlayerStatList<>("Game Score", ShowdownPlayer::getGamePoints, false));
        statCycle.addCycleState(new PlayerStatList<>("Total Kills", ShowdownPlayer::getKills, false));
        statCycle.addCycleState(new PlayerStatList<>("Time Alive", ShowdownPlayer::getPlayerSecondsAlive, true));
        statCycle.startCycle(200);

    }

    public Component getTeamRow (ShowdownTeam team, boolean isOwnTeam) {

        Component teamComponent = getComponentSpaceOfLength(4);

        if (isOwnTeam) {
            teamComponent = teamComponent
                    .append(Component.text("\uE880").color(NamedTextColor.YELLOW))
                    .append(getComponentSpaceOfLength(4));
        }
        else {
            teamComponent = teamComponent
                    .append(getComponentSpaceOfLength(5))
                    .append(getComponentSpaceOfLength(4));
        }

        // Adding crossbow char to team string
        teamComponent = teamComponent.append(
                Component.text(team.getPrefix() + " ").color(team.getColor()).decorate(TextDecoration.BOLD)
        );

        // Adding team name to team string
        String teamName = team.getTeamName() + " Team";
        int teamNameLength = TextUtil.getPixelLengthOfText(teamName);
        teamComponent = teamComponent.append(
                Component.text(teamName).color(team.getColor())
        );

        // Add space to make teams even
        int maxTeamNamePixelLength = Math.max(76, 118 - (game.getRoundsToWin() * 6));
        teamComponent = teamComponent.append(getComponentSpaceOfLength(maxTeamNamePixelLength - teamNameLength));

        // Adding count of players left alive or X mark if team is dead to team string
        if (team.isTeamAlive()) {
            teamComponent = teamComponent.append(
                    Component.text(team.getPlayersLeftAlive() + "\uE881 ").color(NamedTextColor.GREEN)
            );
        } else {
            teamComponent = teamComponent.append(
                    Component.text("0\uE881 ").color(NamedTextColor.DARK_GRAY)
            );
        }
        // Adding rounds won to team string
        int roundsWon = team.getRoundsWon();
        for (int i = 1; i <= game.getRoundsToWin(); i++) {
            if (i <= roundsWon) {
                teamComponent = teamComponent.append(
                        Component.text("■").color(NamedTextColor.GREEN)
                );
            } else {
                teamComponent = teamComponent.append(
                        Component.text("□").color(NamedTextColor.WHITE)
                );
            }
        }

        return teamComponent;
    }

    public void updateServerBoard () {

        teamOrderOnSidebar.clear();
        displayToEveryone = new ArrayList<>(Collections.singletonList(blankComponent()));

        // Show teams and their scores
        List<ShowdownTeam> teams = game.getTeams();
        for (ShowdownTeam team : teams) {
            Component teamComponent = getTeamRow(team, false);

            displayToEveryone.add(teamComponent);
            teamOrderOnSidebar.put(team, displayToEveryone.size() - 1);
        }

        if (game.getGameManager().isEventGame()) {
            spectatorLeaderboardComponents = statCycle.getComponents(game.getPlayers());
        }

        displayToEveryone.add(blankComponent());
        updateAllClientBoards();
    }

    public void updateClientBoard (Player client) {

        ArrayList<Component> clientStringList = new ArrayList<>(displayToEveryone);

        // Client side scoreboards
        ShowdownPlayer player = game.getPlayer(client);
        if (game.getPlayer(client) != null) {

            // Check if player's team is on team order on sidebar
            ShowdownTeam team = game.getPlayerTeam(player);
            if (team != null) {
                if (teamOrderOnSidebar.containsKey(team)) {
                    int slot = teamOrderOnSidebar.get(team);
                    clientStringList.set(slot, getTeamRow(team, true));
                }
            }

            clientStringList.add(getComponentSpaceOfLength(11).append(
                    Component.text("Round Kills: " ).color(NamedTextColor.GREEN)).append(
                    Component.text(player.getPlayerRoundKills()).color(NamedTextColor.YELLOW))
            );

            clientStringList.add(getComponentSpaceOfLength(11).append(
                    Component.text("Game Kills: " ).color(NamedTextColor.GREEN)).append(
                    Component.text(player.getKills()).color(NamedTextColor.YELLOW))
            );

            int secondsAlive = player.getPlayerSecondsAlive();

            clientStringList.add(getComponentSpaceOfLength(11).append(
                    Component.text("Time Alive: " ).color(NamedTextColor.GREEN)).append(
                    Component.text(timerToText(secondsAlive)).color(NamedTextColor.YELLOW))
            );

            clientStringList.add(blankComponent());

            // Add game score
            if (game.getGameManager().isEventGame()) {
                clientStringList.add(generateGameScoreComponent(game.getGamemode(), player, getComponentSpaceOfLength(11)));
                clientStringList.add(blankComponent());
            }
        } else if (!spectatorLeaderboardComponents.isEmpty()) {

            // Add current leaderboard title
            clientStringList.add(getComponentSpaceOfLength(11).append(statCycle.getCurrentStatTitle()));

            // Show top 5 in list from spectator leaderboard components list
            clientStringList.addAll(spectatorLeaderboardComponents);
            clientStringList.add(blankComponent());

        }

        ClientSidebar clientSidebar = getPlayerSidebar(client);
        clientSidebar.setSidebarComponents(clientStringList);
    }

}
