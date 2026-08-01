package neonique.cbcplugin_new.mapconfig.spawns;

import neonique.cbcplugin_new.core.TeamColor;
import neonique.cbcplugin_new.core.TeamLike;
import neonique.cbcplugin_new.util.ConfigUtil;
import neonique.cbcplugin_new.util.VectorUtil;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public interface TeamSpawnList {

    /**
     * Generates and returns a list of spawn configurations for the given colors.
     * It is guaranteed that all colors passed will be in the returned map.
     * @param colors A list of unique team colors.
     * @throws IllegalArgumentException if the colors passed in are incompatible with the spawn list
     * @return Map associating TeamColor to a list of spawn configurations.
     */
    Map<TeamColor, List<StartSpawnConfig>> getTeamSpawns (List<TeamColor> colors);

    /**
     * Generates and returns a list of starting spawns for the given colors.
     * It is guaranteed that all colors passed will be in the returned map.
     * @param colors A list of unique team colors.
     * @param world The world which the spawns are placed in.
     * @throws IllegalArgumentException if the colors passed in are incompatible with the spawn list
     * @return Map associating TeamColor to a list of spawns.
     */
    default Map<TeamColor, List<MapStartSpawn>> getTeamColorSpawnLocations (List<TeamColor> colors, World world) {
        return getTeamSpawns(colors).entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        e -> e.getValue().stream()
                                .map(c -> c.getSpawn(world))
                                .toList()
                ));
    }

    /**
     * Generates and returns a list of starting spawns for the given teams.
     * It is guaranteed that all teams passed will be in the returned map.
     * @param teams A list of unique teams.
     * @param world The world which the spawns are placed in.
     * @throws IllegalArgumentException if the teams passed in are incompatible with the spawn list
     * @return Map associating teams to a list of spawns.
     */
    default <T extends TeamLike> Map<T, List<MapStartSpawn>> getTeamSpawnLocations (List<T> teams, World world) {
        var colorSpawnLocations = getTeamColorSpawnLocations(teams.stream().map(TeamLike::teamColor).toList(), world);
        return teams.stream()
                .collect(Collectors.toMap(
                        t -> t,
                        t -> colorSpawnLocations.get(t.teamColor())
                ));
    }

    /**
     * Parses an object into a list of vectors.
     * @param obj An object, should be a list of lists to parse correctly.
     * @throws IllegalArgumentException if the object could not be parsed
     * @return A list of vectors held by the configuration object.
     */
    static List<Vector> parseLocations (Object obj) {
        try {
            // Check that the object is a list
            if (!(obj instanceof List<?> list))
                throw new IllegalArgumentException("Expected type List");
            // Check that the object is made of all vectors
            return list.stream()
                    .map(o -> {
                        try {
                            return (List<?>) o;
                        } catch (ClassCastException e) {
                            throw new IllegalArgumentException("All elements in List must be of type List");
                        }
                    }).map(VectorUtil::listToVec)
                    .toList();
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Failure parsing object into spawn list", e);
        }
    }

    /**
     * Parses a TeamSpawnList from a map's configuration.
     * @param config The config section holding spawn information.
     * @param requiredColors The team colors required to be held on the spawn config section.
     * @param maxTeams The maximum amount of colors on the spawn config section.
     * @return A TeamSpawnList.
     */
    static TeamSpawnList fromConfig (ConfigurationSection config,
                                     List<TeamColor> requiredColors,
                                     int maxTeams) {

        TeamSpawnListType type = ConfigUtil.requireEnum(config, "assignment_type", TeamSpawnListType.class);
        return type.fromConfig(config, requiredColors, maxTeams);

    }

    enum TeamSpawnListType {

        SINGLE () {
            @Override
            public TeamSpawnList fromConfig (ConfigurationSection config,
                                             List<TeamColor> requiredColors,
                                             int maxTeams) {
                return SingleSpawnList.fromConfig(config);
            }
        },

        ASSIGNED () {
            @Override
            public TeamSpawnList fromConfig (ConfigurationSection config,
                                             List<TeamColor> requiredColors,
                                             int maxTeams) {
                return AssignedSpawnList.fromConfig(config, requiredColors);
            }
        },

        RANDOM () {
            @Override
            public TeamSpawnList fromConfig (ConfigurationSection config,
                                             List<TeamColor> requiredColors,
                                             int maxTeams) {
                return RandomSpawnList.fromConfig(config, maxTeams);
            }
        };

        public abstract TeamSpawnList fromConfig (ConfigurationSection config,
                                                  List<TeamColor> requiredColors,
                                                  int maxTeams);

    }

}


class AssignedSpawnList implements TeamSpawnList {

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
            TeamColor teamColor = TeamColor.valueOf(teamConfig.getKey());
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


class SingleSpawnList implements TeamSpawnList {

    private final List<StartSpawnConfig> teamSpawns;

    private SingleSpawnList (List<StartSpawnConfig> teamSpawns) {
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


class RandomSpawnList implements TeamSpawnList {

    private final static Random GLOBAL_RANDOM = new Random();

    private final Random random;
    private final List<List<StartSpawnConfig>> teamSpawns;

    private RandomSpawnList (Random random, List<List<StartSpawnConfig>> teamSpawns) {
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