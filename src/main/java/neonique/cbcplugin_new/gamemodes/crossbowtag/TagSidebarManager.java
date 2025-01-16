package neonique.cbcplugin_new.gamemodes.crossbowtag;

import neonique.cbcplugin_new.CBCPlugin;
import neonique.cbcplugin_new.enums.CBCGamemode;
import neonique.cbcplugin_new.gamemodes._base.CBCTeam;
import neonique.cbcplugin_new.gamemodes._base.GameSidebarManager;
import neonique.cbcplugin_new.gamemodes._base.PlayerStatObject;
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

    private String currentDisplay = "GAME SCORE";
    private BukkitRunnable changeTask = null;

    private final List<Component> spectatorLeaderboardComponents = new ArrayList<>();

    public TagSidebarManager(GameManager gameManager, CombatManager combatManager, TagGame game) {
        super(gameManager, combatManager, "tagSidebar");

        String before = "CBC: ";

        if (gameManager.isThisGameCBCGame()) {
            before = gameManager.getEventManager().getEventNameShorthand() + " " + gameManager.getEventManager().getGameNameShorthand() + ": ";
        }

        setSidebarTitle(
                smallText(before).color(NamedTextColor.AQUA).append(
                        smallText("CROSSBOW TAG").color(NamedTextColor.YELLOW)
                )
        );

        this.gameManager = gameManager;
        this.game = game;
        this.world = gameManager.getWorld();

        displayToEveryone.add(blankComponent());

        // Start stat change timer
        if (showGamePoints) {
            createLbChangeTimer();
        }
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
        teamComponent = teamComponent.append(getComponentSpaceOfLength(65 - teamNameLength));

        // Adding team's score - adding whitespace to display it evenly on the sidebar
        int teamScore = team.getIntScore();
        if (teamScore < 10) {
            teamComponent = teamComponent.append(getComponentSpaceOfLength(7));
            teamComponent = teamComponent.append(getComponentSpaceOfLength(7));
            teamComponent = teamComponent.append(getComponentSpaceOfLength(7));
        }
        else if (teamScore < 100) {
            teamComponent = teamComponent.append(getComponentSpaceOfLength(7));
            teamComponent = teamComponent.append(getComponentSpaceOfLength(7));
        }
        else if (teamScore < 1000) {
            teamComponent = teamComponent.append(getComponentSpaceOfLength(7));
        }

        if (isOwnTeam) {
            teamComponent = teamComponent.append(Component.text(teamScore).color(NamedTextColor.YELLOW));
        } else {
            teamComponent = teamComponent.append(Component.text(teamScore).color(NamedTextColor.WHITE));
        }

        teamComponent = teamComponent.append(getComponentSpaceOfLength(14));

        // Display the current taggers
        String gamemodeIconTeam = CBCGamemode.SHOWDOWN.getUnicodeIcon(NamedTextColor.YELLOW);
        if (!team.isTeamTaggers()) {
            // Adding count of players left alive or X mark if team is dead to team string
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

        if (showGamePoints) {
            updateSpecLbComponents();
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

            // Add game score
            if (showGamePoints) {
                clientStringList.add(generateGameScoreComponent(game.getGamemode(), tagPlayer, getComponentSpaceOfLength(11)));
                clientStringList.add(blankComponent());
            }
        } else if (!spectatorLeaderboardComponents.isEmpty()) {

            // Add current leaderboard title
            clientStringList.add(getComponentSpaceOfLength(11).append(smallText("TOP " + currentDisplay + ":").color(NamedTextColor.AQUA)));

            // Show top 5 in list from spectator leaderboard components list
            clientStringList.addAll(spectatorLeaderboardComponents);
            clientStringList.add(blankComponent());

        }

        ClientSidebar clientSidebar = clientSidebars.get(player.getUniqueId());
        clientSidebar.setSidebarComponents(clientStringList);

    }

    public void updateSpecLbComponents () {
        // Update leaderboard components
        spectatorLeaderboardComponents.clear();

        // Player is a spectator, show leaderboard
        List<PlayerStatObject> currentLeaderboard = switch (currentDisplay) {
            case "GAME SCORE" -> game.getTopGameScoreList();
            case "TIME SURVIVED" -> game.getTopTimeSurvivedList();
            case "EVADER KILLS" -> game.getTopEvaderKillsList();
            case "KILLS" -> game.getTopKillsList();
            default -> null;
        };


        // Only generate components if leaderboard exists
        if (currentLeaderboard != null) {
            int i = 0;
            // Generate leaderboard components
            for (PlayerStatObject playerStat : currentLeaderboard) {
                spectatorLeaderboardComponents.add(getLeaderboardRow(playerStat));
                i++;
                // Only add 5 players at the most
                if (i == 5) break;
            }
        }
    }

    public Component getLeaderboardRow (PlayerStatObject playerStat) {
        Component playerComponent = getComponentSpaceOfLength(11);

        CBCPlayer player = playerStat.getPlayer();

        // Add placement
        String placementString = playerStat.getPlacement() + ". ";
        playerComponent = playerComponent.append(Component.text(placementString).color(NamedTextColor.WHITE));

        String playerName = player.getName();

        // Add player name
        playerComponent = playerComponent.append(player.getNameComponent());

        // Add space to make names even
        int playerNameLength = TextUtil.getPixelLengthOfText(playerName);
        playerComponent = playerComponent.append(getComponentSpaceOfLength(92 - playerNameLength));

        // Add number
        int value = playerStat.getValue();
        if (Objects.equals(currentDisplay, "TIME SURVIVED")) {
            // If time alive, display in MM:SS format
            if (value < 600) {
                // Add leading space
                playerComponent = playerComponent.append(getComponentSpaceOfLength(7));
            }
            playerComponent = playerComponent.append(Component.text("\uF812" + timerToText(value)).color(NamedTextColor.WHITE));
        }
        else {
            playerComponent = addLeadingSpaceForNumber(playerComponent, value, 4);
            playerComponent = playerComponent.append(Component.text(value).color(NamedTextColor.WHITE));
        }
        return playerComponent;
    }

    public void changeLeaderboardStat () {

        // Change around the statistics in loop
        // Game Score -> Time Survived -> Evaders Killed -> Kills -> (Loop)
        if (Objects.equals(currentDisplay, "GAME SCORE")) {currentDisplay = "TIME SURVIVED";}
        else if (Objects.equals(currentDisplay, "TIME SURVIVED")) {currentDisplay = "EVADER KILLS";}
        else if (Objects.equals(currentDisplay, "EVADER KILLS")) {currentDisplay = "KILLS";}
        else if (Objects.equals(currentDisplay, "KILLS")) {currentDisplay = "GAME SCORE";}

        // Update server board
        updateServerBoard();

        // Start loop again
        createLbChangeTimer();

    }

    public void createLbChangeTimer () {

        // Cancel current task
        if (changeTask != null) {
            if (!changeTask.isCancelled()) {
                changeTask.cancel();
            }
        }

        // Create new task
        changeTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (game.isGameOver()) {
                    cancel();
                    return;
                }
                changeLeaderboardStat();
            }
        };
        changeTask.runTaskLater(CBCPlugin.getPlugin(), 300);
    }


}
