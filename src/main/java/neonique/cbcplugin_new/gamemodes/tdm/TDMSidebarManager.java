package neonique.cbcplugin_new.gamemodes.tdm;

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

import java.util.*;

import static neonique.cbcplugin_new.resourcepack.ResourcePackManager.smallText;
import static neonique.cbcplugin_new.util.TextUtil.blankComponent;
import static neonique.cbcplugin_new.util.TextUtil.getComponentSpaceOfLength;

public class TDMSidebarManager extends GameSidebarManager {

    private final TDMGame game;

    private List<Component> displayToEveryone = new ArrayList<>();

    private final HashMap<CBCTeam, Integer> teamOrderOnSidebar = new HashMap<>();

    // Timer numbers - for spectator leaderboard during cbc events - all numbers are in ticks
    private String currentDisplay = "KILLS";
    private BukkitRunnable changeTask = null;

    private final List<Component> spectatorLeaderboardComponents = new ArrayList<>();

    public TDMSidebarManager(GameManager gameManager, CombatManager combatManager, TDMGame game) {
        super(gameManager, combatManager, "tdmSidebar");
        String before = "CBC: ";

        if (gameManager.isThisGameCBCGame()) {
            before = gameManager.getEventManager().getEventNameShorthand() + " " + gameManager.getEventManager().getGameNameShorthand() + ": ";
        }

        setSidebarTitle(
                smallText(before).color(NamedTextColor.AQUA).append(
                        smallText("TEAM DEATHMATCH").color(NamedTextColor.YELLOW)
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

    public Component getTeamRow (TDMTeam team, boolean isOwnTeam) {

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
        int teamNameLength = TextUtil.getPixelLengthOfText(team.getTeamName() + " Team");

        teamComponent = teamComponent.append(
                Component.text(team.getTeamName() + " Team").color(team.getColor())
        );

        // Add space to make teams even
        teamComponent = teamComponent.append(getComponentSpaceOfLength(80 - teamNameLength));

        int teamKills = team.getKills();
        teamComponent = addLeadingSpaceForNumber(teamComponent, teamKills, 4);

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

        if (showGamePoints) {
            updateSpecLbComponents();
        }

        updateAllClientBoards();
    }

    public void updateClientBoard (Player player) {
        ArrayList<Component> clientStringList = new ArrayList<>(displayToEveryone);

        // Client side scoreboards
        if (game.getPlayer(player) != null) {

            TDMPlayer tdmPlayer = (TDMPlayer) game.getPlayer(player);
            TDMTeam team = (TDMTeam) tdmPlayer.getTeam();

            if (teamOrderOnSidebar.containsKey(team)) {

                int slot = teamOrderOnSidebar.get(team);
                clientStringList.set(slot, getTeamRow(team, true));

            }

            char swordChar = team.getSwordChar(true);

            List<CBCPlayer> playersByKills = team.sortPlayersByKills();

            clientStringList.add(getComponentSpaceOfLength(13).append(smallText("TEAMMATE KILLS:").color(NamedTextColor.AQUA)));

            for (CBCPlayer teammate : playersByKills) {

                boolean isPlayer = Objects.equals(tdmPlayer.getPlayerId(), teammate.getPlayerId());

                Component playerComponent = getComponentSpaceOfLength(5);

                if (isPlayer) {
                    playerComponent = playerComponent
                            .append(Component.text("\uE880").color(NamedTextColor.YELLOW))
                            .append(getComponentSpaceOfLength(5));
                }
                else {
                    playerComponent = playerComponent
                            .append(getComponentSpaceOfLength(5))
                            .append(getComponentSpaceOfLength(5));
                }

                TDMPlayer tdmTeammate = (TDMPlayer) teammate;

                // Add placement
                String placementString = tdmTeammate.getWithinTeamPlacement() + ". ";
                if (isPlayer) {
                    playerComponent = playerComponent.append(Component.text(placementString).color(NamedTextColor.YELLOW));
                } else {
                    playerComponent = playerComponent.append(Component.text(placementString).color(NamedTextColor.WHITE));
                }

                String playerName = teammate.getName();

                // Add player name
                playerComponent = playerComponent.append(Component.text(playerName).color(team.getColor()));

                // Add space to make names even
                int playerNameLength = TextUtil.getPixelLengthOfText(playerName);
                playerComponent = playerComponent.append(getComponentSpaceOfLength(92 - playerNameLength));

                // Add team kills
                int playerKills = teammate.getKills();
                playerComponent = addLeadingSpaceForNumber(playerComponent, playerKills, 4);

                if (isPlayer) {
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
            if (showGamePoints) {
                clientStringList.add(generateGameScoreComponent(game.getGamemode(), tdmPlayer, getComponentSpaceOfLength(13)));
                clientStringList.add(blankComponent());
            }
        }
        else if (!spectatorLeaderboardComponents.isEmpty()) {

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
        List<PlayerStatObject> currentLeaderboard = null;
        if (currentDisplay.equals("KILLS")) {
            currentLeaderboard = game.getTopKillsList();
        }
        else if (currentDisplay.equals("GAME SCORE")) {
            currentLeaderboard = game.getTopGameScoreList();
        }

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
        // Kills -> Game Score -> (loop)
        if (Objects.equals(currentDisplay, "KILLS")) {currentDisplay = "GAME SCORE";}
        else if (Objects.equals(currentDisplay, "GAME SCORE")) {currentDisplay = "KILLS";}

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
        changeTask.runTaskLater(CBCPlugin.getPlugin(), 400);
    }
}