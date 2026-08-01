package neonique.cbcplugin_new.mapconfig;

import neonique.cbcplugin_new.core.TeamColor;
import neonique.cbcplugin_new.util.ConfigUtil;
import org.bukkit.configuration.ConfigurationSection;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Stores the team requirements for a gamemode to be functional on a map.
 * @param minTeams The minimum amount of teams required for a game.
 * @param maxTeams The maximum amount of teams in the game.
 * @param validTeamColors The team colors that are supported.
 */
public record TeamRequirements (int minTeams, int maxTeams, List<TeamColor> validTeamColors) {

    /**
     * Parses a configuration into a team requirements object.
     * The passed config requires the integer "min_teams", integer "max_teams" and a list of valid {@link TeamColor}.
     * @param config Configuration, usually from a map file.
     * @return A TeamRequirements object.
     */
    public static TeamRequirements fromConfig (ConfigurationSection config) {

        int minTeams = ConfigUtil.requireInt(config, "min_teams");
        int maxTeams = ConfigUtil.requireInt(config, "max_teams");
        List<TeamColor> validTeamColors = ConfigUtil.requireStringList(config, "valid_team_colors").stream()
                .map(s -> TeamColor.valueOf(s.toUpperCase()))
                .collect(Collectors.toList());

        return new TeamRequirements(minTeams, maxTeams, validTeamColors);

    }

}
