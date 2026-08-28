package neonique.cbcplugin_new.gamemodes.showdown;

import neonique.cbcplugin_new.core.*;
import neonique.cbcplugin_new.scoreboard.CBCScoreboardManager;
import neonique.cbcplugin_new.testutil.GameTestUtil;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import org.mockbukkit.mockbukkit.entity.PlayerMock;
import org.mockbukkit.mockbukkit.world.WorldMock;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import static neonique.cbcplugin_new.gamemodes.showdown.ShowdownGameTestUtil.BOXED_ASSIGNED_SPAWN_LIST;
import static neonique.cbcplugin_new.gamemodes.showdown.ShowdownGameTestUtil.getShowdownMapData;

public class ShowdownGameLifecycleTest {

    private ServerMock server;
    private JavaPlugin plugin;
    private WorldMock world;
    private CBCScoreboardManager scoreboardManager;

    private ShowdownGame game;

    @BeforeEach
    public void setUp () {

        server = MockBukkit.mock();
        plugin = MockBukkit.load(JavaPlugin.class);
        world = server.addSimpleWorld("world");
        scoreboardManager = new CBCScoreboardManager(server.getScoreboardManager());

        // Create the game object
        game = new ShowdownGame(new GameInitContext(plugin, scoreboardManager, world));

    }

    @AfterEach
    public void tearDown() {
        MockBukkit.unmock();
    }

    @Test
    public void testGameSetup () {

        server.setPlayers(4);
        List<? extends TeamLike> teams = GameTestUtil.mockTeamLikes(Map.of(
                TeamColor.RED, List.of(server.getPlayer(0)),
                TeamColor.BLUE, List.of(server.getPlayer(1)),
                TeamColor.GREEN, List.of(server.getPlayer(2)),
                TeamColor.YELLOW, List.of(server.getPlayer(3))
        ));
        GameContext ctx = new TeamGameContext(
                getShowdownMapData(BOXED_ASSIGNED_SPAWN_LIST, null),
                teams,
                new ShowdownSettings());

        game.setupGame(ctx);

    }

    @Test
    public void testGameCleanup () {

        server.setPlayers(4);
        List<? extends TeamLike> teams = GameTestUtil.mockTeamLikes(Map.of(
                TeamColor.RED, List.of(server.getPlayer(0)),
                TeamColor.BLUE, List.of(server.getPlayer(1)),
                TeamColor.GREEN, List.of(server.getPlayer(2)),
                TeamColor.YELLOW, List.of(server.getPlayer(3))
        ));
        GameContext ctx = new TeamGameContext(
                getShowdownMapData(BOXED_ASSIGNED_SPAWN_LIST, null),
                teams,
                new ShowdownSettings());
        game.setupGame(ctx);

        game.resetGame();

    }

}
