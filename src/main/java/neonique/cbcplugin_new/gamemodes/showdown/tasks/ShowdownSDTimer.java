package neonique.cbcplugin_new.gamemodes.showdown.tasks;

import neonique.cbcplugin_new.core.tasks.TimerTask;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class ShowdownSDTimer extends TimerTask {

    private final Audience audience;
    private final Map<Integer, Consumer<Integer>> timerEventsMap = Map.of(
        30, i -> sendTimerWarning(NamedTextColor.YELLOW, i),
        10, i -> sendTimerWarning(NamedTextColor.GOLD, i),
        3, i -> sendTimerWarning(NamedTextColor.RED, i),
        2, i -> sendTimerWarning(NamedTextColor.RED, i),
        1, i -> sendTimerWarning(NamedTextColor.RED, i)
    );

    public ShowdownSDTimer(Audience audience,
                           int length,
                           Consumer<Integer> onTick,
                           Supplier<Boolean> condition,
                           Runnable onEnd) {
        super(length, onTick, condition, onEnd);
        this.audience = audience;
    }

    @Override
    public Map<Integer, Consumer<Integer>> timerEventsMap () {
        return timerEventsMap;
    }

    private void sendTimerWarning (NamedTextColor color, int seconds) {
        audience.sendMessage(Component.text()
            .content("Sudden Death")
            .color(NamedTextColor.RED)
            .content(" begins in ")
            .color(NamedTextColor.WHITE)
            .content(seconds + (seconds == 1 ? " second!" : " seconds!"))
            .color(color));
        audience.playSound(Sound.sound()
            .source(Sound.Source.MASTER)
            .type(org.bukkit.Sound.UI_BUTTON_CLICK)
            .volume(200)
            .build()
        );
    }

}
