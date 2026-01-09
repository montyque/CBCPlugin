package neonique.cbcplugin_new.gamemodes.assassin;

import neonique.cbcplugin_new.CBCPlugin;
import neonique.cbcplugin_new.enums.PlayerHeadType;
import neonique.cbcplugin_new.gamemodes._base.GameSidebarManager;
import neonique.cbcplugin_new.misc.ClientSidebar;
import neonique.cbcplugin_new.resourcepack.ResourcePackManager;
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
    private final ResourcePackManager resourcePackManager;

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

        resourcePackManager = CBCPlugin.getResourcePackManager();
    }

    public Component getPlayerRow (AssassinPlayer player, boolean isOwnPlayer) {

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
        NamedTextColor nameColor = NamedTextColor.AQUA;
        if (isOwnPlayer) {
            nameColor = NamedTextColor.YELLOW;
        }
        playerComponent = playerComponent.append(
                Component.text(player.getName()).color(nameColor)
        );

        // Add space to make names even
        int playerNameMaxLength = 100;
        int playerNameLength = TextUtil.getPixelLengthOfText(player.getName());
        playerComponent = playerComponent.append(getComponentSpaceOfLength(playerNameMaxLength - playerNameLength));


        // Add targets left
        int targetsLeft = player.getTargetsLeft();
        playerComponent = TextUtil.addLeadingSpaceForNumber(playerComponent, targetsLeft, 2);
        playerComponent = playerComponent.append(Component.text(targetsLeft).color(colorChange));

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

    public void updateClientBoard (Player client) {

        ArrayList<Component> clientStringList = new ArrayList<>(displayToEveryone);

        // Show client's stats if they are in the game
        if (game.getPlayer(client) != null) {

            AssassinPlayer assassinPlayer = game.getPlayer(client);

            // If client cannot be seen on top players list, show
            if (!topPlayers.isEmpty()) {
                if (!topPlayers.contains(assassinPlayer)) {
                    clientStringList.add(blankComponent());
                    // Add client string
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

            clientStringList.add(getComponentSpaceOfLength(11).append(
                    Component.text("Kills: ").color(NamedTextColor.GREEN)).append(
                    Component.text(assassinPlayer.getKills()).color(NamedTextColor.YELLOW))
            );

            clientStringList.add(getComponentSpaceOfLength(11).append(
                    Component.text("Deaths as Target: ").color(NamedTextColor.GREEN)).append(
                    Component.text(assassinPlayer.getTargetDeaths()).color(NamedTextColor.YELLOW))
            );
        }

        clientStringList.add(blankComponent());

        ClientSidebar clientSidebar = getPlayerSidebar(client);
        clientSidebar.setSidebarComponents(clientStringList);

    }

}
