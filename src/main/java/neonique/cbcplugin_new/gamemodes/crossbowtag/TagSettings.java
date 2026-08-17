package neonique.cbcplugin_new.gamemodes.crossbowtag;

import neonique.cbcplugin_new.core.GameSetting;
import neonique.cbcplugin_new.core.GameSettings;

import java.util.Map;

public class TagSettings implements GameSettings {

    private final GameSetting<Integer> roundLength;
    private final GameSetting<Integer> taggerRoundsPerTeam;

    private final GameSetting<Integer> maxEvaderSurvivalPoints;
    private final GameSetting<Integer> evaderSurvivalBonus;

    private final GameSetting<Integer> maxTaggerKillPoints;
    private final GameSetting<Integer> minTaggerKillPoints;
    private final GameSetting<Integer> maxTaggerWipeoutPoints;
    private final GameSetting<Integer> taggerWipeoutBonus;
    private final GameSetting<Integer> taggerRespawnTimer;

    public TagSettings () {

        roundLength = GameSetting.intSettingWithBounds(
                "roundLength",
                "Length of each round in seconds.",
                120,
                1, Integer.MAX_VALUE
        );

        taggerRoundsPerTeam = GameSetting.intSettingWithBounds(
                "taggerRoundsPerTeam",
                "Amount of times each team gets to play as taggers.",
                1,
                1, 16
        );

        maxEvaderSurvivalPoints = GameSetting.intSettingWithBounds(
                "maxEvaderSurvivalPoints",
                "Maximum points an evader can score per round, with points scaling linearly from 0 to this value " +
                        "upwards every second throughout the round.",
                120,
                0, Integer.MAX_VALUE
        );

        evaderSurvivalBonus = GameSetting.intSettingWithBounds(
                "evaderSurvivalBonus",
                "Bonus points an evader scores for surviving a full round.",
                10,
                0, Integer.MAX_VALUE
        );

        maxTaggerKillPoints = GameSetting.intSettingWithBounds(
                "maxTaggerKillPoints",
                "Maximum amount of points a tagger earns for a kill, with points scaling linearly from this value" +
                        "to minTaggerKillPoints throughout the round.",
                60,
                0, Integer.MAX_VALUE
        );

        minTaggerKillPoints = GameSetting.intSettingWithBounds(
                "minTaggerKillPoints",
                "Maximum amount of points a tagger earns for a kill, with points scaling linearly from " +
                        "maxTaggerKillPoints to this value throughout the round.",
                30,
                0, Integer.MAX_VALUE
        );

        maxTaggerWipeoutPoints = GameSetting.intSettingWithBounds(
                "maxTaggerWipeoutPoints",
                "Maximum amount of points the tagging team earns for killing all evaders before the round ends, " +
                        "scaling linearly from this value to 0 as the round progresses.",
                30,
                0, Integer.MAX_VALUE
        );

        taggerWipeoutBonus = GameSetting.intSettingWithBounds(
                "taggerWipeoutBonus",
                "Bonus points the tagging team earns for killing all evaders before the round ends.",
                30,
                0, Integer.MAX_VALUE
        );

        taggerRespawnTimer = GameSetting.intSettingWithBounds(
                "taggerRespawnTimer",
                "Maximum amount of points the tagging team earns for killing all evaders before the round ends, " +
                        "scaling linearly from this value to 0 as the round progresses.",
                4,
                1, Integer.MAX_VALUE
        );

    }

    @Override
    public Map<String, GameSetting<?>> getAllSettings() {
        return Map.of(
                roundLength.name(), roundLength,
                taggerRoundsPerTeam.name(), taggerRoundsPerTeam,
                maxEvaderSurvivalPoints.name(), maxEvaderSurvivalPoints,
                evaderSurvivalBonus.name(), evaderSurvivalBonus,
                maxTaggerKillPoints.name(), maxTaggerKillPoints,
                minTaggerKillPoints.name(), minTaggerKillPoints,
                maxTaggerWipeoutPoints.name(), maxTaggerWipeoutPoints,
                taggerWipeoutBonus.name(), taggerWipeoutBonus,
                taggerRespawnTimer.name(), taggerRespawnTimer
        );
    }

    public int roundLength () {
        return roundLength.value();
    }

    public int taggerRoundsPerTeam () {
        return taggerRoundsPerTeam.value();
    }

    public int maxEvaderSurvivalPoints () {
        return maxEvaderSurvivalPoints.value();
    }

    public int evaderSurvivalBonus () {
        return evaderSurvivalBonus.value();
    }

    public int maxTaggerKillPoints () {
        return maxTaggerKillPoints.value();
    }

    public int minTaggerKillPoints () {
        return minTaggerKillPoints.value();
    }

    public int maxTaggerWipeoutPoints () {
        return maxTaggerWipeoutPoints.value();
    }

    public int taggerWipeoutBonus () {
        return taggerWipeoutBonus.value();
    }

    public int taggerRespawnTimer () {
        return taggerRespawnTimer.value();
    }

}
