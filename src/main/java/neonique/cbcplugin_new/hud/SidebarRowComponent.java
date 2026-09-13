package neonique.cbcplugin_new.hud;

import neonique.cbcplugin_new.util.TextUtil;
import net.kyori.adventure.text.Component;

/**
 *
 * @param componentWidth The ComponentWidth object attached to this row component.
 * @param width The maximum width of this component.
 * @param alignment The horizontal alignment of the component.
 */
public record SidebarRowComponent (ComponentWidth componentWidth, int width, ComponentAlignment alignment) {

    /**
     * @return Gets the component
     */
    public Component component () {

        int spaceWidth = Math.max(0, width - componentWidth.width());
        Component space = TextUtil.getComponentSpaceOfLength(spaceWidth);

        if (alignment == ComponentAlignment.LEFT) {
            return componentWidth.component().append(space);
        } else {
            return space.append(componentWidth.component());
        }

    }

}
