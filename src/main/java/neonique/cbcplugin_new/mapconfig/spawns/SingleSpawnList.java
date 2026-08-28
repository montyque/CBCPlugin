package neonique.cbcplugin_new.mapconfig.spawns;

import neonique.cbcplugin_new.core.TeamColor;
import neonique.cbcplugin_new.util.ConfigUtil;
import org.bukkit.configuration.ConfigurationSection;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class SingleSpawnList implements TeamSpawnList {

    private final List<StartSpawnConfig> teamSpawns;

    public SingleSpawnList (List<StartSpawnConfig> teamSpawns) {
        this.teamSpawns = teamSpawns;
    }

    public static TeamSpawnList fromConfig (ConfigurationSection config) {

        StartSpawnType spawnType = ConfigUtil.requireEnum(config, "spawn_type", StartSpawnType.class);
        List<StartSpawnConfig> spawnList = ConfigUtil.requireVectorList(config, "locations").stream()
                .map(v -> spawnType.fromConfig(config, v))
                .toList();

        return new SingleSpawnList(spawnList);

    }

    @Override
    public Map<TeamColor, List<StartSpawnConfig>> getTeamSpawns(List<TeamColor> colors) {
        return colors.stream().collect(
                Collectors.toMap(
                        c -> c,
                        c -> teamSpawns
                ));
    }

}