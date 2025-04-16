package neonique.cbcplugin_new.gamemodes.koth;

import neonique.cbcplugin_new.gamemodes._base.*;
import neonique.cbcplugin_new.misc.ClientSidebar;
import neonique.cbcplugin_new.util.TextUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static neonique.cbcplugin_new.resourcepack.ResourcePackManager.smallText;
import static neonique.cbcplugin_new.util.TextUtil.*;

public class KOTHSidebarManager extends GameSidebarManager {

    private final KOTHGame game;

    private final List<Component> displayToEveryone = new ArrayList<>();
    private final HashMap<CBCTeam, Integer> teamOrderOnSidebar = new HashMap<>();

    private SidebarStatCycle<KOTHPlayer> statCycle;
    private List<Component> spectatorLeaderboardComponents = new ArrayList<>();

    public KOTHSidebarManager (KOTHGame game) {
        super(game.getGameManager(), "kothSidebar");
        String before = "CBC: ";

        if (game.getGameManager().isEventGame()) {
            before = game.getGameManager().getEventManager().getEventNameShorthand() + " " +
                    game.getGameManager().getEventManager().getGameNameShorthand() + ": ";
        }

        setSidebarTitle(
                smallText(before).color(NamedTextColor.AQUA).append(
                        smallText("KING OF THE HILL").color(NamedTextColor.YELLOW)
                )
        );


        this.game = game;

        // Start stat change timer
        if (game.getGameManager().isEventGame()) {
            createSpectatorStats();
        }
    }

    public void createSpectatorStats () {

        statCycle = new SidebarStatCycle<>(game, this, 101);
        statCycle.addCycleState(new PlayerStatList<>("Game Score", KOTHPlayer::getGamePoints, false));
        statCycle.addCycleState(new PlayerStatList<>("Kills", KOTHPlayer::getKills, false));
        statCycle.addCycleState(new PlayerStatList<>("Time In Hill", KOTHPlayer::getSecondsInHill, true));
        statCycle.addCycleState(new PlayerStatList<>("Points Defended", KOTHPlayer::getPointsDefended, false));
        statCycle.startCycle(200);

    }

    public Component getTeamRow (KOTHTeam team, boolean isOwnTeam) {

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
        if (isOwnTeam) {
            teamComponent = teamComponent.append(Component.text(placementString).color(NamedTextColor.YELLOW));
        } else {
            teamComponent = teamComponent.append(Component.text(placementString).color(NamedTextColor.WHITE));
        }

        // Adding crossbow char to team string
        teamComponent = teamComponent.append(
                Component.text(team.getPrefix() + " ").color(team.getColor()).decorate(TextDecoration.BOLD)
        );

        // Adding team name to team string
        int teamNameLength = TextUtil.getPixelLengthOfText(team.getTeamName() + " Team");

        teamComponent = teamComponent.append(
                Component.text(team.getTeamName() + " Team").color(team.getColor())
        );

        // Add space to make teams even
        teamComponent = teamComponent.append(getComponentSpaceOfLength(97 - teamNameLength));

        int teamScore = team.getScore();
        teamComponent = TextUtil.addLeadingSpaceForNumber(teamComponent, teamScore, 3);

        if (isOwnTeam) {
            teamComponent = teamComponent.append(Component.text(teamScore).color(NamedTextColor.YELLOW));
        } else {
            teamComponent = teamComponent.append(Component.text(teamScore).color(NamedTextColor.WHITE));
        }

        // Check who is holding the gold
        Component goldIcon = Component.text("\uF822\uE463").color(NamedTextColor.WHITE);
        if (game.getPointControlTeam() == team) {
            goldIcon = Component.text("\uF822\uE464").color(NamedTextColor.WHITE);
        }

        teamComponent = teamComponent.append(goldIcon);

        return teamComponent;
    }

    public void updateServerBoard () {

        displayToEveryone.clear();
        displayToEveryone.add(blankComponent());

        // Show teams and their scores
        List<KOTHTeam> teamList = game.getTeamsByScore();
        for (KOTHTeam team : teamList) {
            Component teamComponent = getTeamRow(team, false);

            displayToEveryone.add(teamComponent);
            teamOrderOnSidebar.put(team, displayToEveryone.size() - 1);
        }

        if (game.getGameManager().isEventGame()) {
            spectatorLeaderboardComponents = statCycle.getComponents(game.getKOTHPlayers());
        }

        displayToEveryone.add(blankComponent());
        updateAllClientBoards();
    }

    public void updateClientBoard (Player player) {
        ArrayList<Component> clientStringList = new ArrayList<>(displayToEveryone);

        // Client side scoreboards
        if (game.getPlayer(player) != null) {

            KOTHPlayer kothPlayer = (KOTHPlayer) game.getPlayer(player);

            // Check if player's team is on team order on sidebar
            if (kothPlayer.getTeam() != null) {
                KOTHTeam team = (KOTHTeam) kothPlayer.getTeam();
                if (teamOrderOnSidebar.containsKey(team)) {

                    int slot = teamOrderOnSidebar.get(team);
                    clientStringList.set(slot, getTeamRow(team, true));

                }
            }

            clientStringList.add(getComponentSpaceOfLength(11).append(
                    Component.text("Kills: " ).color(NamedTextColor.GREEN)).append(
                    Component.text(kothPlayer.getKills()).color(NamedTextColor.YELLOW))
            );

            // Make color aqua of time of hill stat if player is currently in the hill
            NamedTextColor color = NamedTextColor.YELLOW;
            if (kothPlayer.isInHill()) {
                color = NamedTextColor.AQUA;
            }
            clientStringList.add(getComponentSpaceOfLength(11).append(
                    Component.text("Time In Hill: " ).color(NamedTextColor.GREEN)).append(
                    Component.text(timerToText(kothPlayer.getSecondsInHill())).color(color))
            );

            clientStringList.add(getComponentSpaceOfLength(11).append(
                    Component.text("Points Defended: " ).color(NamedTextColor.GREEN)).append(
                    Component.text(kothPlayer.getPointsDefended()).color(NamedTextColor.YELLOW))
            );

            clientStringList.add(blankComponent());

            // Add game score
            if (game.getGameManager().isEventGame()) {
                clientStringList.add(generateGameScoreComponent(game.getGamemode(), kothPlayer, getComponentSpaceOfLength(11)));
                clientStringList.add(blankComponent());
            }
        } else if (!spectatorLeaderboardComponents.isEmpty()) {

            // Add current leaderboard title
            clientStringList.add(getComponentSpaceOfLength(11).append(statCycle.getCurrentStatTitle()));

            // Show top 5 in list from spectator leaderboard components list
            clientStringList.addAll(spectatorLeaderboardComponents);
            clientStringList.add(blankComponent());

        }

        ClientSidebar clientSidebar = getPlayerSidebar(player);
        clientSidebar.setSidebarComponents(clientStringList);

    }

}
