package neonique.cbcplugin_new.combat.display;

import neonique.cbcplugin_new.core.CBCPlayer;

import java.util.*;
import java.util.List;

import neonique.cbcplugin_new.util.ConfigUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.configuration.ConfigurationSection;

public record DeathMessageGenerator (List<DeathMessage> direct,
                                     List<DeathMessage> indirect,
                                     List<DeathMessage> self,
                                     Random random) {

    public DeathMessageGenerator (List<DeathMessage> direct,
                                  List<DeathMessage> indirect,
                                  List<DeathMessage> self) {
        this(direct, indirect, self, new Random());
    }

    public static DeathMessageGenerator fromConfig (ConfigurationSection section) {
        return new DeathMessageGenerator(
                listFromConfig(section, "direct"),
                listFromConfig(section, "indirect"),
                listFromConfig(section, "self")
        );
    }

    public static List<DeathMessage> listFromConfig (ConfigurationSection section, String key) {
        return ConfigUtil.getConfigurationSection(section, key).map(
                sec -> ConfigUtil.getAllConfigSections(sec).values().stream()
                        .map(DeathMessage::fromConfig)
                        .toList())
                .orElse(List.of());
    }

    public Component getDeathMessageComponent (CBCPlayer playerKilled, CBCPlayer playerKiller, boolean direct,
                                               TextColor baseColor) {

        DeathMessage deathMessage;
        if (playerKiller != null) {
            if (direct) {
                deathMessage = getRandomMessage(this.direct);
            } else {
                deathMessage = getRandomMessage(this.indirect);
            }
        } else {
            deathMessage = getRandomMessage(this.self);
        }

        // Convert death message to component
        return deathMessage.getMessageComponent(playerKilled, playerKiller, baseColor);

    }

    private DeathMessage getRandomMessage (List<DeathMessage> messagesPool) {
        return messagesPool.get(random.nextInt(messagesPool.size()));
    }

}
