package neonique.cbcplugin_new.gamemodes.rendezvous;

import neonique.cbcplugin_new.CBCPlugin;
import neonique.cbcplugin_new.resourcepack.PlayerHeadType;
import neonique.cbcplugin_new.gamemodes._base.*;
import neonique.cbcplugin_new.managers.GameManager;
import neonique.cbcplugin_new.combat.CombatManager;
import neonique.cbcplugin_new.misc.ClientSidebar;
import neonique.cbcplugin_new.resourcepack.ResourcePackManager;
import neonique.cbcplugin_new.util.TextUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static neonique.cbcplugin_new.resourcepack.ResourcePackManager.smallText;
import static neonique.cbcplugin_new.util.TextUtil.blankComponent;
import static neonique.cbcplugin_new.util.TextUtil.getComponentSpaceOfLength;

public class RendezvousSidebarManager extends GameSidebarManager {

    private final RendezvousGame game;

    private final List<Component> displayToEveryone = new ArrayList<>();
    private final HashMap<RendezvousTeam, Integer> teamOrderOnSidebar = new HashMap<>();

    private final ResourcePackManager packManager;

    private SidebarStatCycle<RendezvousPlayer> statCycle;
    private List<Component> spectatorLeaderboardComponents = new ArrayList<>();

    public RendezvousSidebarManager (GameManager gameManager, CombatManager combatManager, RendezvousGame game) {

        super(gameManager, "rdvGame");
        String before = "CBC: ";

        if (gameManager.isEventGame()) {
            before = gameManager.getEventManager().getEventNameShorthand() + " " + gameManager.getEventManager().getGameNameShorthand() + ": ";
        }

        setSidebarTitle(
                smallText(before).color(NamedTextColor.AQUA).append(
                        smallText("RENDEZVOUS").color(NamedTextColor.YELLOW)
                )
        );

        this.game = game;
        packManager = CBCPlugin.getResourcePackManager();

        // Start stat change timer
        if (game.getGameManager().isEventGame()) {
            createSpectatorStats();
        }
    }

    public void createSpectatorStats () {

        // The expression below adds extra space if there are more than 5 flags
        statCycle = new SidebarStatCycle<>(game, this, 101);
        statCycle.addCycleState(new PlayerStatList<>("Game Score", RendezvousPlayer::getGamePoints, false));
        statCycle.addCycleState(new PlayerStatList<>("Total Kills", RendezvousPlayer::getKills, false));
        statCycle.addCycleState(new PlayerStatList<>("Runner Kills", RendezvousPlayer::getEnemyRunnersKilled, false));
        statCycle.addCycleState(new PlayerStatList<>("Checkpoints Captured", RendezvousPlayer::getCheckpointsCleared, false));
        statCycle.addCycleState(new PlayerStatList<>("Morale Boosts Given", RendezvousPlayer::getMoraleBoostsGiven, false));
        statCycle.startCycle(200);

    }

    public Component getTeamRow (RendezvousTeam team, boolean isOwnTeam) {

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
                Component.text(team.prefix() + " ").color(team.textColor()).decorate(TextDecoration.BOLD)
        );

        // Adding team name to team string
        String teamName = team.name() + " Team";
        int teamNameLength = TextUtil.getPixelLengthOfText(teamName);

        teamComponent = teamComponent.append(
                Component.text(teamName).color(team.textColor())
        );

        // Add space to make teams even
        teamComponent = teamComponent.append(getComponentSpaceOfLength(94 - teamNameLength));

        // Adding team's score - adding whitespace to display it evenly on the sidebar
        int teamScore = team.getScore();
        teamComponent = TextUtil.addLeadingSpaceForNumber(teamComponent, teamScore, 3);

        if (isOwnTeam) {
            teamComponent = teamComponent.append(Component.text(teamScore).color(NamedTextColor.YELLOW));
        } else {
            teamComponent = teamComponent.append(Component.text(teamScore).color(NamedTextColor.WHITE));
        }

        // Display the runner
        RendezvousPlayer runner = team.getRunner();

        if (runner != null) {
            // Get runner's player component
            Component playerHeadComponent = packManager.getPlayerHeadComponent(PlayerHeadType.NORMAL, runner.getOfflinePlayer());
            teamComponent = teamComponent.append(Component.space()).append(playerHeadComponent);
        }

        return teamComponent;
    }

    public void updateServerBoard () {

        displayToEveryone.clear();
        displayToEveryone.add(blankComponent());

        // Show teams and their scores
        List<RendezvousTeam> teamList = game.getTeamsByScore();
        for (RendezvousTeam team : teamList) {
            Component teamComponent = getTeamRow(team, false);

            displayToEveryone.add(teamComponent);
            teamOrderOnSidebar.put(team, displayToEveryone.size() - 1);
        }

        displayToEveryone.add(blankComponent());

        if (game.getGameManager().isEventGame()) {
            spectatorLeaderboardComponents = statCycle.getComponents(game.getPlayers());
        }

        updateAllClientBoards();
    }

    public void updateClientBoard (Player client) {

        ArrayList<Component> clientStringList = new ArrayList<>(displayToEveryone);

        // Client side scoreboards
        RendezvousPlayer player = game.getPlayer(client);
        if (player != null) {

            // Check if player's team is on team order on sidebar
            RendezvousTeam team = game.getPlayerTeam(player);
            if (team != null) {
                if (teamOrderOnSidebar.containsKey(team)) {
                    int slot = teamOrderOnSidebar.get(team);
                    clientStringList.set(slot, getTeamRow(team, true));
                }
            }

            clientStringList.add(getComponentSpaceOfLength(11).append(
                    Component.text("Total Kills: ").color(NamedTextColor.GREEN)).append(
                    Component.text(player.getKills()).color(NamedTextColor.YELLOW))
            );

            clientStringList.add(getComponentSpaceOfLength(11).append(
                    Component.text("Checkpoints: ").color(NamedTextColor.GREEN)).append(
                    Component.text(player.getCheckpointsCleared()).color(NamedTextColor.YELLOW))
            );

            clientStringList.add(getComponentSpaceOfLength(11).append(
                    Component.text("Runners Killed: ").color(NamedTextColor.GREEN)).append(
                    Component.text(player.getEnemyRunnersKilled()).color(NamedTextColor.YELLOW))
            );

            clientStringList.add(getComponentSpaceOfLength(11).append(
                    Component.text("Morale Boosts Given: ").color(NamedTextColor.GREEN)).append(
                    Component.text(player.getMoraleBoostsGiven()).color(NamedTextColor.YELLOW))
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
