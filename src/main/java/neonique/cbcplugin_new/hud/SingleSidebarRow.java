package neonique.cbcplugin_new.hud;

import net.kyori.adventure.text.Component;

public record SingleSidebarRow (SidebarRowComponent component) implements SidebarRow {

    @Override
    public Component component(int rowWidth) {
        return component.component();
    }

    @Override
    public int minWidth() {
        return component.width();
    }

}
