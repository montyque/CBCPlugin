package neonique.cbcplugin_new.lobby;

import neonique.cbcplugin_new.CBCPlugin;
import neonique.cbcplugin_new.mapconfig.MapRepository;
import neonique.cbcplugin_new.scoreboard.CBCScoreboardManager;
import org.bukkit.Location;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.Scoreboard;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import org.mockbukkit.mockbukkit.world.WorldMock;

import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings("markedForRemoval")
public class LobbyTest {

    private ServerMock server;
    private JavaPlugin plugin;
    private WorldMock world;

    private CBCScoreboardManager scoreboardManager;
    private Lobby lobby;

    @BeforeEach
    public void setUp() {

        // Setup server, plugin and world
        server = MockBukkit.mock();
        plugin = MockBukkit.load(JavaPlugin.class);
        world = server.addSimpleWorld("world");

        // Setup lobby dependencies
        scoreboardManager = new CBCScoreboardManager(server.getScoreboardManager());
        MapRepository repository = new MapRepository();

        // Create lobby
        lobby = new Lobby(
                plugin, world, scoreboardManager, repository, new Location(world, 100.5, 20, 100.5)
        );

    }

    @AfterEach
    public void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    public void activateLobbyTest() {

        // Activate the lobby
        lobby.activate(_ -> {});

        // Assertions
        assertTrue(lobby.isActive()); // Lobby should be active
        assertTrue(new Location(world, 100.5, 20, 100.5).distanceSquared(lobby.getLobbySpawn()) < 0.5); // Lobby should have start location

        // Assert teams were created correctly
        Scoreboard mainScoreboard = server.getScoreboardManager().getMainScoreboard();
        assertEquals(8, lobby.getTeams().size());
        assertNotNull(mainScoreboard.getTeam("01redLobby"));
        assertNotNull(mainScoreboard.getTeam("02blueLobby"));
        assertNotNull(mainScoreboard.getTeam("03greenLobby"));
        assertNotNull(mainScoreboard.getTeam("04yellowLobby"));
        assertNotNull(mainScoreboard.getTeam("05cyanLobby"));
        assertNotNull(mainScoreboard.getTeam("06orangeLobby"));
        assertNotNull(mainScoreboard.getTeam("07magentaLobby"));
        assertNotNull(mainScoreboard.getTeam("08purpleLobby"));
        assertNotNull(mainScoreboard.getTeam("09ffaLobby"));
        assertNotNull(mainScoreboard.getTeam("10spectatorLobby"));

    }

    @Test
    public void addPlayerTest() {

        lobby.activate(_ -> {});
        PlayerMock entity = server.addPlayer("player1");

        // Add mock player to lobby
        lobby.newPlayer(entity);

        // Check player
        assertNotNull(lobby.getPlayer(entity));
        LobbyPlayer player = lobby.getPlayer(entity);

        assertAll(
                () -> assertTrue(lobby.players().contains(player))
        );

    }

    @Test
    public void deactivateLobbyTest() {

        lobby.activate(_ -> {});
        lobby.deactivate();

    }

}
