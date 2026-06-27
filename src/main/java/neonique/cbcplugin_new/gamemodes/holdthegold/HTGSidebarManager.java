package neonique.cbcplugin_new.gamemodes.holdthegold;

import neonique.cbcplugin_new.core.CBCTeam;
import neonique.cbcplugin_new.gamemodes._base.*;
import neonique.cbcplugin_new.managers.GameManager;
import neonique.cbcplugin_new.combat.CombatManager;
import neonique.cbcplugin_new.misc.ClientSidebar;
import neonique.cbcplugin_new.util.TextUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.entity.Player;

import java.util.*;

import static neonique.cbcplugin_new.resourcepack.ResourcePackManager.smallText;
import static neonique.cbcplugin_new.util.TextUtil.blankComponent;
import static neonique.cbcplugin_new.util.TextUtil.getComponentSpaceOfLength;

public class HTGSidebarManager extends GameSidebarManager {

    private final HTGGame game;

    private final List<Component> displayToEveryone = new ArrayList<>();
    private final HashMap<CBCTeam, Integer> teamOrderOnSidebar = new HashMap<>();

    private SidebarStatCycle<HTGPlayer> statCycle;
    private List<Component> spectatorLeaderboardComponents = new ArrayList<>();

    public HTGSidebarManager (GameManager gameManager, CombatManager combatManager, HTGGame game) {
        super(gameManager, "htgSidebar");
        String before = "CBC: ";

        if (gameManager.isEventGame()) {
            before = gameManager.getEventManager().getEventNameShorthand() + " " + gameManager.getEventManager().getGameNameShorthand() + ": ";
        }

        setSidebarTitle(
                smallText(before).color(NamedTextColor.AQUA).append(
                        smallText("HOLD THE GOLD").color(NamedTextColor.YELLOW)
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
        statCycle.addCycleState(new PlayerStatList<>("Game Score", HTGPlayer::getGamePoints, false));
        statCycle.addCycleState(new PlayerStatList<>("Kills", HTGPlayer::getKills, false));
        statCycle.addCycleState(new PlayerStatList<>("Gold Score", HTGPlayer::getGoldScore, false));
        statCycle.startCycle(200);

    }

    public Component getTeamRow (HTGTeam team, boolean isOwnTeam) {

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
                Component.text(team.getPrefix() + " ").color(team.textColor()).decorate(TextDecoration.BOLD)
        );

        // Adding team name to team string
        String teamName = team.name() + " Team";
        int teamNameLength = TextUtil.getPixelLengthOfText(teamName);
        teamComponent = teamComponent.append(
                Component.text(teamName).color(team.textColor())
        );

        // Add space to make teams even
        teamComponent = teamComponent.append(getComponentSpaceOfLength(91 - teamNameLength));

        int teamGoldScore = team.getScore();
        teamComponent = TextUtil.addLeadingSpaceForNumber(teamComponent, teamGoldScore, 4);
        if (isOwnTeam) {
            teamComponent = teamComponent.append(Component.text(teamGoldScore).color(NamedTextColor.YELLOW));
        } else {
            teamComponent = teamComponent.append(Component.text(teamGoldScore).color(NamedTextColor.WHITE));
        }

        // Check who is holding the gold
        Component goldIcon = Component.text("\uF822\uE460").color(NamedTextColor.WHITE);
        if (game.getGoldHolder() != null) {
            if (game.getGoldHolder().team() != null) {
                if (game.getGoldHolder().team() == team) {
                    goldIcon = Component.text("\uF822\uE461").color(NamedTextColor.WHITE);
                }
            }
        }

        teamComponent = teamComponent.append(goldIcon);

        return teamComponent;
    }

    public void updateServerBoard () {

        displayToEveryone.clear();
        displayToEveryone.add(blankComponent());

        // Show teams and their scores
        List<HTGTeam> teamList = game.getTeamsByScore();
        for (HTGTeam team : teamList) {
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
        HTGPlayer player = game.getPlayer(client);
        if (game.getPlayer(client) != null) {

            // Check if player's team is on team order on sidebar
            HTGTeam team = game.getPlayerTeam(player);
            if (team != null) {
                if (teamOrderOnSidebar.containsKey(team)) {
                    int slot = teamOrderOnSidebar.get(team);
                    clientStringList.set(slot, getTeamRow(team, true));
                }
            }

            clientStringList.add(getComponentSpaceOfLength(11).append(
                    Component.text("Kills: ").color(NamedTextColor.GREEN)).append(
                    Component.text(player.getKills()).color(NamedTextColor.YELLOW))
            );

            clientStringList.add(getComponentSpaceOfLength(11).append(
                    Component.text("Gold Score: ").color(NamedTextColor.GREEN)).append(
                    Component.text(player.getGoldScore()).color(NamedTextColor.YELLOW))
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
