package neonique.cbcplugin_new.gamemodes.assassin;

import neonique.cbcplugin_new.mapconfig.CBCMap;
import neonique.cbcplugin_new.mapconfig.GamemodeMapData;
import neonique.cbcplugin_new.util.ConfigUtil;
import org.bukkit.World;
import org.bukkit.configuration.Configuration;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;

public class AssassinMapData implements GamemodeMapData {

    private final CBCMap map;
    private final double enemyRadius;
    private final double spawnTargetRadius;

    public AssassinMapData (CBCMap map, Configuration gamemodeConfig) {

        this.map = map;

        enemyRadius = ConfigUtil.getInt(gamemodeConfig, "enemy_radius").orElse(10);
        spawnTargetRadius = ConfigUtil.getInt(gamemodeConfig, "spawn_target_radius").orElse(50);

    }

    @Override
    public CBCMap getMap () {
        return map;
    }

    public List<AssassinSpawn> getAssassinSpawns (AssassinGame game) {
        List<AssassinSpawn> spawns = new ArrayList<>();
        World world = map.getWorld();
        for (Vector spawnVector : map.getSpawnpointCoordinates()) {
            // TODO: allow for ignoring Y
            spawns.add(new AssassinSpawn(game, world, spawnVector, enemyRadius, true));
        }
        return spawns;
    }

    public double spawnTargetRadius () {
        return spawnTargetRadius;
    }

}
