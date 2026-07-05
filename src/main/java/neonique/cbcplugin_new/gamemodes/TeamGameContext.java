package neonique.cbcplugin_new.gamemodes;

import neonique.cbcplugin_new.core.TeamLike;
import neonique.cbcplugin_new.mapconfig.TeamMapData;

import java.util.List;
import java.util.Map;

public record TeamGameContext (TeamMapData mapData,
                               List<TeamLike> teams,
                               GameSettings gameSettings) implements GameContext {}
