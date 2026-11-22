package neonique.cbcplugin_new.hud;

import net.kyori.adventure.text.Component;
import net.megavex.scoreboardlibrary.api.sidebar.Sidebar;

public interface SidebarRow {

    Component component (int rowWidth);

    int minWidth ();

    static SidebarRow blank () {
        return SingleSidebarComponent.empty();
    }

}
