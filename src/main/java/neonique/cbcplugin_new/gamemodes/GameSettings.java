package neonique.cbcplugin_new.gamemodes;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface GameSettings {

    Map<String, GameSetting<?>> getAllSettings ();

    default Set<String> getAllSettingNames () {
        return getAllSettings().keySet();
    }

    default void setSetting (String name, String strValue) {
        GameSetting<?> setting = getSetting(name);
        try {
            setting.parseAndSetValue(strValue);
        } catch (IllegalArgumentException e) {
            throw new InvalidSettingValueException(setting.name(), strValue, e);
        }
    }

    default GameSetting<?> getSetting (String name) {
        GameSetting<?> setting = getAllSettings().get(name);
        if (setting == null) throw new IllegalArgumentException("No setting '" + name + "' found");
        return setting;
    }

    default List<String> getSettingTabCompletions (String name) {
        GameSetting<?> setting = getAllSettings().get(name);
        return setting != null ? setting.tabCompletions() : List.of();
    }

    static GameSettings blank () {
        return new GameSettings() {
            @Override
            public Map<String, GameSetting<?>> getAllSettings () {
                return Map.of();
            }
        };
    }


}
