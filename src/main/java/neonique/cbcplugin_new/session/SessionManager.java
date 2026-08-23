package neonique.cbcplugin_new.session;

import neonique.cbcplugin_new.core.CBCGamemode;
import neonique.cbcplugin_new.core.Game;
import neonique.cbcplugin_new.core.GameContext;
import neonique.cbcplugin_new.core.GameInitContext;
import neonique.cbcplugin_new.lobby.Lobby;
import neonique.cbcplugin_new.mapconfig.MapRepository;
import neonique.cbcplugin_new.scoreboard.CBCScoreboardManager;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.World;
import org.bukkit.plugin.Plugin;

import java.util.UUID;

public class SessionManager {

    private final Plugin plugin;
    private final UUID worldUUID;
    private final CBCScoreboardManager scoreboardManager;
    private final MapRepository mapRepository;

    private SessionState state;
    private Lobby lobby;
    private Game<?> currentGame;

    public SessionManager (Plugin plugin,
                           World world,
                           CBCScoreboardManager scoreboardManager,
                           MapRepository mapRepository) {

        this.plugin = plugin;
        this.worldUUID = world.getUID();
        this.scoreboardManager = scoreboardManager;
        this.mapRepository = mapRepository;

    }

    public void toLobby () {
        state = SessionState.LOBBY;
    }

    public void startGame (CBCGamemode gamemode, GameContext context) {
        state = SessionState.IN_GAME;

        Game<?> game = gamemode.newGameInstance(new GameInitContext(plugin, scoreboardManager, world()));
        game.setupGame(context);
        currentGame = game;
    }

    public void stopGame () {
        currentGame.resetGame();
        toLobby();
    }

    public World world () {
        return Bukkit.getWorld(worldUUID);
    }



}
