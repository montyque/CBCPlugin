package neonique.cbcplugin_new.gamemodes.holdthegold;

import neonique.cbcplugin_new.mapconfig.CBCMap;
import neonique.cbcplugin_new.mapconfig.GamemodeMapData;
import neonique.cbcplugin_new.mapconfig.TeamRequirements;
import neonique.cbcplugin_new.mapconfig.spawns.TeamSpawnList;
import neonique.cbcplugin_new.util.ConfigUtil;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.util.Vector;

import java.util.List;

public record HTGMapData (CBCMap mapData,
                          TeamRequirements teamRequirements,
                          Vector goldSpawn,
                          TeamSpawnList startingSpawns,
                          HTGSpawnData normalSpawns) implements GamemodeMapData {

    public static HTGMapData fromConfig (CBCMap map, Configuration gamemodeConfig) {

        TeamRequirements req = TeamRequirements.fromConfig(gamemodeConfig);

        Vector goldSpawn = ConfigUtil.requireVector(gamemodeConfig, "gold_spawn");

        ConfigurationSection startingSpawnSection = ConfigUtil.requireConfigurationSection(gamemodeConfig, "starting_spawns");
        TeamSpawnList startingSpawns = TeamSpawnList.fromConfig(startingSpawnSection,
                req.validTeamColors(),
                req.maxTeams());

        HTGSpawnData normalSpawns = HTGSpawnData.fromConfig(gamemodeConfig, map.getDefaultSpawns());

        return new HTGMapData(map, req, goldSpawn, startingSpawns, normalSpawns);

    }

}

record HTGSpawnData (List<Vector> spawnVectors,
                     double allyRadius,
                     double enemyRadius,
                     double spawnGoldRadius) {

    public HTGSpawnData {

        if (allyRadius < 0) throw new IllegalArgumentException("Ally radius must not be negative");
        if (enemyRadius < 0) throw new IllegalArgumentException("Enemy radius must not be negative");
        if (spawnGoldRadius < 0) throw new IllegalArgumentException("Enemy radius must not be negative");

    }

    public static HTGSpawnData fromConfig (Configuration config, List<Vector> defaultVectors) {

        return new HTGSpawnData(
                ConfigUtil.getVectorList(config, "spawns_override").orElse(defaultVectors),
                ConfigUtil.getDouble(config, "ally_radius").orElse(15d),
                ConfigUtil.getDouble(config, "enemy_radius").orElse(15d),
                ConfigUtil.getDouble(config, "spawn_gold_radius").orElse(60d)
        );

    }

}
