package neonique.cbcplugin_new.gameobjects;

public class OverallPreset {

    private final String id;

    private final CreeperPreset creeperPreset;
    private final FlamePreset flamePreset;
    private final XbowPreset xbowPreset;

    public OverallPreset (String id, CreeperPreset creeperPreset, FlamePreset flamePreset, XbowPreset xbowPreset) {
        this.id = id;
        this.creeperPreset = creeperPreset;
        this.flamePreset = flamePreset;
        this.xbowPreset = xbowPreset;
    }

    public String getId() {
        return id;
    }

    public CreeperPreset getCreeperPreset() {
        return creeperPreset;
    }

    public FlamePreset getFlamePreset() {
        return flamePreset;
    }

    public XbowPreset getXbowPreset() {
        return xbowPreset;
    }
}
