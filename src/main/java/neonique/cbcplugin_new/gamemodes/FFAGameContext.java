package neonique.cbcplugin_new.gamemodes;

import neonique.cbcplugin_new.core.PlayerLike;
import neonique.cbcplugin_new.core.TeamLike;
import neonique.cbcplugin_new.mapconfig.CBCMap;
import neonique.cbcplugin_new.lobby.LobbyPlayer;
import neonique.cbcplugin_new.lobby.LobbyTeam;
import neonique.cbcplugin_new.mapconfig.GamemodeMapData;
import neonique.cbcplugin_new.mapconfig.TeamMapData;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record FFAGameContext (GamemodeMapData mapData,
                              List<PlayerLike> players,
                              GameSettings gameSettings) implements GameContext {}

/*
public record FFAGameContext(CBCMap map, Map<String, LobbyTeam> teams, Collection<LobbyPlayer> players,) {

    private final CBCMap map;
    private final Collection<LobbyPlayer> players;

    private final Map<String, Boolean> boolVars;
    private final Map<String, Integer> intVars;
    private final Map<String, String> stringVars;

    public FFAGameContext(CBCMap map, LinkedHashMap<String, LobbyTeam> teams, Collection<LobbyPlayer> players,
                          Map<String, Boolean> boolVars, Map<String, Integer> intVars, Map<String, String> stringVars) {

        this.map = map;
        this.teams = teams;
        this.players = players;

        this.boolVars = boolVars;
        this.intVars = intVars;
        this.stringVars = stringVars;

    }

    public CBCMap getMap() {
        return map;
    }

    public Collection<LobbyPlayer> getPlayers() {
        return players;
    }

    public LinkedHashMap<String, LobbyTeam> getTeams() {
        return teams;
    }

    public Map<String, Boolean> getBoolVars() {
        return boolVars;
    }

    public Map<String, Integer> getIntVars() {
        return intVars;
    }

    public Map<String, String> getStringVars() {
        return stringVars;
    }

}*/