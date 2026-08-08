package neonique.cbcplugin_new.combat.weapons.presets;

import neonique.cbcplugin_new.util.ConfigUtil;
import org.bukkit.configuration.ConfigurationSection;

public record CreeperCannonSettings (String name,
                                   double reloadLength,
                                   double launchVelocityModifier,
                                   double damageModifier,
                                   double allyDamageModifier,
                                   int explosionRadius,
                                   double verticalKnockbackCoefficient,
                                   double horizontalKnockbackCoefficient) implements WeaponSettings {

    public static CreeperCannonSettings DEFAULT = new CreeperCannonSettings(
            "DEFAULT",
            4.0,
            1.15,
            1.0,
            0.2,
            3,
            1.25,
            0.42
    );

    public static CreeperCannonSettings fromConfig (String presetName, ConfigurationSection config) {

        return new CreeperCannonSettings(
                presetName,
                ConfigUtil.getDouble(config, "reload_length").orElse(DEFAULT.reloadLength()),
                ConfigUtil.getDouble(config, "launch_velocity_modifier").orElse(DEFAULT.launchVelocityModifier()),
                ConfigUtil.getDouble(config, "damage_modifier").orElse(DEFAULT.damageModifier()),
                ConfigUtil.getDouble(config, "ally_damage_modifier").orElse(DEFAULT.allyDamageModifier()),
                ConfigUtil.getInt(config, "explosion_radius").orElse(DEFAULT.explosionRadius()),
                ConfigUtil.getDouble(config, "vertical_kb").orElse(DEFAULT.verticalKnockbackCoefficient()),
                ConfigUtil.getDouble(config, "horizontal_kb").orElse(DEFAULT.horizontalKnockbackCoefficient())
        );

    }

}
