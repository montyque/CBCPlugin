package neonique.cbcplugin_new.mapconfig;

import neonique.cbcplugin_new.core.TeamColor;

import java.util.List;

public interface TeamMapData extends GamemodeMapData {

    TeamRequirements teamRequirements ();

    default int minTeams () {
        return teamRequirements().minTeams();
    }

    default int maxTeams () {
        return teamRequirements().minTeams();
    }

    default List<TeamColor> validTeamColors () {
        return teamRequirements().validTeamColors();
    }

}
