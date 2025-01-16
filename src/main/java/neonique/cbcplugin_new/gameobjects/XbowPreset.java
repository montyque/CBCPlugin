package neonique.cbcplugin_new.gameobjects;

import org.bukkit.configuration.ConfigurationSection;

import java.util.Objects;

public class XbowPreset extends WeaponPreset {

    private double arrowVelocityModifier; // Arrow velocity multiplier -- default 1.15

    private static XbowPreset defaultPreset;

    static {
        defaultPreset = new XbowPreset("DEFAULT");
    }

    public XbowPreset (String presetName) {

        super(presetName);

        // Set defaults
        setReloadTimer(6.0);
        arrowVelocityModifier = 1.00;

    }

    public static XbowPreset newPreset (String presetName, ConfigurationSection section) {

        XbowPreset preset = new XbowPreset(presetName);

        double reloadTimer = section.getDouble("ReloadTimer", defaultPreset.getReloadTimer());
        double arrowVelocityModifier = section.getDouble("ArrowVelocityModifier", defaultPreset.getArrowVelocityModifier());

        preset.setAll(reloadTimer, arrowVelocityModifier);

        // If this is the default preset
        if (Objects.equals(presetName, "DEFAULT")) {
            defaultPreset = preset;
        }

        System.out.println(reloadTimer);

        return preset;

    }

    public void setAll (double reloadTimer, double arrowVelocityModifier) {

        setReloadTimer(reloadTimer);
        this.arrowVelocityModifier = arrowVelocityModifier;

    }

    public double getArrowVelocityModifier() {
        return arrowVelocityModifier;
    }

    public static XbowPreset getDefaultPreset() {
        return defaultPreset;
    }
}