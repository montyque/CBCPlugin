package neonique.cbcplugin_new.hud;

import net.kyori.adventure.text.Component;

public interface SidebarRow {

    Component component (int rowWidth);

    int minWidth ();

}
