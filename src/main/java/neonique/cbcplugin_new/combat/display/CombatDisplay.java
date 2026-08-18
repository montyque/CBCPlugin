package neonique.cbcplugin_new.combat.display;

import neonique.cbcplugin_new.combat.DeathCause;
import neonique.cbcplugin_new.core.CBCPlayer;
import neonique.cbcplugin_new.resourcepack.ResourcePackManager;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;

import java.time.Duration;

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

    }

    public Component getKillDisplay (CBCPlayer victim, CBCPlayer killer, DeathCause cause) {
        Component deathCauseIcon = cause.deathIconComponent(victim, killer);
        return Component.text()
                .append(ResourcePackManager.smallText(getMultiKillMarker(killer.getMultiKill())))
                .color(NamedTextColor.AQUA)
                .append(Component.space())
                .append(deathCauseIcon)
                .append(victim.nameComponent())
                .build();
    }

    private String getMultiKillMarker (int multiKill) {
        return switch (multiKill) {
            case 1 -> "I";
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
