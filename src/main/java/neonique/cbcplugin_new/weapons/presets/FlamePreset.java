package neonique.cbcplugin_new.weapons.presets;

import org.bukkit.configuration.ConfigurationSection;

import java.util.Objects;

public class FlamePreset extends WeaponPreset {

    private double zoneLife; // Time zone stays alive in seconds
    private double zoneRadius; // Radius of zone in blocks

    private static FlamePreset defaultPreset;

    static {
        defaultPreset = new FlamePreset("DEFAULT");
    }

    public FlamePreset (String presetName) {

        super(presetName);

        // Set defaults
        setReloadTimer(6.2);
        zoneLife = 3.5;
        zoneRadius = 2.5;

    }

    public static FlamePreset newPreset (String presetName, ConfigurationSection section) {

        FlamePreset preset = new FlamePreset(presetName);

        double reloadTimer = section.getDouble("ReloadTimer", defaultPreset.getReloadTimer());
        double zoneLife = section.getDouble("ZoneLife", defaultPreset.getZoneLife());
        double zoneRadius = section.getDouble("ZoneRadius", defaultPreset.getZoneRadius());

        preset.setAll(reloadTimer, zoneLife, zoneRadius);

        // If this is the default preset
        if (Objects.equals(presetName, "DEFAULT")) {
            defaultPreset = preset;
        }

        System.out.println(reloadTimer);

        return preset;

    }

    public void setAll (double reloadTimer, double zoneLife, double zoneRadius) {

        setReloadTimer(reloadTimer);
        this.zoneLife = zoneLife;
        this.zoneRadius = zoneRadius;

    }

    public double getZoneLife() {
        return zoneLife;
    }

    public double getZoneRadius() {
        return zoneRadius;
    }

    public static FlamePreset getDefaultPreset() {
        return defaultPreset;
    }
}
