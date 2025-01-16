package neonique.cbcplugin_new.gamemodes.flagrush;

import neonique.cbcplugin_new.gamemodes._base.CBCTeam;
import neonique.cbcplugin_new.gamemodes._base.GameSidebarManager;
import neonique.cbcplugin_new.gamemodes.ctf.CTFPlayer;
import neonique.cbcplugin_new.managers.GameManager;
import neonique.cbcplugin_new.managers.CombatManager;
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
import static neonique.cbcplugin_new.util.TextUtil.blankComponent;
import static neonique.cbcplugin_new.util.TextUtil.getComponentSpaceOfLength;

public class FlagRushSidebarManager extends GameSidebarManager {

    // Flag rush game variable, used for access
    private final FlagRushGame game;

    private final List<Component> displayToEveryone = new ArrayList<>();
    private final HashMap<CBCTeam, Integer> teamOrderOnSidebar = new HashMap<>();

    public FlagRushSidebarManager(GameManager gameManager, CombatManager combatManager, FlagRushGame game) {

        super(gameManager, combatManager, "flrGame");
        String before = "CBC: ";

        if (gameManager.isThisGameCBCGame()) {
            before = gameManager.getEventManager().getEventNameShorthand() + " " + gameManager.getEventManager().getGameNameShorthand() + ": ";
        }

        setSidebarTitle(
                smallText(before).color(NamedTextColor.AQUA).append(
                        smallText("FLAG RUSH").color(NamedTextColor.YELLOW)
                )
        );

        this.gameManager = gameManager;
        this.game = game;
        this.world = gameManager.getWorld();
    }

    public Component getTeamRow (FlagRushTeam team, boolean isOwnTeam) {

        Component teamComponent = getComponentSpaceOfLength(5);

        if (isOwnTeam) {
            teamComponent = teamComponent
                    .append(Component.text("\uE880").color(NamedTextColor.YELLOW))
                    .append(getComponentSpaceOfLength(5));
        }
        else {
            teamComponent = teamComponent
                    .append(getComponentSpaceOfLength(5))
                    .append(getComponentSpaceOfLength(5));
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
        int teamNameLength = TextUtil.getPixelLengthOfText(team.getTeamName());

        teamComponent = teamComponent.append(
                Component.text(team.getTeamName()).color(team.getColor())
        );

        // Add space to make teams even
        teamComponent = teamComponent.append(getComponentSpaceOfLength(45 - teamNameLength));

        // Adding team's score - adding whitespace to display it evenly on the sidebar
        int teamScore = team.getScore();
        if (teamScore < 10) {
            teamComponent = teamComponent.append(getComponentSpaceOfLength(18));
        }
        else if (teamScore < 100) {
            teamComponent = teamComponent.append(getComponentSpaceOfLength(12));
        }
        else if (teamScore < 1000) {
            teamComponent = teamComponent.append(getComponentSpaceOfLength(6));
        }

        if (isOwnTeam) {
            teamComponent = teamComponent.append(Component.text(teamScore).color(NamedTextColor.YELLOW));
        } else {
            teamComponent = teamComponent.append(Component.text(teamScore).color(NamedTextColor.WHITE));
        }

        teamComponent = teamComponent.append(Component.space());

        // Display the flag's current status, if it's taken and by which team
        CTFPlayer flagHolder = team.getFlagHolder();

        // Check if flag is being held right now
        if (flagHolder == null) {
            // Add green flag to show that flag is at base
            teamComponent = teamComponent.append(
                    Component.text("\uE881").color(NamedTextColor.DARK_GRAY)
            );
            teamComponent = teamComponent.append(
                    Component.text("⚑").color(NamedTextColor.GREEN)
            );
        }
        else {
            // Show which team is holding the flag right now
            CBCTeam flagHolderTeam = flagHolder.getTeam();
            if (flagHolderTeam != null) {
                teamComponent = teamComponent.append(
                        Component.text("\uE881").color(flagHolderTeam.getColor())
                );
            }
            // Add orange flag to show that flag is at base
            teamComponent = teamComponent.append(
                    Component.text("⚑").color(NamedTextColor.GOLD)
            );
        }

        return teamComponent;
    }

    public void updateServerBoard () {

        displayToEveryone.clear();
        displayToEveryone.add(blankComponent());

        // Show teams and their scores
        List<FlagRushTeam> teamList = game.getTeamsByScore();
        for (FlagRushTeam team : teamList) {
            Component teamComponent = getTeamRow(team, false);

            displayToEveryone.add(teamComponent);
            teamOrderOnSidebar.put(team, displayToEveryone.size() - 1);
        }

        displayToEveryone.add(blankComponent());

        updateAllClientBoards();

    }

    public void updateClientBoard (Player player) {

        ArrayList<Component> clientStringList = new ArrayList<>(displayToEveryone);

        // Client side scoreboards
        if (game.getPlayer(player) != null) {

            FlagRushPlayer flrPlayer = (FlagRushPlayer) game.getPlayer(player);

            // Check if player's team is on team order on sidebar
            if (flrPlayer.getTeam() != null) {
                FlagRushTeam team = (FlagRushTeam) flrPlayer.getTeam();
                if (teamOrderOnSidebar.containsKey(team)) {

                    int slot = teamOrderOnSidebar.get(team);
                    clientStringList.set(slot, getTeamRow(team, true));

                }
            }

            clientStringList.add(getComponentSpaceOfLength(11).append(
                    Component.text("Kills: " ).color(NamedTextColor.GREEN)).append(
                    Component.text(flrPlayer.getKills()).color(NamedTextColor.YELLOW))
            );

            clientStringList.add(getComponentSpaceOfLength(11).append(
                    Component.text("Defensive Kills: " ).color(NamedTextColor.GREEN)).append(
                    Component.text(flrPlayer.getDefensiveKills()).color(NamedTextColor.YELLOW))
            );

            clientStringList.add(getComponentSpaceOfLength(11).append(
                    Component.text("Flags Captured: " ).color(NamedTextColor.GREEN)).append(
                    Component.text(flrPlayer.getFlagsCaptured()).color(NamedTextColor.YELLOW))
            );

            clientStringList.add(blankComponent());

            // Add game score
            if (showGamePoints) {
                clientStringList.add(generateGameScoreComponent(game.getGamemode(), flrPlayer, getComponentSpaceOfLength(11)));
                clientStringList.add(blankComponent());
            }
        }

        ClientSidebar clientSidebar = clientSidebars.get(player.getUniqueId());
        clientSidebar.setSidebarComponents(clientStringList);

    }
}
