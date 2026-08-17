package neonique.cbcplugin_new.core;

import neonique.cbcplugin_new.mapconfig.TeamMapData;

import java.util.List;

public record TeamGameContext (TeamMapData mapData,
                               List<TeamLike> teams,
                               GameSettings gameSettings) implements GameContext {}
