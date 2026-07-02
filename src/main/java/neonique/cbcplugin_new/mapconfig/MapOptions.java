package neonique.cbcplugin_new.mapconfig;

import neonique.cbcplugin_new.util.ConfigUtil;
import org.bukkit.configuration.ConfigurationSection;

public record MapOptions (int dayTime,
                   boolean nightVisionEnabled) {

    public static MapOptions DEFAULTS = new MapOptions(
            -1,
            true
    );

    public static MapOptions fromConfig (ConfigurationSection overrideConfig) {
        return fromOverrideConfig(overrideConfig, DEFAULTS);
    }

    public static MapOptions fromOverrideConfig (ConfigurationSection overrideConfig, MapOptions old) {
        if (overrideConfig == null) return old;
        try {
            return new MapOptions(overrideConfig, old);
        } catch (Exception e) {
            throw new IllegalArgumentException("Failure to parse map_options section", e);
        }
    }

    private MapOptions (ConfigurationSection overrideConfig, MapOptions oldOptions) {
        this(
                ConfigUtil.getInt(overrideConfig, "day_time").orElse(oldOptions.dayTime),
                ConfigUtil.getBoolean(overrideConfig, "night_vision_enabled").orElse(oldOptions.nightVisionEnabled)
        );
    }

}