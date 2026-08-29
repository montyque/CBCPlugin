package neonique.cbcplugin_new.session;

import neonique.cbcplugin_new.core.CBCGamemode;
import neonique.cbcplugin_new.core.Game;
import neonique.cbcplugin_new.core.GameContext;
import neonique.cbcplugin_new.core.GameInitContext;
import neonique.cbcplugin_new.lobby.Lobby;
import neonique.cbcplugin_new.practice.PracticeManager;
import neonique.cbcplugin_new.scoreboard.CBCScoreboardManager;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class Session {

    private final Plugin plugin;
    private final World world;
    private final CBCScoreboardManager scoreboardManager;

    private SessionState state = SessionState.INACTIVE;
    private final Lobby lobby;
    private final PracticeManager practiceManager;
    private Game<?> currentGame;

    public Session (Plugin plugin, World world, CBCScoreboardManager scoreboardManager, Lobby lobby, PracticeManager practiceManager) {
        this.plugin = plugin;
        this.world = world;
        this.scoreboardManager = scoreboardManager;
        this.lobby = lobby;
        this.practiceManager = practiceManager;
    }

    public void activateLobby () {

        stateSwitch(SessionState.LOBBY);
        lobby.activate(this::startGame);

        for (Player p : world.getPlayers()) {
            lobby.newPlayer(p);
        }

        for (Player p : Bukkit.getOnlinePlayers()) {
            p.updateCommands();
        }

    }

    public void deactivateLobby () {
        stateSwitch(SessionState.INACTIVE);
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.updateCommands();
        }
    }

    public void startGame (CBCGamemode gamemode, GameContext ctx) {

        if (state == SessionState.IN_GAME) throw new IllegalStateException("A game is already active in this session");

        if (practiceManager.instanceActive()) practiceManager.endInstance();
        stateSwitch(SessionState.IN_GAME);

        currentGame = gamemode.newGameInstance(new GameInitContext(plugin, scoreboardManager, world));
        currentGame.setupGame(ctx);

        for (Player p : Bukkit.getOnlinePlayers()) {
            p.updateCommands();
        }

    }

    public void stopGame () {

        if (state != SessionState.IN_GAME) throw new IllegalStateException("There is no game active in this session");
        activateLobby();

        for (Player p : Bukkit.getOnlinePlayers()) {
            p.updateCommands();
        }

    }

    public void stateSwitch (SessionState newState) {

        if (state == SessionState.LOBBY) {
            lobby.deactivate();
        } else if (state == SessionState.IN_GAME) {
            currentGame.resetGame();
        }

        this.state = newState;

    }

    public SessionState state () {
        return state;
    }

}

