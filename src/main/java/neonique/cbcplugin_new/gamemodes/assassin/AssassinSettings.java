package neonique.cbcplugin_new.gamemodes.assassin;

import neonique.cbcplugin_new.gamemodes.GameSetting;
import neonique.cbcplugin_new.gamemodes.GameSettings;

import java.util.Map;

public class AssassinSettings implements GameSettings {

    private final GameSetting<Integer> targetsToKill;
    private final GameSetting<Integer> targetChangeTimer;

    public AssassinSettings () {

        targetsToKill = GameSetting.intSettingWithBounds(
                "targetsToKill",
                "Amount of targets a player must kill to win the game",
                16,
                1, Integer.MAX_VALUE
        );

        targetChangeTimer = GameSetting.intSettingWithBounds(
                "targetChangeTimer",
                "Maximum amount of time a player can have a target before getting a new one",
                60,
                1, Integer.MAX_VALUE
        );

    }

    @Override
    public Map<String, GameSetting<?>> getAllSettings() {
        return Map.of(
                targetsToKill.name(), targetsToKill,
                targetChangeTimer.name(), targetChangeTimer
        );
    }

    public int targetsToKill () {
        return targetsToKill.value();
    }

    public int targetChangeTimer () {
        return targetChangeTimer.value();
    }

}
