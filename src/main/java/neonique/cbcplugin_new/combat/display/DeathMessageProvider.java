package neonique.cbcplugin_new.combat.display;

import neonique.cbcplugin_new.CBCPlugin;
import neonique.cbcplugin_new.combat.DeathCause;
import neonique.cbcplugin_new.core.CBCPlayer;
import neonique.cbcplugin_new.util.ConfigUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

import static neonique.cbcplugin_new.resourcepack.ResourcePackManager.smallText;

public record DeathMessageProvider (Map<DeathCause, DeathMessageGenerator> generators) {

    public static DeathMessageProvider fromConfig (ConfigurationSection config) {
        return new DeathMessageProvider(
                ConfigUtil.getAllConfigSections(config).entrySet().stream()
                    .filter(e -> Arrays.stream(DeathCause.values())
                            .anyMatch(d -> d.name().equals(e.getKey().toUpperCase())))
                    .collect(Collectors.toUnmodifiableMap(
                            e -> DeathCause.valueOf(e.getKey().toUpperCase()),
                            e -> DeathMessageGenerator.fromConfig(e.getValue())
                    )));
    }

    public static DeathMessageProvider empty () {
        return new DeathMessageProvider(Map.of());
    }

    public Component getDeathMessage(CBCPlayer playerKilled,
                                     CBCPlayer playerKiller,
                                     DeathCause cause,
                                     boolean direct,
                                     TextColor baseColor) {

        // Retrieve death message via cause and circumstances of kill
        DeathMessageGenerator gen = generators.get(cause);

        Component deathMessage = gen.getDeathMessageComponent(playerKilled, playerKiller, direct, baseColor);

        // Add multi kill counter
        if (playerKiller != null) {
            playerKiller.updateMultiKill();
            String multiText = "";
            if (playerKiller.getMultiKill() == 2) {
                multiText = " DOUBLE KILL!";
            } else if (playerKiller.getMultiKill() == 3) {
                multiText = " TRIPLE KILL!";
            } else if (playerKiller.getMultiKill() == 4) {
                multiText = " QUADRA KILL!";
            } else if (playerKiller.getMultiKill() == 5) {
                multiText = " PENTAKILL!";
            }
            deathMessage = deathMessage.append(smallText(multiText).color(NamedTextColor.AQUA).decorate(TextDecoration.BOLD));
        }

        return deathMessage;

    }

    public Component getKillStreakMessage(CBCPlayer playerKiller) {

        Component playerComponent = playerKiller.nameComponent().decorate(TextDecoration.BOLD);

        if (playerKiller.getKillStreak() == 5) {
            return playerComponent.append(Component.text(" is on a killing spree!").color(NamedTextColor.WHITE).decorate(TextDecoration.BOLD));
        } else if (playerKiller.getKillStreak() == 10) {
            return playerComponent.append(Component.text(" is unstoppable!").color(NamedTextColor.WHITE).decorate(TextDecoration.BOLD));
        } else if (playerKiller.getKillStreak() == 15) {
            return playerComponent.append(Component.text(" is dominating!").color(NamedTextColor.WHITE).decorate(TextDecoration.BOLD));
        } else if (playerKiller.getKillStreak() == 20) {
            return playerComponent.append(Component.text(" is legendary!").color(NamedTextColor.WHITE)).decorate(TextDecoration.BOLD);
        }
        return null;
    }

    public Component getKillStreakEndedMessage(CBCPlayer playerKilled, CBCPlayer playerKiller) {

        int killStreak = playerKilled.getKillStreak();

        Component playerKilledComponent = playerKilled.nameComponent();

        if (playerKiller == null) {
            return playerKilledComponent
                    .append(Component.text("'s kill streak of ").color(NamedTextColor.WHITE))
                    .append(Component.text(killStreak).color(NamedTextColor.GREEN))
                    .append(Component.text(" has ended!").color(NamedTextColor.WHITE))
                    .decorate(TextDecoration.BOLD);
        } else {
            return playerKilledComponent
                    .append(Component.text("'s kill streak of ").color(NamedTextColor.WHITE))
                    .append(Component.text(killStreak).color(NamedTextColor.GREEN))
                    .append(Component.text(" has been ended by ").color(NamedTextColor.WHITE))
                    .append(playerKiller.nameComponent())
                    .append(Component.text("!").color(NamedTextColor.WHITE))
                    .decorate(TextDecoration.BOLD);
        }
    }
}
