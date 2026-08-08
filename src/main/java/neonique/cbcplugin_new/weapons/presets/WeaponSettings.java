package neonique.cbcplugin_new.weapons.presets;

public interface WeaponSettings {

    String name();

    double reloadLength();

    default int reloadTicks() {
        return (int) Math.round(reloadLength() * 20);
    }

}
