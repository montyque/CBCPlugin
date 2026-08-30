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
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.Plugin;

public class Session implements Listener {

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

        // Register joining event
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
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
        for (Player p : world.getPlayers()) {
            if (!currentGame.hasPlayer(p)) currentGame.addSpectator(p);
        }

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

    /**
     * This method is responsible for when a player not registered in either a lobby/game joins the server.
     * If a player joins and is already registered in a lobby/game, their onPlayerJoin methods handle those.
     */
    @EventHandler
    public void onPlayerJoin (PlayerJoinEvent e) {

        Player entity = e.getPlayer();
        if (state == SessionState.LOBBY) {
            if (lobby.getPlayer(entity) == null) {
                lobby.newPlayer(entity);
            }
        } else if (state == SessionState.IN_GAME) {
            if (!currentGame.hasPlayer(entity) && !currentGame.hasSpectator(entity)) {
                currentGame.addSpectator(entity);
            }
        }


    }

    public SessionState state () {
        return state;
    }

}

