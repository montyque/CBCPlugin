package neonique.cbcplugin_new.lobby.tasks;

import neonique.cbcplugin_new.core.tasks.TimerTask;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class StartCountdownTask extends TimerTask {

    private final Audience audience;

    public static final Map<Integer, TextColor> colors = Map.of(
            1, TextColor.color(255, 81, 81),
            2, TextColor.color(255, 95, 72),
            3, TextColor.color(255, 110, 63),
            4, TextColor.color(255, 125, 53),
            5, TextColor.color(255, 139, 44),
            6, TextColor.color(255, 159, 41),
            7, TextColor.color(253, 182, 46),
            8, TextColor.color(252, 207, 51),
            9, TextColor.color(250, 230, 56),
            10, TextColor.color(248, 255, 62)
    );

    private final Map<Integer, Consumer<Integer>> eventsMap = IntStream.range(1, 10).boxed()
            .collect(Collectors.toMap(
                    i -> i,
                    i -> (t -> countdownDisplay(t, colors.get(t)))
            ));

    public StartCountdownTask(Audience audience, Runnable onEnd) {
        super(15, _ -> {}, () -> true, onEnd);
        this.audience = audience;
    }

    @Override
    public Map<Integer, Consumer<Integer>> timerEventsMap () {
        return eventsMap;
    }

    public void countdownDisplay (int time, TextColor color) {
        audience.showTitle(Title.title(
                Component.text(time).color(color).decorate(TextDecoration.BOLD),
                Component.empty(),
                Title.Times.times(
                        Duration.ofMillis(0),
                        Duration.ofMillis(1250),
                        Duration.ofMillis(250))
        ));
        audience.sendMessage(Component.text()
                .content("Game starts in ")
                .content(time + " second" + (time == 1 ? "!" : "s!"))
                .color(color)
        );
        audience.playSound(Sound.sound(org.bukkit.Sound.BLOCK_NOTE_BLOCK_PLING, Sound.Source.MASTER, 100, 1));
    }

    public void cancelCountdown (CountdownCancelReason reason, Player cause) {
        this.cancel();

        audience.sendMessage(switch (reason) {
            case COMMAND -> Component.text("Countdown cancelled by " + cause.name() + ".")
                    .color(NamedTextColor.YELLOW);
            case DISCONNECT -> Component.text("Countdown cancelled by " + cause.name() + "'s disconnection from the server.")
                    .color(NamedTextColor.YELLOW);
        });
        audience.playSound(Sound.sound(org.bukkit.Sound.UI_BUTTON_CLICK, Sound.Source.MASTER, 100, 1));
    }

    public enum CountdownCancelReason {
        COMMAND, DISCONNECT
    }

}
