package neonique.cbcplugin_new.gamemodes.kmation;

import neonique.cbcplugin_new.CBCPlugin;
import neonique.cbcplugin_new.resourcepack.PlayerHeadType;
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

public class KMationSidebarManager extends GameSidebarManager {

    private final KMationGame game;
    private final ResourcePackManager resourcePackManager;

    private List<Component> displayToEveryone = new ArrayList<>();

    private List<KMationPlayer> bottomPlayers = new ArrayList<>();
    private List<KMationPlayer> playersInLast = new ArrayList<>();

    public KMationSidebarManager (GameManager gameManager, CombatManager combatManager, KMationGame game) {
        super(gameManager, "kmationSidebar");

        setSidebarTitle(
                smallText("CBC: ").color(NamedTextColor.AQUA).append(
                        smallText("KILLIMINATION").color(NamedTextColor.YELLOW)
                )
        );

        this.game = game;

        displayToEveryone.add(blankComponent());

        resourcePackManager = CBCPlugin.getResourcePackManager();

    }

    public Component getPlayerRow (KMationPlayer player, boolean isOwnPlayer) {

        Component playerComponent = getComponentSpaceOfLength(4);

        NamedTextColor colorChange = NamedTextColor.WHITE;
        if (isOwnPlayer) {
            colorChange = NamedTextColor.YELLOW;
        }

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

        // Add placement
        int playerPlacement = player.getPlacement();
        String placementString = player.getPlacement() + ". ";

        playerComponent = TextUtil.addLeadingSpaceForNumber(playerComponent, playerPlacement, 2);
        playerComponent = playerComponent.append(Component.text(placementString).color(colorChange));

        // Add player head
        Component playerHeadComponent = resourcePackManager.getPlayerHeadComponent(PlayerHeadType.NORMAL, player.getOfflinePlayer());
        playerComponent = playerComponent.append(playerHeadComponent);
        playerComponent = playerComponent.append(getComponentSpaceOfLength(4));

        // Add player name
        NamedTextColor nameColor;
        if (player.isEliminated()) {
            nameColor = NamedTextColor.RED;
        }
        else if (playersInLast.contains(player)) {
            nameColor = NamedTextColor.YELLOW;
        }
        else {
            nameColor = NamedTextColor.GREEN;
        }
        String playerName = player.getName();
        playerComponent = playerComponent.append(player.getNameComponent().color(nameColor));

        // Add space to make names even
        int playerNameMaxLength = 100;
        int playerNameLength = TextUtil.getPixelLengthOfText(playerName);
        playerComponent = playerComponent.append(getComponentSpaceOfLength(playerNameMaxLength - playerNameLength));

        if (!player.isEliminated()) {
            // Add player kills if player is still alive
            int playerKills = player.getCycleKills();
            playerComponent = TextUtil.addLeadingSpaceForNumber(playerComponent, playerKills, 2);
            playerComponent = playerComponent.append(Component.text(playerKills).color(colorChange));


            playerComponent = playerComponent.append(Component.text("\uF822\uE449"));
        }

        return playerComponent;
    }

    public void updateServerBoard () {

        displayToEveryone = new ArrayList<>(Collections.singletonList(blankComponent()));

        playersInLast = game.getPlayersInLast();

        // Get bottom 5 players
        bottomPlayers = game.getSortedPlayers(true, false);
        if (bottomPlayers.size() > 5) {
            bottomPlayers = bottomPlayers
                    .stream()
                    .limit(5)
                    .collect(Collectors.toList());
        }

        bottomPlayers.sort(Comparator.comparingInt(KMationPlayer::getPlacement));

        updateAllClientBoards();
    }

    public void updateClientBoard (Player client) {

        ArrayList<Component> clientStringList = new ArrayList<>(displayToEveryone);

        // Show player's position above list if not in bottom players list
        KMationPlayer player = game.getPlayer(client);
        if (player != null) {
            if (!bottomPlayers.contains(player) && !player.isEliminated()) {
                clientStringList.add(getPlayerRow(player, true));
            }
        }

        // Show bottom players
        for (KMationPlayer botPlayer : bottomPlayers) {
            if (player == null) {
                clientStringList.add(getPlayerRow(botPlayer, false));
            } else {
                clientStringList.add(getPlayerRow(botPlayer, botPlayer == player));
            }
        }

        // Show player's position if they are eliminated and show stats
        if (player != null) {
            // If player cannot be seen on bottom players list, show them above the list
            if (!bottomPlayers.contains(player) && player.isEliminated()) {
                // Add player string
                clientStringList.add(getPlayerRow(player, true));
            }

            // Add stats
            clientStringList.add(blankComponent());

            clientStringList.add(getComponentSpaceOfLength(11).append(
                    Component.text("Game Kills: " ).color(NamedTextColor.GREEN)).append(
                    Component.text(player.getKills()).color(NamedTextColor.YELLOW))
            );

            clientStringList.add(blankComponent());
        }

        ClientSidebar clientSidebar = getPlayerSidebar(client);
        clientSidebar.setSidebarComponents(clientStringList);
    }

}
