package neonique.cbcplugin_new.gamemodes.ctf;

import neonique.cbcplugin_new.CBCPlugin;
import neonique.cbcplugin_new.gamemodes._base.*;
import neonique.cbcplugin_new.gamemodes.crossbowtag.TagPlayer;
import neonique.cbcplugin_new.managers.GameManager;
import neonique.cbcplugin_new.managers.CombatManager;
import neonique.cbcplugin_new.misc.ClientSidebar;
import neonique.cbcplugin_new.playerclasses.CBCPlayer;
import neonique.cbcplugin_new.util.TextUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

import static neonique.cbcplugin_new.resourcepack.ResourcePackManager.smallText;
import static neonique.cbcplugin_new.util.TextUtil.blankComponent;
import static neonique.cbcplugin_new.util.TextUtil.getComponentSpaceOfLength;

public class CTFSidebarManager extends GameSidebarManager {

    protected final CTFGame game;
    private List<Component> displayToEveryone = new ArrayList<>();
    private final HashMap<CBCTeam, Integer> teamOrderOnSidebar = new HashMap<>();

    private SidebarStatCycle<CTFPlayer> statCycle;
    private List<Component> spectatorLeaderboardComponents = new ArrayList<>();

    public CTFSidebarManager (CTFGame game) {
        super(game.getGameManager(), "sdSidebar");
        String before = "CBC: ";

        if (game.getGameManager().isEventGame()) {
            before = game.getGameManager().getEventManager().getEventNameShorthand() + " " + game.getGameManager().getEventManager().getGameNameShorthand() + ": ";
        }

        setSidebarTitle(
                smallText(before).color(NamedTextColor.AQUA).append(
                        smallText("CAPTURE THE FLAG").color(NamedTextColor.YELLOW)
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
        int playerNameMaxLength = 101 + Math.max(0, 9 * (game.getFlagsStart() - 5));
        statCycle = new SidebarStatCycle<>(game, this, playerNameMaxLength);
        statCycle.addCycleState(new PlayerStatList<>("Game Score", CTFPlayer::getGamePoints, false));
        statCycle.addCycleState(new PlayerStatList<>("Total Kills", CTFPlayer::getKills, false));
        statCycle.addCycleState(new PlayerStatList<>("Defensive Kills", CTFPlayer::getDefensiveKills, false));
        statCycle.addCycleState(new PlayerStatList<>("Flags Captured", CTFPlayer::getFlagsCaptured, false));
        statCycle.startCycle(200);

    }

    public Component getTeamRow (CTFTeam team, boolean isOwnTeam) {

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
        int maxTeamNamePixelLength = Math.max(73, 118 - (game.getFlagsStart() * 9));
        teamComponent = teamComponent.append(getComponentSpaceOfLength(maxTeamNamePixelLength - teamNameLength));

        // Adding count of players left alive
        if (team.isTeamEliminated()) {
            teamComponent = teamComponent.append(
                    Component.text("0\uE881 ").color(NamedTextColor.DARK_GRAY)
            );
        } else {
            if (team.getFlagsLeft() <= 0) {
                teamComponent = teamComponent.append(
                        Component.text(team.getAlivePlayers().size() + "\uE881 ").color(NamedTextColor.GOLD)
                );
            }
            else {
                teamComponent = teamComponent.append(
                        Component.text(team.getAlivePlayers().size() + "\uE881 ").color(NamedTextColor.GREEN)
                );
            }
        }

        // Adding count of flags left
        for (int i = 1; i <= game.getFlagsStart(); i++) {
            if (i < team.getFlagsLeft()) {
                teamComponent = teamComponent.append(Component.text("⚑").color(NamedTextColor.GREEN));
            }
            else if (i == team.getFlagsLeft()) {
                if (team.isFlagAtBase()) {
                    teamComponent = teamComponent.append(Component.text("⚑").color(NamedTextColor.GREEN));
                } else {
                    teamComponent = teamComponent.append(Component.text("⚑").color(NamedTextColor.GOLD));
                }
            }
            else {
                teamComponent = teamComponent.append(Component.text("⚑").color(NamedTextColor.DARK_GRAY));
            }
        }

        return teamComponent;
    }

    public void updateServerBoard () {

        teamOrderOnSidebar.clear();
        displayToEveryone = new ArrayList<>(Collections.singletonList(blankComponent()));

        // Show teams and their scores
        for (CTFTeam team : game.getTeams()) {

            Component teamComponent = getTeamRow(team, false);

            displayToEveryone.add(teamComponent);
            teamOrderOnSidebar.put(team, displayToEveryone.size() - 1);
        }

        if (game.getGameManager().isEventGame()) {
            spectatorLeaderboardComponents = statCycle.getComponents(game.getCTFPlayers());
        }

        displayToEveryone.add(blankComponent());
        updateAllClientBoards();
    }

    public void updateClientBoard (Player player) {

        ArrayList<Component> clientStringList = new ArrayList<>(displayToEveryone);

        // Client side scoreboards
        if (game.getPlayer(player) != null) {

            CTFPlayer ctfPlayer = (CTFPlayer) game.getPlayer(player);

            // Check if player's team is on team order on sidebar
            if (ctfPlayer.getTeam() != null) {
                CTFTeam team = (CTFTeam) ctfPlayer.getTeam();
                if (teamOrderOnSidebar.containsKey(team)) {

                    int slot = teamOrderOnSidebar.get(team);
                    clientStringList.set(slot, getTeamRow(team, true));

                }
            }

            clientStringList.add(getComponentSpaceOfLength(11).append(
                    Component.text("Kills: " ).color(NamedTextColor.GREEN)).append(
                    Component.text(ctfPlayer.getKills()).color(NamedTextColor.YELLOW))
            );

            clientStringList.add(getComponentSpaceOfLength(11).append(
                    Component.text("Defensive Kills: " ).color(NamedTextColor.GREEN)).append(
                    Component.text(ctfPlayer.getDefensiveKills()).color(NamedTextColor.YELLOW))
            );

            clientStringList.add(getComponentSpaceOfLength(11).append(
                    Component.text("Flags Captured: " ).color(NamedTextColor.GREEN)).append(
                    Component.text(ctfPlayer.getFlagsCaptured()).color(NamedTextColor.YELLOW))
            );

            clientStringList.add(blankComponent());

            // Add game score
            if (game.getGameManager().isEventGame()) {
                clientStringList.add(generateGameScoreComponent(game.getGamemode(), ctfPlayer, getComponentSpaceOfLength(11)));
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