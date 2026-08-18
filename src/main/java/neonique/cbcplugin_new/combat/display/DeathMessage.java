package neonique.cbcplugin_new.combat.display;

import neonique.cbcplugin_new.core.TeamPlayerLike;
import neonique.cbcplugin_new.util.ConfigUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public record DeathMessage (String middle, String after) {

    public static DeathMessage fromConfig (ConfigurationSection config) {
        return new DeathMessage(
                ConfigUtil.getString(config, "middle").orElse(" was killed by "),
                ConfigUtil.getString(config, "after").orElse("")
        );
    }

    public Component getMessageComponent (@NotNull TeamPlayerLike victim,
                                          @Nullable TeamPlayerLike killer,
                                          TextColor baseColor) {

        if (killer != null) {

            // Return a death message with both the killed player's name and the killer's name
            return Component.text()
                    .append(victim.nameComponent())
                    .append(Component.text(middle))
                    .append(killer.nameComponent())
                    .append(Component.text(after))
                    .build();

        } else {

            // Return a death message with just the killed player's name
            return Component.text()
                    .append(victim.nameComponent())
                    .append(Component.text(middle))
                    .build();

        }

    }
}
