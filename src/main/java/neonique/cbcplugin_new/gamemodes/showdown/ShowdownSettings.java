package neonique.cbcplugin_new.gamemodes.showdown;

import neonique.cbcplugin_new.gamemodes.GameSetting;
import neonique.cbcplugin_new.gamemodes.GameSettings;

import java.util.Map;

public class ShowdownSettings implements GameSettings {

    private final GameSetting<Integer> roundsToWin;
    private final GameSetting<Boolean> glowingPlayers;

    public ShowdownSettings () {

        roundsToWin = GameSetting.intSettingWithBounds(
                "roundsToWin",
                "Amount of rounds a team must win to win the game.",
                4,
                1, 50
        );

        glowingPlayers = GameSetting.booleanSetting(
                "glowingPlayers",
                "Whether players should be permanently glowing during rounds.",
                true
        );

    }

    @Override
    public Map<String, GameSetting<?>> getAllSettings() {
        return Map.of(
                roundsToWin.name(), roundsToWin,
                glowingPlayers.name(), glowingPlayers
        );
    }

    public int roundsToWin () {
        return roundsToWin.value();
    }

    public boolean glowingPlayers () {
        return glowingPlayers.value();
    }

}
