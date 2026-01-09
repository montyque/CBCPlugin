package neonique.cbcplugin_new.gamemodes.throwdown;

import neonique.cbcplugin_new.CBCPlugin;
import neonique.cbcplugin_new.enums.PlayerHeadType;
import neonique.cbcplugin_new.gamemodes._base.GameSidebarManager;
import neonique.cbcplugin_new.managers.GameManager;
import neonique.cbcplugin_new.managers.CombatManager;
import neonique.cbcplugin_new.misc.ClientSidebar;
import neonique.cbcplugin_new.resourcepack.ResourcePackManager;
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
    private final ResourcePackManager resourcePackManager;

    private List<Component> displayToEveryone = new ArrayList<>();

    private List<ThrowdownPlayer> topPlayers = new ArrayList<>();

    public ThrowdownSidebarManager (GameManager gameManager, CombatManager combatManager, ThrowdownGame game) {
        super(gameManager, "tdSidebar");

        setSidebarTitle(
                smallText("CBC: ").color(NamedTextColor.AQUA).append(
                        smallText("THROWDOWN").color(NamedTextColor.YELLOW)
                )
        );

        this.game = game;

        displayToEveryone.add(blankComponent());

        resourcePackManager = CBCPlugin.getResourcePackManager();

    }

    public Component getPlayerRow (ThrowdownPlayer player, boolean isOwnPlayer) {

        Component playerComponent = getComponentSpaceOfLength(4);

        if (isOwnPlayer) {
            playerComponent = playerComponent
                    .append(Component.text("\uE880").color(NamedTextColor.YELLOW))
                    .append(getComponentSpaceOfLength(4));
        }
        else {
            playerComponent = playerComponent
                    .append(getComponentSpaceOfLength(5))
                    .append(getComponentSpaceOfLength(4));
        }

        // Add player head
        Component playerHeadComponent = resourcePackManager.getPlayerHeadComponent(PlayerHeadType.NORMAL, player.getOfflinePlayer());
        playerComponent = playerComponent.append(playerHeadComponent);
        playerComponent = playerComponent.append(getComponentSpaceOfLength(4));

        // Add player name
        NamedTextColor nameColor = NamedTextColor.GREEN;
        if (player.isEliminated()) {
            nameColor = NamedTextColor.RED;
        }
        playerComponent = playerComponent.append(
                Component.text(player.getName()).color(nameColor)
        );

        // Add space to make names even
        int playerNameMaxLength = 100;
        int playerNameLength = TextUtil.getPixelLengthOfText(player.getName());
        playerComponent = playerComponent.append(getComponentSpaceOfLength(playerNameMaxLength - playerNameLength));

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

    public void updateClientBoard (Player client) {

        ArrayList<Component> clientStringList = new ArrayList<>(displayToEveryone);

        // Show player's stats if they are in the game
        ThrowdownPlayer player = game.getPlayer(client);
        if (game.getPlayer(client) != null) {

            // If player cannot be seen on top players list, show
            if (!topPlayers.isEmpty()) {
                if (!topPlayers.contains(player)) {
                    if (topPlayers.get(topPlayers.size() - 1).isEliminated()) clientStringList.add(getComponentSpaceOfLength(11)
                            .append(Component.text("...").color(NamedTextColor.RED)));
                    else clientStringList.add(getComponentSpaceOfLength(11).append(Component.text("...").color(NamedTextColor.GREEN)));
                    // Add player string
                    clientStringList.add(getPlayerRow(player, true));
                }
                else {
                    int index = topPlayers.indexOf(player);
                    if (index != -1) {
                        clientStringList.set(index + 1, getPlayerRow(player, true));
                    }
                }
            }

            clientStringList.add(blankComponent());

            clientStringList.add(getComponentSpaceOfLength(11).append(
                    Component.text("Round Kills: ").color(NamedTextColor.GREEN)).append(
                    Component.text(player.getPlayerRoundKills()).color(NamedTextColor.YELLOW))
            );

            clientStringList.add(getComponentSpaceOfLength(11).append(
                    Component.text("Game Kills: ").color(NamedTextColor.GREEN)).append(
                    Component.text(player.getKills()).color(NamedTextColor.YELLOW))
            );

            clientStringList.add(getComponentSpaceOfLength(11).append(
                    Component.text("Time Alive: ").color(NamedTextColor.GREEN)).append(
                    Component.text(timerToText(player.getPlayerSecondsAlive())).color(NamedTextColor.YELLOW))
            );

        }

        clientStringList.add(blankComponent());

        ClientSidebar clientSidebar = getPlayerSidebar(client);
        clientSidebar.setSidebarComponents(clientStringList);
    }
}
