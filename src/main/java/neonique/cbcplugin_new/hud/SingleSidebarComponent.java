package neonique.cbcplugin_new.hud;

import neonique.cbcplugin_new.util.TextUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;

public record SingleSidebarComponent (Component component, int width) implements SidebarRow {

    public Component component (int minWidth) {
        return component;
    }

    public int minWidth () {
        return width;
    }

    public static SingleSidebarComponent of (TextComponent text) {
        int width = TextUtil.getPixelLengthOfText(text.content());
        return new SingleSidebarComponent(text, width);
    }

    public static SingleSidebarComponent empty () {
        return new SingleSidebarComponent(Component.empty(), 0);
    }

}
