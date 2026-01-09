package neonique.cbcplugin_new.gamemodes.tdm;

import neonique.cbcplugin_new.gamemodes._base.*;
import neonique.cbcplugin_new.managers.GameManager;
import neonique.cbcplugin_new.managers.CombatManager;
import neonique.cbcplugin_new.misc.ClientSidebar;
import neonique.cbcplugin_new.playerclasses.CBCPlayer;
import neonique.cbcplugin_new.util.TextUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.entity.Player;

import java.util.*;

import static neonique.cbcplugin_new.resourcepack.ResourcePackManager.smallText;
import static neonique.cbcplugin_new.util.TextUtil.blankComponent;
import static neonique.cbcplugin_new.util.TextUtil.getComponentSpaceOfLength;

public class TDMSidebarManager extends GameSidebarManager {

    private final TDMGame game;

    private List<Component> displayToEveryone = new ArrayList<>();

    private final HashMap<CBCTeam, Integer> teamOrderOnSidebar = new HashMap<>();

    private SidebarStatCycle<TDMPlayer> statCycle;
    private List<Component> spectatorLeaderboardComponents = new ArrayList<>();

    public TDMSidebarManager(GameManager gameManager, CombatManager combatManager, TDMGame game) {
        super(gameManager, "tdmSidebar");
        String before = "CBC: ";

        if (gameManager.isEventGame()) {
            before = gameManager.getEventManager().getEventNameShorthand() + " " + gameManager.getEventManager().getGameNameShorthand() + ": ";
        }

        setSidebarTitle(
                smallText(before).color(NamedTextColor.AQUA).append(
                        smallText("TEAM DEATHMATCH").color(NamedTextColor.YELLOW)
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
        int playerNameMaxLength = 101;
        statCycle = new SidebarStatCycle<>(game, this, playerNameMaxLength);
        statCycle.addCycleState(new PlayerStatList<>("Game Score", TDMPlayer::getGamePoints, false));
        statCycle.addCycleState(new PlayerStatList<>("Kills", TDMPlayer::getKills, false));
        statCycle.startCycle(200);

    }

    public Component getTeamRow (TDMTeam team, boolean isOwnTeam) {

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
        String teamName = team.getTeamName() + " Team";
        int teamNameLength = TextUtil.getPixelLengthOfText(teamName);

        teamComponent = teamComponent.append(
                Component.text(teamName).color(team.getColor())
        );

        // Add space to make teams even
        teamComponent = teamComponent.append(getComponentSpaceOfLength(90 - teamNameLength));

        int teamKills = team.getKills();
        teamComponent = TextUtil.addLeadingSpaceForNumber(teamComponent, teamKills, 4);

        if (isOwnTeam) {
            teamComponent = teamComponent.append(Component.text(teamKills).color(NamedTextColor.YELLOW));
        } else {
            teamComponent = teamComponent.append(Component.text(teamKills).color(NamedTextColor.WHITE));
        }

        teamComponent = teamComponent.append(Component.text("\uF822" + team.getSwordChar(true)));

        return teamComponent;
    }

    public void updateServerBoard () {

        displayToEveryone = new ArrayList<>(Collections.singletonList(blankComponent()));

        // Show teams and their scores
        List<TDMTeam> teamList = game.getTeamsByKills();
        for (TDMTeam team : teamList) {
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
        TDMPlayer player = game.getPlayer(client);

        if (player != null) {

            TDMTeam team = game.getPlayerTeam(player);

            if (teamOrderOnSidebar.containsKey(team)) {
                int slot = teamOrderOnSidebar.get(team);
                clientStringList.set(slot, getTeamRow(team, true));
            }

            char swordChar = team.getSwordChar(true);

            List<TDMPlayer> playersByKills = team.sortPlayersByKills();
            clientStringList.add(getComponentSpaceOfLength(11).append(smallText("TEAMMATE KILLS:").color(NamedTextColor.AQUA)));

            for (TDMPlayer teamMember : playersByKills) {

                Component playerComponent = getComponentSpaceOfLength(4);

                if (teamMember == player) {
                    playerComponent = playerComponent
                            .append(Component.text("\uE880").color(NamedTextColor.YELLOW))
                            .append(getComponentSpaceOfLength(4));
                }
                else {
                    playerComponent = playerComponent
                            .append(getComponentSpaceOfLength(5))
                            .append(getComponentSpaceOfLength(4));
                }

                // Add placement
                String placementString = teamMember.getWithinTeamPlacement() + ". ";
                if (teamMember == player) {
                    playerComponent = playerComponent.append(Component.text(placementString).color(NamedTextColor.YELLOW));
                } else {
                    playerComponent = playerComponent.append(Component.text(placementString).color(NamedTextColor.WHITE));
                }

                String playerName = teamMember.getName();

                // Add player name
                playerComponent = playerComponent.append(Component.text(playerName).color(team.getColor()));

                // Add space to make names even
                int playerNameLength = TextUtil.getPixelLengthOfText(playerName);
                playerComponent = playerComponent.append(getComponentSpaceOfLength(102 - playerNameLength));

                // Add team kills
                int playerKills = teamMember.getKills();
                playerComponent = TextUtil.addLeadingSpaceForNumber(playerComponent, playerKills, 4);

                if (teamMember == player) {
                    playerComponent = playerComponent.append(Component.text(playerKills).color(NamedTextColor.YELLOW));
                }
                else {
                    playerComponent = playerComponent.append(Component.text(playerKills).color(NamedTextColor.WHITE));
                }

                playerComponent = playerComponent.append(Component.text("\uF822" + swordChar));
                clientStringList.add(playerComponent);
            }

            clientStringList.add(blankComponent());

            // Add game score on sidebar if showing
            if (game.getGameManager().isEventGame()) {
                clientStringList.add(generateGameScoreComponent(game.getGamemode(), player, getComponentSpaceOfLength(11)));
                clientStringList.add(blankComponent());
            }
        }
        else if (!spectatorLeaderboardComponents.isEmpty()) {

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