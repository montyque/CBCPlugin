package neonique.cbcplugin_new.mapconfig.spawns;


import neonique.cbcplugin_new.core.TeamColor;
import neonique.cbcplugin_new.util.ConfigUtil;
import org.bukkit.configuration.ConfigurationSection;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class RandomSpawnList implements TeamSpawnList {

    private final static Random GLOBAL_RANDOM = new Random();

    private final Random random;
    private final List<List<StartSpawnConfig>> teamSpawns;

    public RandomSpawnList (Random random, List<List<StartSpawnConfig>> teamSpawns) {
        this.random = random;
        this.teamSpawns = teamSpawns;
    }

    public static TeamSpawnList fromConfig (ConfigurationSection config,
                                            int maxTeams) {

        StartSpawnType spawnType = ConfigUtil.requireEnum(config, "spawn_type", StartSpawnType.class);

        List<?> configList = ConfigUtil.requireList(config, "locations");
        List<List<StartSpawnConfig>> spawnList = configList.stream()
                .map(l -> TeamSpawnList.parseLocations(l).stream()
                        .map(v -> spawnType.fromConfig(config, v))
                        .toList())
                .toList();

        if (spawnList.size() < maxTeams) {
            throw new IllegalArgumentException("Not enough spawn sets provided, at least " + maxTeams + " required");
        }

        return new RandomSpawnList(GLOBAL_RANDOM, spawnList);

    }

    @Override
    public Map<TeamColor, List<StartSpawnConfig>> getTeamSpawns(List<TeamColor> colors) {

        if (teamSpawns.size() > colors.size()) {
            throw new IllegalArgumentException("Spawn list cannot support " + colors.size() + " team(s), " +
                    "can only support up to " + colors.size());
        }

        List<List<StartSpawnConfig>> shuffledSpawns = new ArrayList<>(teamSpawns);
        List<TeamColor> shuffledColors = new ArrayList<>(colors);
        Collections.shuffle(shuffledSpawns, random);
        Collections.shuffle(shuffledColors, random);

        return IntStream.range(0, shuffledColors.size()).boxed()
                .collect(Collectors.toMap(
                        shuffledColors::get,
                        shuffledSpawns::get
                ));

    }

}