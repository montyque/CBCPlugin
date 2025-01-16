package neonique.cbcplugin_new.enums;

import net.kyori.adventure.key.Key;

public enum ResourcePackFont {

    DEFAULT (Key.key("minecraft", "default")),
    SMALL_5X5 (Key.key("cbc_customfonts", "smallfont")),
    SMALL_5X5_RAISED (Key.key("cbc_customfonts", "smallfont_raised")),
    LARGE_X2_NORMAL (Key.key("cbc_customfonts", "largefont")),
    LARGE_X2_NORMAL_UP (Key.key("cbc_customfonts", "largefontup"));

    private final Key fontKey;

    ResourcePackFont(Key fontKey) {
        this.fontKey = fontKey;
    }

    public Key getFontKey() {
        return fontKey;
    }

}
