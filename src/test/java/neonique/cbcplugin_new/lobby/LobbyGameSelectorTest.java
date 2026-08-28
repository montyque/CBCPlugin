package neonique.cbcplugin_new.lobby;

import neonique.cbcplugin_new.combat.display.DeathMessageProvider;
import neonique.cbcplugin_new.core.CBCGamemode;
import neonique.cbcplugin_new.core.TeamColor;
import neonique.cbcplugin_new.gamemodes.showdown.ShowdownMapData;
import neonique.cbcplugin_new.mapconfig.*;
import neonique.cbcplugin_new.mapconfig.spawns.*;
import neonique.cbcplugin_new.scoreboard.CBCScoreboardManager;
import net.kyori.adventure.audience.Audiences;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import org.mockbukkit.mockbukkit.world.WorldMock;

import java.util.*;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class LobbyGameSelectorTest {

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
            new TeamRequirements(2, 4, Arrays.stream(TeamColor.values()).toList()),
            SPAWN_LIST,
            null
    );

    private ServerMock server;
    private JavaPlugin plugin;
    private WorldMock world;
    private MapRepository repository;
    private CBCScoreboardManager scoreboardManager;

    private LobbyGameSelector gameSelector;

    @BeforeEach
    public void setUp() {

        // Setup server, plugin and world
        server = MockBukkit.mock();
        plugin = MockBukkit.load(JavaPlugin.class);
        world = server.addSimpleWorld("world");

        // Setup dependencies
        scoreboardManager = new CBCScoreboardManager(server.getScoreboardManager());
        repository = new MapRepository();
        repository.addMap(BASE_MAP_DATA);

        // Create game selector
        gameSelector = new LobbyGameSelector(repository, world);

    }

    @AfterEach
    public void tearDown() {
        MockBukkit.unmock();
    }

    private List<LobbyPlayer> createMockLobbyPlayers (int count) {
        List<LobbyPlayer> players = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            PlayerMock entity = server.addPlayer();
            players.add(new LobbyPlayer(entity));
        }
        return players;
    }

    private Map<TeamColor, LobbyTeam> createLobbyTeams () {
        var teams = Map.of(
                TeamColor.RED, new LobbyTeam("01", "red", "Red", "R", TeamColor.RED),
                TeamColor.BLUE, new LobbyTeam("02", "blue", "Blue", "B", TeamColor.BLUE),
                TeamColor.GREEN, new LobbyTeam( "03", "green", "Green", "G", TeamColor.GREEN),
                TeamColor.YELLOW, new LobbyTeam("04", "yellow", "Yellow", "Y", TeamColor.YELLOW),
                TeamColor.CYAN, new LobbyTeam("05", "cyan", "Cyan", "C", TeamColor.CYAN),
                TeamColor.ORANGE, new LobbyTeam("06", "orange", "Orange", "O", TeamColor.ORANGE),
                TeamColor.MAGENTA, new LobbyTeam("07", "magenta", "Magenta", "M", TeamColor.MAGENTA),
                TeamColor.PURPLE, new LobbyTeam("08", "purple", "Purple", "P", TeamColor.PURPLE)
        );
        teams.values().forEach(l -> l.registerTeam(scoreboardManager));
        return teams;
    }

    private List<LobbyTeam> getTeamsWithPlayers (Collection<LobbyTeam> teams) {
        return teams.stream()
                .filter(t -> !t.onlinePlayers().isEmpty())
                .toList();
    }

    private GamemodeMapData getMapDataWithConstraints (int minTeams, int maxTeams, List<TeamColor> validTeamColors) {
        return new ShowdownMapData(
                BASE_MAP_DATA,
                new TeamRequirements(minTeams, maxTeams, validTeamColors),
                SPAWN_LIST,
                null
        );
    }

    private GamemodeMapData getMapDataWithConstraints (int minTeams, int maxTeams) {
        return getMapDataWithConstraints(minTeams, maxTeams, Arrays.stream(TeamColor.values()).toList());
    }



    @Test
    public void testStartValidationSuccess () {

        Map<TeamColor, LobbyTeam> teams = createLobbyTeams();
        List<LobbyPlayer> lobbyPlayers = createMockLobbyPlayers(2);
        teams.get(TeamColor.RED).addPlayer(lobbyPlayers.get(0));
        teams.get(TeamColor.BLUE).addPlayer(lobbyPlayers.get(1));
        gameSelector.setGameSelection(CBCGamemode.SHOWDOWN, GAMEMODE_MAP_DATA);

        // Should run without any errors
        gameSelector.validateGameStart(getTeamsWithPlayers(teams.values()), lobbyPlayers);

    }

    @Test
    public void testStartValidationNotEnoughTeams () {

        Map<TeamColor, LobbyTeam> teams = createLobbyTeams();
        List<LobbyPlayer> lobbyPlayers = createMockLobbyPlayers(2);
        teams.get(TeamColor.RED).addPlayer(lobbyPlayers.getFirst());
        gameSelector.setGameSelection(CBCGamemode.SHOWDOWN, getMapDataWithConstraints(2, 4));

        assertThrows(
                IllegalGameConditionsException.class,
                () -> gameSelector.validateGameStart(getTeamsWithPlayers(teams.values()), lobbyPlayers)
        );

    }

    @Test
    public void testStartValidationTooManyTeams () {

        Map<TeamColor, LobbyTeam> teams = createLobbyTeams();
        List<LobbyPlayer> lobbyPlayers = createMockLobbyPlayers(3);
        teams.get(TeamColor.RED).addPlayer(lobbyPlayers.get(0));
        teams.get(TeamColor.BLUE).addPlayer(lobbyPlayers.get(1));
        teams.get(TeamColor.GREEN).addPlayer(lobbyPlayers.get(2));
        gameSelector.setGameSelection(CBCGamemode.SHOWDOWN, getMapDataWithConstraints(1, 2));

        assertThrows(
                IllegalGameConditionsException.class,
                () -> gameSelector.validateGameStart(getTeamsWithPlayers(teams.values()), lobbyPlayers)
        );

    }

    @Test
    public void testStartValidationInvalidTeamColors () {

        Map<TeamColor, LobbyTeam> teams = createLobbyTeams();
        List<LobbyPlayer> lobbyPlayers = createMockLobbyPlayers(2);
        teams.get(TeamColor.BLUE).addPlayer(lobbyPlayers.get(0));
        teams.get(TeamColor.GREEN).addPlayer(lobbyPlayers.get(1));
        gameSelector.setGameSelection(CBCGamemode.SHOWDOWN,
                getMapDataWithConstraints(1, 2, List.of(TeamColor.RED, TeamColor.BLUE)));

        assertThrows(
                IllegalGameConditionsException.class,
                () -> gameSelector.validateGameStart(getTeamsWithPlayers(teams.values()), lobbyPlayers)
        );

    }


}
