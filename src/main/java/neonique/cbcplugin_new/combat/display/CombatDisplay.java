package neonique.cbcplugin_new.combat.display;

import neonique.cbcplugin_new.CBCPlugin;
import neonique.cbcplugin_new.combat.DeathCause;
import neonique.cbcplugin_new.core.CBCPlayer;
import neonique.cbcplugin_new.resourcepack.ResourcePackManager;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.title.Title;

import java.time.Duration;
import java.util.Optional;

public class CombatDisplay {

    private final Audience audience;
    private DeathMessageProvider deathMessageProvider;

    public CombatDisplay (Audience audience) {
        this.audience = audience;
    }
    
    public void setDeathMessageProvider (DeathMessageProvider deathMessageProvider) {
        this.deathMessageProvider = deathMessageProvider;
    }

    public Component getDeathMessage (CBCPlayer victim, CBCPlayer killer, DeathCause cause, boolean direct) {
        return Component.text()
                .append(Component.text("["))
                .append(cause.deathIconComponent(victim, killer))
                .append(Component.text("] "))
                .append(deathMessageProvider.getDeathMessage(victim, killer, cause, direct, NamedTextColor.GRAY))
                .color(NamedTextColor.GRAY)
                .build();
    }

    public void onPlayerDeath (CBCPlayer victim, CBCPlayer killer, DeathCause cause, boolean direct) {

        // Send death message
        audience.sendMessage(getDeathMessage(victim, killer, cause, direct));

        // Play death effect
        if (victim.isOnline()) {
            cause.playDeathEffect(audience, victim.getPlayer().getLocation(), victim);
        }

        // Show kill display to killer
        if (killer != null) {
            killer.showTitle(Title.title(
                    Component.text(""),
                    getKillDisplay(victim, killer, cause),
                    Title.Times.times(
                            Duration.ofMillis(0),
                            Duration.ofMillis(1000),
                            Duration.ofMillis(250)
                    )
            ));
        }

        // Check for any kill streaks ending
        Optional<Component> ksEnded = getKillStreakEndedComponent(victim, killer);
        CBCPlugin.getPlugin().getLogger().info(ksEnded.isPresent() + " ");
        if (ksEnded.isPresent()) {
            audience.sendMessage(ksEnded.get());
            audience.playSound(Sound.sound(
                    org.bukkit.Sound.BLOCK_BEACON_DEACTIVATE, Sound.Source.MASTER, 5, 1
            ));
        }

        // Check for any kill streak starting messages
        if (killer != null) {
            Optional<Component> ks = getKillStreakComponent(killer);
            if (ks.isPresent()) {
                audience.sendMessage(ks.get());
                audience.playSound(Sound.sound(
                        org.bukkit.Sound.ENTITY_ELDER_GUARDIAN_CURSE, Sound.Source.MASTER, 5, 1
                ));
            }
        }

    }

    public Component getKillDisplay (CBCPlayer victim, CBCPlayer killer, DeathCause cause) {
        Component deathCauseIcon = cause.deathIconComponent(victim, killer);
        return Component.text()
                .append(ResourcePackManager.smallText(getMultiKillMarker(killer.getMultiKill())))
                .color(NamedTextColor.AQUA)
                .append(Component.space())
                .append(deathCauseIcon)
                .append(Component.space())
                .append(victim.nameComponent())
                .build();
    }

    public Optional<Component> getKillStreakComponent (CBCPlayer killer) {
        return getKillStreakMessage(killer).map(
                s -> Component.text()
                        .append(Component.text("["))
                        .append(Component.text("\uE405")).color(NamedTextColor.WHITE)
                        .append(Component.text("] "))
                        .append(killer.nameComponent())
                        .append(Component.text(s))
                        .color(TextColor.color(255, 154, 71))
                        .build()
        );
    }

    public Optional<String> getKillStreakMessage (CBCPlayer killer) {
        return Optional.ofNullable(switch (killer.getKillStreak()) {
            case 5 -> " is on a killing spree!";
            case 10 -> " is unstoppable!";
            case 15 -> " is dominating!";
            case 20 -> " is legendary!";
            default -> null;
        });
    }

    public Optional<Component> getKillStreakEndedComponent (CBCPlayer victim, CBCPlayer killer) {
        int killStreak = victim.getKillStreak();
        if (killStreak >= 5) {
            TextComponent.Builder builder = Component.text()
                    .append(Component.text("["))
                    .append(Component.text("\uE406")).color(NamedTextColor.WHITE)
                    .append(Component.text("] "))
                    .append(victim.nameComponent())
                    .append(Component.text("'s kill streak of "))
                    .append(Component.text(killStreak).color(TextColor.fromHexString("#adefff")));

            if (killer == null) {
                builder.append(Component.text(" has ended!"));
            } else {
                builder.append(Component.text(" has been ended by "))
                        .append(killer.nameComponent())
                        .append(Component.text("!"));
            }
            return Optional.of(
                    builder
                            .color(TextColor.fromHexString("#66b0ff"))
                            .build()
            );
        } else {
            return Optional.empty();
        }

    }

    private String getMultiKillMarker (int multiKill) {
        return switch (multiKill) {
            case 1 -> "";
            case 2 -> "II";
            case 3 -> "III";
            case 4 -> "IV";
            case 5 -> "V";
            case 6 -> "VI";
            case 7 -> "VII";
            case 8 -> "VIII";
            case 9 -> "IX";
            case 10 -> "X";
            default -> "X+";
        };
    }

}
