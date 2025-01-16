package neonique.cbcplugin_new.gamemodes.kmation;

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

public class KMationSidebarManager extends GameSidebarManager {

    private final KMationGame game;

    private List<Component> displayToEveryone = new ArrayList<>();

    private List<KMationPlayer> bottomPlayers = new ArrayList<>();
    private List<KMationPlayer> playersInLast = new ArrayList<>();

    public KMationSidebarManager (GameManager gameManager, CombatManager combatManager, KMationGame game) {
        super(gameManager, combatManager, "kmationSidebar");

        setSidebarTitle(
                smallText("CBC: ").color(NamedTextColor.AQUA).append(
                        smallText("KILLIMINATION").color(NamedTextColor.YELLOW)
                )
        );

        this.gameManager = gameManager;
        this.game = game;
        this.world = gameManager.getWorld();

        displayToEveryone.add(blankComponent());

    }

    public Component getPlayerRow (KMationPlayer player, boolean isOwnPlayer) {

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

        // Adding rank of player
        String placementString;
        if (player.getPlacement() < 10) {
            placementString = getSpaceOfLength(6) + player.getPlacement() + ". ";
        }
        else {
            placementString = player.getPlacement() + ". ";
        }
        if (isOwnPlayer) {
            playerComponent = playerComponent.append(Component.text(placementString).color(NamedTextColor.YELLOW));
        } else {
            playerComponent = playerComponent.append(Component.text(placementString).color(NamedTextColor.WHITE));
        }

        // Adding player name
        int teamNameLength = TextUtil.getPixelLengthOfText(player.getName());

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

        playerComponent = playerComponent.append(
                Component.text(player.getName()).color(nameColor)
        );

        // Add space to make player names even
        playerComponent = playerComponent.append(getComponentSpaceOfLength(95 - teamNameLength));

        if (!player.isEliminated()) {
            // Add player kills if player is still alive
            int playerKills = player.getCycleKills();
            if (playerKills < 10) {
                playerComponent = playerComponent.append(getComponentSpaceOfLength(6));
            }
            if (isOwnPlayer) {
                playerComponent = playerComponent.append(Component.text(playerKills).color(NamedTextColor.YELLOW));
            } else {
                playerComponent = playerComponent.append(Component.text(playerKills).color(NamedTextColor.WHITE));
            }

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

    public void updateClientBoard (Player player) {

        ArrayList<Component> clientStringList = new ArrayList<>(displayToEveryone);

        // Show player's position if not in bottom players list
        KMationPlayer kMationPlayer = null;
        if (game.getPlayer(player) != null) {
            kMationPlayer = (KMationPlayer) game.getPlayer(player);
            // If player cannot be seen on bottom players list, show them above the list
            if (!bottomPlayers.contains(kMationPlayer) && !kMationPlayer.isEliminated()) {
                // Add player string
                clientStringList.add(getPlayerRow(kMationPlayer, true));
            }
        }

        // Show bottom players
        for (KMationPlayer botPlayer : bottomPlayers) {
            if (kMationPlayer == null) {
                clientStringList.add(getPlayerRow(botPlayer, false));
            }
            else {
                clientStringList.add(getPlayerRow(botPlayer, kMationPlayer.getPlayerId().equals(botPlayer.getPlayerId())));
            }
        }

        // Show player's position if they are eliminated and show stats
        if (kMationPlayer != null) {
            // If player cannot be seen on bottom players list, show them above the list
            if (!bottomPlayers.contains(kMationPlayer) && kMationPlayer.isEliminated()) {
                // Add player string
                clientStringList.add(getPlayerRow(kMationPlayer, true));
            }

            // Add stats
            clientStringList.add(blankComponent());

            clientStringList.add(getComponentSpaceOfLength(13).append(
                    Component.text("Game Kills: " ).color(NamedTextColor.GREEN)).append(
                    Component.text(kMationPlayer.getKills()).color(NamedTextColor.YELLOW))
            );

            clientStringList.add(blankComponent());
        }

        ClientSidebar clientSidebar = clientSidebars.get(player.getUniqueId());
        clientSidebar.setSidebarComponents(clientStringList);
    }

}
