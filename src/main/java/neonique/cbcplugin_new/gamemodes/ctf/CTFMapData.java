package neonique.cbcplugin_new.gamemodes.ctf;

import neonique.cbcplugin_new.core.TeamColor;
import neonique.cbcplugin_new.mapconfig.CBCMap;
import neonique.cbcplugin_new.mapconfig.GamemodeMapData;
import neonique.cbcplugin_new.mapconfig.TeamMapData;
import neonique.cbcplugin_new.mechanics.DeathBorder;
import neonique.cbcplugin_new.util.ConfigUtil;
import neonique.cbcplugin_new.util.VectorUtil;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class CTFMapData implements TeamMapData {

    private final CBCMap map;

    private final int minTeams;
    private final int maxTeams;
    private final Set<TeamColor> validTeamColors;

    private final Map<TeamColor, CTFBaseInfo> teamBaseInfo;
    private final double defensiveKillRadius;
    private final List<Integer> respawnTimers;
    private final DeathBorder.DeathBorderOptions deathBorderInfo;

    public CTFMapData (CBCMap map, Configuration gamemodeConfig) {

        this.map = map;

        minTeams = ConfigUtil.requireInt(gamemodeConfig, "min_teams");
        maxTeams = ConfigUtil.requireInt(gamemodeConfig, "max_teams");
        validTeamColors = ConfigUtil.requireStringList(gamemodeConfig, "valid_team_colors").stream()
                .map(s -> TeamColor.valueOf(s.toUpperCase()))
                .collect(Collectors.toUnmodifiableSet());


        // TODO: validate minTeams <= maxTeams
        ConfigurationSection baseConfigSection = ConfigUtil.requireConfigurationSection(gamemodeConfig, "bases");
        Map<String, ConfigurationSection> baseConfigs = ConfigUtil.getAllConfigSections(baseConfigSection);
        teamBaseInfo = new HashMap<>();
        for (var baseConfig : baseConfigs.entrySet()) {

            // TODO: throw error if color not present
            TeamColor baseTeamColor = TeamColor.valueOf(baseConfig.getKey());
            if (!validTeamColors.contains(baseTeamColor)) {
                // TODO: throw error as color is not valid
                continue;
            }
            CTFBaseInfo base = parseBase(baseConfig.getValue());
            teamBaseInfo.put(baseTeamColor, base);

        }

        if (validTeamColors.size() != teamBaseInfo.size()) {
            // TODO: throw error as not all valid colors are included
        }

        // Parse defensive kill radius
        defensiveKillRadius = ConfigUtil.requireDouble(gamemodeConfig, "defensive_kill_radius");

        // Parse respawn timers - default value if not provided is 4
        respawnTimers = ConfigUtil.getList(gamemodeConfig, "respawn_timer")
                .map(l -> l.stream()
                        .map(o -> (int) o)
                        .toList())
                .orElse(List.of(4));

        // Parse sudden death border
        deathBorderInfo = ConfigUtil.getConfigurationSection(gamemodeConfig, "sudden_death_border")
                .map(DeathBorder.DeathBorderOptions::fromConfig)
                .orElse(null);

    }

    public CTFBaseInfo parseBase (ConfigurationSection config) {
        return new CTFBaseInfo(
                ConfigUtil.requireVector(config, "flag").add(VectorUtil.BLOCK_CENTER_OFFSET),
                ConfigUtil.requireVectorList(config, "spawns").stream()
                        .map(v -> v.add(VectorUtil.BLOCK_CENTER_OFFSET))
                        .toList()
        );
    }

    public record CTFBaseInfo (Vector flagLocation, List<Vector> spawns) {}

    public CTFBase getBase (TeamColor color) {
        CTFBaseInfo info = teamBaseInfo.get(color);
        return new CTFBase(
                VectorUtil.vecToLocation(info.flagLocation, map.getWorld()),
                info.spawns.stream()
                        .map(v -> VectorUtil.vecToLocation(v, map.getWorld()))
                        .toList()
        );
    }

    public double defensiveKillRadius () {
        return defensiveKillRadius;
    }

    public List<Integer> respawnTimers () {
        return respawnTimers;
    }

    public DeathBorder.DeathBorderOptions deathBorderInfo () {
        return deathBorderInfo;
    }

    @Override
    public CBCMap getMap () {
        return map;
    }

    @Override
    public int minTeams () {
        return minTeams;
    }

    @Override
    public int maxTeams () {
        return maxTeams;
    }

    @Override
    public Set<TeamColor> validTeamColors () {
        return validTeamColors;
    }

}
