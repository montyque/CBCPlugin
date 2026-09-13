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

    /**
     * @return Gets the component
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
