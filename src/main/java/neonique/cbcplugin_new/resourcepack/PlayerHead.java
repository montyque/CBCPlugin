package neonique.cbcplugin_new.resourcepack;

import neonique.cbcplugin_new.enums.PlayerHeadType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;

import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.UUID;

import static neonique.cbcplugin_new.resourcepack.ResourcePackManager.normalText;

public class PlayerHead {

    private final HashMap<PlayerHeadType, Component> playerHeadComponents;
    private final UUID playerUUID;

    public PlayerHead (UUID playerUUID) {
        this.playerUUID = playerUUID;

        playerHeadComponents = new HashMap<>();
    }

    public void setHead (BufferedImage head) {

        // Add normal head
        Component normal = imageToPlayerHead(0xE800, head);
        playerHeadComponents.put(PlayerHeadType.NORMAL, normal);

        // Add transparent head
        Component transparent = imageToPlayerHead(0xE808, head);
        playerHeadComponents.put(PlayerHeadType.TRANSPARENT, transparent);

        // Add normal head
        Component normal_24up = imageToPlayerHead(0xE810, head);
        playerHeadComponents.put(PlayerHeadType.DOWN_24_NORMAL, normal_24up);

        // Add normal head
        Component transparent_24up = imageToPlayerHead(0xE818, head);
        playerHeadComponents.put(PlayerHeadType.DOWN_24_TRANSPARENT, transparent_24up);

    }

    public void setHeadOneCharacter (int slot) {

        // Add normal head
        Component normal = normalText(String.valueOf((char) (0xE000 + slot)));
        playerHeadComponents.put(PlayerHeadType.NORMAL, normal);

        // Add transparent head
        Component transparent = normalText(String.valueOf((char) (0xE040 + slot)));
        playerHeadComponents.put(PlayerHeadType.TRANSPARENT, transparent);

        // Add normal head
        Component normal_24up = normalText(String.valueOf((char) (0xE080 + slot)));
        playerHeadComponents.put(PlayerHeadType.DOWN_24_NORMAL, normal_24up);

        // Add normal head
        Component transparent_24up = normalText(String.valueOf((char) (0xE0C0 + slot)));
        playerHeadComponents.put(PlayerHeadType.DOWN_24_TRANSPARENT, transparent_24up);
    }

    public Component getPlayerHead (PlayerHeadType type) {
        return playerHeadComponents.getOrDefault(type, null);
    }

    public Component imageToPlayerHead (int startingChar, BufferedImage head) {
        Component headComponent = Component.text("");
        for (int x = 0; x < 8; x++) {
            for (int y = 0; y < 8; y++) {
                TextColor pixelColor = TextColor.color(head.getRGB(x, y));
                // Get text
                String character = Character.toString((char) (startingChar + y));
                if (y != 7) {
                    headComponent = headComponent.append(normalText(character + "\uF802").color(pixelColor));
                } else {
                    headComponent = headComponent.append(normalText(character).color(pixelColor));
                }
            }

            if (x != 7) {
                headComponent = headComponent.append(normalText("\uF801"));
            }
        }
        return headComponent;
    }


}
