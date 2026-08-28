package neonique.cbcplugin_new.lobby;

import neonique.cbcplugin_new.CBCPlugin;
import neonique.cbcplugin_new.combat.display.DeathMessageProvider;
import neonique.cbcplugin_new.core.CBCGamemode;
import neonique.cbcplugin_new.core.TeamColor;
import neonique.cbcplugin_new.gamemodes.showdown.ShowdownMapData;
import neonique.cbcplugin_new.mapconfig.*;
import neonique.cbcplugin_new.mapconfig.spawns.*;
import neonique.cbcplugin_new.scoreboard.CBCScoreboardManager;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.util.Vector;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import org.mockbukkit.mockbukkit.world.WorldMock;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static neonique.cbcplugin_new.lobby.LobbyTestUtil.*;
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

        repository.addMap(BASE_MAP_DATA);
        repository.addGamemodeMap(CBCGamemode.SHOWDOWN, GAMEMODE_MAP_DATA);

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

        assertNotNull(lobby.getPlayer(entity));
        LobbyPlayer player = lobby.getPlayer(entity);
        assertTrue(lobby.players().contains(player));

    }

    @Test
    public void addPlayerToTeamTest() {

        lobby.activate(_ -> {});
        PlayerMock entity = server.addPlayer("player1");
        lobby.newPlayer(entity);
        LobbyPlayer player = lobby.getPlayer(entity);
        LobbyTeam team = lobby.getTeams().get(TeamColor.RED);

        // Add mock player to red team
        lobby.playerJoinTeam(player, team, true);

        // Check if the player is assigned to the team, and that the player is in the team
        assertEquals(team, player.getAssignedTeam());
        assertTrue(team.isPlayerInTeam(player));
        // Check if the player is on the right scoreboard team
        Scoreboard mainScoreboard = server.getScoreboardManager().getMainScoreboard();
        assertTrue(mainScoreboard.getTeam("01redLobby").hasPlayer(entity));

    }

    @Test
    public void setPlayerSpectatorTest() {

        lobby.activate(_ -> {});
        PlayerMock entity = server.addPlayer("player1");
        lobby.newPlayer(entity);
        LobbyPlayer player = lobby.getPlayer(entity);

        // Set player to spectator
        lobby.playerSetSpectator(player);

        // Check if the player is a spectator and is not assigned to any team
        assertTrue(player.isSpectator());
        assertNull(player.getAssignedTeam());
        // Check if the player is on the right scoreboard team
        Scoreboard mainScoreboard = server.getScoreboardManager().getMainScoreboard();
        assertTrue(mainScoreboard.getTeam("10spectatorLobby").hasPlayer(entity));

    }

    @Test
    public void togglePlayerSpectatorTest() {

        lobby.activate(_ -> {});
        PlayerMock entity = server.addPlayer("player1");
        lobby.newPlayer(entity);
        LobbyPlayer player = lobby.getPlayer(entity);

        // Toggle player to spectator
        lobby.playerToggleSpectator(player);

        // Check if the player is a spectator and is not assigned to any team
        assertTrue(player.isSpectator());
        assertNull(player.getAssignedTeam());
        // Check if the player is on the right scoreboard team
        Scoreboard mainScoreboard = server.getScoreboardManager().getMainScoreboard();
        assertTrue(mainScoreboard.getTeam("10spectatorLobby").hasPlayer(entity));

        // Toggle player back to non-spectator
        lobby.playerToggleSpectator(player);

        // Check if the player is a spectator and is not assigned to any team
        assertFalse(player.isSpectator());
        assertNull(player.getAssignedTeam());
        // Check if the player is on the right scoreboard team
        assertTrue(mainScoreboard.getTeam("09ffaLobby").hasPlayer(entity));

    }

    @Test
    public void startGameTest() {
        boolean[] started = new boolean[]{false};
        lobby.activate(_ -> {
            started[0] = true;
        });
        lobby.startGame();
        assertTrue(started[0]);
    }

    @Test
    public void testStartCountdownFailWithNoGamemode () {
        lobby.activate(_ -> {});
        assertThrows(IllegalGameConditionsException.class, () -> lobby.startGameCountdown());
    }

    @Test
    public void testStartCountdownStopWhenPlayerLeaves () {

        lobby.activate(_ -> {});

        PlayerMock entity = server.addPlayer("player1");
        lobby.newPlayer(entity);
        LobbyPlayer player = lobby.getPlayer(entity);

        lobby.playerJoinTeam(player, TeamColor.RED, true);
        lobby.gameSelector().setGameSelection(CBCGamemode.SHOWDOWN, getMapDataWithConstraints(1, 2));
        lobby.startGameCountdown();

        assertTrue(lobby.isGameStarting());

        entity.disconnect();

        assertFalse(lobby.isGameStarting());

    }

    @Test
    public void deactivateLobbyTest() {
        lobby.activate(_ -> {});
        lobby.deactivate();
    }

}
