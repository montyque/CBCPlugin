package neonique.cbcplugin_new.hud;

import neonique.cbcplugin_new.CBCPlugin;
import net.kyori.adventure.text.Component;
import net.megavex.scoreboardlibrary.api.sidebar.component.ComponentSidebarLayout;
import net.megavex.scoreboardlibrary.api.sidebar.component.SidebarComponent;
import org.bukkit.entity.Player;

public interface SidebarProvider {

    /// Returns whether the sidebar should be visible for a player.
    boolean visible (Player player);

    /// Display title of the sidebar.
    SidebarComponent title ();

    /// Returns the lines of a sidebar for a given player.
    SidebarComponent lines (Player player);

    default ComponentSidebarLayout getSidebarLayout (Player player) {
        CBCPlugin.getPlugin().getLogger().info("Getting sidebar layout for player: " + player.getName());
        return new ComponentSidebarLayout(title(), lines(player));
    }

    static SidebarProvider hidden () {
        return new EmptySidebarProvider();
    }

    class EmptySidebarProvider implements SidebarProvider {
        @Override public boolean visible(Player player) {
            return false;
        }
        @Override public SidebarComponent title() {
            return SidebarComponent.staticLine(Component.empty());
        }
        @Override public SidebarComponent lines(Player player) {
            return SidebarComponent.staticLine(Component.empty());
        }
    }

}
