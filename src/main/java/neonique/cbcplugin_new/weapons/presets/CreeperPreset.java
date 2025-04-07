package neonique.cbcplugin_new.weapons.presets;

import org.bukkit.configuration.ConfigurationSection;

import java.util.Objects;

public class CreeperPreset extends WeaponPreset {

    private double launchVelocityModifier; // Velocity multiplier -- default 1.15
    private double creeperAllyDamageRatio; // Amount of damage it does to allies compared to amount of damage it does to enemies -- default 0.2
    private int creeperExplosionRadius; // Explosion radius of creeper -- default 3

    private double verticalKnockbackCoefficient;
    private double horizontalKnockbackCoefficient;

    private static CreeperPreset defaultPreset;

    static {
        defaultPreset = new CreeperPreset("DEFAULT");
    }

    public CreeperPreset (String presetName) {

        super(presetName);

        // Set defaults
        setReloadTimer(4.0);
        launchVelocityModifier = 1.15;
        creeperAllyDamageRatio = 0.2;
        creeperExplosionRadius = 3;

        horizontalKnockbackCoefficient = 1.25;
        verticalKnockbackCoefficient = 0.42;

    }

    public static CreeperPreset newPreset (String presetName, ConfigurationSection section) {

        CreeperPreset preset = new CreeperPreset(presetName);

        double reloadTimer = section.getDouble("ReloadTimer", defaultPreset.getReloadTimer());
        double velocityModifier = section.getDouble("VelocityModifier", defaultPreset.getLaunchVelocityModifier());
        double creeperAllyDamageRatio = section.getDouble("AllyDamageRatio", defaultPreset.getCreeperAllyDamageRatio());
        int creeperExplosionRadius = section.getInt("CreeperExplosionRadius", defaultPreset.getCreeperExplosionRadius());

        double horizontalKnockbackCoefficient = section.getDouble("HorizontalKnockbackCoefficient", defaultPreset.getHorizontalKnockbackCoefficient());
        double verticalKnockbackCoefficient = section.getDouble("VerticalKnockbackCoefficient", defaultPreset.getVerticalKnockbackCoefficient());

        preset.setAll(reloadTimer, velocityModifier, creeperAllyDamageRatio, creeperExplosionRadius, horizontalKnockbackCoefficient,
                verticalKnockbackCoefficient);

        // If this is the default preset
        if (Objects.equals(presetName, "DEFAULT")) {
            defaultPreset = preset;
        }

        System.out.println(reloadTimer);

        return preset;

    }

    public void setAll (double reloadTimer, double velocityModifier, double creeperAllyDamageRatio,
                        int creeperExplosionRadius, double horizontalKnockbackCoefficient, double verticalKnockbackCoefficient) {

        setReloadTimer(reloadTimer);
        this.launchVelocityModifier = velocityModifier;
        this.creeperAllyDamageRatio = creeperAllyDamageRatio;
        this.creeperExplosionRadius = creeperExplosionRadius;

        this.horizontalKnockbackCoefficient = horizontalKnockbackCoefficient;
        this.verticalKnockbackCoefficient = verticalKnockbackCoefficient;

    }

    public double getLaunchVelocityModifier() {
        return launchVelocityModifier;
    }

    public double getCreeperAllyDamageRatio() {
        return creeperAllyDamageRatio;
    }

    public int getCreeperExplosionRadius() {
        return creeperExplosionRadius;
    }

    public double getVerticalKnockbackCoefficient() {
        return verticalKnockbackCoefficient;
    }

    public double getHorizontalKnockbackCoefficient() {
        return horizontalKnockbackCoefficient;
    }

    public static CreeperPreset getDefaultPreset() {
        return defaultPreset;
    }
}
