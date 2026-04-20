package neonique.cbcplugin_new.gamemodes._base;

import neonique.cbcplugin_new.gamemodes.CBCGamemode;
import neonique.cbcplugin_new.scoreboard.CBCScoreboardManager;
import neonique.cbcplugin_new.managers.GameManager;
import neonique.cbcplugin_new.misc.ClientSidebar;
import neonique.cbcplugin_new.playerclasses.CBCPlayer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Scoreboard;

import java.util.Collection;
import java.util.HashMap;
import java.util.UUID;

import static neonique.cbcplugin_new.util.TextUtil.getComponentSpaceOfLength;

public abstract class GameSidebarManager {

    private final CBCScoreboardManager scoreboardManager;

    private final HashMap<UUID, ClientSidebar> clientSidebars;
    private final String sidebarObjectiveName;
    private Component sidebarTitle;
    private boolean active = false;

    public GameSidebarManager (GameManager gameManager, String sidebarObjectiveName) {

        this.sidebarObjectiveName = sidebarObjectiveName;
        this.scoreboardManager = gameManager.getCbcScoreboardManager();

        this.sidebarTitle = Component.space();
        clientSidebars = new HashMap<>();

    }


    public GameSidebarManager (GameManager gameManager, String sidebarObjectiveName, Component sidebarTitle) {

        this.sidebarObjectiveName = sidebarObjectiveName;
        this.scoreboardManager = gameManager.getCbcScoreboardManager();

        this.sidebarTitle = sidebarTitle;
        clientSidebars = new HashMap<>();

    }

    public void setupSidebar (Collection<Player> players) {
        active = true;
        for (Player player : players) {
            addPlayerSidebar(player);
        }
    }

    public void removeSidebar () {
        for (UUID playerUUID : clientSidebars.keySet()) {
            removePlayerSidebar(playerUUID);
        }
        clientSidebars.clear();
        active = false;
    }

    public void removePlayerSidebar (UUID playerUUID) {
        ClientSidebar playerSidebar = clientSidebars.getOrDefault(playerUUID, null);
        if (playerSidebar == null) return;
        playerSidebar.removeSidebar();
    }

    public void addPlayerSidebar (Player player) {

        // Get scoreboard from player
        Scoreboard playerScoreboard = scoreboardManager.getPlayerScoreboard(player.getUniqueId());
        if (playerScoreboard == null) {
            scoreboardManager.addPlayer(player);
            playerScoreboard = scoreboardManager.getPlayerScoreboard(player.getUniqueId());
        }

        ClientSidebar playerSidebar = new ClientSidebar(player, playerScoreboard, sidebarObjectiveName, sidebarTitle, true);
        clientSidebars.put(player.getUniqueId(), playerSidebar);

        updateClientBoard(player);

    }

    public void removePlayerSidebar (Player player) {
        clientSidebars.remove(player.getUniqueId());
    }

    public void updateServerBoard () {}

    public void updateClientBoard (Player player) {}

    public void updateAllClientBoards () {
        for (UUID uuid : clientSidebars.keySet()) {
            Player player = Bukkit.getPlayer(uuid);
            if (player == null) continue;
            updateClientBoard(player);
        }
    }

    public boolean isActive() {
        return active;
    }

    public void setSidebarTitle (Component newSidebarTitle) {
        this.sidebarTitle = newSidebarTitle;
    }

    public Component generateGameScoreComponent(CBCGamemode gamemode, CBCPlayer player, Component startingComponent) {

        Component gameScoreComponent = startingComponent;

        gameScoreComponent = gameScoreComponent.append(Component.text("Game Score: ").color(NamedTextColor.AQUA));

        // Get team icon for gamemode
        String gamemodeIcon;
        if (player.getTeam() != null) {
            gamemodeIcon = gamemode.getUnicodeIcon(player.getTeam().getColor());
        }
        else {
            gamemodeIcon = gamemode.getUnicodeIcon(NamedTextColor.WHITE);
        }

        gameScoreComponent = gameScoreComponent.append(Component.text(gamemodeIcon + " ").color(NamedTextColor.WHITE));
        gameScoreComponent = gameScoreComponent.append(Component.text(player.getGamePoints()).color(NamedTextColor.YELLOW));
        return gameScoreComponent;

    }

    public Component addLeadingSpaceForNumber (Component component, int num, int digits) {
        Component newComponent = component;
        if (num < 1000 && digits >= 4) {
            newComponent = newComponent.append(getComponentSpaceOfLength(7));
        }
        if (num < 100 && digits >= 3) {
            newComponent = newComponent.append(getComponentSpaceOfLength(7));
        }
        if (num < 10 && digits >= 2) {
            newComponent = newComponent.append(getComponentSpaceOfLength(7));
        }
        return newComponent;
    }

    public ClientSidebar getPlayerSidebar (Player player) {
        return clientSidebars.get(player.getUniqueId());
    }
}
