package neonique.cbcplugin_new.gamemodes.assassin;

import neonique.cbcplugin_new.resourcepack.PlayerHeadType;
import neonique.cbcplugin_new.resourcepack.ResourcePackManager;
import neonique.cbcplugin_new.scoreboard.ClientSidebar;
import neonique.cbcplugin_new.scoreboard.SidebarProvider;
import neonique.cbcplugin_new.util.TextUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static neonique.cbcplugin_new.resourcepack.ResourcePackManager.smallText;
import static neonique.cbcplugin_new.util.TextUtil.blankComponent;
import static neonique.cbcplugin_new.util.TextUtil.getComponentSpaceOfLength;

public class AssassinSidebarProvider implements SidebarProvider {

    private final AssassinGame game;
    private final ResourcePackManager resourcePackManager;

    private List<Component> displayToEveryone = new ArrayList<>();
    private List<AssassinPlayer> topPlayers = new ArrayList<>();

    public AssassinSidebarProvider (AssassinGame game) {
        this.game = game;
        this.resourcePackManager = game.getResourcePackManager();
    }

    @Override
    public Component getSidebarHeader() {
        return smallText("CBC: ").color(NamedTextColor.AQUA)
                .append(smallText("ASSASSIN").color(NamedTextColor.YELLOW));
    }

    @Override
    public List<Component> getClientDisplay(Player client) {

        List<Component> clientStringList = new ArrayList<>(displayToEveryone);

        // Show client's stats if they are in the game
        if (game.hasPlayer(client)) {

            AssassinPlayer player = game.getPlayer(client);

            // If client cannot be seen on top players list, show
            if (!topPlayers.isEmpty()) {
                if (!topPlayers.contains(player)) {
                    clientStringList.add(blankComponent());
                    clientStringList.add(getPlayerRow(player, true));
                } else {
                    int index = topPlayers.indexOf(player);
                    if (index != -1) {
                        clientStringList.set(index + 1, getPlayerRow(player, true));
                    }
                }
            }

            clientStringList.add(blankComponent());

            clientStringList.add(getComponentSpaceOfLength(11).append(
                    Component.text("Kills: ").color(NamedTextColor.GREEN)).append(
                    Component.text(player.getKills()).color(NamedTextColor.YELLOW))
            );

            clientStringList.add(getComponentSpaceOfLength(11).append(
                    Component.text("Deaths as Target: ").color(NamedTextColor.GREEN)).append(
                    Component.text(player.getTargetDeaths()).color(NamedTextColor.YELLOW))
            );
        }

        clientStringList.add(blankComponent());
        return clientStringList;

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

    }

    private Component getPlayerRow (AssassinPlayer player, boolean isOwnPlayer) {

        Component playerComponent = getComponentSpaceOfLength(4);
        NamedTextColor colorChange = isOwnPlayer ? NamedTextColor.WHITE : NamedTextColor.YELLOW;

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
                Component.text(player.name()).color(nameColor)
        );

        // Add space to make names even
        int playerNameMaxLength = 100;
        int playerNameLength = TextUtil.getPixelLengthOfText(player.name());
        playerComponent = playerComponent.append(getComponentSpaceOfLength(playerNameMaxLength - playerNameLength));

        // Add targets left
        int targetsLeft = player.getTargetsLeft();
        playerComponent = TextUtil.addLeadingSpaceForNumber(playerComponent, targetsLeft, 2);
        playerComponent = playerComponent.append(Component.text(targetsLeft).color(colorChange));

        return playerComponent;

    }

}
