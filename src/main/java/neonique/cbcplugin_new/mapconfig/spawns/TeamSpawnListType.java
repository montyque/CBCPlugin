package neonique.cbcplugin_new.mapconfig.spawns;

import neonique.cbcplugin_new.core.TeamColor;
import org.bukkit.configuration.ConfigurationSection;

import java.util.List;

    public enum TeamSpawnListType {

        SINGLE() {
            @Override
            public TeamSpawnList fromConfig(ConfigurationSection config,
                                            List<TeamColor> requiredColors,
                                            int maxTeams) {
                return SingleSpawnList.fromConfig(config);
            }
        },

        ASSIGNED() {
            @Override
            public TeamSpawnList fromConfig(ConfigurationSection config,
                                            List<TeamColor> requiredColors,
                                            int maxTeams) {
                return AssignedSpawnList.fromConfig(config, requiredColors);
            }
        },

        RANDOM() {
            @Override
            public TeamSpawnList fromConfig(ConfigurationSection config,
                                            List<TeamColor> requiredColors,
                                            int maxTeams) {
                return RandomSpawnList.fromConfig(config, maxTeams);
            }
        };

        public abstract TeamSpawnList fromConfig(ConfigurationSection config,
                                                 List<TeamColor> requiredColors,
                                                 int maxTeams);

    }
