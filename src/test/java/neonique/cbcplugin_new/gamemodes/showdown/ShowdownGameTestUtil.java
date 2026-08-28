package neonique.cbcplugin_new.gamemodes.showdown;

import neonique.cbcplugin_new.combat.display.DeathMessageProvider;
import neonique.cbcplugin_new.core.TeamColor;
import neonique.cbcplugin_new.mapconfig.CBCMapData;
import neonique.cbcplugin_new.mapconfig.MapOptions;
import neonique.cbcplugin_new.mapconfig.TeamRequirements;
import neonique.cbcplugin_new.mapconfig.spawns.*;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.util.Vector;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class ShowdownGameTestUtil {

    public static final TeamSpawnList BOXED_ASSIGNED_SPAWN_LIST = new AssignedSpawnList(Map.of(
            TeamColor.RED, List.of(new BoxSpawnConfig(new Vector(150, 50, 150), new Vector(3, 3, 3))),
            TeamColor.BLUE, List.of(new BoxSpawnConfig(new Vector(150, 50, 150), new Vector(3, 3, 3))),
            TeamColor.GREEN, List.of(new BoxSpawnConfig(new Vector(150, 50, 150), new Vector(3, 3, 3))),
            TeamColor.YELLOW, List.of(new BoxSpawnConfig(new Vector(150, 50, 150), new Vector(3, 3, 3)))
    ));

    static CBCMapData getBaseMapData () {
        return new CBCMapData(
                "test",
                "Test",
                Material.AIR,
                new Vector(0, 100, 0),
                new Vector(10, 100, 10),
                new Vector(-10, 100, 10),
                List.of(new Vector(0, 100, 0)),
                BOXED_ASSIGNED_SPAWN_LIST,
                MapOptions.DEFAULTS,
                Map.of(),
                DeathMessageProvider.empty()
        );
    }

    static ShowdownMapData getShowdownMapData (TeamSpawnList spawns, ShowdownSuddenDeathData suddenDeathData) {
        return new ShowdownMapData(
                getBaseMapData(),
                new TeamRequirements(1, 8, Arrays.stream(TeamColor.values()).toList()),
                spawns,
                suddenDeathData
        );
    }

}
