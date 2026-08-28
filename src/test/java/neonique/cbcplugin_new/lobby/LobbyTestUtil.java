package neonique.cbcplugin_new.lobby;

import neonique.cbcplugin_new.combat.display.DeathMessageProvider;
import neonique.cbcplugin_new.core.TeamColor;
import neonique.cbcplugin_new.gamemodes.showdown.ShowdownMapData;
import neonique.cbcplugin_new.mapconfig.CBCMapData;
import neonique.cbcplugin_new.mapconfig.GamemodeMapData;
import neonique.cbcplugin_new.mapconfig.MapOptions;
import neonique.cbcplugin_new.mapconfig.TeamRequirements;
import neonique.cbcplugin_new.mapconfig.spawns.*;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.util.Vector;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;

import java.util.*;

public class LobbyTestUtil {

    public static final TeamSpawnList SPAWN_LIST = new SingleSpawnList(List.of(new StartSpawnConfig() {
        @Override
        public MapStartSpawn getSpawn(World world) {
            return new FrozenSpawn(world, vec());
        }
        @Override
        public Vector vec() {
            return new Vector(0, 100, 0);
        }
    }));

    public static final CBCMapData BASE_MAP_DATA = new CBCMapData(
            "test",
            "Test",
            Material.AIR,
            new Vector(0, 100, 0),
            new Vector(10, 100, 10),
            new Vector(-10, 100, 10),
            List.of(new Vector(0, 100, 0)),
            SPAWN_LIST,
            MapOptions.DEFAULTS,
            Map.of(),
            DeathMessageProvider.empty()
    );

    public static final ShowdownMapData GAMEMODE_MAP_DATA = new ShowdownMapData(
            BASE_MAP_DATA,
            new TeamRequirements(1, 8, Arrays.stream(TeamColor.values()).toList()),
            SPAWN_LIST,
            null
    );

    static GamemodeMapData getMapDataWithConstraints(int minTeams, int maxTeams, List<TeamColor> validTeamColors) {
        return new ShowdownMapData(
                BASE_MAP_DATA,
                new TeamRequirements(minTeams, maxTeams, validTeamColors),
                SPAWN_LIST,
                null
        );
    }

    static GamemodeMapData getMapDataWithConstraints(int minTeams, int maxTeams) {
        return getMapDataWithConstraints(minTeams, maxTeams, Arrays.stream(TeamColor.values()).toList());
    }

    static List<LobbyPlayer> createMockLobbyPlayers (ServerMock server, int count) {
        List<LobbyPlayer> players = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            PlayerMock entity = server.addPlayer();
            players.add(new LobbyPlayer(entity));
        }
        return players;
    }

    static List<LobbyPlayer> createMockLobbyPlayers (ServerMock server, Lobby lobby, int count) {
        List<LobbyPlayer> players = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            PlayerMock entity = server.addPlayer();
            lobby.newPlayer(entity);
            players.add(lobby.getPlayer(entity));
        }
        return players;
    }

}
