package neonique.cbcplugin_new.gamemodes.ctf;

import neonique.cbcplugin_new.core.TeamColor;
import neonique.cbcplugin_new.mapconfig.CBCMap;
import neonique.cbcplugin_new.mapconfig.TeamMapData;
import neonique.cbcplugin_new.mapconfig.TeamRequirements;
import neonique.cbcplugin_new.mechanics.DeathBorder;
import neonique.cbcplugin_new.util.ConfigUtil;
import neonique.cbcplugin_new.util.VectorUtil;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public record CTFMapData (CBCMap map,
                          TeamRequirements teamRequirements,
                          Map<TeamColor, CTFBaseInfo> teamBaseInfo,
                          double defensiveKillRadius,
                          List<Integer> respawnTimers,
                          DeathBorder.DeathBorderOptions deathBorderInfo) implements TeamMapData {


    public static CTFMapData fromConfig (CBCMap map, Configuration gamemodeConfig) {

        TeamRequirements req = TeamRequirements.fromConfig(gamemodeConfig);
        Map<TeamColor, CTFBaseInfo> baseInfo = parseTeamBases(gamemodeConfig, req);

        // Parse defensive kill radius
        double defensiveKillRadius = ConfigUtil.requireDouble(gamemodeConfig, "defensive_kill_radius");

        // Parse respawn timers - default value if not provided is 4
        List<Integer> respawnTimers = ConfigUtil.getList(gamemodeConfig, "respawn_timer")
                .map(l -> l.stream()
                        .map(o -> (int) o)
                        .toList())
                .orElse(List.of(4));

        // Parse sudden death border
        DeathBorder.DeathBorderOptions deathBorderInfo = ConfigUtil.getConfigurationSection(gamemodeConfig, "sudden_death_border")
                .map(DeathBorder.DeathBorderOptions::fromConfig)
                .orElse(null);

        return new CTFMapData(map, req, baseInfo, defensiveKillRadius, respawnTimers, deathBorderInfo);


    }

    private static Map<TeamColor, CTFBaseInfo> parseTeamBases (ConfigurationSection config, TeamRequirements req) {

        Map<TeamColor, CTFBaseInfo> teamBases = new HashMap<>();
        ConfigurationSection baseConfigSection = ConfigUtil.requireConfigurationSection(config, "bases");
        Map<String, ConfigurationSection> baseConfigs = ConfigUtil.getAllConfigSections(baseConfigSection);
        for (var baseConfig : baseConfigs.entrySet()) {

            // TODO: throw error if color not present
            TeamColor baseTeamColor = TeamColor.valueOf(baseConfig.getKey());
            if (!req.validTeamColors().contains(baseTeamColor)) {
                // TODO: throw error as color is not valid
                continue;
            }
            CTFBaseInfo base = parseBase(baseConfig.getValue());
            teamBases.put(baseTeamColor, base);

        }

        if (req.validTeamColors().size() != teamBases.size()) {
            // TODO: throw error as not all valid colors are included
        }

        return Map.copyOf(teamBases);

    }

    private static CTFBaseInfo parseBase (ConfigurationSection config) {
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

}
