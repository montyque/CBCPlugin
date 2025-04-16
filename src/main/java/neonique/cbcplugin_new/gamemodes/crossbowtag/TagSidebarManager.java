package neonique.cbcplugin_new.gamemodes.crossbowtag;

import neonique.cbcplugin_new.CBCPlugin;
import neonique.cbcplugin_new.enums.CBCGamemode;
import neonique.cbcplugin_new.gamemodes._base.*;
import neonique.cbcplugin_new.managers.GameManager;
import neonique.cbcplugin_new.misc.ClientSidebar;
import neonique.cbcplugin_new.playerclasses.CBCPlayer;
import neonique.cbcplugin_new.util.TextUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

import static neonique.cbcplugin_new.resourcepack.ResourcePackManager.smallText;
import static neonique.cbcplugin_new.util.TextUtil.*;

public class TagSidebarManager extends GameSidebarManager {

    private final TagGame game;

    private final List<Component> displayToEveryone = new ArrayList<>();
    private final HashMap<CBCTeam, Integer> teamOrderOnSidebar = new HashMap<>();

    private SidebarStatCycle<TagPlayer> statCycle;
    private List<Component> spectatorLeaderboardComponents = new ArrayList<>();

    public TagSidebarManager(GameManager gameManager, TagGame game) {
        super(gameManager, "tagSidebar");
        this.game = game;

        String before = "CBC: ";
        if (gameManager.isEventGame()) {
            before = gameManager.getEventManager().getEventNameShorthand() + " " + gameManager.getEventManager().getGameNameShorthand() + ": ";
        }

        setSidebarTitle(
                smallText(before).color(NamedTextColor.AQUA).append(
                        smallText("CROSSBOW TAG").color(NamedTextColor.YELLOW)
                )
        );

        displayToEveryone.add(blankComponent());

        // Start stat change timer
        if (gameManager.isEventGame()) {
            createSpectatorStats();
        }
    }

    public void createSpectatorStats () {

        statCycle = new SidebarStatCycle<>(game, this, 101);
        statCycle.addCycleState(new PlayerStatList<>("Game Score", TagPlayer::getGamePoints, false));
        statCycle.addCycleState(new PlayerStatList<>("Evaders Killed", TagPlayer::getEvadersKilled, false));
        statCycle.addCycleState(new PlayerStatList<>("Time Survived", TagPlayer::getSecondsSurvived, true));
        statCycle.addCycleState(new PlayerStatList<>("Total Kills", TagPlayer::getKills, false));
        statCycle.startCycle(200);

    }

    public Component getTeamRow (TagTeam team, boolean isOwnTeam) {

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

        // Add team one letter prefix
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
        teamComponent = teamComponent.append(getComponentSpaceOfLength(75 - teamNameLength));

        // Adding team's score - adding whitespace to display it evenly on the sidebar
        int teamScore = team.getIntScore();
        teamComponent = TextUtil.addLeadingSpaceForNumber(teamComponent, teamScore, 4);

        if (isOwnTeam) {
            teamComponent = teamComponent.append(Component.text(teamScore).color(NamedTextColor.YELLOW));
        } else {
            teamComponent = teamComponent.append(Component.text(teamScore).color(NamedTextColor.WHITE));
        }

        teamComponent = teamComponent.append(getComponentSpaceOfLength(12));

        // Display the current taggers
        String gamemodeIconTeam = CBCGamemode.SHOWDOWN.getUnicodeIcon(NamedTextColor.YELLOW);
        if (!team.isTeamTaggers()) {
            if (!team.getAlivePlayers().isEmpty()) {
                teamComponent = teamComponent.append(
                        Component.text(team.getAlivePlayers().size() + "\uE881").color(NamedTextColor.GREEN)
                );
            } else {
                teamComponent = teamComponent.append(
                        Component.text("0\uE881").color(NamedTextColor.DARK_GRAY)
                );
            }
        } else {
            teamComponent = teamComponent.append(
                    Component.text(team.getPlayers().size()).color(NamedTextColor.YELLOW).append(Component.text(gamemodeIconTeam).color(NamedTextColor.WHITE))
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
            spectatorLeaderboardComponents = statCycle.getComponents(game.getTagPlayers());
        }

        displayToEveryone.add(blankComponent());
        updateAllClientBoards();

    }

    public void updateClientBoard (Player player) {

        ArrayList<Component> clientStringList = new ArrayList<>(displayToEveryone);

        // Client side scoreboards
        if (game.getPlayer(player) != null) {

            TagPlayer tagPlayer = (TagPlayer) game.getPlayer(player);

            // Check if player's team is on team order on sidebar
            if (tagPlayer.getTeam() != null) {
                TagTeam team = (TagTeam) tagPlayer.getTeam();
                if (teamOrderOnSidebar.containsKey(team)) {

                    int slot = teamOrderOnSidebar.get(team);
                    clientStringList.set(slot, getTeamRow(team, true));

                }
            }

            clientStringList.add(getComponentSpaceOfLength(11).append(
                    Component.text("Points Scored: " ).color(NamedTextColor.GREEN)).append(
                    Component.text(tagPlayer.getIntPointsScored()).color(NamedTextColor.YELLOW))
            );

            clientStringList.add(getComponentSpaceOfLength(11).append(
                    Component.text("Time Survived: " ).color(NamedTextColor.GREEN)).append(
                    Component.text(timerToText(tagPlayer.getSecondsSurvived())).color(NamedTextColor.YELLOW))
            );

            clientStringList.add(getComponentSpaceOfLength(11).append(
                    Component.text("Evaders Killed: " ).color(NamedTextColor.GREEN)).append(
                    Component.text(tagPlayer.getEvadersKilled()).color(NamedTextColor.YELLOW))
            );

            clientStringList.add(blankComponent());

            // Add game score if an event is running
            if (game.getGameManager().isEventGame()) {
                clientStringList.add(generateGameScoreComponent(game.getGamemode(), tagPlayer, getComponentSpaceOfLength(11)));
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
