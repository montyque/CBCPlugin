package neonique.cbcplugin_new.gamemodes._base;

import neonique.cbcplugin_new.CBCPlugin;
import neonique.cbcplugin_new.resourcepack.PlayerHeadType;
import neonique.cbcplugin_new.playerclasses.CBCPlayer;
import neonique.cbcplugin_new.resourcepack.ResourcePackManager;
import neonique.cbcplugin_new.util.TextUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static neonique.cbcplugin_new.resourcepack.ResourcePackManager.smallText;
import static neonique.cbcplugin_new.util.TextUtil.getComponentSpaceOfLength;
import static neonique.cbcplugin_new.util.TextUtil.timerToText;

public class SidebarStatCycle<T extends CBCPlayer> {

    private final Game game;
    private final GameSidebarManager sidebarManager;
    private final ResourcePackManager resourcePackManager;
    private final ArrayList<PlayerStatList<T>> cycleStates = new ArrayList<>();

    private int cyclePosition = 0;

    // Player row display decisions
    private final int playerNameMaxLength;

    public SidebarStatCycle(Game game, GameSidebarManager sidebarManager, int playerNameMaxLength) {
        this.game = game;
        this.sidebarManager = sidebarManager;
        this.resourcePackManager = CBCPlugin.getResourcePackManager();
        this.playerNameMaxLength = playerNameMaxLength;
    }

    public void addCycleState (PlayerStatList<T> cycleState) {
        cycleStates.add(cycleState);
    }

    public void nextPosition() {
        cyclePosition++;
        if (cyclePosition == cycleStates.size()) {
            cyclePosition = 0;
        }
        sidebarManager.updateServerBoard();
    }

    public void startCycle(int ticksToCycle) {
        BukkitRunnable cyclingTask = new BukkitRunnable() {
            @Override
            public void run() {
                if (game.isGameOver()) {
                    this.cancel();
                    return;
                }
                nextPosition();
            }
        };

        cyclingTask.runTaskTimer(CBCPlugin.getPlugin(), 0, ticksToCycle);
    }

    public List<Component> getComponents (Collection<T> players) {

        List<Component> components = new ArrayList<>();

        PlayerStatList<T> currentLeaderboard = getCurrentStat();
        ArrayList<T> sortedList = currentLeaderboard.getSortedList(players, false);

        int i = 0;
        while (i < 5 && sortedList.size() > i) {
            T player = sortedList.get(i);
            int placement = currentLeaderboard.getPlacementOfPlayerSorted(sortedList, player);
            components.add(getLeaderboardRow(player, placement, currentLeaderboard.getPlayerStat(player),
                    currentLeaderboard.isTimeFormatted()));
            i++;
        }

        return components;

    }

    public Component getLeaderboardRow (CBCPlayer player, int placement, int value, boolean displayAsTime) {

        Component playerComponent = getComponentSpaceOfLength(11);

        // Add placement
        String placementString = placement + ". ";
        playerComponent = playerComponent.append(Component.text(placementString).color(NamedTextColor.WHITE));

        // Add player head
        Component playerHeadComponent = resourcePackManager.getPlayerHeadComponent(PlayerHeadType.NORMAL, player.getOfflinePlayer());
        playerComponent = playerComponent.append(playerHeadComponent);
        playerComponent = playerComponent.append(getComponentSpaceOfLength(4));

        // Add player name
        String playerName = player.getName();
        playerComponent = playerComponent.append(player.getNameComponent());

        // Add space to make names even
        int playerNameLength = TextUtil.getPixelLengthOfText(playerName);
        playerComponent = playerComponent.append(getComponentSpaceOfLength(playerNameMaxLength - playerNameLength));

        // Add value
        if (displayAsTime) {
            if (value < 600) {
                playerComponent = playerComponent.append(getComponentSpaceOfLength(7));
            }
            playerComponent = playerComponent.append(Component.text("\uF812" + timerToText(value)).color(NamedTextColor.WHITE));
        }
        else {
            playerComponent = TextUtil.addLeadingSpaceForNumber(playerComponent, value, 4);
            playerComponent = playerComponent.append(Component.text(value).color(NamedTextColor.WHITE));
        }

        return playerComponent;
    }

    private PlayerStatList<T> getCurrentStat() {
        return cycleStates.get(cyclePosition);
    }

    public Component getCurrentStatTitle() {
        return smallText("TOP " + getCurrentStat().getName() + ":").color(NamedTextColor.AQUA);
    }

}
