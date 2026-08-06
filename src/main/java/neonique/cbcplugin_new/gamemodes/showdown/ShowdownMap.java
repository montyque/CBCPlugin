package neonique.cbcplugin_new.gamemodes.showdown;

import neonique.cbcplugin_new.mapconfig.CBCMap;
import neonique.cbcplugin_new.mapconfig.spawns.MapStartSpawn;
import neonique.cbcplugin_new.mechanics.DeathBorder;
import org.bukkit.World;

import java.util.List;
import java.util.Map;

public class ShowdownMap {

    private final World world;
    private final CBCMap map;
    private final ShowdownMapData data;

    public ShowdownMap (World world, ShowdownMapData data) {
        this.world = world;
        this.data = data;
        this.map = new CBCMap(world, data.mapData());
    }

    public Map<ShowdownTeam, List<MapStartSpawn>> roundSpawns (List<ShowdownTeam> teams) {
        return data.spawns().getTeamSpawnLocations(teams, world);
    }

    public DeathBorder.DeathBorderOptions deathBorderOptions () {
        return data.suddenDeathData().borderOptions();
    }

    public CBCMap map() {
        return map;
    }

    public ShowdownMapData data() {
        return data;
    }

    public boolean suddenDeathEnabled () {
        return data.suddenDeathData() != null;
    }

    public int suddenDeathTimer () {
        return data.suddenDeathData().timer();
    }

}
