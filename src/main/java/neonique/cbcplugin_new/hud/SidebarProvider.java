package neonique.cbcplugin_new.hud;

import net.megavex.scoreboardlibrary.api.sidebar.component.ComponentSidebarLayout;
import net.megavex.scoreboardlibrary.api.sidebar.component.SidebarComponent;
import org.bukkit.entity.Player;

public interface SidebarProvider {

    /// Title of the sidebar.
    SidebarComponent title ();

    /// Returns the lines of a sidebar for a given player
    SidebarComponent lines (Player player);

    default ComponentSidebarLayout getSidebarLayout (Player player) {
        return new ComponentSidebarLayout(title(), lines(player));
    }

}
