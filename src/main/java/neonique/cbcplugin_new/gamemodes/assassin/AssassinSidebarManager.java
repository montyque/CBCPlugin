package neonique.cbcplugin_new.gamemodes.assassin;

import neonique.cbcplugin_new.gamemodes._base.GameSidebarManager;
import neonique.cbcplugin_new.misc.ClientSidebar;
import neonique.cbcplugin_new.util.TextUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static neonique.cbcplugin_new.resourcepack.ResourcePackManager.smallText;
import static neonique.cbcplugin_new.util.TextUtil.*;
import static neonique.cbcplugin_new.util.TextUtil.blankComponent;

public class AssassinSidebarManager extends GameSidebarManager {

    private final AssassinGame game;

    private List<Component> displayToEveryone = new ArrayList<>();
    private List<AssassinPlayer> topPlayers = new ArrayList<>();

    public AssassinSidebarManager (AssassinGame game) {
        super(game.getGameManager(), "asSidebar",
                smallText("CBC: ").color(NamedTextColor.AQUA).append(
                        smallText("ASSASSIN").color(NamedTextColor.YELLOW)
                )
        );

        this.game = game;
        displayToEveryone.add(blankComponent());
    }

    public Component getPlayerRow (AssassinPlayer player, boolean isOwnPlayer) {

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

        NamedTextColor nameColor = NamedTextColor.AQUA;
        if (isOwnPlayer) {
            nameColor = NamedTextColor.YELLOW;
        }

        playerComponent = playerComponent.append(
                Component.text(player.getName()).color(nameColor)
        );

        // Add space to make player names even
        playerComponent = playerComponent.append(getComponentSpaceOfLength(95 - teamNameLength));

        // Add targets left
        int targetsLeft = player.getTargetsLeft();
        if (targetsLeft < 10) {
            playerComponent = playerComponent.append(getComponentSpaceOfLength(6));
        }
        if (isOwnPlayer) {
            playerComponent = playerComponent.append(Component.text(targetsLeft).color(NamedTextColor.YELLOW));
        } else {
            playerComponent = playerComponent.append(Component.text(targetsLeft).color(NamedTextColor.WHITE));
        }

        return playerComponent;
    }

    public void updateServerBoard () {

        displayToEveryone = new ArrayList<>(Collections.singletonList(blankComponent()));

        // Get the top 8 players
        topPlayers = game.getPlayersByTargets();
        if (topPlayers.size() > 8) {
            topPlayers = topPlayers
                    .stream()
                    .limit(8)
                    .collect(Collectors.toList());
        }

        // Show top players
        for (AssassinPlayer topPlayer : topPlayers) {
            displayToEveryone.add(getPlayerRow(topPlayer, false));
        }

        updateAllClientBoards();
    }

    public void updateClientBoard (Player player) {

        ArrayList<Component> clientStringList = new ArrayList<>(displayToEveryone);

        // Show player's stats if they are in the game
        if (game.getPlayer(player) != null) {

            AssassinPlayer assassinPlayer = (AssassinPlayer) game.getPlayer(player);

            // If player cannot be seen on top players list, show
            if (!topPlayers.isEmpty()) {
                if (!topPlayers.contains(assassinPlayer)) {
                    clientStringList.add(blankComponent());
                    // Add player string
                    clientStringList.add(getPlayerRow(assassinPlayer, true));
                }
                else {
                    int index = topPlayers.indexOf(assassinPlayer);
                    if (index != -1) {
                        clientStringList.set(index + 1, getPlayerRow(assassinPlayer, true));
                    }
                }
            }

            clientStringList.add(blankComponent());

            clientStringList.add(getComponentSpaceOfLength(13).append(
                    Component.text("Kills: " ).color(NamedTextColor.GREEN)).append(
                    Component.text(assassinPlayer.getKills()).color(NamedTextColor.YELLOW))
            );

            clientStringList.add(getComponentSpaceOfLength(13).append(
                    Component.text("Deaths as Target: " ).color(NamedTextColor.GREEN)).append(
                    Component.text(assassinPlayer.getTargetDeaths()).color(NamedTextColor.YELLOW))
            );
        }

        clientStringList.add(blankComponent());

        ClientSidebar clientSidebar = getPlayerSidebar(player);
        clientSidebar.setSidebarComponents(clientStringList);

    }

}
