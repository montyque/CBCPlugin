package neonique.cbcplugin_new.core.tasks;

import neonique.cbcplugin_new.core.CBCPlayer;
import neonique.cbcplugin_new.core.CBCTeam;
import neonique.cbcplugin_new.core.TeamColor;
import neonique.cbcplugin_new.core.CBCGamemode;
import neonique.cbcplugin_new.util.TextUtil;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import net.kyori.adventure.sound.Sound;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.Collection;
import java.util.function.Supplier;

public class BaseStartGameTimer {

    private final TimerTask timer;

    private final Audience audience;
    private final Supplier<Collection<? extends CBCPlayer>> players;
    private final Supplier<Collection<Player>> spectators;
    private final CBCGamemode gamemode;
    private final String mapName;
    private final Runnable gameStarter;

    public BaseStartGameTimer (Audience audience,
                               int length,
                               Supplier<Collection<? extends CBCPlayer>> players,
                               Supplier<Collection<Player>> spectators,
                               CBCGamemode gamemode,
                               String mapName,
                               Supplier<Boolean> runCondition,
                               Runnable gameStarter) {

        this.audience = audience;
        this.players = players;
        this.spectators = spectators;
        this.gamemode = gamemode;
        this.mapName = mapName;
        this.gameStarter = gameStarter;

        this.timer = new TimerTask(length, this::onTick, runCondition, this::onEnd);

    }

    public void start (Plugin plugin) {
        displayTitle(timer.getSecs());
        timer.runTaskTimer(plugin, 20, 20);
    }

    public void onTick (int secs) {
        displayTitle(secs);
        if (secs <= 5) {
            audience.playSound(Sound.sound(org.bukkit.Sound.BLOCK_NOTE_BLOCK_PLING, Sound.Source.MASTER, 100, 1));
        }
    }

    public void onEnd () {
        audience.clearTitle();
        audience.playSound(Sound.sound(org.bukkit.Sound.BLOCK_NOTE_BLOCK_PLING, Sound.Source.MASTER, 100, 2));
        gameStarter.run();
    }

    public void displayTitle (int secs) {

        for (CBCPlayer player : players.get()) {
            player.showTitle(getTitle(secs, player.team()));
        }

        for (Player spectator : spectators.get()) {
            spectator.showTitle(getTitle(secs, null));
        }

    }

    public Title getTitle (int secs, CBCTeam<?> team) {
        return Title.title(
                getTitleComponent(secs, team),
                getSubtitleComponent(secs, team),
                TextUtil.titleTimes(0, 3000, 500)
        );
    }

    public Component getTitleComponent (int secs, CBCTeam<?> team) {
        TextColor textColor = team != null ? team.textColor() : gamemode.getColor();
        TeamColor teamColor = team != null ? team.teamColor() : null;
        String gamemodeIcon = gamemode.getIcon(teamColor);
        return Component.text()
                .content(gamemodeIcon)
                .color(NamedTextColor.WHITE)
                .decoration(TextDecoration.BOLD, TextDecoration.State.FALSE)
                .content(gamemode.getGamemodeName())
                .color(textColor)
                .decoration(TextDecoration.BOLD, TextDecoration.State.TRUE)
                .content(gamemodeIcon)
                .color(NamedTextColor.WHITE)
                .decoration(TextDecoration.BOLD, TextDecoration.State.FALSE)
                .build();
    }

    public Component getSubtitleComponent (int secs, CBCTeam<?> team) {
        TextColor textColor = team != null ? team.textColor() : gamemode.getColor();
        if (secs <= 5) {
            return Component.text()
                    .content("Starting in ")
                    .color(NamedTextColor.WHITE)
                    .decorate(TextDecoration.BOLD)
                    .content(String.valueOf(secs))
                    .color(textColor)
                    .decorate(TextDecoration.BOLD)
                    .build();
        } else {
             return Component.text()
                    .content(mapName)
                    .decorate(TextDecoration.BOLD)
                    .color(NamedTextColor.WHITE)
                    .build();
        }
    }

}
