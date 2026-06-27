package neonique.cbcplugin_new.weapons;

import neonique.cbcplugin_new.core.CBCPlayer;
import neonique.cbcplugin_new.core.TeamLike;
import neonique.cbcplugin_new.weapons.presets.CreeperPreset;
import neonique.cbcplugin_new.weapons.presets.FlamePreset;
import neonique.cbcplugin_new.weapons.presets.WeaponPreset;
import neonique.cbcplugin_new.weapons.presets.XbowPreset;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WeaponFactory {

    private CreeperPreset creeperVar;
    private FlamePreset flameVar;
    private XbowPreset xbowVar;

    private Map<String, CreeperPreset> creeperTeamOverrides;
    private Map<String, FlamePreset> flameTeamOverrides;
    private Map<String, XbowPreset> xbowTeamOverrides;

    public List<CrossbowWeapon> getPlayerBaseWeapons (CBCPlayer player) {

        List<CrossbowWeapon> weapons = new ArrayList<>();

        String teamId = player.teamOptional()
                .map(TeamLike::id)
                .orElse(null);
        CreeperPreset playerCreeperVar = creeperTeamOverrides.containsKey(teamId) ? creeperTeamOverrides.get(teamId) : creeperVar;
        FlamePreset playerFlameVar = flameTeamOverrides.containsKey(teamId) ? flameTeamOverrides.get(teamId) : flameVar;
        XbowPreset playerXbowVar = xbowTeamOverrides.containsKey(teamId) ? xbowTeamOverrides.get(teamId) : xbowVar;

        weapons.add(new CreeperCannon(player, playerCreeperVar));
        weapons.add(new FlameZoner(player, playerFlameVar));
        weapons.add(new XBow(player, playerXbowVar));

        return weapons;

    }

    public void resetWeaponPresetsToDefault () {
        creeperVar = CreeperPreset.getDefaultPreset();
        flameVar = FlamePreset.getDefaultPreset();
        xbowVar = XbowPreset.getDefaultPreset();

        creeperTeamOverrides = new HashMap<>();
        flameTeamOverrides = new HashMap<>();
        xbowTeamOverrides = new HashMap<>();
    }

    public void setCreeperVar(CreeperPreset creeperVar) {
        this.creeperVar = creeperVar;
    }

    public void setFlameVar(FlamePreset flameVar) {
        this.flameVar = flameVar;
    }

    public void setXbowVar(XbowPreset xbowVar) {
        this.xbowVar = xbowVar;
    }

    public void addTeamCreeperOverrides(String teamId, CreeperPreset preset) {
        if (creeperTeamOverrides.containsKey(teamId) && preset == creeperVar) {
            creeperTeamOverrides.remove(teamId);
        }
        creeperTeamOverrides.put(teamId, preset);
    }

    public void addTeamFlameOverrides(String teamId, FlamePreset preset) {
        if (flameTeamOverrides.containsKey(teamId) && preset == flameVar) {
            flameTeamOverrides.remove(teamId);
        }
        flameTeamOverrides.put(teamId, preset);
    }

    public void addTeamXbowOverrides(String teamId, XbowPreset preset) {
        if (xbowTeamOverrides.containsKey(teamId) && preset == xbowVar) {
            xbowTeamOverrides.remove(teamId);
        }
        xbowTeamOverrides.put(teamId, preset);
    }

    public WeaponPreset getWeaponVar (WeaponType weaponType) {
        if (weaponType == WeaponType.CREEPER) return creeperVar;
        else if (weaponType == WeaponType.FLAME) return flameVar;
        else if (weaponType == WeaponType.XBOW) return xbowVar;
        else return null;
    }

}
