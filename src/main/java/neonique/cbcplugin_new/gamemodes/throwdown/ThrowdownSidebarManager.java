package neonique.cbcplugin_new.gamemodes.throwdown;

import neonique.cbcplugin_new.gamemodes._base.GameSidebarManager;
import neonique.cbcplugin_new.managers.GameManager;
import neonique.cbcplugin_new.managers.CombatManager;
import neonique.cbcplugin_new.misc.ClientSidebar;
import neonique.cbcplugin_new.util.TextUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

import static neonique.cbcplugin_new.resourcepack.ResourcePackManager.smallText;
import static neonique.cbcplugin_new.util.TextUtil.*;

public class ThrowdownSidebarManager extends GameSidebarManager {

    private final ThrowdownGame game;

    private List<Component> displayToEveryone = new ArrayList<>();

    private List<ThrowdownPlayer> topPlayers = new ArrayList<>();

    public ThrowdownSidebarManager (GameManager gameManager, CombatManager combatManager, ThrowdownGame game) {
        super(gameManager, combatManager, "tdSidebar");

        setSidebarTitle(
                smallText("CBC: ").color(NamedTextColor.AQUA).append(
                        smallText("THROWDOWN").color(NamedTextColor.YELLOW)
                )
        );

        this.gameManager = gameManager;
        this.game = game;
        this.world = gameManager.getWorld();

        displayToEveryone.add(blankComponent());

    }

    public Component getPlayerRow (ThrowdownPlayer player, boolean isOwnPlayer) {

        Component playerComponent = getComponentSpaceOfLength(5);

        if (isOwnPlayer) {
            playerComponent = playerComponent
                    .append(Component.text("\uE880").color(NamedTextColor.YELLOW))
                    .append(getComponentSpaceOfLength(5));
        }
        else {
            playerComponent = playerComponent
                    .append(getComponentSpaceOfLength(5))
                    .append(getComponentSpaceOfLength(5));
        }

        // Adding player name
        int teamNameLength = TextUtil.getPixelLengthOfText(player.getName());

        NamedTextColor nameColor;
        if (player.isEliminated()) {
            nameColor = NamedTextColor.RED;
        }
        else {
            nameColor = NamedTextColor.GREEN;
        }

        playerComponent = playerComponent.append(
                Component.text(player.getName()).color(nameColor)
        );

        // Add space to make player names even
        playerComponent = playerComponent.append(getComponentSpaceOfLength(95 - teamNameLength));

        // Add rounds won
        int roundsWon = player.getRoundsWon();
        for (int i = 1; i <= game.getRoundsToWin(); i++) {
            if (i <= roundsWon) {
                playerComponent = playerComponent.append(Component.text("■").color(NamedTextColor.GREEN));
            } else {
                playerComponent = playerComponent.append(Component.text("□").color(NamedTextColor.GRAY));
            }
        }

        return playerComponent;
    }

    public void updateServerBoard () {

        displayToEveryone = new ArrayList<>(Collections.singletonList(blankComponent()));

        // Get top players
        topPlayers.clear();
        topPlayers.addAll(game.getSortedPlayersIncludingEliminated());
        if (topPlayers.size() > 7) {
            topPlayers = topPlayers
                    .stream()
                    .limit(7)
                    .collect(Collectors.toList());
        }

        // Show top players
        for (ThrowdownPlayer topPlayer : topPlayers) {
            displayToEveryone.add(getPlayerRow(topPlayer, false));
        }

        updateAllClientBoards();
    }

    public void updateClientBoard (Player player) {

        ArrayList<Component> clientStringList = new ArrayList<>(displayToEveryone);

        // Show player's stats if they are in the game
        if (game.getPlayer(player) != null) {

            ThrowdownPlayer throwdownPlayer = (ThrowdownPlayer) game.getPlayer(player);

            // If player cannot be seen on top players list, show
            if (!topPlayers.isEmpty()) {
                if (!topPlayers.contains(throwdownPlayer)) {
                    if (topPlayers.get(topPlayers.size() - 1).isEliminated()) clientStringList.add(getComponentSpaceOfLength(13)
                            .append(Component.text("...").color(NamedTextColor.RED)));
                    else clientStringList.add(getComponentSpaceOfLength(13).append(Component.text("...").color(NamedTextColor.GREEN)));
                    // Add player string
                    clientStringList.add(getPlayerRow(throwdownPlayer, true));
                }
                else {
                    int index = topPlayers.indexOf(throwdownPlayer);
                    if (index != -1) {
                        clientStringList.set(index + 1, getPlayerRow(throwdownPlayer, true));
                    }
                }
            }

            clientStringList.add(blankComponent());

            clientStringList.add(getComponentSpaceOfLength(13).append(
                    Component.text("Round Kills: " ).color(NamedTextColor.GREEN)).append(
                    Component.text(throwdownPlayer.getPlayerRoundKills()).color(NamedTextColor.YELLOW))
            );

            clientStringList.add(getComponentSpaceOfLength(13).append(
                    Component.text("Game Kills: " ).color(NamedTextColor.GREEN)).append(
                    Component.text(throwdownPlayer.getKills()).color(NamedTextColor.YELLOW))
            );

            int secondsAlive = throwdownPlayer.getPlayerSecondsAlive();
            int minutes = secondsAlive / 60;
            int seconds = secondsAlive % 60;

            clientStringList.add(getComponentSpaceOfLength(13).append(
                    Component.text("Time Alive: " ).color(NamedTextColor.GREEN)).append(
                    Component.text(String.format("%d:%02d", minutes, seconds)).color(NamedTextColor.YELLOW))
            );

        }

        clientStringList.add(blankComponent());

        ClientSidebar clientSidebar = clientSidebars.get(player.getUniqueId());
        clientSidebar.setSidebarComponents(clientStringList);
    }
}
