package neonique.cbcplugin_new.gamemodes.showdown.tasks;

import neonique.cbcplugin_new.core.CBCPlayer;
import neonique.cbcplugin_new.core.CBCTeam;
import neonique.cbcplugin_new.core.CBCGamemode;
import neonique.cbcplugin_new.core.tasks.BaseStartGameTimer;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.function.Supplier;

public class ShowdownStartRoundTimer extends BaseStartGameTimer {

    private final int round;

    public ShowdownStartRoundTimer(Audience audience,
                                   Supplier<Collection<? extends CBCPlayer>> players,
                                   Supplier<Collection<Player>> spectators,
                                   String mapName,
                                   Supplier<Boolean> runCondition,
                                   Runnable gameStarter,
                                   int round) {
        super(audience, round == 1 ? 10 : 5, players, spectators, CBCGamemode.SHOWDOWN, mapName, runCondition, gameStarter);
        this.round = round;
    }

    public Component getTitleComponent (int secs, CBCTeam<?> team) {
        TextColor textColor = team != null ? team.textColor() : CBCGamemode.SHOWDOWN.getColor();
        if (round == 1 && secs <= 5) {
            return super.getTitleComponent(secs, team);
        } else {
            return Component.text()
                    .content("ROUND " + round)
                    .color(textColor)
                    .build();
        }
    }

    public Component getSubtitleComponent (int secs, CBCTeam<?> team) {
        TextColor textColor = team != null ? team.textColor() : CBCGamemode.SHOWDOWN.getColor();
        if (round == 1) {
            return super.getSubtitleComponent(secs, team);
        } else {
            return Component.text()
                    .content("Starting in ")
                    .color(NamedTextColor.WHITE)
                    .decorate(TextDecoration.BOLD)
                    .content(String.valueOf(secs))
                    .color(textColor)
                    .decorate(TextDecoration.BOLD)
                    .build();
        }
    }

}