package neonique.cbcplugin_new.gamemodes.crossbowtag;

import neonique.cbcplugin_new.mapconfig.TeamRequirements;
import neonique.cbcplugin_new.mapconfig.spawns.TeamSpawnList;
import neonique.cbcplugin_new.mapconfig.CBCMap;
import neonique.cbcplugin_new.mapconfig.TeamMapData;
import neonique.cbcplugin_new.util.ConfigUtil;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.ConfigurationSection;

public record TagMapData (CBCMap map,
                          TeamRequirements teamRequirements,
                          TeamSpawnList taggerSpawnList,
                          TeamSpawnList evaderSpawnList,
                          boolean evadersFrozenOnSetup) implements TeamMapData {

    public static TagMapData fromConfig (CBCMap map, Configuration gamemodeConfig) {

        TeamRequirements req = TeamRequirements.fromConfig(gamemodeConfig);

        ConfigurationSection taggerSpawnSection = ConfigUtil.requireConfigurationSection(gamemodeConfig, "tagger_spawns");
        TeamSpawnList taggerSpawnList = TeamSpawnList.fromConfig(taggerSpawnSection,
                req.validTeamColors(),
                req.maxTeams());

        ConfigurationSection evaderSpawnSection = ConfigUtil.requireConfigurationSection(gamemodeConfig, "evader_spawns");
        TeamSpawnList evaderSpawnList = TeamSpawnList.fromConfig(evaderSpawnSection,
                req.validTeamColors(),
                req.maxTeams());

        boolean evadersFrozenOnSetup = ConfigUtil.getBoolean(gamemodeConfig, "evaders_frozen_on_setup")
                .orElse(true);

        return new TagMapData(map, req,
                taggerSpawnList, evaderSpawnList, evadersFrozenOnSetup);

    }

}
