package neonique.cbcplugin_new.combat.weapons.presets;

import neonique.cbcplugin_new.util.ConfigUtil;
import org.bukkit.configuration.ConfigurationSection;

public record FlameZonerSettings (String name,
                                 double reloadLength,
                                 double zoneLife,
                                 double zoneRadius) implements WeaponSettings {

    public static FlameZonerSettings DEFAULT = new FlameZonerSettings(
            "DEFAULT",
            6.25,
            3.5,
            2.5
    );

    public static FlameZonerSettings fromConfig (String presetName, ConfigurationSection config) {

        return new FlameZonerSettings(
                presetName,
                ConfigUtil.getDouble(config, "reload_length").orElse(DEFAULT.reloadLength()),
                ConfigUtil.getDouble(config, "zone_life").orElse(DEFAULT.zoneLife()),
                ConfigUtil.getDouble(config, "zone_radius").orElse(DEFAULT.zoneRadius())
        );

    }

    public int zoneLifeTicks() {
        return (int) Math.round(zoneLife() * 20);
    }

}
