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

}