package neonique.cbcplugin_new.hud;

import neonique.cbcplugin_new.CBCPlugin;
import net.megavex.scoreboardlibrary.api.ScoreboardLibrary;
import net.megavex.scoreboardlibrary.api.sidebar.Sidebar;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class SidebarManager implements Listener {

    private final Plugin plugin;
    private final ScoreboardLibrary library;
    private SidebarProvider provider = null;
    private final Map<UUID, Sidebar> playerSidebars = new HashMap<>();
    private boolean active = false;

    public SidebarManager (Plugin plugin, ScoreboardLibrary library) {
        this.plugin = plugin;
        this.library = library;
    }

    public void setProvider (SidebarProvider provider) {
        this.provider = provider;
    }

    /// Activates the sidebar manager.
    /// @throws IllegalArgumentException If the sidebar manager is already active.
    public void activate () {
        if (active) throw new IllegalStateException("Sidebar manager is already active");
        active = true;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    /// Deactivates the sidebar manager and cleans up all sidebars.
    /// @throws IllegalStateException If the sidebar manager is not active.
    public void deactivate () {
        if (!active) throw new IllegalStateException("Sidebar manager is not active");
        active = false;
        Map.copyOf(playerSidebars).forEach((u, s) -> s.close());
        HandlerList.unregisterAll(this);
    }

    /// Adds the given UUID to the list of players tracked by the sidebar manager.
    /// If the player with the given UUID is online, a new sidebar will be created for them.
    /// @param playerUUID The UUID of the player to add.
    public void registerPlayer (UUID playerUUID) {
        Player p = Bukkit.getPlayer(playerUUID);
        if (p != null && p.isOnline()) {
            assignNewSidebar(p);
        } else {
            playerSidebars.put(playerUUID, null);
        }
    }

    /// Assigns a new sidebar to the given player.
    /// @param player The player to assign a new sidebar to.
    public void assignNewSidebar (Player player) {
        Sidebar sidebar = library.createSidebar();
        sidebar.addPlayer(player);
        playerSidebars.put(player.getUniqueId(), sidebar);
    }

    /// Unregisters a player from the sidebar, which also closes the player's sidebar.
    /// @param uuid The UUID of the player to remove.
    public void removePlayerUUID (UUID uuid) {
        Optional.ofNullable(playerSidebars.get(uuid)).ifPresent(Sidebar::close);
        playerSidebars.remove(uuid);
    }

    public boolean isActive () {
        return active;
    }

    /// Updates a player with the given UUID.
    /// If the player is not online, this method does nothing.
    /// @throws IllegalArgumentException If a player with the UUID does not exist.
    public void updatePlayer (UUID playerUUID) {
        if (!active) throw new IllegalStateException("Sidebar manager is not active");
        Player p = Bukkit.getPlayer(playerUUID);
        if (p == null) return;
        updatePlayer(p);
    }

    /// Updates a player's sidebar display, given information from the provider.
    /// If the player is not online, this method does nothing.
    public void updatePlayer (Player player) {
        if (!active) throw new IllegalStateException("Sidebar manager is not active");
        if (provider == null) throw new IllegalStateException("Sidebar provider is not set");
        if (!player.isOnline()) return;
        Sidebar sidebar = playerSidebars.get(player.getUniqueId());
        provider.getSidebarLayout(player).apply(sidebar);
    }

    /// Updates all sidebars stored in the sidebar manager.
    public void updateAll () {
        if (!active) throw new IllegalStateException("Sidebar manager is not active");
        if (provider == null) throw new IllegalStateException("Sidebar provider is not set");
        playerSidebars.forEach((u, s) -> updatePlayer(u));
    }

    /// Event handler for when a player joins the server.
    /// If the player is registered in the sidebar manager, they will be assigned a new sidebar.
    @EventHandler
    public void onPlayerJoin (PlayerJoinEvent e) {
        Player p = e.getPlayer();
        if (playerSidebars.containsKey(p.getUniqueId())) {
            assignNewSidebar(p);
            updatePlayer(p);
        }
    }

    /// Event handler for when a player leaves the server.
    /// If the player is registered in the sidebar manager, their sidebar will be closed and removed.
    @EventHandler
    public void onPlayerQuit (PlayerQuitEvent e) {
        Player p = e.getPlayer();
        if (playerSidebars.containsKey(p.getUniqueId())) {
            Sidebar s = playerSidebars.get(p.getUniqueId());
            if (s != null) s.close();
            playerSidebars.put(p.getUniqueId(), null);
        }
    }

}
