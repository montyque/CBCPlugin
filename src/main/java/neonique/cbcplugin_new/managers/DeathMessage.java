package neonique.cbcplugin_new.managers;

import neonique.cbcplugin_new.core.CBCPlayer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public class DeathMessage {

    private String beforeKilled = "";
    private String afterKilled = "";
    private String afterKiller = "";

    public DeathMessage (String beforeKilled, String afterKilled, String afterKiller) {

        this.beforeKilled = beforeKilled;
        this.afterKilled = afterKilled;
        this.afterKiller = afterKiller;

    }

    public DeathMessage (String deathMessage) {

        // Find beforeKilled and afterKilled
        String[] deathMessageComponents = deathMessage.split("<KILLED>|<KILLER>");

        if (deathMessageComponents.length == 1) {
            afterKilled = deathMessageComponents[0];
        }
        else if (deathMessageComponents.length == 2) {
            beforeKilled = deathMessageComponents[0];
            afterKilled = deathMessageComponents[1];
        }
        else if (deathMessageComponents.length == 3) {
            beforeKilled = deathMessageComponents[0];
            afterKilled = deathMessageComponents[1];
            afterKiller = deathMessageComponents[2];
        }
        else {
            // Raise error

        }

    }

    public Component getMessageComponent (CBCPlayer playerKilled, CBCPlayer playerKiller) {

        if (playerKiller != null && afterKiller != null) {

            // Return a death message with both the killed player's name and the killer's name
            return Component.text(beforeKilled).color(NamedTextColor.GRAY)
                    .append(playerKilled.getNameComponent())
                    .append(Component.text(afterKilled).color(NamedTextColor.GRAY))
                    .append(playerKiller.getNameComponent())
                    .append(Component.text(afterKiller).color(NamedTextColor.GRAY));

        }

        else {

            // Return a death message with just the killed player's name
            return Component.text()
                    .content(beforeKilled).color(NamedTextColor.GRAY)
                    .append(playerKilled.getNameComponent())
                    .append(Component.text().content(afterKilled).color(NamedTextColor.GRAY)).build();

        }
    }
}
