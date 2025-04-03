package neonique.cbcplugin_new.gamemodes.koth;

import neonique.cbcplugin_new.CBCPlugin;
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

public class KOTHSidebarManager extends GameSidebarManager {

    private final KOTHGame game;

    private final List<Component> displayToEveryone = new ArrayList<>();
    private final HashMap<CBCTeam, Integer> teamOrderOnSidebar = new HashMap<>();

    private String currentDisplay = "GAME SCORE";
    private BukkitRunnable changeTask = null;

    private final List<Component> spectatorLeaderboardComponents = new ArrayList<>();

    public KOTHSidebarManager (GameManager gameManager, CombatManager combatManager, KOTHGame game) {
        super(gameManager, combatManager, "kothSidebar");
        String before = "CBC: ";

        if (gameManager.isThisGameCBCGame()) {
            before = gameManager.getEventManager().getEventNameShorthand() + " " + gameManager.getEventManager().getGameNameShorthand() + ": ";
        }

        setSidebarTitle(
                smallText(before).color(NamedTextColor.AQUA).append(
                        smallText("KING OF THE HILL").color(NamedTextColor.YELLOW)
                )
        );


        this.gameManager = gameManager;
        this.game = game;
        this.world = gameManager.getWorld();

        // Start stat change timer
        if (showGamePoints) {
            createLbChangeTimer();
        }

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
        teamComponent = teamComponent.append(getComponentSpaceOfLength(80 - teamNameLength));

        int teamScore = team.getScore();
        if (teamScore < 10) {
            teamComponent = teamComponent.append(getComponentSpaceOfLength(7));
            teamComponent = teamComponent.append(getComponentSpaceOfLength(7));
        }
        else if (teamScore < 100) {
            teamComponent = teamComponent.append(getComponentSpaceOfLength(7));
        }

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
            if (showGamePoints) {
                clientStringList.add(generateGameScoreComponent(game.getGamemode(), kothPlayer, getComponentSpaceOfLength(11)));
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
            case "KILLS" -> game.getTopKillsList();
            case "HILL CAPTURES" -> game.getTopHillCapturesList();
            case "POINTS DEFENDED" -> game.getTopPointsDefendedList();
            case "TIME IN HILL" -> game.getTopTimeInHillList();
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
        if (Objects.equals(currentDisplay, "TIME IN HILL")) {
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
        // Game Score -> Kills -> Gold Score
        if (Objects.equals(currentDisplay, "GAME SCORE")) {currentDisplay = "KILLS";}
        else if (Objects.equals(currentDisplay, "KILLS")) {currentDisplay = "HILL CAPTURES";}
        else if (Objects.equals(currentDisplay, "HILL CAPTURES")) {currentDisplay = "POINTS DEFENDED";}
        else if (Objects.equals(currentDisplay, "POINTS DEFENDED")) {currentDisplay = "TIME IN HILL";}
        else if (Objects.equals(currentDisplay, "TIME IN HILL")) {currentDisplay = "GAME SCORE";}


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
