package neonique.cbcplugin_new.gamemodes.showdown.tasks;

import neonique.cbcplugin_new.core.tasks.TimerTask;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class ShowdownNextRoundTimer extends TimerTask {

    private final Audience audience;

    public ShowdownNextRoundTimer(Audience audience,
                                  int length,
                                  Supplier<Boolean> condition,
                                  Runnable onEnd) {
        super(length, (i) -> {}, condition, onEnd);
        this.audience = audience;
    }

    private void sendTimerWarning (int seconds) {
        audience.sendMessage(Component.text()
                .content("Next round starting in ")
                .color(NamedTextColor.GREEN)
                .content(String.valueOf(seconds))
                .color(NamedTextColor.YELLOW)
        );
    }

    @Override
    public Map<Integer, Consumer<Integer>> timerEventsMap () {
        return Map.of(
                5, this::sendTimerWarning,
                4, this::sendTimerWarning,
                3, this::sendTimerWarning,
                2, this::sendTimerWarning,
                1, this::sendTimerWarning
        );
    }

}
