package neonique.cbcplugin_new.weapons.presets;

import neonique.cbcplugin_new.util.ConfigUtil;
import org.bukkit.configuration.ConfigurationSection;

public record XbowSettings (String name,
                            double reloadLength,
                            double arrowVelocityModifier) implements WeaponSettings {

    public static XbowSettings DEFAULT = new XbowSettings(
            "DEFAULT",
            6.00,
            1.00
    );

    public static XbowSettings fromConfig (String presetName, ConfigurationSection config) {

        return new XbowSettings(
                presetName,
                ConfigUtil.getDouble(config, "reload_length").orElse(DEFAULT.reloadLength()),
                ConfigUtil.getDouble(config, "zone_life").orElse(DEFAULT.arrowVelocityModifier())
        );

    }

}
