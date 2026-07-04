package neonique.cbcplugin_new.mapconfig;

import neonique.cbcplugin_new.core.TeamColor;

import java.util.Set;

public interface TeamMapData extends GamemodeMapData {

    int minTeams ();
    int maxTeams ();
    Set<TeamColor> validTeamColors ();

}
