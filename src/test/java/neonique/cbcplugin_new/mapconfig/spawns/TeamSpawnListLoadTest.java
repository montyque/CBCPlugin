package neonique.cbcplugin_new.mapconfig.spawns;

import neonique.cbcplugin_new.core.TeamColor;
import org.bukkit.configuration.Configuration;
import org.bukkit.util.Vector;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static neonique.cbcplugin_new.testutil.TestUtil.configFromString;

public class TeamSpawnListLoadTest {

    @Test
    public void testLoadingAssignedSpawnList () {

        List<TeamColor> testTeams = List.of(TeamColor.RED, TeamColor.BLUE);
        String configString = """
                assignment_type: ASSIGNED
                spawn_type: FROZEN
                locations:
                    red:
                    - [-293.5, 95.0, 3016.5]
                    blue:
                    - [-249.5, 95.0, 2898.5]
                """;
        Configuration config = configFromString(configString);

        TeamSpawnList list = TeamSpawnList.fromConfig(config, testTeams, testTeams.size());

        assertAll(
                () -> assertInstanceOf(AssignedSpawnList.class, list),
                () -> assertTrue(list.getTeamSpawns(testTeams).containsKey(TeamColor.RED)),
                () -> assertTrue(list.getTeamSpawns(testTeams).containsKey(TeamColor.BLUE)),
                () -> assertEquals(
                        new Vector(-293.5, 95.0, 3016.5),
                        list.getTeamSpawns(testTeams).get(TeamColor.RED).getFirst().vec()),
                () -> assertEquals(
                        new Vector(-249.5, 95.0, 2898.5),
                        list.getTeamSpawns(testTeams).get(TeamColor.BLUE).getFirst().vec())
        );

    }

    @Test
    public void testLoadingSingleSpawnList () {

        List<TeamColor> testTeams = List.of(TeamColor.RED, TeamColor.BLUE);
        String configString = """
                assignment_type: SINGLE
                spawn_type: FROZEN
                locations:
                - [-293.5, 95.0, 3016.5]
                - [-249.5, 95.0, 2898.5]
                """;
        Configuration config = configFromString(configString);

        TeamSpawnList list = TeamSpawnList.fromConfig(config, testTeams, testTeams.size());

        Map<TeamColor, List<StartSpawnConfig>> spawns = list.getTeamSpawns(testTeams);
        assertAll(
                () -> assertInstanceOf(SingleSpawnList.class, list),
                () -> assertTrue(spawns.containsKey(TeamColor.RED)),
                () -> assertTrue(spawns.containsKey(TeamColor.BLUE)),
                () -> assertEquals(
                        new Vector(-293.5, 95.0, 3016.5),
                        spawns.get(TeamColor.BLUE).get(0).vec()),
                () -> assertEquals(
                        new Vector(-249.5, 95.0, 2898.5),
                        spawns.get(TeamColor.BLUE).get(1).vec()),
                () -> assertEquals(spawns.get(TeamColor.RED), spawns.get(TeamColor.BLUE))
        );

    }

}
