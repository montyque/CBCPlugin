package neonique.cbcplugin_new.gamemodes.ctf;

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

public class CTFSidebarManager extends GameSidebarManager {

    protected final CTFGame game;

    private List<Component> displayToEveryone = new ArrayList<>();

    private final HashMap<CBCTeam, Integer> teamOrderOnSidebar = new HashMap<>();

    // Timer numbers - for spectator leaderboard during cbc events - all numbers are in ticks
    private String currentDisplay = "KILLS";
    private BukkitRunnable changeTask = null;

    private final List<Component> spectatorLeaderboardComponents = new ArrayList<>();

    public CTFSidebarManager (GameManager gameManager, CombatManager combatManager, CTFGame game) {
        super(gameManager, combatManager, "sdSidebar");
        String before = "CBC: ";

        if (gameManager.isThisGameCBCGame()) {
            before = gameManager.getEventManager().getEventNameShorthand() + " " + gameManager.getEventManager().getGameNameShorthand() + ": ";
        }

        setSidebarTitle(
                smallText(before).color(NamedTextColor.AQUA).append(
                        smallText("CAPTURE THE FLAG").color(NamedTextColor.YELLOW)
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
        int teamNameLength = TextUtil.getPixelLengthOfText(team.getTeamName());

        teamComponent = teamComponent.append(
                Component.text(team.getTeamName()).color(team.getColor())
        );

        // Add space to make teams even
        teamComponent = teamComponent.append(getComponentSpaceOfLength(60 - teamNameLength));

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
            if (showGamePoints) {
                clientStringList.add(generateGameScoreComponent(game.getGamemode(), ctfPlayer, getComponentSpaceOfLength(11)));
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
            case "KILLS" -> game.getTopKillsList();
            case "GAME SCORE" -> game.getTopGameScoreList();
            case "CAPTURES" -> game.getTopCapturesList();
            case "DEFENSIVE KILLS" -> game.getTopDKills();
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
        playerComponent = addLeadingSpaceForNumber(playerComponent, value, 4);
        playerComponent = playerComponent.append(Component.text(value).color(NamedTextColor.WHITE));
        return playerComponent;
    }

    public void changeLeaderboardStat () {

        // Change around the statistics in loop
        // Kills -> Game Score -> Checkpoints -> Runner Kills (loop)
        if (Objects.equals(currentDisplay, "KILLS")) {currentDisplay = "GAME SCORE";}
        else if (Objects.equals(currentDisplay, "GAME SCORE")) {currentDisplay = "CAPTURES";}
        else if (Objects.equals(currentDisplay, "CAPTURES")) {currentDisplay = "DEFENSIVE KILLS";}
        else if (Objects.equals(currentDisplay, "DEFENSIVE KILLS")) {currentDisplay = "KILLS";}

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