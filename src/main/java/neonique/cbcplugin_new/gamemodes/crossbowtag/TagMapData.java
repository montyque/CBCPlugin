package neonique.cbcplugin_new.gamemodes.crossbowtag;

import neonique.cbcplugin_new.core.TeamColor;
import neonique.cbcplugin_new.mapconfig.spawns.TeamSpawnList;
import neonique.cbcplugin_new.mapconfig.CBCMap;
import neonique.cbcplugin_new.mapconfig.TeamMapData;
import neonique.cbcplugin_new.util.ConfigUtil;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.ConfigurationSection;

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

        ConfigurationSection evaderSpawnSection = ConfigUtil.requireConfigurationSection(gamemodeConfig, "evader_spawns");
        TeamSpawnList evaderSpawnList = TeamSpawnList.fromConfig(evaderSpawnSection,
                validTeamColors,
                maxTeams);

        ConfigurationSection taggerSpawnSection = ConfigUtil.requireConfigurationSection(gamemodeConfig, "evader_spawns");
        TeamSpawnList taggerSpawnList = TeamSpawnList.fromConfig(taggerSpawnSection,
                validTeamColors,
                maxTeams);

        boolean evadersFrozenOnSetup = ConfigUtil.getBoolean(gamemodeConfig, "evaders_frozen_on_setup")
                .orElse(true);

        return new TagMapData(map, minTeams, maxTeams, validTeamColors,
                evaderSpawnList, taggerSpawnList, evadersFrozenOnSetup);

    }

}
