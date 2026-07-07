package neonique.cbcplugin_new.gamemodes.crossbowtag;

import neonique.cbcplugin_new.core.TeamColor;
import neonique.cbcplugin_new.gamemodes.TeamSpawnList;
import neonique.cbcplugin_new.mapconfig.CBCMap;
import neonique.cbcplugin_new.mapconfig.TeamMapData;
import neonique.cbcplugin_new.util.ConfigUtil;
import org.bukkit.configuration.Configuration;

import java.util.List;
import java.util.stream.Collectors;

public record TagMapData (CBCMap map,
                          int minTeams,
                          int maxTeams,
                          List<TeamColor> validTeamColors,
                          TeamSpawnList taggerSpawnList,
                          TeamSpawnList evaderSpawnList,
                          boolean evadersFrozenOnSetup) implements TeamMapData {

    public static TagMapData fromConfig (CBCMap map, Configuration gamemodeConfig) {

        int minTeams = ConfigUtil.requireInt(gamemodeConfig, "min_teams");
        int maxTeams = ConfigUtil.requireInt(gamemodeConfig, "max_teams");
        List<TeamColor> validTeamColors = ConfigUtil.requireStringList(gamemodeConfig, "valid_team_colors").stream()
                .map(s -> TeamColor.valueOf(s.toUpperCase()))
                .collect(Collectors.toList());

        TeamSpawnList evaderSpawnList = TeamSpawnList.fromConfig(gamemodeConfig,
                "evader_spawns_type",
                "evader_spawns",
                validTeamColors,
                maxTeams);

        TeamSpawnList taggerSpawnList = TeamSpawnList.fromConfig(gamemodeConfig,
                "tagger_spawns_type",
                "tagger_spawns",
                validTeamColors,
                maxTeams);

        boolean evadersFrozenOnSetup = ConfigUtil.getBoolean(gamemodeConfig, "evaders_frozen_on_setup")
                .orElse(true);

        return new TagMapData(map, minTeams, maxTeams, validTeamColors,
                evaderSpawnList, taggerSpawnList, evadersFrozenOnSetup);

    }

}
