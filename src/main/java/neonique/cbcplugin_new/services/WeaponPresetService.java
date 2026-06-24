package neonique.cbcplugin_new.services;

import neonique.cbcplugin_new.CBCPlugin;
import neonique.cbcplugin_new.weapons.WeaponType;
import neonique.cbcplugin_new.mechanics.OverallPreset;
import neonique.cbcplugin_new.weapons.presets.CreeperPreset;
import neonique.cbcplugin_new.weapons.presets.FlamePreset;
import neonique.cbcplugin_new.weapons.presets.XbowPreset;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WeaponPresetService {

    private Map<String, CreeperPreset> creeperPresets;
    private Map<String, FlamePreset> flamePresets;
    private Map<String, XbowPreset> xbowPresets;
    private Map<String, OverallPreset> overallPresets;

    public void loadWeaponPresets () {

        creeperPresets = new HashMap<>();
        flamePresets = new HashMap<>();
        xbowPresets = new HashMap<>();

        // Attempt to find weapons folder
        File weaponsFolderFile = new File(CBCPlugin.getPlugin().getDataFolder(), "weapons");
        // Attempt to make this a directory
        if (!weaponsFolderFile.exists()) {
            boolean folderMade = weaponsFolderFile.mkdir();
            if (!folderMade) {
                return;
            }
        }

        // Get config file
        File file = new File(weaponsFolderFile, "weaponpresets.yml");
        if (!file.exists()) return;

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);

        // Creeper presets
        ConfigurationSection creeperSection = config.getConfigurationSection("CreeperPresets");
        if (creeperSection != null) {
            System.out.println("Creeper preset section found");
            for (String key : creeperSection.getKeys(false)) {
                ConfigurationSection creeperPresetSection = creeperSection.getConfigurationSection(key);
                if (creeperPresetSection != null) {
                    CreeperPreset preset = CreeperPreset.newPreset(key.toUpperCase(), creeperPresetSection);
                    creeperPresets.put(key, preset);
                }
            }
        }

        // Flame presets
        ConfigurationSection flameSection = config.getConfigurationSection("FlamePresets");
        if (flameSection != null) {
            System.out.println("Flame preset section found");
            for (String key : flameSection.getKeys(false)) {
                ConfigurationSection flamePresetSection = flameSection.getConfigurationSection(key);
                if (flamePresetSection != null) {
                    FlamePreset preset = FlamePreset.newPreset(key.toUpperCase(), flamePresetSection);
                    flamePresets.put(key, preset);
                }
            }
        }

        // Xbow presets
        ConfigurationSection xbowSection = config.getConfigurationSection("XbowPresets");
        if (xbowSection != null) {
            System.out.println("Flame preset section found");
            for (String key : xbowSection.getKeys(false)) {
                ConfigurationSection xbowPresetSection = xbowSection.getConfigurationSection(key);
                if (xbowPresetSection != null) {
                    XbowPreset preset = XbowPreset.newPreset(key.toUpperCase(), xbowPresetSection);
                    xbowPresets.put(key, preset);
                }
            }
        }

        // Put overall presets
        overallPresets = new HashMap<>();

        ConfigurationSection overallSection = config.getConfigurationSection("OverallPresets");
        if (overallSection != null) {
            System.out.println("Overall preset section found");
            for (String key : overallSection.getKeys(false)) {
                ConfigurationSection overallPresetSection = overallSection.getConfigurationSection(key);
                if (overallPresetSection != null) {

                    String creeperPreset = overallPresetSection.getString("Creeper");
                    String flamePreset = overallPresetSection.getString("Flame");
                    String xbowPreset = overallPresetSection.getString("Xbow");

                    if (!creeperPresets.containsKey(creeperPreset)) {
                        continue;
                    }
                    if (!flamePresets.containsKey(flamePreset)) {
                        continue;
                    }
                    if (!xbowPresets.containsKey(xbowPreset)) {
                        continue;
                    }

                    // Add overall preset
                    OverallPreset overallPreset = new OverallPreset(key, creeperPresets.get(creeperPreset), flamePresets.get(flamePreset),
                            xbowPresets.get(xbowPreset));

                    overallPresets.put(key, overallPreset);

                }
            }
        }
    }

    public CreeperPreset getCreeperPresetById (String presetId) {
        return creeperPresets.get(presetId);
    }

    public FlamePreset getFlamePresetById (String presetId) {
        return flamePresets.get(presetId);
    }

    public XbowPreset getXbowPresetById (String presetId) {
        return xbowPresets.get(presetId);
    }

    public OverallPreset getOverallPreset (String presetId) {
        return overallPresets.get(presetId);
    }

    public List<String> getPresetIds (WeaponType weaponType) {
        if (weaponType == WeaponType.CREEPER) {
            return new ArrayList<>(creeperPresets.keySet());
        }
        else if (weaponType == WeaponType.FLAME) {
            return new ArrayList<>(flamePresets.keySet());
        }
        else if (weaponType == WeaponType.XBOW) {
            return new ArrayList<>(xbowPresets.keySet());
        }
        return null;
    }

    public List<String> getOverallPresetIds () {
        return new ArrayList<>(overallPresets.keySet());
    }

}
