package neonique.cbcplugin_new.hud;

import net.kyori.adventure.text.Component;

import java.util.function.Supplier;

public record ComponentWidth (Supplier<Component> componentSupplier, Supplier<Integer> widthSupplier) {

    public Component component () {
        return componentSupplier.get();
    }

    public int width () {
        return widthSupplier.get();
    }

}
