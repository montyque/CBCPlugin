package neonique.cbcplugin_new.gamemodes.showdown;

import neonique.cbcplugin_new.mapconfig.CBCMap;
import neonique.cbcplugin_new.mapconfig.TeamMapData;
import neonique.cbcplugin_new.mapconfig.TeamRequirements;
import neonique.cbcplugin_new.mapconfig.spawns.TeamSpawnList;
import neonique.cbcplugin_new.mechanics.DeathBorder;
import neonique.cbcplugin_new.util.ConfigUtil;
import org.bukkit.configuration.ConfigurationSection;

public record ShowdownMapData (CBCMap map,
                               TeamRequirements teamRequirements,
                               TeamSpawnList spawns,
                               ShowdownSuddenDeathData suddenDeathData) implements TeamMapData {

    public static ShowdownMapData fromConfig (CBCMap map, ConfigurationSection gamemodeConfig) {

        TeamRequirements req = TeamRequirements.fromConfig(gamemodeConfig);

        ConfigurationSection spawnSection = ConfigUtil.requireConfigurationSection(gamemodeConfig, "spawns");
        TeamSpawnList spawnList = TeamSpawnList.fromConfig(spawnSection,
                req.validTeamColors(),
                req.maxTeams());

        ShowdownSuddenDeathData suddenDeathData = ConfigUtil.getConfigurationSection(gamemodeConfig, "sudden_death")
                .map(ShowdownSuddenDeathData::fromConfig)
                .orElse(null);

        return new ShowdownMapData(map, req, spawnList, suddenDeathData);

    }

    public boolean suddenDeathEnabled () {
        return suddenDeathData != null;
    }

    public int suddenDeathTimer () {
        return suddenDeathData.timer();
    }

}

record ShowdownSuddenDeathData (int timer, DeathBorder.DeathBorderOptions borderOptions) {

    public static ShowdownSuddenDeathData fromConfig (ConfigurationSection config) {

        return new ShowdownSuddenDeathData(
                ConfigUtil.requireInt(config, "timer"),
                DeathBorder.DeathBorderOptions.fromConfig(ConfigUtil.requireConfigurationSection(config, "death_border"))
        );

    }

}