package neonique.cbcplugin_new.gamemodes._base;

import neonique.cbcplugin_new.enums.CBCGamemode;
import neonique.cbcplugin_new.managers.CBCScoreboardManager;
import neonique.cbcplugin_new.managers.GameManager;
import neonique.cbcplugin_new.managers.CombatManager;
import neonique.cbcplugin_new.misc.ClientSidebar;
import neonique.cbcplugin_new.playerclasses.CBCPlayer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;

import java.util.HashMap;
import java.util.UUID;

import static neonique.cbcplugin_new.util.TextUtil.getComponentSpaceOfLength;

public abstract class GameSidebarManager {

    protected GameManager gameManager;
    protected CombatManager combatManager;
    protected World world;

    protected CBCScoreboardManager scoreboardManager;

    protected HashMap<UUID, ClientSidebar> clientSidebars;
    private Component sidebarTitle;
    private String sidebarObjectiveName;

    protected boolean active = false;
    protected boolean showGamePoints = false;

    public GameSidebarManager (GameManager gameManager, CombatManager combatManager, String sidebarObjectiveName) {

        this.gameManager = gameManager;
        this.combatManager = combatManager;
        this.sidebarObjectiveName = sidebarObjectiveName;
        this.world = gameManager.getWorld();

        this.scoreboardManager = gameManager.getCbcScoreboardManager();

        sidebarTitle = Component.text("");
        clientSidebars = new HashMap<>();

        if (gameManager.isThisGameCBCGame()) {
            showGamePoints = true;
        }
    }

    public void setupSidebar () {

        active = true;

        // Create sidebars for all online players
        for (Player player : world.getPlayers()) {
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

        ClientSidebar playerSidebar = clientSidebars.getOrDefault(player.getUniqueId(), null);

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

    public void setSidebarVisible (Objective objective) {
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);
    }

    public boolean isActive() {
        return active;
    }

    public void setSidebarTitle (Component newSidebarTitle) {
        this.sidebarTitle = newSidebarTitle;
    }

    public void setSidebarObjectiveName (String sidebarObjectiveName) {
        this.sidebarObjectiveName = sidebarObjectiveName;
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

    public boolean isShowGamePoints() {
        return showGamePoints;
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
}
