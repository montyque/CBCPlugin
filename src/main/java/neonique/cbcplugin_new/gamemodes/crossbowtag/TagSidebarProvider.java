package neonique.cbcplugin_new.gamemodes.crossbowtag;

import neonique.cbcplugin_new.core.CBCTeam;
import neonique.cbcplugin_new.gamemodes.CBCGamemode;
import neonique.cbcplugin_new.gamemodes._base.PlayerStatList;
import neonique.cbcplugin_new.gamemodes._base.SidebarStatCycle;
import neonique.cbcplugin_new.managers.GameManager;
import neonique.cbcplugin_new.scoreboard.ClientSidebar;
import neonique.cbcplugin_new.scoreboard.SidebarProvider;
import neonique.cbcplugin_new.util.TextUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static neonique.cbcplugin_new.util.TextUtil.*;

public class TagSidebarProvider implements SidebarProvider {

    private final TagGame game;

    private final List<Component> displayToEveryone = new ArrayList<>();
    private final HashMap<CBCTeam, Integer> teamOrderOnSidebar = new HashMap<>();

    private SidebarStatCycle<TagPlayer> statCycle;
    private List<Component> spectatorLeaderboardComponents = new ArrayList<>();

    public TagSidebarProvider (TagGame game) {
        this.game = game;
        if (game.getGameManager().isEventGame()) {
            createSpectatorStats();
        }
    }




    private void createSpectatorStats () {
        statCycle = new SidebarStatCycle<>(game, this, 101);
        statCycle.addCycleState(new PlayerStatList<>("Game Score", TagPlayer::getGamePoints, false));
        statCycle.addCycleState(new PlayerStatList<>("Evaders Killed", TagPlayer::getEvadersKilled, false));
        statCycle.addCycleState(new PlayerStatList<>("Time Survived", TagPlayer::getSecondsSurvived, true));
        statCycle.addCycleState(new PlayerStatList<>("Total Kills", TagPlayer::getKills, false));
        statCycle.startCycle(200);
    }

    private Component getTeamRow (TagTeam team, boolean isOwnTeam) {

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

        // Adding rank of team
        String placementString = team.getPlacement() + ". ";
        teamComponent = teamComponent.append(Component.text(placementString)
                .color(isOwnTeam ? NamedTextColor.YELLOW : NamedTextColor.WHITE));

        // Add team one letter prefix
        teamComponent = teamComponent.append(
                Component.text(team.prefix() + " ").color(team.textColor()).decorate(TextDecoration.BOLD)
        );

        // Adding team name to team string
        String teamName = team.name() + " Team";
        int teamNameLength = TextUtil.getPixelLengthOfText(teamName);
        teamComponent = teamComponent.append(
                Component.text(teamName).color(team.textColor())
        );

        // Add space to make teams even
        teamComponent = teamComponent.append(getComponentSpaceOfLength(75 - teamNameLength));

        // Adding team's score - adding whitespace to display it evenly on the sidebar
        int teamScore = team.getIntScore();
        teamComponent = TextUtil.addLeadingSpaceForNumber(teamComponent, teamScore, 4);
        teamComponent = teamComponent.append(Component.text(teamScore).color(isOwnTeam ? NamedTextColor.YELLOW : NamedTextColor.WHITE));
        teamComponent = teamComponent.append(getComponentSpaceOfLength(12));

        // Display the current taggers
        String gamemodeIconTeam = CBCGamemode.SHOWDOWN.getUnicodeIcon(NamedTextColor.YELLOW);
        if (!team.isTeamTaggers()) {
            if (!team.alivePlayers().isEmpty()) {
                teamComponent = teamComponent.append(
                        Component.text(team.alivePlayers().size() + "\uE881").color(NamedTextColor.GREEN)
                );
            } else {
                teamComponent = teamComponent.append(
                        Component.text("0\uE881").color(NamedTextColor.DARK_GRAY)
                );
            }
        } else {
            teamComponent = teamComponent.append(
                    Component.text(team.players().size()).color(NamedTextColor.YELLOW).append(Component.text(gamemodeIconTeam).color(NamedTextColor.WHITE))
            );
        }

        return teamComponent;

    }

    public void updateServerBoard () {

        displayToEveryone.clear();
        displayToEveryone.add(blankComponent());

        // Show teams and their scores
        List<TagTeam> teamList = game.getTeamsByScore();
        for (TagTeam team : teamList) {
            Component teamComponent = getTeamRow(team, false);

            displayToEveryone.add(teamComponent);
            teamOrderOnSidebar.put(team, displayToEveryone.size() - 1);
        }

        if (statCycle != null) {
            spectatorLeaderboardComponents = statCycle.getComponents(game.getPlayers());
        }

        displayToEveryone.add(blankComponent());
        updateAllClientBoards();

    }

    @Override
    public Component getSidebarHeader() {
        return null;
    }

    @Override
    public List<Component> getClientDisplay(Player client) {

        ArrayList<Component> clientStringList = new ArrayList<>(displayToEveryone);
        TagPlayer player = game.getPlayer(client);

        if (player != null) {

            // Check if player's team is on team order on sidebar
            if (player.team() != null) {
                TagTeam team = game.getTypedTeam(player.team());
                if (teamOrderOnSidebar.containsKey(team)) {

                    int slot = teamOrderOnSidebar.get(team);
                    clientStringList.set(slot, getTeamRow(team, true));

                }
            }

            clientStringList.add(getComponentSpaceOfLength(11).append(
                    Component.text("Points Scored: " ).color(NamedTextColor.GREEN)).append(
                    Component.text(player.getIntPointsScored()).color(NamedTextColor.YELLOW))
            );

            clientStringList.add(getComponentSpaceOfLength(11).append(
                    Component.text("Time Survived: " ).color(NamedTextColor.GREEN)).append(
                    Component.text(timerToText(player.getSecondsSurvived())).color(NamedTextColor.YELLOW))
            );

            clientStringList.add(getComponentSpaceOfLength(11).append(
                    Component.text("Evaders Killed: " ).color(NamedTextColor.GREEN)).append(
                    Component.text(player.getEvadersKilled()).color(NamedTextColor.YELLOW))
            );

            clientStringList.add(blankComponent());

            // Add game score if an event is running
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

        return clientStringList;

    }
}
