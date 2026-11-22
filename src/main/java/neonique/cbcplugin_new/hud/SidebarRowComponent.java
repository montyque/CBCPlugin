package neonique.cbcplugin_new.hud;

import neonique.cbcplugin_new.util.TextUtil;
import net.kyori.adventure.text.Component;

/**
 *
 * @param component The SingleSidebarComponent object attached to this row component.
 * @param width The maximum width of this component.
 * @param alignment The horizontal alignment of the component.
 */
public record SidebarRowComponent (SingleSidebarComponent component, int width, ComponentAlignment alignment) {

    public static SidebarRowComponent space () {
        return new SidebarRowComponent(SingleSidebarComponent.of(Component.space()), 4, ComponentAlignment.LEFT);
    }

    public static SidebarRowComponent space (int width) {
        return new SidebarRowComponent(SingleSidebarComponent.empty(), width, ComponentAlignment.LEFT);
    }

    /**
     * @return Gets the component.
     */
    public Component getComponent () {

        int spaceWidth = Math.max(0, width - component.width());
        Component space = TextUtil.getComponentSpaceOfLength(spaceWidth);

        if (alignment == ComponentAlignment.LEFT) {
            return component.component().append(space);
        } else {
            return space.append(component.component());
        }

    }

}
