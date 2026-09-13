package neonique.cbcplugin_new.hud;

import net.kyori.adventure.text.Component;

public record SingleSidebarComponent (Component component, int width) implements SidebarRow {

    public Component component (int minWidth) {
        return component;
    }

    public int minWidth () {
        return width;
    }

}
