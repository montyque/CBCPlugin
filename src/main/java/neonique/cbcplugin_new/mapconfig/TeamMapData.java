package neonique.cbcplugin_new.mapconfig;

import neonique.cbcplugin_new.core.TeamColor;

import java.util.List;

public interface TeamMapData extends GamemodeMapData {

    int minTeams ();
    int maxTeams ();
    List<TeamColor> validTeamColors ();

}
