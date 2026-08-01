package neonique.cbcplugin_new.combat;

import neonique.cbcplugin_new.core.CBCPlayer;
import neonique.cbcplugin_new.managers.DeathMessageManager;
import neonique.cbcplugin_new.resourcepack.ResourcePackManager;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;

import java.time.Duration;

public class CombatDisplay {

    private final Audience audience;
    private final DeathMessageManager deathMessageManager;

    public CombatDisplay (Audience audience, DeathMessageManager deathMessageManager) {
        this.audience = audience;
        this.deathMessageManager = deathMessageManager;
    }

    public void onPlayerDeath (CBCPlayer victim, CBCPlayer killer, DeathCause cause, boolean direct) {

        // Send death message
        audience.sendMessage(getDeathMessage(victim, killer, cause, direct));

        // Play death effect
        if (victim.isOnline()) {
            cause.playDeathEffect(audience, victim.getPlayer().getLocation(), victim);
        }

        // Show kill display to killer
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

    public Component getDeathMessage (CBCPlayer victim, CBCPlayer killer, DeathCause cause, boolean direct) {
        return deathMessageManager.getDeathMessage(victim, killer, cause, direct);
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
