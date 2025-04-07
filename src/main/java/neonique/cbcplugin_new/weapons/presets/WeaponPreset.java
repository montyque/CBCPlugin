package neonique.cbcplugin_new.weapons.presets;

public class WeaponPreset {

    private final String presetName;

    private double reloadTimer; // Time it takes for weapon to reload in seconds -- default 4s

    public WeaponPreset (String presetName) {

        // Set name of preset
        this.presetName = presetName;

    }

    public String getPresetName() {
        return presetName;
    }

    public void setReloadTimer(double reloadTimer) {
        this.reloadTimer = reloadTimer;
    }

    public double getReloadTimer() {
        return reloadTimer;
    }

    public int getReloadTicks() {
        return (int) Math.round(getReloadTimer() * 10);
    }


}
