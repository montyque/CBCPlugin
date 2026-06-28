package neonique.cbcplugin_new.scoreboard;

import neonique.cbcplugin_new.CBCPlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.scoreboard.Scoreboard;

import java.util.*;

public class SidebarManager implements Listener {

    private static final String SIDEBAR_OBJECTIVE_NAME = "cbcSidebar";

    private final CBCScoreboardManager scoreboardManager;
    private final Map<UUID, ClientSidebar> clientSidebars = new HashMap<>();

    private boolean active = false;

    private SidebarProvider provider = null;
    private final Map<UUID, SidebarProvider> providerOverrides = new HashMap<>();

    public SidebarManager (CBCScoreboardManager scoreboardManager) {
        this.scoreboardManager = scoreboardManager;
    }

    public void activate () {

        this.active = true;
        CBCPlugin.getPlugin().registerListener(this);

        // Add all players on the server to the sidebar
        for (Player p : Bukkit.getOnlinePlayers()) {
            addPlayerSidebar(p);
        }


    }

    public void deactivate () {

        this.active = false;
        for (UUID playerUUID : clientSidebars.keySet()) {
            removePlayerSidebar(playerUUID);
        }

        CBCPlugin.getPlugin().unregisterListener(this);
        clientSidebars.clear();

    }

    public void setProvider (SidebarProvider provider, boolean clearOverrides) {
        this.provider = provider;
        updateAll();
        if (clearOverrides) providerOverrides.clear();
    }

    public void setOverride (Player player, SidebarProvider provider) {
        providerOverrides.put(player.getUniqueId(), provider);
        updateClientBoard(player);
    }

    public void addPlayerSidebar (Player player) {

        // Get scoreboard from player
        Scoreboard playerScoreboard = scoreboardManager.getPlayerScoreboard(player.getUniqueId());
        if (playerScoreboard == null) {
            scoreboardManager.addPlayer(player);
            playerScoreboard = scoreboardManager.getPlayerScoreboard(player.getUniqueId());
        }
        ClientSidebar playerSidebar = new ClientSidebar(player, playerScoreboard, SIDEBAR_OBJECTIVE_NAME, true);
        clientSidebars.put(player.getUniqueId(), playerSidebar);
        updateClientBoard(player);
    }

    public void removePlayerSidebar (UUID playerUUID) {
        ClientSidebar playerSidebar = clientSidebars.get(playerUUID);
        if (playerSidebar == null) return;
        playerSidebar.removeSidebar();
    }

    public void removePlayerSidebar (Player player) {
        removePlayerSidebar(player.getUniqueId());
    }

    public void updateClientBoard (Player client) {

        ClientSidebar sidebar = clientSidebars.get(client.getUniqueId());
        if (sidebar == null) return;

        SidebarProvider provider = getProvider(client);

        if (provider != null) {
            sidebar.setSidebarComponents(provider.getClientDisplay(client));
        } else {
            sidebar.clearSidebar();
        }

    }

    public void updateAll () {
        for (UUID uuid : clientSidebars.keySet()) {
            Player client = Bukkit.getPlayer(uuid);
            if (client != null) updateClientBoard(client);
        }
    }

    public boolean isActive() {
        return active;
    }

    public SidebarProvider getProvider (Player player) {
        return providerOverrides.getOrDefault(player.getUniqueId(), provider);
    }

    @EventHandler
    public void playerJoinServer (PlayerJoinEvent event) {
        if (!active) return;
        Player p = event.getPlayer();
        addPlayerSidebar(p);
        updateClientBoard(p);
    }

    @EventHandler
    public void playerLeaveServer (PlayerQuitEvent event) {
        if (!active) return;
        Player p = event.getPlayer();
        removePlayerSidebar(p);
    }

}
