package neonique.cbcplugin_new.gamemodes.ctf;

import neonique.cbcplugin_new.core.GameSetting;
import neonique.cbcplugin_new.core.GameSettings;

import java.util.Map;

public class CTFSettings implements GameSettings {

    private final GameSetting<Integer> startingFlags;
    private final GameSetting<Integer> firstFlagRemovalTimer;
    private final GameSetting<Integer> nextFlagRemovalTimer;

    public CTFSettings () {

        startingFlags = GameSetting.intSettingWithBounds(
                "startingFlags",
                "Number of flags every team starts the game with",
                4,
                1, 20
        );

        firstFlagRemovalTimer = GameSetting.intSettingWithBounds(
                "firstFlagRemoveTimer",
                "Amount of seconds until all teams automatically lose a flag for the first time",
                900,
                0, Integer.MAX_VALUE
        );

        nextFlagRemovalTimer = GameSetting.intSettingWithBounds(
                "nextFlagRemoveTimer",
                "Amount of seconds until all teams automatically lose a flag after the first time",
                300,
                0, Integer.MAX_VALUE
        );

    }

    @Override
    public Map<String, GameSetting<?>> getAllSettings() {
        return Map.of(
                startingFlags.name(), startingFlags,
                firstFlagRemovalTimer.name(), firstFlagRemovalTimer,
                nextFlagRemovalTimer.name(), nextFlagRemovalTimer
        );
    }

    public Integer startingFlags () {
        return startingFlags.value();
    }

    public Integer firstFlagRemovalTimer () {
        return firstFlagRemovalTimer.value();
    }

    public Integer nextFlagRemovalTimer () {
        return nextFlagRemovalTimer.value();
    }

}
