package neonique.cbcplugin_new.mapconfig.spawns;

import neonique.cbcplugin_new.core.TeamColor;
import neonique.cbcplugin_new.util.ConfigUtil;
import org.bukkit.configuration.ConfigurationSection;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AssignedSpawnList implements TeamSpawnList {

    private final Map<TeamColor, List<StartSpawnConfig>> teamSpawns;

    private AssignedSpawnList (Map<TeamColor, List<StartSpawnConfig>> teamSpawns) {
        this.teamSpawns = teamSpawns;
    }

    public static TeamSpawnList fromConfig (ConfigurationSection config,
                                            List<TeamColor> requiredColors) {

        StartSpawnType spawnType = ConfigUtil.requireEnum(config, "spawn_type", StartSpawnType.class);
        ConfigurationSection spawnConfig = ConfigUtil.requireConfigurationSection(config, "locations");

        // Must ensure all colors in requiredColors are included in config section
        // Must ensure no invalid colors in requiredColors are included in config section
        Map<String, Object> teamConfigs = spawnConfig.getValues(true);
        Map<TeamColor, List<StartSpawnConfig>> spawns = new HashMap<>();

        for (var teamConfig : teamConfigs.entrySet()) {
            TeamColor teamColor = TeamColor.valueOf(teamConfig.getKey().toUpperCase());
            if (!requiredColors.contains(teamColor)) {
                throw new IllegalArgumentException(teamColor.color() + " is not an allowed color");
            }

            List<StartSpawnConfig> spawnConfigs = TeamSpawnList.parseLocations(teamConfig.getValue()).stream()
                    .map(t -> spawnType.fromConfig(config, t))
                    .toList();
            spawns.put(teamColor, spawnConfigs);

        }

        // TODO: highlight missing color values
        if (requiredColors.size() != spawns.size()) {
            throw new IllegalArgumentException("Missing required color");
        }

        return new AssignedSpawnList(spawns);

    }

    @Override
    public Map<TeamColor, List<StartSpawnConfig>> getTeamSpawns(List<TeamColor> colors) {

        // Throw exception if a color passed in is not valid
        colors.stream()
                .filter(c -> !teamSpawns.containsKey(c))
                .forEach(c -> {
                    throw new IllegalArgumentException("Team color " + c + " does not have spawns assigned to it");}
                );

        return teamSpawns;
    }

}
