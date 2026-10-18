package neonique.cbcplugin_new.hud;

import net.megavex.scoreboardlibrary.api.ScoreboardLibrary;
import net.megavex.scoreboardlibrary.api.sidebar.Sidebar;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class SidebarManager {

    private final ScoreboardLibrary library;
    private final SidebarProvider provider;
    private final Map<UUID, Sidebar> playerSidebars = new HashMap<>();
    private boolean active = false;

    public SidebarManager (ScoreboardLibrary library, SidebarProvider provider) {
        this.library = library;
        this.provider = provider;
    }

    /// Activates the sidebar manager.
    /// @throws IllegalArgumentException If the sidebar manager is already active.
    public void activate () {
        if (active) throw new IllegalStateException("Sidebar manager is already active");
        active = true;
    }

    /// Deactivates the sidebar manager and cleans up all sidebars.
    /// @throws IllegalStateException If the sidebar manager is not active.
    public void deactivate () {
        if (!active) throw new IllegalStateException("Sidebar manager is not active");
        active = false;
        Map.copyOf(playerSidebars).forEach((u, s) -> s.close());
    }

    /// Creates and assigns a sidebar to the given player. The manager will start tracking this sidebar.
    /// @param uuid The UUID of the player to add.
    public void registerPlayerUUID (UUID uuid) {
        playerSidebars.put(uuid, library.createSidebar());
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
    /// @throws IllegalArgumentException If a player with the UUID does not exist.
    public void updatePlayer (UUID playerUUID) {
        if (!active) throw new IllegalStateException("Sidebar manager is not active");
        Player p = Bukkit.getPlayer(playerUUID);
        if (p == null) throw new IllegalArgumentException("Player UUID provided does not have a Player entity attached");
        updatePlayer(p);
    }

    /// Updates a player's sidebar display, given information from the provider.
    public void updatePlayer (Player player) {
        if (!active) throw new IllegalStateException("Sidebar manager is not active");
        Sidebar sidebar = playerSidebars.get(player.getUniqueId());
        provider.getSidebarLayout(player).apply(sidebar);
    }

    /// Updates all sidebars stored in the sidebar manager.
    public void updateAll () {
        if (!active) throw new IllegalStateException("Sidebar manager is not active");
        playerSidebars.forEach((u, s) -> updatePlayer(u));
    }

}
