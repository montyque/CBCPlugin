package neonique.cbcplugin_new.hud;

import neonique.cbcplugin_new.util.TextUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentBuilder;
import net.kyori.adventure.text.TextComponent;

import java.util.List;

public record LeftRightSidebarRow (List<SidebarRowComponent> left,
                                   List<SidebarRowComponent> right) implements SidebarRow {

    /**
     * Get the row's contents as a component.
     * @param rowWidth The width that the component should be in pixels.
     * @return The row's contents as a component.
     */
    public Component component (int rowWidth) {

        int leftWidth = getLeftWidth();
        int rightWidth = getRightWidth();
        int spaceWidth = Math.max(0, rowWidth - (leftWidth + rightWidth));
        Component space = TextUtil.getComponentSpaceOfLength(spaceWidth);

        return Component.text().append(leftComponent(), space, rightComponent()).build();

    }

    /**
     * @return The left side of the row as a component.
     */
    public Component leftComponent () {
        TextComponent.Builder result = Component.text();
        for (SidebarRowComponent c : left) {
            result.append(c.component());
        }
        return result.build();
    }

    /**
     * @return The right side of the row as a component.
     */
    public Component rightComponent () {
        TextComponent.Builder result = Component.text();
        for (SidebarRowComponent c : left) {
            result.append(c.component());
        }
        return result.build();
    }

    /**
     * @return The width of the left side of the component in pixels.
     */
    public int getLeftWidth () {
        int total = left.size() - 1;
        for (SidebarRowComponent c : left) {
            total += c.width();
        }
        return total;
    }

    /**
     * @return The width of the right side of the component in pixels.
     */
    public int getRightWidth () {
        int total = right.size() - 1;
        for (SidebarRowComponent c : right) {
            total += c.width();
        }
        return total;
    }

    /**
     * @return The width of the row at a minimum, with 1 pixel spacing between the left and right components.
     */
    public int minWidth () {
        return getLeftWidth() + getRightWidth() + 1;
    }

}
