package neonique.cbcplugin_new.gamemodes.rendezvous;

import neonique.cbcplugin_new.CBCPlugin;
import neonique.cbcplugin_new.enums.PlayerHeadType;
import neonique.cbcplugin_new.gamemodes._base.CBCTeam;
import neonique.cbcplugin_new.gamemodes._base.GameSidebarManager;
import neonique.cbcplugin_new.gamemodes._base.PlayerStatObject;
import neonique.cbcplugin_new.managers.GameManager;
import neonique.cbcplugin_new.managers.CombatManager;
import neonique.cbcplugin_new.misc.ClientSidebar;
import neonique.cbcplugin_new.playerclasses.CBCPlayer;
import neonique.cbcplugin_new.resourcepack.ResourcePackManager;
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
import static neonique.cbcplugin_new.util.TextUtil.blankComponent;
import static neonique.cbcplugin_new.util.TextUtil.getComponentSpaceOfLength;

public class RendezvousSidebarManager extends GameSidebarManager {

    private final RendezvousGame game;

    private final List<Component> displayToEveryone = new ArrayList<>();
    private final HashMap<CBCTeam, Integer> teamOrderOnSidebar = new HashMap<>();

    private final ResourcePackManager packManager;

    // Timer numbers - for spectator leaderboard during cbc events - all numbers are in ticks
    private String currentDisplay = "KILLS";
    private BukkitRunnable changeTask = null;

    private final List<Component> spectatorLeaderboardComponents = new ArrayList<>();

    public RendezvousSidebarManager (GameManager gameManager, CombatManager combatManager, RendezvousGame game) {

        super(gameManager, combatManager, "rdvGame");
        String before = "CBC: ";

        if (gameManager.isThisGameCBCGame()) {
            before = gameManager.getEventManager().getEventNameShorthand() + " " + gameManager.getEventManager().getGameNameShorthand() + ": ";
        }

        setSidebarTitle(
                smallText(before).color(NamedTextColor.AQUA).append(
                        smallText("RENDEZVOUS").color(NamedTextColor.YELLOW)
                )
        );

        this.gameManager = gameManager;
        this.game = game;
        this.world = gameManager.getWorld();

        packManager = CBCPlugin.getResourcePackManager();

        // Start stat change timer
        if (showGamePoints) {
            createLbChangeTimer();
        }
    }

    public Component getTeamRow (RendezvousTeam team, boolean isOwnTeam) {

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
        teamComponent = teamComponent.append(getComponentSpaceOfLength(60 - teamNameLength));

        // Adding team's score - adding whitespace to display it evenly on the sidebar
        int teamScore = team.getScore();
        if (teamScore < 10) {
            teamComponent = teamComponent.append(getComponentSpaceOfLength(12));
        }
        else if (teamScore < 100) {
            teamComponent = teamComponent.append(getComponentSpaceOfLength(6));
        }

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

        if (showGamePoints) {
            updateSpecLbComponents();
        }

        updateAllClientBoards();
    }

    public void updateClientBoard (Player player) {

        ArrayList<Component> clientStringList = new ArrayList<>(displayToEveryone);

        // Client side scoreboards
        if (game.getPlayer(player) != null) {

            RendezvousPlayer rdvPlayer = (RendezvousPlayer) game.getPlayer(player);

            // Check if player's team is on team order on sidebar
            if (rdvPlayer.getTeam() != null) {
                RendezvousTeam team = (RendezvousTeam) rdvPlayer.getTeam();
                if (teamOrderOnSidebar.containsKey(team)) {

                    int slot = teamOrderOnSidebar.get(team);
                    clientStringList.set(slot, getTeamRow(team, true));

                }
            }

            clientStringList.add(getComponentSpaceOfLength(11).append(
                    Component.text("Total Kills: " ).color(NamedTextColor.GREEN)).append(
                    Component.text(rdvPlayer.getKills()).color(NamedTextColor.YELLOW))
            );

            clientStringList.add(getComponentSpaceOfLength(11).append(
                    Component.text("Checkpoints: " ).color(NamedTextColor.GREEN)).append(
                    Component.text(rdvPlayer.getCheckpointsCleared()).color(NamedTextColor.YELLOW))
            );

            clientStringList.add(getComponentSpaceOfLength(11).append(
                    Component.text("Runners Killed: " ).color(NamedTextColor.GREEN)).append(
                    Component.text(rdvPlayer.getEnemyRunnersKilled()).color(NamedTextColor.YELLOW))
            );

            clientStringList.add(blankComponent());

            // Add game score
            if (showGamePoints) {
                clientStringList.add(generateGameScoreComponent(game.getGamemode(), rdvPlayer, getComponentSpaceOfLength(11)));
                clientStringList.add(blankComponent());
            }
        } else if (!spectatorLeaderboardComponents.isEmpty()) {

            // Add current leaderboard title
            clientStringList.add(getComponentSpaceOfLength(13).append(smallText("TOP " + currentDisplay + ":").color(NamedTextColor.AQUA)));

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
            case "KILLS" -> game.getTopKillsList();
            case "GAME SCORE" -> game.getTopGameScoreList();
            case "CHECKPOINTS" -> game.getTopCheckpointsList();
            case "RUNNER KILLS" -> game.getTopRunnerKills();
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
        Component playerComponent = getComponentSpaceOfLength(5);

        playerComponent = playerComponent
                .append(getComponentSpaceOfLength(5))
                .append(getComponentSpaceOfLength(5));

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
        playerComponent = addLeadingSpaceForNumber(playerComponent, value, 4);
        playerComponent = playerComponent.append(Component.text(value).color(NamedTextColor.WHITE));
        return playerComponent;
    }

    public void changeLeaderboardStat () {

        // Change around the statistics in loop
        // Kills -> Game Score -> Checkpoints -> Runner Kills (loop)
        if (Objects.equals(currentDisplay, "KILLS")) {currentDisplay = "GAME SCORE";}
        else if (Objects.equals(currentDisplay, "GAME SCORE")) {currentDisplay = "CHECKPOINTS";}
        else if (Objects.equals(currentDisplay, "CHECKPOINTS")) {currentDisplay = "RUNNER KILLS";}
        else if (Objects.equals(currentDisplay, "RUNNER KILLS")) {currentDisplay = "KILLS";}

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
